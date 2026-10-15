package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import java.util.Optional;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class LogboundCompass extends Item {
	public static final int RECALIBRATION_SECONDS = 9 * 60;
	public static final String NEXT_ISLAND = "darkspawn:charted_island";
	public static final String FIRST_COVE = "darkspawn:first_cove";
	public static final BlockPos FIRST_COVE_CENTER = new BlockPos(192, 65, 0);
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("logbound_compass"), new LogboundCompass());
	private LogboundCompass() {
		super(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("logbound_compass")))
			.stacksTo(1).component(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false)));
	}
	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ITEM));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) server.getPlayerList().getPlayers().forEach(LogboundCompass::tickNavigation);
		});
	}
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer && player.isAlive() && !player.isSpectator()) {
			if (!level.dimension().equals(Pirate.LEVEL)) {
				player.sendOverlayMessage(Component.translatable("message.darkspawn.logbound_wrong_dimension"));
			} else {
				if(player.isShiftKeyDown()&&SeaMenu.available(serverPlayer)){SeaMenu.open(serverPlayer);return InteractionResult.SUCCESS;}
                var state = Pirate.state(serverPlayer);
				if (state.logboundDestination.isEmpty()) state.logboundDestination = FIRST_COVE;
				observeArrival(serverPlayer);
				if (destination(state) != null && state.logboundArrived && state.logboundSecondsRemaining == 0) advanceRoute(serverPlayer);
				updateTarget(player.getItemInHand(hand), serverPlayer);
				player.sendOverlayMessage(Component.literal(status(serverPlayer)));
			}
		}
		return InteractionResult.SUCCESS;
	}
	@Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (owner instanceof ServerPlayer player) updateTarget(stack, player);
	}
	private static void updateTarget(ItemStack stack, ServerPlayer player) {
		// Personal Navigation: Item copies carry only a display target, never the holder's discovery progress.
		var pos = destination(Pirate.state(player));
		var target = pos == null ? Optional.<GlobalPos>empty() : Optional.of(GlobalPos.of(Pirate.LEVEL, pos));
		var tracker = new LodestoneTracker(target, false);
		if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) stack.set(DataComponents.LODESTONE_TRACKER, tracker);
	}
	public static boolean observeArrival(ServerPlayer player) {
		var state = Pirate.state(player);
		if (!player.isAlive() || player.isSpectator() || !player.level().dimension().equals(Pirate.LEVEL)
			|| destination(state) == null || state.logboundArrived) return false;
		if (!insideIsland(player.blockPosition(), destination(state))) return false;
		state.logboundArrived = true;
		if (FIRST_COVE.equals(state.logboundDestination)) state.logboundCoveDiscovered = true;
		state.logboundSecondsRemaining = RECALIBRATION_SECONDS;
		if(!state.logboundVisited.contains(destination(state)))state.logboundVisited.add(destination(state));
		player.sendSystemMessage(Component.translatable("message.darkspawn.logbound_" + (FIRST_COVE.equals(state.logboundDestination) ? "arrived" : "next_arrived")));
		return true;
	}
	private static BlockPos destination(PiratePlayerState state) {
		return FIRST_COVE.equals(state.logboundDestination) ? FIRST_COVE_CENTER : NEXT_ISLAND.equals(state.logboundDestination) ? state.logboundNextIsland : null;
	}
	private static boolean insideIsland(BlockPos pos, BlockPos center) {
		// Cove Boundary: Match the authored island footprint, with a bounded surface arrival band; no chunk lookup.
		return Math.abs((long) pos.getX() - center.getX()) <= 24 && Math.abs((long) pos.getZ() - center.getZ()) <= 24 && pos.getY() >= 60 && pos.getY() <= 84;
	}
	public static void tickNavigation(ServerPlayer player) {
		HarborGuide.observeContract(player);
		IslandDiscoveries.observe(player);
		if (observeArrival(player)) return;
		var state = Pirate.state(player);
		// Island Study: Only online, living, non-spectator time inside the current discovered island counts.
		if (!player.isAlive() || player.isSpectator() || !player.level().dimension().equals(Pirate.LEVEL)
			|| destination(state) == null || !state.logboundArrived || !insideIsland(player.blockPosition(), destination(state))) return;
		if (state.logboundSecondsRemaining > 0 && --state.logboundSecondsRemaining == 0) advanceRoute(player);
	}
	public static boolean advanceRoute(ServerPlayer player) {
		var state = Pirate.state(player);
		if (!player.isAlive() || player.isSpectator() || !player.level().dimension().equals(Pirate.LEVEL)
			|| destination(state) == null || !state.logboundArrived || state.logboundSecondsRemaining > 0) return false;
		long now = player.level().getGameTime();
		if (now < state.logboundSearchAfter) return false;
		state.logboundSearchAfter = now + 600;
		// Bounded Charting: Native structure lookup only on completion or explicit retry, never every tick.
		var holder = player.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, Darkspawn.id("pirate_island")));
		if(!state.logboundVisited.contains(destination(state)))state.logboundVisited.add(destination(state));
		var located = findUnvisitedIsland(player,holder);
		if (located == null || !player.level().getWorldBorder().isWithinBounds(located)) {
			player.sendSystemMessage(Component.translatable("message.darkspawn.logbound_search_failed")); return false;
		}
		state.logboundNextIsland = new BlockPos(located.getX(), 65, located.getZ());
		state.logboundCoveDiscovered = true;
		state.logboundDestination = NEXT_ISLAND;
		state.logboundArrived = false;
		player.sendSystemMessage(Component.translatable("message.darkspawn.logbound_next_destination"));
		return true;
	}
	private static BlockPos findUnvisitedIsland(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure> holder) {
		var level=player.level();
		if(!level.getServer().getWorldGenSettings().options().generateStructures())return null;
		var state=Pirate.state(player);var generatorState=level.getChunkSource().getGeneratorState();
		var candidates=new java.util.ArrayList<BlockPos>();
		for(var placement:generatorState.getPlacementsForStructure(holder)) {
			if(!(placement instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread))continue;
			int rx=Math.floorDiv(player.blockPosition().getX()>>4,spread.spacing()),rz=Math.floorDiv(player.blockPosition().getZ()>>4,spread.spacing());
			for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++) {
				var chunk=spread.getPotentialStructureChunk(generatorState.getLevelSeed(),(rx+x)*spread.spacing(),(rz+z)*spread.spacing());
				var center=new BlockPos(chunk.getMinBlockX(),65,chunk.getMinBlockZ());
				if(!center.equals(FIRST_COVE_CENTER)&&!state.logboundVisited.contains(center)&&level.getWorldBorder().isWithinBounds(center))candidates.add(center);
			}
		}
		candidates.sort(java.util.Comparator.comparingDouble(pos->pos.distSqr(player.blockPosition())));
		// Personal Charts: Inspect at most 25 nearby candidates; never consume shared exploration references.
		for(var center:candidates.stream().distinct().limit(25).toList()) {
			var chunk=level.getChunk(center.getX()>>4,center.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
			var start=chunk.getAllStarts().get(holder.value());
			if(start!=null&&start.isValid()&&start.getBoundingBox().isInside(center)){state.logboundKind=IslandDiscoveries.kind(start);return center;}
		}
		return null;
	}
	public static String status(ServerPlayer player) {
		var state=Pirate.state(player);var target=destination(state);
		if(target==null)return "No available chart. Your saved progress is preserved.";
		String name=FIRST_COVE.equals(state.logboundDestination)?IslandDiscoveries.NAMES[0]:state.logboundKind>=0&&state.logboundKind<IslandDiscoveries.NAMES.length?IslandDiscoveries.NAMES[state.logboundKind]:"Charted island";
		long distance=Math.round(Math.hypot(target.getX()+.5-player.getX(),target.getZ()+.5-player.getZ()));
		String study=!state.logboundArrived?"Sailing":state.logboundSecondsRemaining==0?"Ready to chart":String.format(java.util.Locale.ROOT,"Study %d:%02d%s",state.logboundSecondsRemaining/60,state.logboundSecondsRemaining%60,insideIsland(player.blockPosition(),target)?"":" (paused)");
		return name+" | "+distance+" blocks | Discovered "+state.logboundVisited.size()+" | "+study;
	}
}
