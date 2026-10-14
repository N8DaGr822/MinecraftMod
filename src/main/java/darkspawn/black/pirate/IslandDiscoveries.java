package darkspawn.black.pirate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import darkspawn.black.worldgen.BossLandmarkStructure;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

public final class IslandDiscoveries {
	public static final String ENCOUNTER_TAG="darkspawn_island_encounter";
	public static final String[] NAMES={"Castaway Cove","Saltwatch Fort","The Pale Gull","Lastlight Beacon","Tideglass Shrine","Rootbound Camp"};
	private static final String[] CLUES={
		"A castaway's trail leads to shelter and salvage. The sea gate's builders sailed here before you.",
		"The watch abandoned its guns facing inland. Ammunition remains in the fort cache.",
		"The captain's log describes a light answering from beneath the waves. Inspect the cargo cache for salvage.",
		"The keeper fled after a signal answered from below. Climb to the lantern to investigate; tools remain downstairs.",
		"Seven tides, seven bindings. These ruins guarded a prison, not a treasure vault. Examine the relic cache.",
		"Corsair cargo lies in a crimson-marked camp. Rival scavengers guard repair timber and a lost manifest."};
	public record Island(BlockPos center,int kind) { }
	private record Entry(BlockPos center,int mask) {
		private static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(BlockPos.CODEC.fieldOf("center").forGetter(Entry::center),Codec.INT.fieldOf("mask").forGetter(Entry::mask)).apply(i,Entry::new));
	}
	public static final class Encounters extends SavedData {
		private final Map<BlockPos,Integer> spawned=new HashMap<>();
		public static final Codec<Encounters> CODEC=Entry.CODEC.listOf().xmap(entries->{var state=new Encounters();entries.forEach(e->state.spawned.put(e.center(),e.mask()));return state;},state->state.spawned.entrySet().stream().map(e->new Entry(e.getKey(),e.getValue())).toList());
		public static final SavedDataType<Encounters> TYPE=new SavedDataType<>(Darkspawn.id("pirate_encounters"),Encounters::new,CODEC,null);
		public int mask(BlockPos center){return spawned.getOrDefault(center,0);}
		void record(BlockPos center,int mask){spawned.put(center,mask);setDirty();}
	}
	private static final class Budget {long second=-1;int attempts,count;}
	private static final WeakHashMap<ServerLevel,Budget> BUDGETS=new WeakHashMap<>();
	private IslandDiscoveries() { }
	public static void initialize() {
		UseBlockCallback.EVENT.register((player,level,hand,hit)->{
			if(player instanceof ServerPlayer server&&hand==InteractionHand.MAIN_HAND)inspect(server,hit.getBlockPos());
			return InteractionResult.PASS;
		});
	}
	public static int kind(net.minecraft.world.level.levelgen.structure.StructureStart start) {
		for(var piece:start.getPieces())if(piece instanceof BossLandmarkStructure.LandmarkPiece landmark) {
			var path=landmark.templateId().getPath();
			for(int i=0;i<NAMES.length;i++)if(path.equals("pirate/island_"+i))return i;
		}
		return -1;
	}
	public static Island at(ServerLevel level,BlockPos pos) {
		if(!level.dimension().equals(Pirate.LEVEL)||pos.getY()<60||pos.getY()>84)return null;
		if(Math.abs((long)pos.getX()-192)<=24&&Math.abs((long)pos.getZ())<=24)return new Island(LogboundCompass.FIRST_COVE_CENTER,0);
		var structure=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Darkspawn.id("pirate_island"))).value();
		// Loaded Survey: Island bounds extend 24 blocks from their start; no discovery query requests chunks.
		for(int x=(pos.getX()>>4)-2;x<=(pos.getX()>>4)+2;x++)for(int z=(pos.getZ()>>4)-2;z<=(pos.getZ()>>4)+2;z++) {
			var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;
			var start=chunk.getAllStarts().get(structure);
			if(start!=null&&start.isValid()&&start.getBoundingBox().isInside(pos)) {int kind=kind(start);if(kind>=0)return new Island(new BlockPos(x*16,65,z*16),kind);}
		}
		return null;
	}
	public static void observe(ServerPlayer player) {
		if(!player.isAlive()||player.isSpectator())return;
		var island=at(player.level(),player.blockPosition());if(island==null)return;
		var state=Pirate.state(player);int bit=1<<island.kind();
		if((state.pirateClues&bit)==0){state.pirateClues|=bit;player.sendSystemMessage(Component.literal(NAMES[island.kind()]+": "+CLUES[island.kind()]));}
		if(island.center().equals(state.logboundNextIsland))state.logboundKind=island.kind();
		if(island.kind()==3&&player.getY()>=76&&Math.abs(player.getX()-island.center().getX())<=5&&Math.abs(player.getZ()-island.center().getZ())<=6)complete(player,2);
		spawn(player,island);
		PirateDiplomacy.observe(player,island);
        PirateActivities.observe(player,island);PirateOfficers.observe(player,island);
	}
	public static boolean inspect(ServerPlayer player,BlockPos pos) {
		if(!player.isAlive()||player.isSpectator()||player.distanceToSqr(Vec3.atCenterOf(pos))>36||!player.level().getBlockState(pos).is(Blocks.CHEST))return false;
		var island=at(player.level(),pos);if(island==null)return false;
		boolean factionObjective=PirateProgression.inspect(player,island,pos);
        // One Offering Per Click: Do not spend shards twice when both shrine commissions are active.
        if(!factionObjective)factionObjective=PirateActivities.inspect(player,island,pos);
		factionObjective=PirateOfficers.inspect(player,island,pos)||factionObjective;
		if(island.kind()==2&&pos.equals(island.center().offset(3,0,-4)))return complete(player,0)||factionObjective;
		if(island.kind()==4&&pos.equals(island.center().offset(0,0,-6)))return complete(player,1)||factionObjective;
		return factionObjective;
	}
	private static boolean complete(ServerPlayer player,int index) {
		var state=Pirate.state(player);
		if(state.islandContracts[index]!=1||(state.islandObjectives&(1<<index))!=0)return false;
		state.islandObjectives|=1<<index;player.sendSystemMessage(Component.literal("Contract objective complete. Return to Briggs or the harbor board."));return true;
	}
	public static int spawn(ServerPlayer player,Island island) {
		var level=player.level();
		if(!player.isAlive()||player.isSpectator()||player.isCreative()||!level.dimension().equals(Pirate.LEVEL)||island.center().equals(LogboundCompass.FIRST_COVE_CENTER)
			||level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS))return 0;
		var saved=level.getDataStorage().computeIfAbsent(Encounters.TYPE);int mask=saved.mask(island.center()),total=island.kind()==0?1:2;
		if((mask&((1<<total)-1))==((1<<total)-1))return 0;
		var budget=BUDGETS.computeIfAbsent(level,key->new Budget());
		if(budget.second!=level.getGameTime()/20){budget.second=level.getGameTime()/20;budget.attempts=0;budget.count=0;for(var e:level.getAllEntities())if(e.isAlive()&&e.entityTags().contains(ENCOUNTER_TAG))budget.count++;}
		int spawned=0;
		for(int slot=0;slot<total;slot++) {
			if((mask&(1<<slot))!=0)continue;
			if(budget.count>=48||budget.attempts++>=8)break;
			var pos=island.center().offset(slot==0?-7:7,0,10);
			if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null||!level.getBlockState(pos.below()).isSolidRender()||!level.isEmptyBlock(pos)||!level.isEmptyBlock(pos.above()))continue;
			if(level.players().stream().anyMatch(p->p.distanceToSqr(Vec3.atCenterOf(pos))<16))continue;
			Mob mob=switch(island.kind()) {
				case 1 -> EntityTypes.PILLAGER.create(level,EntitySpawnReason.EVENT);
				case 2,4 -> EntityTypes.DROWNED.create(level,EntitySpawnReason.EVENT);
				case 3 -> EntityTypes.SKELETON.create(level,EntitySpawnReason.EVENT);
				case 5 -> EntityTypes.PIGLIN.create(level,EntitySpawnReason.EVENT);
				default -> EntityTypes.HUSK.create(level,EntitySpawnReason.EVENT);
			};
			if(mob==null)continue;
			mob.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
			if(!level.noCollision(mob)||!level.getEntities(mob,mob.getBoundingBox()).isEmpty()){mob.discard();continue;}
			if(mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin)piglin.setImmuneToZombification(true);
			var faction=PirateDiplomacy.islandFaction(island.kind());
            if(faction!=null)mob.addTag(PirateDiplomacy.FACTION_TAG+faction.id);
            if(mob instanceof net.minecraft.world.entity.raid.Raider raider)raider.setCanJoinRaid(false);
            mob.setPersistenceRequired();mob.addTag(ENCOUNTER_TAG);mob.setCustomName(Component.literal(NAMES[island.kind()]+" " + (island.kind()==5?"Cargo Raider":island.kind()==1?"Renegade Sentry":"Sentry")));
			if(island.kind()==5)mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(slot==0?Items.GOLDEN_SWORD:Items.CROSSBOW));
			if(island.kind()==1)mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CROSSBOW));
			if(island.kind()==3)mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
			if(island.kind()==4&&slot==0)mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TRIDENT));
			if(island.kind()==2||island.kind()==3||island.kind()==4)mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
			if(level.addFreshEntity(mob)){mask|=1<<slot;saved.record(island.center(),mask);spawned++;budget.count++;}else mob.discard();
		}
		return spawned;
	}
	public static String clue(int kind){return kind>=0&&kind<CLUES.length?CLUES[kind]:"Uncharted island";}
}
