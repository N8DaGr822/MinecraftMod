package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
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

public final class Pirate {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Darkspawn.id("pirate_seas"));
	public static final BlockPos LANDING = new BlockPos(0, 65, 0);
	public static final BlockPos EXIT = new BlockPos(0, 65, -4);
	public static final Block GATE = block("sea_gate", false);
	public static final Block RETURN = block("harbor_return", true);
	public static final Item GATE_ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("sea_gate"), new BlockItem(GATE,
		new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("sea_gate"))).useBlockDescriptionPrefix()));
	private Pirate() { }
	private static Block block(String name, boolean extraction) {
		return Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id(name), new SeaGateBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id(name))).strength(extraction ? -1 : 4, 3600000)
			.sound(SoundType.METAL).lightLevel(state -> 12).pushReaction(PushReaction.IMMOVEABLE), extraction));
	}
	public static PiratePlayerState state(ServerPlayer player) { return ((PiratePlayer) player).darkspawn$pirate(); }
	public static void initialize() {
		PirateIslandStructure.initialize();
		Ships.initialize();
		PirateDrinks.initialize();
        SeaMenu.initialize();SeaTerritory.initialize();SevenSeals.initialize();
		LogboundCompass.initialize();
		PartyNavigation.initialize();
		HarborGuide.initialize();
		IslandDiscoveries.initialize();
		PirateActivities.initialize();
		PirateDiplomacy.initialize();
		FactionSkirmishes.initialize();
		ServerPlayerEvents.COPY_FROM.register((old, next, alive) -> state(next).copyFrom(state(old)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(GATE_ITEM));
	}
	private static boolean nearby(ServerPlayer player, BlockPos pos) { return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 36; }
	private static boolean safe(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || !player.onGround() || player.isPassenger() || player.isVehicle()
			|| player.isSleeping() || player.isFallFlying() || player.isUsingItem() || player.isOnFire() || player.isInWater()
			|| player.hurtTime > 0 || player.isChangingDimension() || !player.connection.hasClientLoaded()
			|| player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
			|| player.inventoryMenu.getSlot(darkspawn.black.inventory.InventoryExpansion.TRASH_MENU_SLOT).hasItem()
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
		if (!origin.dimension().equals(Level.OVERWORLD) || !nearby(player, gate) || !origin.getBlockState(gate).is(GATE)) {
			message(player, "overworld"); return false;
		}
		if (!safe(player)) { message(player, "unsafe"); return false; }
		var destination = origin.getServer().getLevel(LEVEL);
		if (destination == null) { message(player, "unavailable"); return false; }
		PirateHarbor.prepare(destination);
		var arrival = safePosition(player, destination, LANDING);
		if (arrival == null) { message(player, "blocked"); return false; }
		var previous = state(player).entrance;
		state(player).entrance = GlobalPos.of(origin.dimension(), gate.immutable());
		if (!teleport(player, destination, arrival)) { state(player).entrance = previous; return false; }
		message(player, "welcome"); return true;
	}
	public static boolean leave(ServerPlayer player, BlockPos terminal) {
		if (!player.level().dimension().equals(LEVEL) || !nearby(player, terminal) || !player.level().getBlockState(terminal).is(RETURN)) return false;
		if (!safe(player)) { message(player, "unsafe"); return false; }
		var entrance = state(player).entrance;
		var destination = entrance == null ? null : player.level().getServer().getLevel(entrance.dimension());
		Vec3 arrival = destination != null && destination.dimension().equals(Level.OVERWORLD) ? safePosition(player, destination, entrance.pos()) : null;
		if (arrival == null) {
			// Harbor Recovery: A lost or obstructed entrance falls back to the Overworld spawn.
			destination = player.level().getServer().overworld();
			arrival = safePosition(player, destination, destination.getRespawnData().pos());
		}
		if (arrival == null) { message(player, "blocked"); return false; }
		return teleport(player, destination, arrival);
	}
	private static boolean teleport(ServerPlayer player, ServerLevel level, Vec3 pos) {
		if (player.teleport(new TeleportTransition(level, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.PLACE_PORTAL_TICKET)) == null) return false;
		player.fallDistance = 0;
		state(player).lastTravel = level.getServer().overworld().getGameTime();
		((darkspawn.black.mixin.OneBlockSaveAccess) level.getServer().getPlayerList()).darkspawn$saveProfile(player);
		return true;
	}
	public static void message(ServerPlayer player, String key) { player.sendOverlayMessage(Component.translatable("message.darkspawn.pirate_" + key)); }
}
