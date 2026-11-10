package darkspawn.black.mount;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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

/** Tamed, owner-ridden mount with persistent companion care. */
public abstract class PrototypeMount extends PathfinderMob implements OwnableEntity {
	public static final int FORWARD = 1, BACK = 2, LEFT = 4, RIGHT = 8, ASCEND = 16, DESCEND = 32, DIG = 64, ABILITY = 128;
	final MountCombat combat = new MountCombat(this);
	public final MountCare care = new MountCare(this);
	private static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(PrototypeMount.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(PrototypeMount.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> ATTACK_TICKS = SynchedEntityData.defineId(PrototypeMount.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> OWNER = SynchedEntityData.defineId(PrototypeMount.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
	private UUID trustingPlayer;
	private int trust;
	private long nextFeeding, trustExpires;
	private int buttons;
	private long lastControls = Long.MIN_VALUE;
	private float flightPose, previousFlightPose;
	private boolean wildMount;

	protected PrototypeMount(EntityType<? extends PrototypeMount> type, Level level) {
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
		builder.define(BABY, false);
		builder.define(ATTACK_TICKS, 0);
		builder.define(OWNER, Optional.empty());
	}
	@Override public EntityReference<LivingEntity> getOwnerReference() { return entityData.get(OWNER).orElse(null); }
	public boolean isTamed() { return getOwnerReference() != null; }
	public boolean isOwnedBy(Player player) { return isTamed() && getOwnerReference().matches(player); }
	void assignOwner(UUID owner) {
		entityData.set(OWNER, Optional.of(EntityReference.of(owner)));
		wildMount = false; trustingPlayer = null; trust = 0;
		clearControls(); combat.stop();
	}
	@Override public boolean isBaby() { return entityData.get(BABY); }
	void setBabyMount(boolean baby) { entityData.set(BABY, baby); }
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (BABY.equals(accessor)) refreshDimensions();
	}
	@Override public void die(DamageSource source) {
		// Veterinary Recovery: Owned companions leave one saved record instead of death loot.
		if (level() instanceof ServerLevel && isTamed() && !isRemoved()) MountCompanions.defeated(this);
		else super.die(source);
	}
	public Item tamingFood() {
		if (this instanceof WoollyRhino) return Items.WHEAT;
		if (this instanceof FlyingSquirrel) return Items.SWEET_BERRIES;
		if (this instanceof PoptartCat) return Items.COOKIE;
		if (this instanceof DragonMount) return Items.COOKED_BEEF;
		if (this instanceof WingedUnicorn) return Items.GOLDEN_APPLE;
		if (this instanceof RidingGolem) return Items.IRON_INGOT;
		if (this instanceof Sasquatch) return Items.APPLE;
		if (this instanceof Cerberus) return Items.BONE;
		if (this instanceof Leopard) return Items.CHICKEN;
		if (this instanceof Seahorse) return Items.KELP;
		if (this instanceof GiantScorpion) return Items.SPIDER_EYE;
		if (this instanceof Crocodile) return Items.COD;
		if (this instanceof GiantMole) return Items.BEETROOT;
		return Items.RABBIT;
	}
	private InteractionResult feed(Player player, InteractionHand hand, ServerLevel level) {
		long now = level.getGameTime();
		if (now < nextFeeding) {
			player.sendOverlayMessage(Component.translatable("message.darkspawn.mount_feed_wait"));
			return InteractionResult.SUCCESS;
		}
		if (isTamed()) {
			if (getHealth() >= getMaxHealth()) {
				player.sendOverlayMessage(Component.translatable("message.darkspawn.mount_full_health"));
				return InteractionResult.SUCCESS;
			}
			heal(4);
		} else {
			// Mount Trust: Reserve an active attempt for one feeder; another player cannot take its final feeding.
			if (trustingPlayer != null && !trustingPlayer.equals(player.getUUID()) && now < trustExpires) {
				player.sendOverlayMessage(Component.translatable("message.darkspawn.mount_trusting_other"));
				return InteractionResult.SUCCESS;
			}
			if (!player.getUUID().equals(trustingPlayer)) trust = 0;
			trustingPlayer = player.getUUID();
			trustExpires = now + 1200;
			if (++trust >= 3) {
				entityData.set(OWNER, Optional.of(EntityReference.of((LivingEntity) player)));
				wildMount = false;
				trustingPlayer = null;
				trust = 0;
				player.sendOverlayMessage(Component.translatable("message.darkspawn.mount_tamed"));
			} else player.sendOverlayMessage(Component.translatable("message.darkspawn.mount_trust", trust, 3));
		}
		player.getItemInHand(hand).consume(1, player);
		nextFeeding = now + 40;
		level.sendParticles(isTamed() ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER,
			getX(), getY() + getBbHeight(), getZ(), 6, .35, .2, .35, 0);
		return InteractionResult.SUCCESS;
	}
	protected String mountingMessage() { return "message.darkspawn.griffin_mounted"; }
	protected boolean canFly() { return true; }
	protected boolean canGlide() { return false; }
	protected boolean canSwim() { return false; }
	protected double swimmingSpeed() { return .3; }
	protected float groundSpeed() { return .25F; }
	// Mount Voices: Native animal/material cues respect resource packs and avoid another overlapping music track.
	private SoundEvent voice(int cue) {
		if (this instanceof Leopard) return switch (cue) {
			case 1 -> SoundEvents.OCELOT_HURT; case 2 -> SoundEvents.OCELOT_DEATH; default -> SoundEvents.OCELOT_AMBIENT;
		};
		if (this instanceof Seahorse) return switch (cue) {
			case 1 -> SoundEvents.TROPICAL_FISH_HURT; case 2 -> SoundEvents.TROPICAL_FISH_DEATH; default -> SoundEvents.TROPICAL_FISH_AMBIENT;
		};
		if (this instanceof GiantScorpion) return switch (cue) {
			case 1 -> SoundEvents.SPIDER_HURT; case 2 -> SoundEvents.SPIDER_DEATH; default -> SoundEvents.SPIDER_AMBIENT;
		};
		if (this instanceof Crocodile) return switch (cue) {
			case 1 -> SoundEvents.TURTLE_HURT; case 2 -> SoundEvents.TURTLE_DEATH; default -> SoundEvents.TURTLE_AMBIENT_LAND;
		};
		if (this instanceof PoptartCat) return switch (cue) {
			case 1 -> SoundEvents.CAT_HURT_BABY.value(); case 2 -> SoundEvents.CAT_DEATH_BABY.value(); default -> SoundEvents.CAT_PURR_BABY.value();
		};
		if (this instanceof WingedUnicorn) return switch (cue) {
			case 1 -> SoundEvents.HORSE_HURT; case 2 -> SoundEvents.HORSE_DEATH; default -> SoundEvents.HORSE_AMBIENT;
		};
		if (this instanceof RidingGolem) return switch (cue) {
			case 1 -> SoundEvents.IRON_GOLEM_HURT; case 2 -> SoundEvents.IRON_GOLEM_DEATH; default -> SoundEvents.IRON_GOLEM_REPAIR;
		};
		if (this instanceof DragonMount) return cue == 0 ? SoundEvents.ENDER_DRAGON_GROWL : SoundEvents.ENDER_DRAGON_HURT;
		if (this instanceof Sasquatch) return switch (cue) {
			case 1 -> SoundEvents.POLAR_BEAR_HURT; case 2 -> SoundEvents.POLAR_BEAR_DEATH; default -> SoundEvents.POLAR_BEAR_AMBIENT;
		};
		if (this instanceof WoollyRhino || this instanceof Cerberus) return switch (cue) {
			case 1 -> SoundEvents.RAVAGER_HURT; case 2 -> SoundEvents.RAVAGER_DEATH; default -> SoundEvents.RAVAGER_AMBIENT;
		};
		return switch (cue) {
			case 1 -> SoundEvents.FOX_HURT; case 2 -> SoundEvents.FOX_DEATH; default -> SoundEvents.FOX_AMBIENT;
		};
	}
	@Override protected SoundEvent getAmbientSound() { return voice(0); }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return voice(1); }
	@Override protected SoundEvent getDeathSound() { return voice(2); }
	@Override protected float getSoundVolume() { return this instanceof DragonMount ? .25F : .45F; }
	@Override public int getAmbientSoundInterval() { return 300; }
	public boolean flying() { return entityData.get(FLYING); }
	// Flight Presentation: Blend on entity ticks, never on render calls or shared model instances.
	@Override public void tick() {
		super.tick();
		// Save Migration: A legacy rider cannot bypass newly required ownership after a reload.
		if (level() instanceof ServerLevel && getFirstPassenger() instanceof Player player && !isOwnedBy(player)) player.stopRiding();
		if (level() instanceof ServerLevel server) {
			if (entityData.get(ATTACK_TICKS) > 0) entityData.set(ATTACK_TICKS, entityData.get(ATTACK_TICKS) - 1);
			combat.tick(server);
			care.tick(server);
		}
		previousFlightPose = flightPose;
		flightPose = Math.clamp(flightPose + (flying() ? .125F : -.125F), 0F, 1F);
	}
	public float flightPose(float partialTick) { return net.minecraft.util.Mth.lerp(partialTick, previousFlightPose, flightPose); }
	// Attack Presentation: Synced remaining ticks let riders and newly tracking observers see the same recovery.
	void startAttackAnimation(int ticks) { entityData.set(ATTACK_TICKS, ticks); }
	public float attackPose(float partialTick) {
		float remaining = Math.max(0, entityData.get(ATTACK_TICKS) - partialTick);
		float amount = Math.clamp(remaining / (this instanceof WoollyRhino ? 3F : this instanceof DragonMount ? 12F : 8F), 0F, 1F);
		return amount * amount * (3F - 2F * amount);
	}
	protected Vec3 unoccupiedInput() { return new Vec3(0, -.4, 0); }
	@Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
			net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason reason,
			net.minecraft.world.entity.SpawnGroupData groupData) {
		wildMount = reason == net.minecraft.world.entity.EntitySpawnReason.NATURAL || reason == net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION;
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}
	// Wild Mounts: Taming preserves encounters; incomplete trust alone does not bypass population caps.
	@Override public boolean isPersistenceRequired() { return isTamed() || !wildMount || hasCustomName(); }
	@Override public int getMaxSpawnClusterSize() { return 1; }
	@Override protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (passenger instanceof Player) wildMount = false;
	}
	// One server simulation drives both the rider and observers; clients send buttons, never positions.
	@Override public boolean isClientAuthoritative() { return false; }
	// Mount Authority: The rider must also accept server positions instead of sending vehicle positions back.
	@Override protected boolean isLocalClientAuthoritative() { return false; }
	@Override public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player player && isOwnedBy(player) ? player : null;
	}
	@Override protected boolean canAddPassenger(Entity passenger) {
		return passenger instanceof Player player && isOwnedBy(player) && getPassengers().isEmpty() && isAlive()
			&& !isBaby() && !care.breeding() && !care.offerPending();
	}
	@Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!isAlive() || player.isSpectator()) return InteractionResult.PASS;
		var held = player.getItemInHand(hand);
		if (!held.isEmpty() && !held.is(tamingFood())) return InteractionResult.PASS;
		if (held.isEmpty() && player.isSecondaryUseActive() && (isOwnedBy(player) || care.offeredTo(player))) {
			if (player instanceof net.minecraft.server.level.ServerPlayer server && care.accessible(player) && !isVehicle())
				server.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, viewer) -> new MountMenu(id, inventory, this), getDisplayName()));
			return InteractionResult.SUCCESS;
		}
		if (isTamed() && !isOwnedBy(player)) {
			if (level() instanceof ServerLevel) player.sendOverlayMessage(Component.translatable("message.darkspawn.mount_other_owner"));
			return InteractionResult.FAIL;
		}
		if (held.is(tamingFood())) return level() instanceof ServerLevel server ? feed(player, hand, server) : InteractionResult.SUCCESS;
		if (!isTamed()) {
			if (level() instanceof ServerLevel) player.sendOverlayMessage(Component.translatable("message.darkspawn.mount_needs_taming", tamingFood().getDefaultInstance().getHoverName()));
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) return InteractionResult.PASS;
		if (isBaby() || care.breeding() || care.offerPending()) {
			if (level() instanceof ServerLevel) MountCompanions.message(player, "mount_unavailable");
			return InteractionResult.SUCCESS;
		}
		if (!isAlive() || isVehicle() || player.isPassenger()) return InteractionResult.PASS;
		if (!riderFits(player, Vec3.ZERO)) {
			if (level() instanceof ServerLevel) player.sendOverlayMessage(Component.translatable("message.darkspawn.griffin_no_room"));
			return InteractionResult.FAIL;
		}
		if (level() instanceof ServerLevel && player.startRiding(this)) {
			clearControls();
			var message = Component.translatable(mountingMessage());
			if (combat.supported()) message.append(" ").append(Component.translatable("message.darkspawn.mount_ability_hint"));
			player.sendOverlayMessage(message);
		}
		return InteractionResult.SUCCESS;
	}
	public boolean acceptControls(Player sender, int input) {
		if (!(level() instanceof ServerLevel) || !isAlive() || getControllingPassenger() != sender
				|| input < 0 || (input & ~255) != 0) return false;
		buttons = input;
		lastControls = level().getGameTime();
		return true;
	}
	private void clearControls() { buttons = 0; lastControls = Long.MIN_VALUE; }
	private boolean freshControls() {
		return lastControls != Long.MIN_VALUE && level().getGameTime() - lastControls <= 10;
	}
	protected boolean diggingRequested() { return freshControls() && (buttons & DIG) != 0; }
	boolean abilityRequested() { return freshControls() && (buttons & ABILITY) != 0; }
	boolean forwardRequested() { return freshControls() && (buttons & FORWARD) != 0 && (buttons & BACK) == 0; }
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
	@Override protected float getRiddenSpeed(Player player) { return groundSpeed(); }
	private boolean riderFits(LivingEntity rider, Vec3 offset) {
		return level().noCollision(rider, rider.getDimensions(rider.getPose())
			.makeBoundingBox(getPassengerRidingPosition(rider).add(offset)));
	}
	@Override public void travel(Vec3 input) {
		if (!(level() instanceof ServerLevel)) return;
		boolean ridden = getControllingPassenger() != null;
		// Aquatic Mounts: Space/X control depth without enabling flight or accepting client positions.
		if (canSwim() && isInWater()) {
			entityData.set(FLYING, false);
			setNoGravity(true);
			resetFallDistance();
			if (!ridden) input = care.followInput();
			Vec3 target = new Vec3(input.x, input.y, input.z).scale(swimmingSpeed())
				.yRot(-getYRot() * (float) (Math.PI / 180));
			Vec3 velocity = getDeltaMovement().scale(.65).add(target.scale(.35));
			if (ridden && !riderFits(getControllingPassenger(), velocity)) velocity = Vec3.ZERO;
			setDeltaMovement(velocity);
			move(MoverType.SELF, velocity);
			return;
		}
		// Companion Movement: Ground navigation supplies input; airborne followers use local steering.
		if (!ridden) input = canFly() && care.following ? care.followInput()
			: care.following && !canFly() ? input : unoccupiedInput();
		boolean takeoff = canFly() && (ridden || care.following) && input.y > 0 && !isInWater() && !isInLava();
		if (takeoff || (canFly() || canGlide()) && !onGround() && !isInWater() && !isInLava()) entityData.set(FLYING, true);
		if (flying() && (onGround() && (input.y <= 0 || !canFly()) || isInWater() || isInLava())) entityData.set(FLYING, false);
		setNoGravity(flying());
		if (!flying()) {
			Vec3 before = position();
			if (ridden && input.y > 0 && onGround() && !isInWater() && !isInLava()) jumpFromGround();
			super.travel(new Vec3(input.x, 0, input.z));
			if (ridden && !riderFits(getControllingPassenger(), Vec3.ZERO)) {
				setPos(before);
				setDeltaMovement(Vec3.ZERO);
			}
			return;
		}
		resetFallDistance();
		double descent = canGlide() ? (input.y < 0 ? -.45 : -.10) : input.y * .32;
		Vec3 target = new Vec3(input.x * .6, descent, input.z * .6)
			.yRot(-getYRot() * (float) (Math.PI / 180));
		Vec3 velocity = getDeltaMovement().scale(.65).add(target.scale(.35));
		if (canGlide()) velocity = new Vec3(velocity.x, Math.min(-.08, velocity.y), velocity.z);
		if (getY() >= level().getMaxY() - 3 && velocity.y > 0) velocity = new Vec3(velocity.x, 0, velocity.z);
		// The rider's head must fit too, even when it is above the mount's collision box.
		if (ridden && !riderFits(getControllingPassenger(), velocity)) {
			velocity = new Vec3(0, Math.min(0, velocity.y), 0);
			if (!riderFits(getControllingPassenger(), velocity)) velocity = Vec3.ZERO;
		}
		setDeltaMovement(velocity);
		move(MoverType.SELF, velocity);
		if (onGround() && (input.y <= 0 || !canFly())) {
			entityData.set(FLYING, false);
			setNoGravity(false);
			setDeltaMovement(Vec3.ZERO);
		}
	}
	@Override protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		clearControls();
		combat.stop();
	}
	@Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return canFly() || canGlide() ? false : super.causeFallDamage(distance, multiplier, source); }
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
		// Preserve the original griffin checkpoint's saved-data key.
		output.putBoolean("GriffinFlying", flying());
		output.putBoolean("WildMount", wildMount);
		EntityReference.store(getOwnerReference(), output, "MountOwner");
		if (trustingPlayer != null) output.store("MountTrustPlayer", UUIDUtil.CODEC, trustingPlayer);
		output.putInt("MountTrust", trust);
		output.putLong("MountNextFeeding", nextFeeding);
		output.putLong("MountTrustExpires", trustExpires);
		combat.save(output);
		care.save(output);
	}
	@Override protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		wildMount = input.getBooleanOr("WildMount", false);
		entityData.set(OWNER, Optional.ofNullable(EntityReference.<LivingEntity>read(input, "MountOwner")));
		trustingPlayer = input.read("MountTrustPlayer", UUIDUtil.CODEC).orElse(null);
		trust = trustingPlayer == null || isTamed() ? 0 : Math.clamp(input.getIntOr("MountTrust", 0), 0, 2);
		nextFeeding = input.getLongOr("MountNextFeeding", 0);
		trustExpires = input.getLongOr("MountTrustExpires", 0);
		combat.load(input);
		care.load(input);
		entityData.set(FLYING, (canFly() || canGlide()) && input.getBooleanOr("GriffinFlying", false));
		setNoGravity(flying());
		clearControls();
	}
}
