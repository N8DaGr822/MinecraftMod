package darkspawn.black.mount;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import darkspawn.black.legacy.NoahLegacy;
import darkspawn.black.legacy.NoahLedger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Noah's unique companion uses the existing server-authoritative aquatic riding controls. */
public final class Thalassar extends PrototypeMount implements GeoEntity {
    public static final DataTicket<Boolean> SWIMMING = DataTicket.create("noah_swimming", Boolean.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int dashTicks;
    public Thalassar(EntityType<? extends Thalassar> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() { return PrototypeMount.attributes().add(Attributes.MAX_HEALTH,80).add(Attributes.MOVEMENT_SPEED,.22); }
    public void bond(Player owner) { assignOwner(owner.getUUID()); }
    @Override protected boolean canFly() { return false; }
    @Override protected boolean canSwim() { return true; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected float groundSpeed() { return .22F; }
    @Override protected Vec3 unoccupiedInput() { return Vec3.ZERO; }
    @Override protected double swimmingSpeed() { return (dashTicks>0?1.05:.55)*(getControllingPassenger() instanceof ServerPlayer p&&NoahLegacy.dominion(p)?1.25:1); }
    @Override protected String mountingMessage() { return "message.darkspawn.thalassar_controls"; }
    @Override public Item tamingFood() { return Items.KELP; }
    @Override protected SoundEvent getAmbientSound() { return isInWater()?SoundEvents.DOLPHIN_AMBIENT_WATER:SoundEvents.HORSE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return isInWater()?SoundEvents.DOLPHIN_HURT:SoundEvents.HORSE_HURT; }
    @Override protected SoundEvent getDeathSound() { return isInWater()?SoundEvents.DOLPHIN_DEATH:SoundEvents.HORSE_DEATH; }
    @Override public boolean causeFallDamage(double distance,float multiplier,DamageSource source) { return false; }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        // Unique Bond: Only the conch creates ownership; ordinary food taming bypasses its recovery record.
        if(!isTamed()||player.isSecondaryUseActive())return InteractionResult.PASS;
        return super.mobInteract(player,hand);
    }
    @Override public void tick() {
        super.tick();
        if(!(level() instanceof ServerLevel level)||!isTamed()||isRemoved())return;
        NoahLegacy.track(this);
        if(getControllingPassenger() instanceof ServerPlayer rider)NoahLegacy.award(rider,"ride");
        if(dashTicks>0)dashTicks--;
        if(getControllingPassenger() instanceof ServerPlayer player&&abilityRequested()&&isInWater()) {
            var ledger=NoahLedger.get(level);var data=ledger.entry(player.getUUID());long now=NoahLedger.time(level);
            if(now>=data.getLongOr("DashReady",0)) {
                dashTicks=15;data.putLong("DashReady",now+100);ledger.setDirty();
                triggerAnim("action","dash");NoahLegacy.guardianSurge(player,this);
            }
        }
    }
    @Override public void die(DamageSource source) {
        if(level() instanceof ServerLevel&&isTamed()) {
            // Recovery Ownership: Invalid/command-created duplicates must never overwrite the real companion.
            if(!isRemoved()&&!NoahLegacy.rest(this,true))discard();
        }
        else super.die(source);
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Thalassar>("movement",4,test -> test.setAndContinue(RawAnimation.begin().thenLoop(
            "animation.thalassar."+(test.getDataOrDefault(SWIMMING,false)?"swim":test.isMoving()?"walk":"idle")))));
        controllers.add(new AnimationController<Thalassar>("action",2,test -> PlayState.STOP)
            .triggerableAnim("dash",RawAnimation.begin().thenPlay("animation.thalassar.dash"))
            .triggerableAnim("summon",RawAnimation.begin().thenPlay("animation.thalassar.summon")));
    }
}
