package darkspawn.black.creaturemounts;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Desert mount with an owner-controlled poison sting. */
public final class GiantScorpion extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.SPIDER_EYE; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) {
        return switch (cue) { case 1 -> net.minecraft.sounds.SoundEvents.SPIDER_HURT; case 2 -> net.minecraft.sounds.SoundEvents.SPIDER_DEATH; default -> net.minecraft.sounds.SoundEvents.SPIDER_AMBIENT; };
    }
    @Override protected Ability ability() { return Ability.STING; }
    public GiantScorpion(EntityType<? extends GiantScorpion> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 44).add(Attributes.MOVEMENT_SPEED, .3);
    }
    @Override public boolean canFly() { return false; }
    @Override public float groundSpeed() { return .3F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
