package darkspawn.black.creaturemounts;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Swamp mount with ground travel and controlled swimming. */
public final class Crocodile extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.COD; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) {
        return switch (cue) { case 1 -> net.minecraft.sounds.SoundEvents.TURTLE_HURT; case 2 -> net.minecraft.sounds.SoundEvents.TURTLE_DEATH; default -> net.minecraft.sounds.SoundEvents.TURTLE_AMBIENT_LAND; };
    }
    @Override protected Ability ability() { return Ability.BITE; }
    public Crocodile(EntityType<? extends Crocodile> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 48).add(Attributes.MOVEMENT_SPEED, .25);
    }
    @Override public boolean canFly() { return false; }
    @Override protected boolean canSwim() { return true; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected double swimmingSpeed() { return .32; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_swimming_mounted"; }
}
