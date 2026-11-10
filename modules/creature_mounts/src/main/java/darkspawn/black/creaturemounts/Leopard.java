package darkspawn.black.creaturemounts;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Jungle mount with a faster stride and a higher grounded jump. */
public final class Leopard extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.CHICKEN; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) {
        return switch (cue) { case 1 -> net.minecraft.sounds.SoundEvents.OCELOT_HURT; case 2 -> net.minecraft.sounds.SoundEvents.OCELOT_DEATH; default -> net.minecraft.sounds.SoundEvents.OCELOT_AMBIENT; };
    }
    public Leopard(EntityType<? extends Leopard> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 32).add(Attributes.MOVEMENT_SPEED, .4)
            .add(Attributes.JUMP_STRENGTH, .65).add(Attributes.SAFE_FALL_DISTANCE, 6);
    }
    @Override public boolean canFly() { return false; }
    @Override public float groundSpeed() { return .4F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
