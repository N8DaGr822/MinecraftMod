package darkspawn.black.mount;

import darkspawn.black.ecosystem.RegionalKind;
import darkspawn.black.ecosystem.RegionalMob;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Per-mount combat state; only the server decides targets, damage and cooldowns. */
final class MountCombat {
    private final PrototypeMount mount;
    private long nextUse;
    private int chargeTicks;
    private Vec3 previousPosition;
    private final Set<UUID> chargeHits = new HashSet<>();

    MountCombat(PrototypeMount mount) { this.mount = mount; }
    boolean supported() {
        return mount instanceof DragonMount || mount instanceof WoollyRhino
            || mount instanceof GiantScorpion || mount instanceof Crocodile;
    }
    boolean charging() { return chargeTicks > 0; }
    void stop() { chargeTicks = 0; chargeHits.clear(); previousPosition = null; mount.startAttackAnimation(0); }
    void save(ValueOutput output) { output.putLong("MountNextAbility", nextUse); }
    void load(ValueInput input) {
        nextUse = input.getLongOr("MountNextAbility", 0);
        stop();
    }
    void tick(ServerLevel level) {
        if (!supported()) return;
        if (!mount.isAlive() || !(mount.getControllingPassenger() instanceof Player rider)
                || !rider.isAlive() || rider.isSpectator()) { stop(); return; }
        if (charging()) {
            // Charge Cancellation: Releasing forward, stale packets, water or a wall end the burst immediately.
            if (!mount.forwardRequested() || !mount.onGround() || mount.isInWater() || mount.isInLava()
                    || mount.horizontalCollision) { stop(); return; }
            charge(level, rider);
            if (--chargeTicks == 0) stop();
            return;
        }
        if (!mount.abilityRequested() || level.getGameTime() < nextUse) return;
        int cooldown;
        String ability;
        if (mount instanceof WoollyRhino) {
            if (!mount.forwardRequested() || !mount.onGround() || mount.isInWater() || mount.isInLava()) return;
            chargeTicks = 20;
            previousPosition = mount.position();
            chargeHits.clear();
            cooldown = 100;
            ability = "charge";
            mount.startAttackAnimation(20);
            mount.playSound(SoundEvents.RAVAGER_ROAR, .6F, 1.2F);
        } else if (mount instanceof DragonMount) {
            mount.startAttackAnimation(12);
            breath(level, rider);
            cooldown = 80;
            ability = "breath";
        } else {
            mount.startAttackAnimation(8);
            melee(level, rider);
            cooldown = mount instanceof GiantScorpion ? 60 : 40;
            ability = mount instanceof GiantScorpion ? "sting" : "bite";
        }
        nextUse = level.getGameTime() + cooldown;
        rider.sendOverlayMessage(Component.translatable("message.darkspawn.mount_ability_used",
            Component.translatable("ability.darkspawn." + ability), cooldown / 20));
    }
    private boolean hostile(LivingEntity target, Player rider) {
        if (!target.isAlive() || target.isSpectator() || target == mount || target == rider
                || target instanceof Player || rider.isAlliedTo(target) || target.isAlliedTo(rider)
                || mount.isAlliedTo(target) || target.isPassengerOfSameVehicle(mount)) return false;
        if (target instanceof OwnableEntity owned && owned.getOwnerReference() != null) return false;
        // Regional Wildlife: Monster inheritance alone includes peaceful and neutral Darkspawn creatures.
        if (target instanceof RegionalMob regional) return regional.kind().temper == RegionalKind.Temper.HOSTILE;
        if (!(target instanceof Enemy) || target.getType().getCategory() != MobCategory.MONSTER) return false;
        if (target instanceof NeutralMob && target instanceof Mob mob)
            return mob.getTarget() == rider || mob.getTarget() == mount;
        return true;
    }
    private boolean visible(Vec3 origin, Vec3 end) {
        return mount.level().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, mount)).getType() == HitResult.Type.MISS;
    }
    private boolean inCone(LivingEntity target, Vec3 origin, Vec3 direction, double range, double cosine) {
        Vec3 delta = target.getBoundingBox().getCenter().subtract(origin);
        return delta.lengthSqr() <= range * range && delta.normalize().dot(direction) >= cosine
            && visible(origin, target.getBoundingBox().getCenter());
    }
    private void breath(ServerLevel level, Player rider) {
        Vec3 origin = mount.getEyePosition();
        Vec3 direction = rider.getLookAngle();
        var source = mount.damageSources().source(DamageTypes.FIREBALL, mount, rider);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, mount.getBoundingBox().inflate(8),
                target -> hostile(target, rider) && inCone(target, origin, direction, 8, .85))) {
            if (target.hurtServer(level, source, 8)) target.igniteForSeconds(3);
        }
        // Breath Presentation: Ray-clipped particles, with no fire placement or explosion entity.
        for (double distance = .5; distance <= 8; distance += .5) {
            Vec3 point = origin.add(direction.scale(distance));
            if (!visible(origin, point)) break;
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 5, .12, .12, .12, .015);
        }
        mount.playSound(SoundEvents.ENDER_DRAGON_SHOOT, .6F, 1F);
    }
    private void melee(ServerLevel level, Player rider) {
        Vec3 origin = mount.getBoundingBox().getCenter();
        Vec3 direction = Vec3.directionFromRotation(0, rider.getYRot());
        var target = level.getEntitiesOfClass(LivingEntity.class, mount.getBoundingBox().inflate(3.5),
                candidate -> hostile(candidate, rider) && inCone(candidate, origin, direction, 3.5, .5))
            .stream().min(Comparator.comparingDouble(mount::distanceToSqr)).orElse(null);
        boolean sting = mount instanceof GiantScorpion;
        Vec3 point = origin.add(direction.scale(1.4));
        level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 8, .3, .2, .3, .1);
        mount.playSound(sting ? SoundEvents.SPIDER_HURT : SoundEvents.PLAYER_ATTACK_STRONG, .6F, .8F);
        if (target == null) return;
        var source = mount.damageSources().source(DamageTypes.MOB_ATTACK, mount, rider);
        if (target.hurtServer(level, source, sting ? 4 : 8)) {
            target.addEffect(new MobEffectInstance(sting ? MobEffects.POISON : MobEffects.SLOWNESS,
                sting ? 100 : 40, 0), rider);
        }
    }
    private void charge(ServerLevel level, Player rider) {
        Vec3 movement = mount.position().subtract(previousPosition);
        Vec3 previous = previousPosition;
        previousPosition = mount.position();
        // Charge Hits: Actual movement is required, and every target can be hit only once per burst.
        if (movement.horizontalDistanceSqr() < .0004 || movement.lengthSqr() > 16) return;
        var bounds = mount.getBoundingBox().expandTowards(previous.subtract(mount.position())).inflate(.35, .1, .35);
        var source = mount.damageSources().source(DamageTypes.MOB_ATTACK, mount, rider);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> hostile(target, rider) && !chargeHits.contains(target.getUUID())
                    && visible(mount.getEyePosition(), target.getBoundingBox().getCenter()))) {
            chargeHits.add(target.getUUID());
            if (target.hurtServer(level, source, 10))
                target.knockback(1.2, -movement.x, -movement.z, source, 10);
        }
        level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + .15, mount.getZ(), 3, .35, .05, .35, .02);
    }
}
