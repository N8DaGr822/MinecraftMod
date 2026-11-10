package darkspawn.black.creaturemounts;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Naturally spawning riding prototype. */
public final class Sasquatch extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.APPLE; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) {
        return switch (cue) { case 1 -> net.minecraft.sounds.SoundEvents.POLAR_BEAR_HURT; case 2 -> net.minecraft.sounds.SoundEvents.POLAR_BEAR_DEATH; default -> net.minecraft.sounds.SoundEvents.POLAR_BEAR_AMBIENT; };
    }
    public Sasquatch(EntityType<? extends Sasquatch> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.MOVEMENT_SPEED, .30F);
    }
    @Override public boolean canFly() { return false; }
    @Override public float groundSpeed() { return .30F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
