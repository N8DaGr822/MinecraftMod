package darkspawn.black.mount;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Naturally spawning riding prototype. */
public final class WingedUnicorn extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.GOLDEN_APPLE; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) {
        return switch (cue) { case 1 -> net.minecraft.sounds.SoundEvents.HORSE_HURT; case 2 -> net.minecraft.sounds.SoundEvents.HORSE_DEATH; default -> net.minecraft.sounds.SoundEvents.HORSE_AMBIENT; };
    }
    public WingedUnicorn(EntityType<? extends WingedUnicorn> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 36).add(Attributes.MOVEMENT_SPEED, .32F);
    }
    @Override public boolean canFly() { return true; }
    @Override public float groundSpeed() { return .32F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_flying_mounted"; }
}
