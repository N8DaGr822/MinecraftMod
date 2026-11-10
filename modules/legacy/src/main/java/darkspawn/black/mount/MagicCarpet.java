package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Crafted one-seat flight using the existing authoritative mount controls and recovery. */
public final class MagicCarpet extends PrototypeMount {
    public MagicCarpet(EntityType<? extends MagicCarpet> type, Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() { return PrototypeMount.attributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.2); }
    public static boolean allowedLevel(Level level) { return level.dimension()==Level.OVERWORLD || level.dimension()==Level.NETHER || level.dimension()==Level.END; }
    @Override public boolean canTeleport(Level from, Level to) { return allowedLevel(to) && super.canTeleport(from,to); }
    @Override public net.minecraft.world.entity.Entity teleport(net.minecraft.world.level.portal.TeleportTransition transition) {
        return allowedLevel(transition.newLevel()) ? super.teleport(transition) : null;
    }
    @Override public Item tamingFood() { return Items.AMETHYST_SHARD; }
    @Override protected boolean canFly() { return true; }
    @Override protected float groundSpeed() { return .2F; }
    @Override protected String mountingMessage() { return "message.darkspawn.magic_carpet_mounted"; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return net.minecraft.sounds.SoundEvents.WOOL_HIT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return net.minecraft.sounds.SoundEvents.WOOL_BREAK; }
}
