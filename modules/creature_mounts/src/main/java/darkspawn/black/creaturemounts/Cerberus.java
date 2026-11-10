package darkspawn.black.creaturemounts;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Naturally spawning riding prototype. */
public final class Cerberus extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.BONE; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) {
        return switch (cue) { case 1 -> net.minecraft.sounds.SoundEvents.RAVAGER_HURT; case 2 -> net.minecraft.sounds.SoundEvents.RAVAGER_DEATH; default -> net.minecraft.sounds.SoundEvents.RAVAGER_AMBIENT; };
    }
    public Cerberus(EntityType<? extends Cerberus> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.MOVEMENT_SPEED, .34F);
    }
    @Override public boolean canFly() { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override public void tick() {
        super.tick();
        // Cerberus Protection: Clear burning while mounted without adding or removing potion effects.
        if (level() instanceof net.minecraft.server.level.ServerLevel) {
            clearFire();
            if (getControllingPassenger() != null) getControllingPassenger().clearFire();
        }
    }
    @Override public float groundSpeed() { return .34F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
