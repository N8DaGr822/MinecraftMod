package darkspawn.black.forsaken;

import darkspawn.black.Darkspawn;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public final class Forsaken {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Darkspawn.id("forsaken"));
	public static final BlockPos LANDING = new BlockPos(0, 80, 0);
	public static final BlockPos EXIT = LANDING.offset(0, 0, -4);
	public static final Block GATE = block("quarantine_gate", properties -> new QuarantineGateBlock(properties.strength(5, 1200)
		.lightLevel(state -> state.getValue(QuarantineGateBlock.ACTIVE) ? 12 : 0)));
	public static final Block ARCHIVE = block("quarantine_archive", properties -> new QuarantineTerminalBlock(properties.strength(-1, 3600000).lightLevel(state -> 8), false));
	public static final Block RETURN = block("quarantine_return", properties -> new QuarantineTerminalBlock(properties.strength(-1, 3600000).lightLevel(state -> 15), true));
	public static final Item GATE_ITEM = item("quarantine_gate", properties -> new BlockItem(GATE, properties.useBlockDescriptionPrefix()));
	public static final Item KEY = item("containment_key", properties -> new ContainmentKeyItem(properties.stacksTo(1)));
	public static final MenuType<QuarantineMenu> MENU = Registry.register(BuiltInRegistries.MENU, Darkspawn.id("quarantine"), new MenuType<>(QuarantineMenu::new, FeatureFlags.VANILLA_SET));
	private Forsaken() { }
	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		return Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id(name), factory.apply(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id(name))).sound(SoundType.METAL).pushReaction(PushReaction.IMMOVEABLE)));
	}
	private static Item item(String name, Function<Item.Properties, Item> factory) {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}
	public static ForsakenPlayerState state(ServerPlayer player) { return ((ForsakenPlayer) player).darkspawn$forsaken(); }
	public static void initialize() {
		ForsakenCityStructure.initialize();
		ServerPlayerEvents.COPY_FROM.register((old, next, alive) -> state(next).copyFrom(state(old)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> { entries.accept(GATE_ITEM); entries.accept(KEY); });
	}
	public static boolean unlocked(ServerLevel level) {
		var end = level.getServer().getLevel(Level.END);
		return end != null && end.getDragonFight() != null && end.getDragonFight().hasPreviouslyKilledDragon();
	}
	public static boolean validFrame(ServerLevel level, BlockPos center) {
		var state = level.getBlockState(center);
		if (!state.is(GATE)) return false;
		var right = state.getValue(QuarantineGateBlock.FACING).getClockWise();
		// Gate Shape: Five wide, five high; controller replaces the bottom-center obsidian.
		for (int x = -2; x <= 2; x++) for (int y = 0; y <= 4; y++) {
			if (x == 0 && y == 0) continue;
			var pos = center.relative(right, x).above(y);
			if (!level.hasChunkAt(pos)) return false;
			var block = level.getBlockState(pos);
			if (Math.abs(x) == 2 || y == 0 || y == 4) { if (!block.is(Blocks.OBSIDIAN)) return false; }
			else if (!block.isAir()) return false;
		}
		return true;
	}
	private static boolean nearby(ServerPlayer player, BlockPos pos) { return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 36; }
	public static boolean activate(ServerPlayer player, BlockPos pos) {
		var level = player.level();
		if (!level.dimension().equals(Level.OVERWORLD) || player.isSpectator() || !player.isAlive() || !nearby(player, pos)
			|| !player.isHolding(KEY) || !level.getBlockState(pos).is(GATE)) return false;
		if (!unlocked(level)) { message(player, "dragon_locked"); return false; }
		if (!validFrame(level, pos)) { message(player, "frame"); return false; }
		level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(QuarantineGateBlock.ACTIVE, true));
		message(player, "activated"); return true;
	}
	public static void openFabricator(ServerPlayer player, BlockPos pos) {
		if (!nearby(player, pos) || !state(player).researched || player.isSpectator() || !player.isAlive()) { message(player, "research_needed"); return; }
		var block = player.level().getBlockState(pos);
		if (!block.is(ARCHIVE) && !block.is(GATE)) return;
		player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new QuarantineMenu(id, inventory, pos), Component.translatable("container.darkspawn.quarantine")));
	}
	private static boolean safe(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || !player.onGround() || player.isPassenger() || player.isVehicle()
			|| player.isSleeping() || player.isFallFlying() || player.isUsingItem() || player.isOnFire() || player.isInWater()
			|| player.hurtTime > 0 || player.isChangingDimension() || !player.connection.hasClientLoaded()
			|| player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
			|| !player.getShoulderEntityLeft().isEmpty() || !player.getShoulderEntityRight().isEmpty()) return false;
		for (var entity : player.level().getEntities(player, player.getBoundingBox().inflate(16))) {
			if (entity instanceof net.minecraft.world.entity.Leashable leash && leash.getLeashHolder() == player) return false;
		}
		long now = player.level().getServer().overworld().getGameTime();
		return state(player).lastTravel == Long.MIN_VALUE || now - state(player).lastTravel >= 40;
	}
	public static Vec3 safePosition(ServerPlayer player, ServerLevel level, BlockPos center) {
		// Bounded Arrival: Load only the destination neighborhood, never excavate another player's build.
		for (int radius = 0; radius <= 4; radius++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
			if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
			for (int y = 0; y <= 4; y++) {
				var pos = center.offset(x, y % 2 == 0 ? y / 2 : -(y + 1) / 2, z);
				if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() <= level.getMinY() || pos.getY() + 1 >= level.getMaxY()) continue;
				level.getChunk(pos);
				var floor = level.getBlockState(pos.below());
				if (!floor.isSolidRender() || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
					|| !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
				var at = Vec3.atBottomCenterOf(pos); var box = player.getBoundingBox().move(at.subtract(player.position()));
				if (level.noCollision(player, box) && level.getEntities(player, box).isEmpty()
					&& level.players().stream().noneMatch(other -> other != player && other.getBoundingBox().intersects(box))) return at;
			}
		}
		return null;
	}
	public static boolean enter(ServerPlayer player, BlockPos gate) {
		var origin = player.level();
		if (!origin.dimension().equals(Level.OVERWORLD) || !nearby(player, gate) || !origin.getBlockState(gate).is(GATE)) return false;
		if (!safe(player)) { message(player, "unsafe"); return false; }
		if (!origin.getBlockState(gate).getValue(QuarantineGateBlock.ACTIVE)) { message(player, "key_needed"); return false; }
		if (!validFrame(origin, gate)) {
			origin.setBlockAndUpdate(gate, origin.getBlockState(gate).setValue(QuarantineGateBlock.ACTIVE, false));
			message(player, "frame"); return false;
		}
		if (!unlocked(origin)) { message(player, "dragon_locked"); return false; }
		var destination = origin.getServer().getLevel(LEVEL);
		if (destination == null) { message(player, "unavailable"); return false; }
		ForsakenLanding.prepare(destination);
		var arrival = safePosition(player, destination, LANDING);
		if (arrival == null) { message(player, "blocked"); return false; }
		var previous = state(player).entrance;
		state(player).entrance = GlobalPos.of(origin.dimension(), gate.immutable());
		if (!teleport(player, destination, arrival)) { state(player).entrance = previous; return false; }
		return true;
	}
	public static boolean leave(ServerPlayer player, BlockPos terminal) {
		if (!player.level().dimension().equals(LEVEL) || !nearby(player, terminal) || !player.level().getBlockState(terminal).is(RETURN)) return false;
		if (!safe(player)) { message(player, "unsafe"); return false; }
		var entrance = state(player).entrance;
		var destination = entrance == null ? null : player.level().getServer().getLevel(entrance.dimension());
		Vec3 arrival = destination != null && destination.dimension().equals(Level.OVERWORLD) ? safePosition(player, destination, entrance.pos()) : null;
		if (arrival == null) {
			// Extraction Recovery: Missing or obstructed gates use the native outside respawn destination.
			var fallback = TeleportTransition.createDefault(player, TeleportTransition.DO_NOTHING);
			destination = fallback.newLevel();
			if (!destination.dimension().equals(Level.OVERWORLD)) { message(player, "blocked"); return false; }
			arrival = safePosition(player, destination, BlockPos.containing(fallback.position()));
		}
		if (arrival == null) { message(player, "blocked"); return false; }
		return teleport(player, destination, arrival);
	}
	private static boolean teleport(ServerPlayer player, ServerLevel level, Vec3 pos) {
		if (player.teleport(new TeleportTransition(level, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.PLACE_PORTAL_TICKET)) == null) return false;
		player.fallDistance = 0;
		state(player).lastTravel = level.getServer().overworld().getGameTime();
		// Reuse the existing player-save accessor: inventory, dimension and extraction link persist together.
		((darkspawn.black.mixin.OneBlockSaveAccess) level.getServer().getPlayerList()).darkspawn$saveProfile(player);
		return true;
	}
	public static void message(ServerPlayer player, String key) { player.sendOverlayMessage(Component.translatable("message.darkspawn.forsaken_" + key)); }
}
