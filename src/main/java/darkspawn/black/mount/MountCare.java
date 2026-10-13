package darkspawn.black.mount;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Persistent companion behavior and cargo; transient offers never survive a reload. */
public final class MountCare {
    private final PrototypeMount mount;
    public final SimpleContainer cargo = new SimpleContainer(9);
    public boolean following;
    private int growthTicks;
    private long loveUntil, breedAfter;
    private UUID recipient;
    private long offerUntil;

    MountCare(PrototypeMount mount) { this.mount = mount; }
    public boolean breeding() { return loveUntil > mount.level().getGameTime(); }
    public boolean offeredTo(Player player) { return player.getUUID().equals(recipient) && offerUntil > mount.level().getGameTime(); }
    public boolean offerPending() { return recipient != null && offerUntil > mount.level().getGameTime(); }
    public boolean available() { return mount.isAlive() && !mount.isRemoved() && !mount.isVehicle() && !mount.isPassenger() && !breeding() && !offerPending(); }
    public boolean accessible(Player player) {
        return player.isAlive() && !player.isSpectator() && mount.isAlive() && !mount.isRemoved()
            && player.level() == mount.level() && player.distanceToSqr(mount) <= 64
            && (mount.isOwnedBy(player) || offeredTo(player));
    }
    public void setFollowing(boolean value) {
        following = value; loveUntil = 0;
        mount.getNavigation().stop();
        // Stay Command: Clear the movement controller's last waypoint as well as the path.
        mount.getMoveControl().setWantedPosition(mount.getX(), mount.getY(), mount.getZ(), 0);
        mount.setDeltaMovement(Vec3.ZERO);
    }
    public void makeBaby() { growthTicks = 24000; mount.setBabyMount(true); }
    public void save(ValueOutput output) {
        output.putBoolean("MountFollowing", following);
        output.putInt("MountGrowth", growthTicks);
        output.putLong("MountBreedAfter", breedAfter);
        output.store("MountCargo", ItemContainerContents.CODEC, ItemContainerContents.fromItems(cargo.getItems()));
    }
    public void load(ValueInput input) {
        following = input.getBooleanOr("MountFollowing", false);
        growthTicks = Math.clamp(input.getIntOr("MountGrowth", 0), 0, 24000);
        mount.setBabyMount(growthTicks > 0);
        loveUntil = 0; // Breeding needs two currently present parents, never a stale saved request.
        breedAfter = input.getLongOr("MountBreedAfter", 0);
        recipient = null; offerUntil = 0;
        cargo.clearContent();
        input.read("MountCargo", ItemContainerContents.CODEC).ifPresent(contents -> contents.copyInto(cargo.getItems()));
    }
    public void tick(ServerLevel level) {
        if (growthTicks > 0 && --growthTicks == 0) mount.setBabyMount(false);
        if (!mount.isTamed() || !mount.isAlive()) return;
        if (mount.isVehicle()) { mount.getNavigation().stop(); loveUntil = 0; return; }
        Player owner = level.getPlayerByUUID(mount.getOwnerReference().getUUID());
        if (following && owner != null && owner.isAlive() && !owner.isSpectator() && !owner.isPassenger()
                && !mount.isLeashed() && mount.distanceToSqr(owner) > 9 && mount.distanceToSqr(owner) < 1024) {
            if (!mount.canFly() && !(mount.canSwim() && mount.isInWater()) && mount.tickCount % 10 == 0)
                mount.getNavigation().moveTo(owner, 1.15);
        } else mount.getNavigation().stop();
        if (breeding() && mount.tickCount % 10 == 0 && readyParent()) {
            var mate = level.getEntitiesOfClass(PrototypeMount.class, mount.getBoundingBox().inflate(4),
                other -> other != mount && other.getType() == mount.getType() && other.care.breeding()
                    && other.care.readyParent() && other.getOwnerReference() != null
                    && other.getOwnerReference().getUUID().equals(mount.getOwnerReference().getUUID()))
                .stream().min(Comparator.comparingDouble(mount::distanceToSqr)).orElse(null);
            if (mate != null) breed(level, mate);
        }
    }
    Vec3 followInput() {
        if (!following || mount.isLeashed() || !(mount.level() instanceof ServerLevel level) || !mount.isTamed()) return Vec3.ZERO;
        var owner = level.getPlayerByUUID(mount.getOwnerReference().getUUID());
        if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.isPassenger()) return Vec3.ZERO;
        Vec3 delta = owner.position().subtract(mount.position());
        if (delta.lengthSqr() <= 9 || delta.lengthSqr() >= 1024 || mount instanceof Seahorse && !owner.isInWater()) return Vec3.ZERO;
        if (mount.canFly()) delta = delta.add(0, 2, 0);
        return delta.normalize().scale(.65).yRot(mount.getYRot() * (float)(Math.PI / 180));
    }
    private boolean readyParent() {
        return mount.isTamed() && !mount.isBaby() && !following && !mount.isVehicle() && !mount.isPassenger()
            && !offerPending() && mount.isAlive() && mount.getHealth() >= mount.getMaxHealth()
            && mount.level().getGameTime() >= breedAfter;
    }
    public boolean requestBreeding(ServerPlayer player) {
        if (!mount.isOwnedBy(player) || !accessible(player) || !readyParent() || breeding()) return false;
        if (!MountCompanions.consume(player, mount.tamingFood())) return false;
        loveUntil = mount.level().getGameTime() + 600;
        ((ServerLevel) mount.level()).sendParticles(ParticleTypes.HEART, mount.getX(), mount.getY()+mount.getBbHeight(), mount.getZ(), 7, .4,.3,.4,0);
        return true;
    }
    private void breed(ServerLevel level, PrototypeMount mate) {
        var child = (PrototypeMount) mount.getType().create(level, EntitySpawnReason.BREEDING);
        if (child == null) return;
        child.assignOwner(mount.getOwnerReference().getUUID()); child.care.makeBaby();
        Vec3 center = mount.position().add(mate.position()).scale(.5);
        boolean placed = false;
        for (int x = -2; x <= 2 && !placed; x++) for (int z = -2; z <= 2 && !placed; z++) {
            child.setPos(center.add(x, 0, z));
            if (MountCompanions.safePlacement(level, child)) placed = true;
        }
        if (!placed || !level.addFreshEntity(child)) { child.discard(); return; }
        loveUntil = mate.care.loveUntil = 0;
        breedAfter = mate.care.breedAfter = level.getGameTime() + 6000;
        level.sendParticles(ParticleTypes.HEART, child.getX(), child.getY()+1, child.getZ(), 8,.4,.4,.4,0);
    }
    public boolean offer(ServerPlayer owner, ServerPlayer next) {
        if (!mount.isOwnedBy(owner) || !accessible(owner) || !available() || next == null || next == owner
                || !next.isAlive() || next.isSpectator() || next.level() != mount.level() || next.distanceToSqr(mount) > 64) return false;
        recipient = next.getUUID(); offerUntil = mount.level().getGameTime() + 1200;
        next.sendSystemMessage(Component.translatable("message.darkspawn.mount_transfer_offer", owner.getName(), mount.getName()));
        return true;
    }
    public boolean accept(ServerPlayer next) {
        if (!accessible(next) || !offeredTo(next) || mount.isVehicle() || mount.isPassenger() || breeding()) return false;
        var owner = ((ServerLevel)mount.level()).getPlayerByUUID(mount.getOwnerReference().getUUID());
        if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.distanceToSqr(mount) > 64) return false;
        mount.assignOwner(next.getUUID()); recipient = null; offerUntil = 0; setFollowing(false);
        return true;
    }
    public void cancelOffer() { recipient = null; offerUntil = 0; }
}
