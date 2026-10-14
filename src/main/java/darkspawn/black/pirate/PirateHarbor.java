package darkspawn.black.pirate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import darkspawn.black.worldgen.BossLandmarkStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PirateHarbor extends SavedData {
	public static final Codec<PirateHarbor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("initialized", false).forGetter(value -> value.initialized),
		Codec.BOOL.optionalFieldOf("shipyard", false).forGetter(value -> value.shipyard),
		Codec.BOOL.optionalFieldOf("briggs", false).forGetter(value -> value.briggs),
		Codec.BOOL.optionalFieldOf("board", false).forGetter(value -> value.board)
	).apply(instance, PirateHarbor::new));
	public static final SavedDataType<PirateHarbor> TYPE = new SavedDataType<>(Darkspawn.id("pirate_harbor"), () -> new PirateHarbor(false, false, false, false), CODEC, null);
	private boolean initialized, shipyard, briggs, board;
	private PirateHarbor(boolean initialized, boolean shipyard, boolean briggs, boolean board) { this.initialized = initialized; this.shipyard = shipyard; this.briggs = briggs; this.board = board; }
	public static void prepare(ServerLevel level) {
		if (!level.dimension().equals(Pirate.LEVEL)) throw new IllegalArgumentException("Harbor requires Pirate Seas");
		var data = level.getDataStorage().computeIfAbsent(TYPE);
		if (data.initialized) { addShipyard(level, data); addGuide(level, data); return; }
		// One-Time Harbor: Native templates place finite supplies; later visits preserve builds and loot.
		place(level, "harbor", new BlockPos(-24, 40, -24));
		// Guaranteed Discovery: A visible first island avoids a seed-dependent empty first voyage.
		place(level, "island_0", new BlockPos(168, 40, -24));
		data.initialized = true; data.setDirty(); addShipyard(level, data); addGuide(level, data);
	}
	private static void addGuide(ServerLevel level, PirateHarbor data) {
		// Harbor Migration: Add services only in clear space; never rebuild an established harbor.
		if (!data.board) {
			var pos=HarborGuide.BOARD_POS;level.getChunk(pos);
			if(level.isEmptyBlock(pos)&&level.getBlockState(pos.below()).isSolidRender()) {
				level.setBlockAndUpdate(pos,HarborGuide.BOARD.defaultBlockState());data.board=true;data.setDirty();
			}
		}
		if(data.briggs)return;
		for(int x=-4;x<=-1;x++)for(int z=1;z<=4;z++) {
			var pos=new BlockPos(x,65,z);level.getChunk(pos);
			if(!level.isEmptyBlock(pos)||!level.isEmptyBlock(pos.above())||!level.getBlockState(pos.below()).isSolidRender())continue;
			var guide=net.minecraft.world.entity.EntityTypes.VILLAGER.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			if(guide==null)return;
			guide.setPos(x+.5,65,z+.5);
			if(!level.noCollision(guide)||!level.getEntities(guide,guide.getBoundingBox()).isEmpty()){guide.discard();continue;}
			guide.addTag(HarborGuide.BRIGGS_TAG);guide.setNoAi(true);guide.setPermanentlyInvulnerable(true);guide.setPersistenceRequired();
			guide.setCustomName(net.minecraft.network.chat.Component.literal("Old Salt Briggs"));guide.setCustomNameVisible(true);
			if(level.addFreshEntity(guide)){data.briggs=true;data.setDirty();}else guide.discard();
			return;
		}
	}
	private static void addShipyard(ServerLevel level, PirateHarbor data) {
		if (data.shipyard) return;
		var pos = new BlockPos(0,65,24); level.getChunk(pos);
		// Harbor Upgrade: Add only in free space; never replace a player edit or replenish supplies.
		if (level.isEmptyBlock(pos)) level.setBlockAndUpdate(pos, Ships.SHIPYARD.defaultBlockState());
		data.shipyard = true; data.setDirty();
	}
	private static void place(ServerLevel level, String name, BlockPos origin) {
		var piece = new BossLandmarkStructure.LandmarkPiece(level.getStructureTemplateManager(), Darkspawn.id("pirate/" + name), origin, Rotation.NONE);
		var bounds = piece.getBoundingBox();
		for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) level.getChunk(x, z);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(level.getSeed()), bounds, new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4), origin);
	}
}
