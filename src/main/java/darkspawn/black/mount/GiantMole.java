package darkspawn.black.mount;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Soul Sand Valley mount with deliberate, server-owned tunnel excavation. */
public final class GiantMole extends PrototypeMount {
    private long nextDig;
    public GiantMole(EntityType<? extends GiantMole> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 48).add(Attributes.MOVEMENT_SPEED, .22);
    }
    @Override protected boolean canFly() { return false; }
    @Override protected float groundSpeed() { return .22F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mole_mounted"; }
    @Override public void travel(Vec3 input) {
        if (level() instanceof ServerLevel level && getControllingPassenger() instanceof ServerPlayer rider
                && diggingRequested() && (input.x != 0 || input.z != 0) && level.getGameTime() >= nextDig) {
            nextDig = level.getGameTime() + 5;
            digAhead(level, rider, input);
        }
        super.travel(input);
    }
    private void digAhead(ServerLevel level, ServerPlayer rider, Vec3 input) {
        if (!rider.mayBuild() || rider.isSpectator() || isInWater() || isInLava()) return;
        var direction = new Vec3(input.x, 0, input.z).normalize().yRot(-getYRot() * (float) (Math.PI / 180));
        var bounds = getBoundingBox().expandTowards(direction.scale(.65));
        var riderBounds = rider.getDimensions(rider.getPose()).makeBoundingBox(getPassengerRidingPosition(rider));
        bounds = bounds.minmax(riderBounds.expandTowards(direction.scale(.65))).deflate(.01);
        // Mole Tunnels: Preserve the floor; excavate body and rider clearance only, at most one short slice per pulse.
        var claw = new ItemStack(Items.DIAMOND_PICKAXE);
        int broken = 0;
        for (var cursor : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            var pos = cursor.immutable();
            if (pos.getY() < Math.ceil(getY() - .001)) continue;
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(rider, pos)) continue;
            var state = level.getBlockState(pos);
            // Mining Permissions: Unbreakable blocks, fluids and block entities never become a tunneling shortcut.
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !state.getFluidState().isEmpty()
                    || state.hasBlockEntity() || rider.blockActionRestricted(level, pos, rider.gameMode.getGameModeForPlayer())) continue;
            if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, rider, pos, state, null)) continue;
            if (level.destroyBlock(pos, false, rider)) {
                // Claw Loot: Diamond-tier digging produces normal drops, without Silk Touch or Fortune.
                if (!rider.preventsBlockDrops()) Block.dropResources(state, level, pos, null, rider, claw);
                PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, rider, pos, state, null);
                if (++broken >= 12) break;
            }
        }
    }
}
