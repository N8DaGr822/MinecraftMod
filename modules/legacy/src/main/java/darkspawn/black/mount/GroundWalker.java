package darkspawn.black.mount;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Crafted ground machine; ownership and recovery use the existing companion registry. */
public final class GroundWalker extends PrototypeMount {
    @Override protected boolean machine() { return true; }
    @Override protected boolean careMenu() { return false; }
    @Override protected boolean canFollow() { return false; }
    @Override protected boolean capsuleStorable() { return onGround(); }
    private long nextRepair;
    public GroundWalker(EntityType<? extends GroundWalker> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, .18F)
            .add(Attributes.STEP_HEIGHT, 1);
    }
    @Override public boolean canFly() { return false; }
    @Override public float groundSpeed() { return .18F; }
    @Override public Item tamingFood() { return Items.IRON_INGOT; }
    @Override protected String mountingMessage() { return "message.darkspawn.ground_walker_mounted"; }
    @Override public boolean canTeleport(Level from, Level to) { return MagicCarpet.allowedLevel(to) && super.canTeleport(from, to); }
    @Override public net.minecraft.world.entity.Entity teleport(net.minecraft.world.level.portal.TeleportTransition transition) {
        return MagicCarpet.allowedLevel(transition.newLevel()) ? super.teleport(transition) : null;
    }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || player.isSpectator()) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        if (!held.isEmpty() && !held.is(Items.IRON_INGOT)) return InteractionResult.PASS;
        // Machine Ownership: Crafted placement binds the pilot; iron never starts animal taming.
        if (!isOwnedBy(player)) return InteractionResult.FAIL;
        if (held.is(Items.IRON_INGOT)) {
            if (level() instanceof ServerLevel server) {
                if (getHealth() < getMaxHealth() && server.getGameTime() >= nextRepair) {
                    heal(4); held.consume(1, player); nextRepair = server.getGameTime() + 40;
                    playSound(net.minecraft.sounds.SoundEvents.IRON_GOLEM_REPAIR, .6F, 1);
                    player.sendSystemMessage(Component.translatable("message.darkspawn.ground_walker_repaired"));
                } else player.sendSystemMessage(Component.translatable("message.darkspawn.ground_walker_repair_wait"));
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            if (level() instanceof ServerLevel) player.sendSystemMessage(Component.translatable("message.darkspawn.ground_walker_care"));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output); output.putLong("WalkerNextRepair", nextRepair);
    }
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input); nextRepair = input.getLongOr("WalkerNextRepair", 0);
        care.setFollowing(false);
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return net.minecraft.sounds.SoundEvents.IRON_GOLEM_DEATH; }
}
