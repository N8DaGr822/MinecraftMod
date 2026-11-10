package darkspawn.black.mount;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/** Ocean mount; riders still need their own air supply or Water Breathing. */
public final class Seahorse extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.KELP; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) {
        return switch (cue) { case 1 -> net.minecraft.sounds.SoundEvents.TROPICAL_FISH_HURT; case 2 -> net.minecraft.sounds.SoundEvents.TROPICAL_FISH_DEATH; default -> net.minecraft.sounds.SoundEvents.TROPICAL_FISH_AMBIENT; };
    }
    @Override protected boolean waterBound() { return true; }
    public Seahorse(EntityType<? extends Seahorse> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, .05);
    }
    @Override public boolean canFly() { return false; }
    @Override protected boolean canSwim() { return true; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public float groundSpeed() { return .05F; }
    @Override protected double swimmingSpeed() { return .38; }
    @Override public boolean checkSpawnObstruction(LevelReader level) { return level.isUnobstructed(this); }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_swimming_mounted"; }
}
