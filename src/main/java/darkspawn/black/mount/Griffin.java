package darkspawn.black.mount;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Summon-only movement prototype. Taming and the companion lifecycle follow separately. */
public final class Griffin extends PathfinderMob {
	public static final int FORWARD = 1, BACK = 2, LEFT = 4, RIGHT = 8, ASCEND = 16, DESCEND = 32;
	private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(Griffin.class, EntityDataSerializers.BOOLEAN);
	private int buttons;
	private long lastControls = Long.MIN_VALUE;

	public Griffin(EntityType<? extends Griffin> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}
	public static AttributeSupplier.Builder attributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, .25)
			.add(Attributes.STEP_HEIGHT, 1);
	}
	@Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FLYING, false);
	}
	public boolean flying() { return entityData.get(FLYING); }
	// One server simulation drives both the rider and observers; clients send buttons, never positions.
	@Override public boolean isClientAuthoritative() { return false; }
	@Override public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player player ? player : null;
	}
	@Override protected boolean canAddPassenger(Entity passenger) {
		return passenger instanceof Player && getPassengers().isEmpty() && isAlive();
	}
	@Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.isSecondaryUseActive() || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
		if (!isAlive() || isVehicle() || player.isPassenger()) return InteractionResult.PASS;
		if (!riderFits(player, Vec3.ZERO)) {
			if (level() instanceof ServerLevel) player.sendOverlayMessage(Component.translatable("message.darkspawn.griffin_no_room"));
			return InteractionResult.FAIL;
		}
		if (level() instanceof ServerLevel && player.startRiding(this)) {
			clearControls();
			player.sendOverlayMessage(Component.translatable("message.darkspawn.griffin_mounted"));
		}
		return InteractionResult.SUCCESS;
	}
	public boolean acceptControls(Player sender, int input) {
		if (!(level() instanceof ServerLevel) || !isAlive() || getControllingPassenger() != sender
				|| input < 0 || (input & ~63) != 0) return false;
		buttons = input;
		lastControls = level().getGameTime();
		return true;
	}
	private void clearControls() { buttons = 0; lastControls = Long.MIN_VALUE; }
	private boolean freshControls() {
		return lastControls != Long.MIN_VALUE && level().getGameTime() - lastControls <= 10;
	}
	public static Vec3 inputVector(int buttons) {
		int x = ((buttons & LEFT) != 0 ? 1 : 0) - ((buttons & RIGHT) != 0 ? 1 : 0);
		int z = ((buttons & FORWARD) != 0 ? 1 : 0) - ((buttons & BACK) != 0 ? 1 : 0);
		int y = ((buttons & ASCEND) != 0 ? 1 : 0) - ((buttons & DESCEND) != 0 ? 1 : 0);
		Vec3 horizontal = new Vec3(x, 0, z);
		if (horizontal.lengthSqr() > 1) horizontal = horizontal.normalize();
		return new Vec3(horizontal.x, y, horizontal.z);
	}
	@Override protected Vec3 getRiddenInput(Player player, Vec3 ignored) {
		return freshControls() ? inputVector(buttons) : new Vec3(0, flying() ? -1 : 0, 0);
	}
	@Override protected void tickRidden(Player player, Vec3 input) {
		super.tickRidden(player, input);
		setYRot(player.getYRot());
		setXRot(0);
		yBodyRot = getYRot();
		yHeadRot = getYRot();
		getNavigation().stop();
	}
	@Override protected float getRiddenSpeed(Player player) { return .25F; }
	private boolean riderFits(LivingEntity rider, Vec3 offset) {
		return level().noCollision(rider, rider.getDimensions(rider.getPose())
			.makeBoundingBox(getPassengerRidingPosition(rider).add(offset)));
	}
	@Override public void travel(Vec3 input) {
		if (!(level() instanceof ServerLevel)) return;
		boolean ridden = getControllingPassenger() != null;
		if (!ridden) input = new Vec3(0, -.4, 0);
		boolean takeoff = ridden && input.y > 0 && !isInWater() && !isInLava();
		if (takeoff || !onGround() && !isInWater() && !isInLava()) entityData.set(FLYING, true);
		if (flying() && (onGround() && input.y <= 0 || isInWater() || isInLava())) entityData.set(FLYING, false);
		setNoGravity(flying());
		if (!flying()) {
			Vec3 before = position();
			super.travel(new Vec3(input.x, 0, input.z));
			if (ridden && !riderFits(getControllingPassenger(), Vec3.ZERO)) {
				setPos(before);
				setDeltaMovement(Vec3.ZERO);
			}
			return;
		}
		resetFallDistance();
		Vec3 target = new Vec3(input.x * .6, input.y * .32, input.z * .6)
			.yRot(-getYRot() * (float) (Math.PI / 180));
		Vec3 velocity = getDeltaMovement().scale(.65).add(target.scale(.35));
		if (getY() >= level().getMaxY() - 3 && velocity.y > 0) velocity = new Vec3(velocity.x, 0, velocity.z);
		// The rider's head must fit too, even though it is above the griffin's own collision box.
		if (ridden && !riderFits(getControllingPassenger(), velocity)) {
			velocity = new Vec3(0, Math.min(0, velocity.y), 0);
			if (!riderFits(getControllingPassenger(), velocity)) velocity = Vec3.ZERO;
		}
		setDeltaMovement(velocity);
		move(MoverType.SELF, velocity);
		if (onGround() && input.y <= 0) {
			entityData.set(FLYING, false);
			setNoGravity(false);
			setDeltaMovement(Vec3.ZERO);
		}
	}
	@Override protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		clearControls();
	}
	@Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }
	@Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		// Find actual floor with enough room for the player, including slopes and low ceilings.
		BlockPos origin = blockPosition();
		for (int radius = 1; radius <= 2; radius++) {
			for (int dy = 1; dy >= -2; dy--) {
				for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
					Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), origin.offset(dx, dy, dz), true);
					if (safe != null && !passenger.getDimensions(passenger.getPose()).makeBoundingBox(safe).intersects(getBoundingBox())) return safe;
				}
			}
		}
		// Emergency midair dismount: enough slow falling for descent from normal world heights.
		if (level() instanceof ServerLevel) {
			passenger.resetFallDistance();
			passenger.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 6000, 0, false, true));
		}
		return super.getDismountLocationForPassenger(passenger);
	}
	@Override protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("GriffinFlying", flying());
	}
	@Override protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(FLYING, input.getBooleanOr("GriffinFlying", false));
		setNoGravity(flying());
		clearControls();
	}
}
