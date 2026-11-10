package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Naturally spawning riding prototype. */
public final class Cerberus extends PrototypeMount {
    public Cerberus(EntityType<? extends Cerberus> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.MOVEMENT_SPEED, .34F);
    }
    @Override protected boolean canFly() { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override public void tick() {
        super.tick();
        // Cerberus Protection: Clear burning while mounted without adding or removing potion effects.
        if (level() instanceof net.minecraft.server.level.ServerLevel) {
            clearFire();
            if (getControllingPassenger() != null) getControllingPassenger().clearFire();
        }
    }
    @Override protected float groundSpeed() { return .34F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
