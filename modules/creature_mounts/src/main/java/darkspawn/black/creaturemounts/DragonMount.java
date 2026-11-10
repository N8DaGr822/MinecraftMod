package darkspawn.black.creaturemounts;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Naturally spawning riding prototype. */
public final class DragonMount extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.COOKED_BEEF; }
    @Override protected net.minecraft.sounds.SoundEvent voice(int cue) { return cue == 0 ? net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL : net.minecraft.sounds.SoundEvents.ENDER_DRAGON_HURT; }
    @Override protected float getSoundVolume() { return .25F; }
    @Override protected Ability ability() { return Ability.BREATH; }
    @Override protected float attackPoseTicks() { return 12F; }
    public DragonMount(EntityType<? extends DragonMount> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, .25F);
    }
    @Override public boolean canFly() { return true; }
    @Override public float groundSpeed() { return .25F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_flying_mounted"; }
}
