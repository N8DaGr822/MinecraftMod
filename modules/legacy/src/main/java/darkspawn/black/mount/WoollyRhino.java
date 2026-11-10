package darkspawn.black.mount;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Snow mount with an owner-controlled damaging charge. */
public final class WoollyRhino extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.WHEAT; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) {
        return switch (cue) { case 1 -> net.minecraft.sounds.SoundEvents.RAVAGER_HURT; case 2 -> net.minecraft.sounds.SoundEvents.RAVAGER_DEATH; default -> net.minecraft.sounds.SoundEvents.RAVAGER_AMBIENT; };
    }
    @Override protected Ability ability() { return Ability.CHARGE; }
    @Override protected float attackPoseTicks() { return 3F; }
    public WoollyRhino(EntityType<? extends WoollyRhino> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, .30F);
    }
    @Override public boolean canFly() { return false; }
    @Override public boolean canGlide() { return false; }
    @Override public float groundSpeed() { return charging() ? .65F : .30F; }
    @Override protected String mountingMessage() { return "message.darkspawn.woolly_rhino_mounted"; }
}
