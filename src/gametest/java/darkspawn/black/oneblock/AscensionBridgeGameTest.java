package darkspawn.black.oneblock;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class AscensionBridgeGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "ascension-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		var pos = helper.absolutePos(new BlockPos(1, 2, 2)); level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
		player.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5); ready(player);
		return player;
	}
	private static void ready(ServerPlayer player) {
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket()); player.hasChangedDimension(); player.setOnGround(true);
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }

    private static AscensionLedger.Team team(ServerPlayer p) {
        Ascension.create(p,"Test team");return AscensionLedger.get(p.level()).member(p.getUUID());
    }
    private static void travel(GameTestHelper h,ServerPlayer p,UUID id) {
        ready(p);h.assertTrue(Ascension.travel(p,id),"Safe Ascension travel succeeds");ready(p);
    }

    private static net.minecraft.core.Direction direction(AscensionLedger.Team a,AscensionLedger.Team b) {
        return net.minecraft.core.Direction.getNearest(b.center().getX()-a.center().getX(),0,b.center().getZ()-a.center().getZ(),null);
    }
    private static void place(GameTestHelper h,ServerPlayer p,BlockPos anchor,net.minecraft.core.Direction facing) {
        p.level().setBlockAndUpdate(anchor,Blocks.STONE.defaultBlockState());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_PLANKS,2));
        var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(anchor),facing,anchor,false);
        var context=new net.minecraft.world.item.context.BlockPlaceContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,p.getMainHandItem(),hit);
        h.assertTrue(((net.minecraft.world.item.BlockItem)Items.OAK_PLANKS).place(context)==net.minecraft.world.InteractionResult.SUCCESS,"Eligible bridge placement succeeds");
    }
    @GameTest public void playerPlacementsSaveOwnershipAndNeighborsCannotTakeOver(GameTestHelper h) {
        var a=player(h);var b=player(h);
        try {
            var ta=team(a);var tb=team(b);var d=direction(ta,tb);travel(h,a,ta.id());travel(h,b,tb.id());
            var anchor=ta.center().relative(d,100);var pos=anchor.relative(d);place(h,a,anchor,d);
            var data=AscensionBridges.get(a.level());var record=data.at(pos);
            h.assertTrue(record!=null&&record.owner().equals(ta.id())&&record.corridor().equals(AscensionBridges.corridor(AscensionLedger.get(a.level()),tb,pos)),"Placement records team and canonical corridor");
            h.assertTrue(!b.gameMode.destroyBlock(pos)&&!AscensionProtection.canPlace(b,pos),"Other endpoint cannot remove or replace owned bridge");
            h.assertTrue(!AscensionProtection.canPlace(a,pos.relative(d),Blocks.CHEST.defaultBlockState())&&!AscensionProtection.canPlace(a,pos.relative(d),Blocks.SAND.defaultBlockState()),"Containers and gravity blocks cannot become neutral bridges");
            h.assertTrue(!AscensionProtection.canFluid(a,pos),"Owned bridges still reject fluid placement");
            var saved=AscensionBridges.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,data).getOrThrow();
            h.assertTrue(AscensionBridges.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow().at(pos).equals(record),"Ownership round-trips through durable format");
            h.assertTrue(a.gameMode.destroyBlock(pos)&&data.at(pos)==null,"Owner breaking removes the ownership record");
            var returnPos=OneBlock.state(b).returnPos;travel(h,b,ta.id());h.assertTrue(OneBlock.state(b).returnPos.equals(returnPos),"Visiting keeps the original outside return point");
        }finally{remove(a);remove(b);}h.succeed();
    }
    @GameTest public void archivalResumesCleanupAndPreservesNeighborsAndChangedBlocks(GameTestHelper h) {
        var a=player(h);var b=player(h);
        try {
            var ta=team(a);var tb=team(b);var d=direction(ta,tb);travel(h,a,ta.id());travel(h,b,tb.id());var level=a.level();
            var pa=ta.center().relative(d,101);var pb=tb.center().relative(d.getOpposite(),101);
            place(h,a,pa.relative(d.getOpposite()),d);place(h,b,pb.relative(d),d.getOpposite());
            var changed=pa.above();var bridges=AscensionBridges.get(level);var corridor=bridges.at(pa).corridor();
            level.setBlockAndUpdate(changed,Blocks.DIAMOND_BLOCK.defaultBlockState());bridges.record(changed,ta.id(),corridor,"minecraft:oak_planks");
            Ascension.archive(a);h.assertTrue(!AscensionLedger.get(level).archived(ta.id()),"Archival cannot strand a member inside Ascension");
            travel(h,a,null);Ascension.archive(a);h.assertTrue(AscensionLedger.get(level).archived(ta.id()),"Outside owner can explicitly archive their team");
            var encoded=AscensionBridges.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,bridges).getOrThrow();
            var restored=AscensionBridges.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();level.getDataStorage().set(AscensionBridges.TYPE,restored);
            restored.cleanup(level);
            h.assertTrue(level.isEmptyBlock(pa)&&restored.at(pa)==null,"Reloaded ownership resumes archived bridge cleanup");
            h.assertTrue(level.getBlockState(pb).is(Blocks.OAK_PLANKS)&&restored.at(pb).owner().equals(tb.id()),"Neighbor-owned bridge remains");
            h.assertTrue(level.getBlockState(changed).is(Blocks.DIAMOND_BLOCK)&&restored.at(changed)==null,"Administrative replacement is preserved");
            h.assertTrue(AscensionLedger.get(level).at(ta.center()).archived()&&!AscensionProtection.canPlace(b,ta.center()),"Archived island cell remains reserved and protected");
            h.assertTrue(!AscensionProtection.canPlace(b,pb.relative(d.getOpposite())),"No new extension toward archived destination");
        }finally{remove(a);remove(b);}h.succeed();
    }
    @GameTest public void cleanupIsBoundedAndNeverLoadsMissingChunks(GameTestHelper h) {
        var p=player(h);
        try {
            var t=team(p);var level=h.getLevel().getServer().getLevel(Ascension.LEVEL);var ledger=AscensionLedger.get(level);var data=AscensionBridges.get(level);
            // Fixture: An unloaded record models a pending cleanup entry restored from disk.
            var unloaded=new BlockPos(8000125,80,8000000);h.assertTrue(!level.hasChunkAt(unloaded),"Fixture chunk starts unloaded");
            data.record(unloaded,t.id(),"unloaded-fixture","minecraft:oak_planks");
            for(int i=0;i<80;i++){var pos=t.center().offset(101+i%7,0,i/7);level.setBlockAndUpdate(pos,Blocks.OAK_PLANKS.defaultBlockState());data.record(pos,t.id(),"budget-fixture","minecraft:oak_planks");}
            var chunkEdge=new BlockPos(6000015,80,6000008);level.getChunkAt(chunkEdge);
            level.setBlock(chunkEdge,Blocks.OAK_PLANKS.defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
            h.assertTrue(!level.hasChunkAt(chunkEdge.east()),"Adjacent edge chunk starts unloaded");data.record(chunkEdge,t.id(),"edge-fixture","minecraft:oak_planks");
            ledger.archive(p.getUUID());int removed=data.cleanup(level);
            h.assertTrue(removed<=AscensionBridges.CLEANUP_BUDGET&&data.at(unloaded)!=null&&!level.hasChunkAt(unloaded),"Cleanup respects its budget without loading pending chunks");
            for(int i=0;i<10;i++)data.cleanup(level);
            h.assertTrue(data.at(t.center().offset(101,0,0))==null&&data.at(unloaded)!=null&&!level.hasChunkAt(unloaded),"Unloaded entries do not starve later loaded work");
            h.assertTrue(data.at(chunkEdge)==null&&level.isEmptyBlock(chunkEdge)&&!level.hasChunkAt(chunkEdge.east()),"Boundary cleanup does not load adjacent chunks through shape updates");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void pistonsAndNativeFluidTicksCannotCrossClaimEdges(GameTestHelper h) {
        var p=player(h);
        try {
            var t=team(p);travel(h,p,t.id());var level=p.level();var east=net.minecraft.core.Direction.EAST;
            var piston=t.center().offset(99,2,20);level.setBlockAndUpdate(piston,Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,east));level.setBlockAndUpdate(piston.east(),Blocks.STONE.defaultBlockState());
            h.assertTrue(!new net.minecraft.world.level.block.piston.PistonStructureResolver(level,piston,east,true).resolve(),"Piston cannot push a claim block into neutral space");
            var inside=t.center().offset(90,2,20);level.setBlockAndUpdate(inside,Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,east));level.setBlockAndUpdate(inside.east(),Blocks.STONE.defaultBlockState());
            h.assertTrue(new net.minecraft.world.level.block.piston.PistonStructureResolver(level,inside,east,true).resolve(),"Ordinary motion within an active claim is preserved");
            for(var block:new net.minecraft.world.level.block.Block[]{Blocks.WATER,Blocks.LAVA}){
                var source=t.center().offset(100,3,block==Blocks.WATER?30:40);level.setBlockAndUpdate(source.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(source.east().below(),Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(source,block.defaultBlockState());var fluid=level.getFluidState(source);
                ((net.minecraft.world.level.material.FlowingFluid)fluid.getType()).tick(level,source,level.getBlockState(source),fluid);
                h.assertTrue(level.getFluidState(source.east()).isEmpty(),"Native fluid tick cannot flow into neutral columns");
            }
            travel(h,p,null);
            h.assertTrue(!new net.minecraft.world.level.block.piston.PistonStructureResolver(level,inside,east,true).resolve(),"Supported machines pause when no team member is present");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void explosionsAndFireCannotDestroyBridgeTerrain(GameTestHelper h) {
        var a=player(h);var b=player(h);
        try {
            var ta=team(a);var tb=team(b);var d=direction(ta,tb);travel(h,a,ta.id());var level=a.level();var anchor=ta.center().relative(d,100);place(h,a,anchor,d);var pos=anchor.relative(d);
            level.explode(null,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,4,true,net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            h.assertTrue(level.getBlockState(pos).is(Blocks.OAK_PLANKS)&&AscensionBridges.get(level).at(pos)!=null,"Explosion preserves bridge blocks and ownership");
            h.assertTrue(!level.canSpreadFireAround(pos),"Native fire and lava ignition spread is disabled in Ascension");
            level.setBlockAndUpdate(pos.above(),Blocks.FIRE.defaultBlockState());for(int i=0;i<30;i++)level.getBlockState(pos.above()).tick(level,pos.above(),level.getRandom());
            h.assertTrue(level.getBlockState(pos).is(Blocks.OAK_PLANKS),"Fire ticks do not consume a wooden bridge");
        }finally{remove(a);remove(b);}h.succeed();
    }
    @GameTest public void nativeMachinesCannotOutputAcrossClaimBoundaries(GameTestHelper h) {
        var p=player(h);
        try {
            var t=team(p);travel(h,p,t.id());var level=p.level();var east=net.minecraft.core.Direction.EAST;
            for(var block:new net.minecraft.world.level.block.Block[]{Blocks.DISPENSER,Blocks.DROPPER}){
                var pos=t.center().offset(100,2,block==Blocks.DISPENSER?50:60);var state=block.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING,east);level.setBlockAndUpdate(pos,state);
                var container=(net.minecraft.world.level.block.entity.DispenserBlockEntity)level.getBlockEntity(pos);container.setItem(0,new ItemStack(Items.WATER_BUCKET));state.tick(level,pos,level.getRandom());
                h.assertTrue(container.getItem(0).is(Items.WATER_BUCKET)&&level.isEmptyBlock(pos.east()),"Dispenser/dropper does not consume or output across boundary");
            }
            var pos=t.center().offset(100,2,70);var state=Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING,east);level.setBlockAndUpdate(pos,state);level.setBlockAndUpdate(pos.east(),Blocks.CHEST.defaultBlockState());
            var hopper=(net.minecraft.world.level.block.entity.HopperBlockEntity)level.getBlockEntity(pos);hopper.setItem(0,new ItemStack(Items.DIAMOND));
            net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level,pos,state,hopper);
            h.assertTrue(hopper.getItem(0).is(Items.DIAMOND)&&((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos.east())).isEmpty(),"Hopper cannot transfer into a neutral container");
            var inside=t.center().offset(90,2,70);level.setBlockAndUpdate(inside,state);level.setBlockAndUpdate(inside.east(),Blocks.CHEST.defaultBlockState());
            var local=(net.minecraft.world.level.block.entity.HopperBlockEntity)level.getBlockEntity(inside);local.setItem(0,new ItemStack(Items.DIAMOND));
            net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level,inside,state,local);
            h.assertTrue(local.isEmpty()&&((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(inside.east())).getItem(0).is(Items.DIAMOND),"Ordinary active-claim hopper transfer remains functional");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void environmentRestrictionsDoNotChangeOutsideDimensions(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,30,1));level.setBlockAndUpdate(pos,Blocks.DIRT.defaultBlockState());
        new net.minecraft.world.level.ServerExplosion(level,null,null,null,Vec3.atCenterOf(pos),4,false,net.minecraft.world.level.Explosion.BlockInteraction.DESTROY).explode();
        h.assertTrue(!level.getBlockState(pos).is(Blocks.DIRT),"Outside explosions retain normal terrain destruction");
        h.assertTrue(AscensionProtection.canOperate(level,pos,pos.east()),"Outside machines need no Ascension member");
        h.assertTrue(AscensionProtection.sameClaim(level,pos,pos.east()),"Outside fluid propagation is unrestricted by Ascension");h.succeed();
    }

}
