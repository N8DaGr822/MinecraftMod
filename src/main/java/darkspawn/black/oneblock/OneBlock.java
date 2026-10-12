package darkspawn.black.oneblock;

import darkspawn.black.Darkspawn;
import darkspawn.black.inventory.ArmorLoadouts;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public final class OneBlock {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Darkspawn.id("oneblock"));
	public static final BlockPos CORE_POS = new BlockPos(0, 79, 0);
	public static final int TRAVEL_TICKS = 60;
	public static final Block[] RESOURCES = { Blocks.DIRT, Blocks.OAK_LOG, Blocks.GRASS_BLOCK, Blocks.STONE,
		Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY, Blocks.MOSS_BLOCK,
		Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE, Blocks.NETHERRACK,
		Blocks.NETHER_QUARTZ_ORE, Blocks.SOUL_SAND, Blocks.GLOWSTONE, Blocks.END_STONE };
	private static final int[][] POOLS = { {0,0,0,1,1,1,2}, {3,3,3,4,4,5,6,8}, {0,2,7,7,8,9,10},
		{3,4,5,5,11,12,13,14}, {15,15,15,16,17,18}, {19,19,14,13,12,16,18} };
	public static final OneBlockCore CORE = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("oneblock_core"),
		new OneBlockCore(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id("oneblock_core")))
			.strength(1.5F, 3600000).pushReaction(PushReaction.IMMOVEABLE)));
	private OneBlock() { }
	public static boolean island(Level level) { return level.dimension().equals(LEVEL); }
	public static OneBlockPlayerState state(ServerPlayer player) { return ((OneBlockPlayer) player).darkspawn$oneBlock(); }
	public static OneBlockProgress progress(ServerLevel level) { return level.getDataStorage().computeIfAbsent(OneBlockProgress.TYPE); }

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(OneBlockActionPayload.TYPE, OneBlockActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OneBlockStatusPayload.TYPE, OneBlockStatusPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(OneBlockActionPayload.TYPE, (payload, context) -> request(context.player(), payload.action()));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var player = handler.player;
			// Recovery: The active items and inactive profile are saved together with the dimension marker.
			if (state(player).inside != island(player.level())) switchItems(player);
			state(player).pendingTicks = 0; sync(player);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> state(handler.player).pendingTicks = 0);
		ServerPlayerEvents.COPY_FROM.register((old, next, alive) -> state(next).copyFrom(state(old)));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && (taken > 0 || !blocked && base > 0)) { state(player).hurtTicks = 100; cancel(player); }
		});
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, block, entity) -> !island(level) || !pos.equals(CORE_POS)
			|| player instanceof ServerPlayer && progress((ServerLevel) level).lastBreakTick != level.getGameTime());
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, block, entity) -> {
			if (island(level) && pos.equals(CORE_POS) && block.is(CORE) && player instanceof ServerPlayer serverPlayer) mined(serverPlayer);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var player : server.getPlayerList().getPlayers()) tick(player);
			if (server.getTickCount() % 20 == 0) {
				var level = server.getLevel(LEVEL);
				if (level != null && !level.players().isEmpty()) ensureIsland(level);
				server.getPlayerList().getPlayers().forEach(OneBlock::sync);
			}
		});
	}

	public static void ensureIsland(ServerLevel level) {
		var data = progress(level);
		if (!data.initialized) {
			for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) level.setBlockAndUpdate(CORE_POS.offset(x, 0, z), Blocks.DIRT.defaultBlockState());
			level.setBlockAndUpdate(CORE_POS.below(), Blocks.BEDROCK.defaultBlockState());
			data.initialized = true; data.setDirty();
		}
		// Core Recovery: Environmental loss never awards resources or advances the shared phase.
		if (!level.getBlockState(CORE_POS).is(CORE)) level.setBlockAndUpdate(CORE_POS, CORE.defaultBlockState().setValue(OneBlockCore.RESOURCE, data.resource));
	}

	public static void mined(ServerPlayer player) {
		var level = player.level(); var data = progress(level);
		if (data.lastBreakTick == level.getGameTime()) return;
		data.lastBreakTick = level.getGameTime();
		if (!player.preventsBlockDrops()) {
			data.mined();
			// Renewable Essentials: Every phase continues to supply dirt/wood and samples previous pools.
			int phase = level.getRandom().nextInt(4) == 0 ? level.getRandom().nextInt(data.phase + 1) : data.phase;
			data.resource = data.total % 10 == 0 ? 0 : data.total % 10 == 1 ? 1 : POOLS[phase][level.getRandom().nextInt(POOLS[phase].length)];
			if (data.total % 25 == 0) supply(level, data.phase);
			if (data.total % 50 == 0) creature(level, data.phase);
		}
		level.setBlockAndUpdate(CORE_POS, CORE.defaultBlockState().setValue(OneBlockCore.RESOURCE, data.resource));
	}

	private static BlockPos space(ServerLevel level) {
		for (int radius = 1; radius <= 4; radius++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
			var pos = CORE_POS.offset(x, 1, z);
			if ((x != 0 || z != 0) && level.getBlockState(pos.below()).isSolidRender() && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
				&& level.getEntities(null, new net.minecraft.world.phys.AABB(pos).expandTowards(0, 1, 0)).isEmpty()) return pos;
		}
		return null;
	}
	private static void supply(ServerLevel level, int phase) {
		var pos = space(level); if (pos == null) return;
		level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
		if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
		chest.setItem(0, new ItemStack(Items.OAK_SAPLING, 2)); chest.setItem(1, new ItemStack(Items.WHEAT_SEEDS, 4)); chest.setItem(2, new ItemStack(Items.APPLE, 3));
		chest.setItem(3, new ItemStack(Items.WATER_BUCKET));
		if (phase >= 1) { chest.setItem(4, new ItemStack(Items.LAVA_BUCKET)); chest.setItem(5, new ItemStack(Items.STRING, 3)); }
		if (phase >= 2) { chest.setItem(6, new ItemStack(Items.CARROT, 2)); chest.setItem(7, new ItemStack(Items.POTATO, 2)); chest.setItem(8, new ItemStack(Items.SUGAR_CANE, 2)); }
		if (phase >= 3) { chest.setItem(9, new ItemStack(Items.BONE, 3)); chest.setItem(10, new ItemStack(Items.DIAMOND)); }
		if (phase >= 4) { chest.setItem(11, new ItemStack(Items.NETHER_WART, 3)); chest.setItem(12, new ItemStack(Items.BLAZE_ROD)); }
		if (phase >= 5) { chest.setItem(13, new ItemStack(Items.ENDER_PEARL, 2)); chest.setItem(14, new ItemStack(Items.OBSIDIAN, 4)); }
		chest.setChanged();
	}
	private static void creature(ServerLevel level, int phase) {
		if (level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(CORE_POS).inflate(32)).size() >= 8) return;
		var pos = space(level); if (pos == null) return;
		String[] passive = { "cow", "sheep", "chicken", "pig" };
		String name = phase < 3 ? passive[level.getRandom().nextInt(passive.length)] : phase == 3 ? "zombie" : phase == 4 ? "zombified_piglin" : "enderman";
		var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(name));
		var entity = type.create(level, EntitySpawnReason.EVENT);
		if (entity == null) return;
		entity.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
		if (level.noCollision(entity)) { if (entity instanceof Mob mob) mob.setPersistenceRequired(); level.addFreshEntity(entity); }
	}

	public static void request(ServerPlayer player, int action) {
		if (!safe(player)) { message(player, "unsafe"); return; }
		if (action == 1 && island(player.level())) {
			var data = progress(player.level());
			if (data.advance()) { for (var member : player.level().players()) message(member, "advanced"); }
			sync(player); return;
		}
		if (action != 0 || state(player).pendingTicks > 0) return;
		if (player.level().getServer().getLevel(LEVEL) == null) { message(player, "unavailable"); return; }
		state(player).pendingTicks = TRAVEL_TICKS; state(player).departure = player.position();
		message(player, "travelling"); sync(player);
	}
	private static boolean safe(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || !player.onGround() || player.isPassenger() || player.isVehicle() || player.isSleeping()
			|| player.isChangingDimension() || !player.connection.hasClientLoaded()
			|| player.isFallFlying() || player.isUsingItem() || player.isOnFire() || player.isInWater() || state(player).hurtTicks > 0
			|| player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
			|| !player.getShoulderEntityLeft().isEmpty() || !player.getShoulderEntityRight().isEmpty() || ArmorLoadouts.state(player).pending >= 0) return false;
		for (int slot = 1; slot <= 4; slot++) if (!player.inventoryMenu.getSlot(slot).getItem().isEmpty()) return false;
		for (var entity : player.level().getEntities(player, player.getBoundingBox().inflate(16))) {
			if (entity instanceof net.minecraft.world.entity.Leashable leash && leash.getLeashHolder() == player) return false;
		}
		return true;
	}
	public static void tick(ServerPlayer player) {
		var state = state(player); if (state.hurtTicks > 0) state.hurtTicks--;
		if (state.pendingTicks == 0) return;
		if (!safe(player) || state.departure == null || state.departure.distanceToSqr(player.position()) > .25) { cancel(player); return; }
		if (--state.pendingTicks == 0) travel(player);
	}
	private static void cancel(ServerPlayer player) {
		if (state(player).pendingTicks > 0) { state(player).pendingTicks = 0; message(player, "cancelled"); sync(player); }
	}
	public static Vec3 arrival(ServerLevel level) {
		ensureIsland(level);
		var pos = space(level); if (pos != null) return Vec3.atBottomCenterOf(pos);
		// Emergency Return: Clear only the two blocks above the indestructible source, dropping obstructions normally.
		level.destroyBlock(CORE_POS.above(), true); level.destroyBlock(CORE_POS.above(2), true);
		return Vec3.atBottomCenterOf(CORE_POS.above());
	}
	public static boolean travel(ServerPlayer player) {
		if (!safe(player)) return false;
		var state = state(player); var server = player.level().getServer(); boolean entering = !island(player.level());
		var destination = server.getLevel(entering ? LEVEL : state.returnLevel);
		if (destination == null) { message(player, "unavailable"); return false; }
		Vec3 pos = entering ? arrival(destination) : state.returnPos;
		if (!entering && (!destination.noCollision(player, player.getBoundingBox().move(pos.subtract(player.position())))
			|| !destination.getBlockState(BlockPos.containing(pos).below()).isSolidRender() || !destination.getFluidState(BlockPos.containing(pos)).isEmpty())) {
			var fallback = TeleportTransition.createDefault(player, TeleportTransition.DO_NOTHING);
			if (island(fallback.newLevel())) { message(player, "return_blocked"); return false; }
			destination = fallback.newLevel(); pos = fallback.position();
		}
		var origin = player.level(); var oldPos = player.position(); float yaw = player.getYRot(), pitch = player.getXRot();
		state.travelling = true;
		try {
			// Atomic Travel: No asynchronous boundary or item drop exists between teleport and profile replacement.
			if (player.teleport(new TeleportTransition(destination, pos, Vec3.ZERO, entering ? 0 : state.returnYaw, entering ? 0 : state.returnPitch, TeleportTransition.PLACE_PORTAL_TICKET)) == null) return false;
			switchItems(player);
			if (entering) { state.returnLevel = origin.dimension(); state.returnPos = oldPos; state.returnYaw = yaw; state.returnPitch = pitch; }
			player.fallDistance = 0; ((darkspawn.black.mixin.OneBlockSaveAccess) server.getPlayerList()).darkspawn$saveProfile(player);
			sync(player); return true;
		} finally { state.travelling = false; }
	}
	public static void switchItems(ServerPlayer player) {
		var state = state(player); var outgoing = OneBlockItems.capture(player);
		state.other.apply(player); state.other = outgoing; state.inside = island(player.level());
		darkspawn.black.equipment.ArmorStyleBonuses.update(player); darkspawn.black.equipment.LegendaryArmor.update(player);
		ArmorLoadouts.sync(player); player.inventoryMenu.broadcastFullState();
	}
	public static void sync(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, OneBlockStatusPayload.TYPE)) return;
		var level = player.level().getServer().getLevel(LEVEL); if (level == null) return;
		var data = progress(level);
		ServerPlayNetworking.send(player, new OneBlockStatusPayload(data.phase, data.breaks, state(player).pendingTicks));
	}
	public static void message(ServerPlayer player, String name) { player.sendOverlayMessage(Component.translatable("message.darkspawn.oneblock_" + name)); }
}
