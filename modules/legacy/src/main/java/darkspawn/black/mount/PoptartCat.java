package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Nyan-inspired flying mount; rare wild cats hover in the upper Overworld. */
public final class PoptartCat extends PrototypeMount {
    private static final DustParticleOptions[] RAINBOW = java.util.stream.IntStream.of(
        0xFF3030, 0xFF9820, 0xFFE838, 0x40E858, 0x3498FF, 0xAD55FF)
        .mapToObj(color -> new DustParticleOptions(color, .8F)).toArray(DustParticleOptions[]::new);
    private boolean wildSkyCat;
    public void setWildSkyCat() { wildSkyCat = true; }
    @Override public boolean isPersistenceRequired() { return isTamed() || super.isPersistenceRequired() && (!wildSkyCat || hasCustomName()); }
    @Override protected Vec3 unoccupiedInput() { return wildSkyCat && !isTamed() ? Vec3.ZERO : super.unoccupiedInput(); }
    @Override protected void addPassenger(net.minecraft.world.entity.Entity passenger) {
        super.addPassenger(passenger);
        // Claim on mounting, before a same-tick dismount/save can leave the cat eligible for despawning.
        if (passenger instanceof net.minecraft.world.entity.player.Player) wildSkyCat = false;
    }
    @Override public void tick() {
        super.tick();
        // Rainbow Trail: Observer clients emit six ordered stripes from the rear, with no terrain edits or network spam.
        if (level().isClientSide() && isAlive() && isVehicle()) {
            Vec3 rear = new Vec3(0, 0, 1.05).yRot(-getYRot() * (float) (Math.PI / 180));
            for (int stripe = 0; stripe < RAINBOW.length; stripe++) {
                level().addParticle(RAINBOW[stripe], getX() + rear.x, getY() + .95 - stripe * .12,
                    getZ() + rear.z, rear.x * .09, 0, rear.z * .09);
            }
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("WildSkyCat", wildSkyCat);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        wildSkyCat = input.getBooleanOr("WildSkyCat", false);
    }
    public PoptartCat(EntityType<? extends PoptartCat> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 28).add(Attributes.MOVEMENT_SPEED, .28F);
    }
    @Override protected boolean canFly() { return true; }
    @Override protected float groundSpeed() { return .28F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_flying_mounted"; }
}
