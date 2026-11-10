package darkspawn.black.mount;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import darkspawn.black.Darkspawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class AircraftCoreItem extends Item {
    public static Item ITEM;
    private AircraftCoreItem(Properties properties) { super(properties); }
    public static void initialize() {
        var key=ResourceKey.create(Registries.ITEM,Darkspawn.id("aircraft_core"));
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new AircraftCoreItem(new Properties().setId(key).stacksTo(1)));
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ITEM));
    }
    static boolean permitted(BlockState state) {
        return !state.hasBlockEntity() && state.getFluidState().isEmpty()
            && (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(BlockTags.WOOL) || state.is(Blocks.GLASS) || state.is(Blocks.IRON_BLOCK));
    }
    static boolean validBuild(List<AirframeBlock> build) {
        if (build.size()<5 || build.size()>9) return false;
        var positions=new HashSet<BlockPos>();
        for (var block:build) if (block.pos().getY()!=0 || Math.abs(block.pos().getX())>1 || Math.abs(block.pos().getZ())>1
            || !permitted(block.state()) || !positions.add(block.pos())) return false;
        return positions.contains(BlockPos.ZERO) && positions.contains(new BlockPos(1,0,0)) && positions.contains(new BlockPos(-1,0,0))
            && positions.contains(new BlockPos(0,0,1)) && positions.contains(new BlockPos(0,0,-1));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        boolean ok=context.getClickedFace()==Direction.UP && assemble(player,context.getClickedPos());
        if (ok && !player.isCreative()) context.getItemInHand().shrink(1);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(ok ? "message.darkspawn.aircraft_assembled" : "message.darkspawn.aircraft_blocked"));
        return ok ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
    static boolean assemble(ServerPlayer player, BlockPos origin) {
        var level=player.level();
        if (!player.isAlive() || player.isSpectator() || !player.mayBuild() || player.isPassenger()
            || !MagicCarpet.allowedLevel(level) || player.distanceToSqr(Vec3.atCenterOf(origin))>64) return false;
        var build=new ArrayList<AirframeBlock>();
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            var pos=origin.offset(x,0,z);
            if (!level.hasChunkAt(pos) || !level.mayInteract(player,pos)) return false;
            var state=level.getBlockState(pos);
            if (!state.isAir()) build.add(new AirframeBlock(new BlockPos(x,0,z),state));
        }
        if (!validBuild(build)) return false;
        var aircraft=new Aircraft(Mounts.AIRCRAFT,level); aircraft.setPos(Vec3.atBottomCenterOf(origin));
        var bounds=aircraft.getBoundingBox();
        if (!level.getWorldBorder().isWithinBounds(bounds) || bounds.minY<level.getMinY() || bounds.maxY>level.getMaxY()
            || !level.getEntities(aircraft,bounds).isEmpty()) return false;
        for (var pos:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ),BlockPos.containing(bounds.maxX-.001,bounds.maxY-.001,bounds.maxZ-.001))) {
            if (!level.hasChunkAt(pos)) return false;
            if (build.stream().noneMatch(block -> origin.offset(block.pos()).equals(pos)) && !level.getBlockState(pos).isAir()) return false;
        }
        if (!level.getBlockState(origin.below()).isFaceSturdy(level,origin.below(),Direction.UP)) return false;
        // Assembly Transaction: Preserve exact states and roll back if insertion cannot complete.
        for (var block:build) level.setBlock(origin.offset(block.pos()),Blocks.AIR.defaultBlockState(),3);
        aircraft.setBuild(build); aircraft.assignOwner(player.getUUID()); aircraft.setOnGround(true);
        if (!MountCompanions.safePlacement(level,aircraft) || !level.addFreshEntity(aircraft)) {
            aircraft.discard();
            for (var block:build) level.setBlock(origin.offset(block.pos()),block.state(),3);
            return false;
        }
        return true;
    }
}
