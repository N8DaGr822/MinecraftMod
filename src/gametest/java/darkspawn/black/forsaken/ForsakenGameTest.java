package darkspawn.black.forsaken;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.Darkspawn;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ForsakenGameTest {
	private static ServerPlayer player(GameTestHelper helper, BlockPos pos) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "forsaken-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5); ready(player); return player;
	}
	private static void ready(ServerPlayer player) {
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket()); player.hasChangedDimension(); player.setOnGround(true);
		Forsaken.state(player).lastTravel = Long.MIN_VALUE;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	private static BlockPos frame(GameTestHelper helper, int offset) {
		var center = helper.absolutePos(new BlockPos(1 + offset, 3, 1)); var level = helper.getLevel();
		for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
			level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
			for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		for (int x = -2; x <= 2; x++) for (int y = 0; y <= 4; y++) if (Math.abs(x) == 2 || y == 0 || y == 4) level.setBlockAndUpdate(center.offset(x, y, 0), Blocks.OBSIDIAN.defaultBlockState());
		level.setBlockAndUpdate(center, Forsaken.GATE.defaultBlockState()); return center;
	}
	private static void unlock(GameTestHelper helper) {
		helper.getLevel().getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
	}
	@GameTest public void dragonGateAndFrameAreValidatedWithoutConsumingKey(GameTestHelper helper) {
		var gate = frame(helper, 0); var player = player(helper, gate.south());
		var end = helper.getLevel().getServer().getLevel(Level.END); var before = end.getDragonFight();
		try {
			player.getInventory().setItem(0, new ItemStack(Forsaken.KEY));
			end.setDragonFight(EnderDragonFight.createDefault());
			helper.assertTrue(!Forsaken.activate(player, gate), "Before the global dragon victory a complete gate remains locked");
			unlock(helper);
			helper.assertTrue(Forsaken.activate(player, gate) && player.getInventory().getItem(0).is(Forsaken.KEY), "A supplied key activates without being consumed; research is only a crafting requirement");
			helper.getLevel().setBlockAndUpdate(gate.offset(2, 4, 0), Blocks.AIR.defaultBlockState());
			helper.assertTrue(!Forsaken.enter(player, gate) && !helper.getLevel().getBlockState(gate).getValue(QuarantineGateBlock.ACTIVE), "Broken frames deactivate and reject travel");
			helper.assertTrue(!Forsaken.activate(player, gate), "Incomplete frame rejects reactivation");
		} finally { end.setDragonFight(before); remove(player); }
		helper.succeed();
	}
	@GameTest public void archiveIsReusableAndFabricationIsAtomicAndResearchGated(GameTestHelper helper) {
		var gate = frame(helper, 0); var player = player(helper, gate.south()); var other = player(helper, gate.south(2));
		var archive = gate.west(3); var level = helper.getLevel(); level.setBlockAndUpdate(archive, Forsaken.ARCHIVE.defaultBlockState());
		try {
			var forbidden = new QuarantineMenu(50, other.getInventory(), archive);
			helper.assertTrue(!forbidden.clickMenuButton(other, 0), "Forged fabrication actions require saved research");
			var hit = new BlockHitResult(Vec3.atCenterOf(archive), Direction.UP, archive, false);
			((QuarantineTerminalBlock) Forsaken.ARCHIVE).useWithoutItem(level.getBlockState(archive), level, archive, player, hit);
			((QuarantineTerminalBlock) Forsaken.ARCHIVE).useWithoutItem(level.getBlockState(archive), level, archive, other, hit);
			helper.assertTrue(Forsaken.state(player).researched && Forsaken.state(other).researched && level.getBlockState(archive).is(Forsaken.ARCHIVE), "Both players learn without consuming the archive");
			var menu = (QuarantineMenu) player.containerMenu;
			player.getInventory().setItem(0, new ItemStack(Items.IRON_BLOCK, 2));
			helper.assertTrue(!menu.clickMenuButton(player, 0) && player.getInventory().getItem(0).getCount() == 2, "Missing ingredients consume nothing");
			int slot = 0; for (var cost : QuarantineMenu.GATE_COST) player.getInventory().setItem(slot++, new ItemStack(cost.item(), cost.count()));
			helper.assertTrue(menu.clickMenuButton(player, 0) && menu.getCarried().is(Forsaken.GATE_ITEM), "Complete payment produces one controller");
			helper.assertTrue(!menu.clickMenuButton(player, 0) && menu.getCarried().getCount() == 1, "Repeated action cannot duplicate the carried result");
			for (int i = 0; i < 4; i++) helper.assertTrue(player.getInventory().getItem(i).isEmpty(), "Ingredients consumed exactly once");
			player.closeContainer(); other.closeContainer();
			helper.assertTrue(player.getInventory().getItem(0).is(Forsaken.GATE_ITEM), "Closing the menu returns the crafted output safely");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}
	@GameTest public void twoEntrancesKeepIndependentReturnLinksAndNormalItems(GameTestHelper helper) {
		var first = frame(helper, 0); var second = frame(helper, 20);
		var player = player(helper, first.south()); var other = player(helper, second.south());
		var end = helper.getLevel().getServer().getLevel(Level.END); var before = end.getDragonFight();
		try {
			unlock(helper);
			for (var p : new ServerPlayer[] { player, other }) {
				var gate = p == player ? first : second;
				p.getInventory().setItem(0, new ItemStack(Forsaken.KEY)); Forsaken.activate(p, gate);
				p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
				p.getInventory().getItem(0).set(DataComponents.CUSTOM_NAME, Component.literal("Expedition supplies"));
				helper.assertTrue(Forsaken.enter(p, gate), "An unresearched traveler can enter an open gate"); ready(p);
				helper.assertTrue(p.level().dimension().equals(Forsaken.LEVEL) && p.getInventory().getItem(0).getCount() == 7, "Expedition travel retains items");
			}
			helper.assertTrue(Forsaken.state(player).entrance.pos().equals(first) && Forsaken.state(other).entrance.pos().equals(second), "Shared destination does not overwrite entrance ownership");
			for (var p : new ServerPlayer[] { player, other }) {
				p.teleportTo(Forsaken.EXIT.getX() + .5, Forsaken.EXIT.getY(), Forsaken.EXIT.getZ() + 1.5); ready(p);
				ForsakenContent.contaminate(p);
				helper.assertTrue(p.hasEffect(ForsakenContent.CONTAMINATION), "Exposure exists before extraction");
				helper.assertTrue(Forsaken.leave(p, Forsaken.EXIT), "Both travelers can extract");
				helper.assertTrue(!p.hasEffect(ForsakenContent.CONTAMINATION), "Successful extraction cures contamination before saving");
				var gate = p == player ? first : second;
				helper.assertTrue(p.level() == helper.getLevel() && p.position().distanceToSqr(Vec3.atCenterOf(gate)) < 40, "Each returns near their own gate");
				helper.assertTrue(p.getInventory().getItem(0).getHoverName().getString().equals("Expedition supplies"), "Item components survive round trip");
			}
		} finally { end.setDragonFight(before); remove(player); remove(other); }
		helper.succeed();
	}
	@GameTest public void researchAndEntrancePersistAndSurviveDeathCopy(GameTestHelper helper) {
		var gate = frame(helper, 0); var player = player(helper, gate.south()); var other = player(helper, gate.south(2));
		try {
			Forsaken.state(player).researched = true; Forsaken.state(player).entrance = GlobalPos.of(Level.OVERWORLD, gate);
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess()); player.saveWithoutId(output);
			Forsaken.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING, other.registryAccess(), output.buildResult()));
			helper.assertTrue(Forsaken.state(other).researched && Forsaken.state(other).entrance.equals(Forsaken.state(player).entrance), "Player save preserves permanent knowledge and gate identity");
			Forsaken.state(other).researched = false;
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, other, false);
			helper.assertTrue(Forsaken.state(other).researched && Forsaken.state(other).entrance.pos().equals(gate), "Death keeps knowledge and return link without modifying inventory policy");
			helper.assertTrue(output.buildResult().getIntOr("darkspawn_forsaken_version", 0) == 1, "Persistent state has an explicit schema version");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}
	@GameTest public void blockedArrivalsDoNotExcavateAndLandingDoesNotReset(GameTestHelper helper) {
		var gate = frame(helper, 0); var player = player(helper, gate.south());
		try {
			var level = helper.getLevel(); var center = gate.east(30);
			for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -3; y <= 4; y++) level.setBlockAndUpdate(center.offset(x, y, z), Blocks.STONE.defaultBlockState());
			helper.assertTrue(Forsaken.safePosition(player, level, center) == null && level.getBlockState(center).is(Blocks.STONE), "Blocked arrival fails without breaking construction");
			var city = level.getServer().getLevel(Forsaken.LEVEL); ForsakenLanding.prepare(city);
			var custom = Forsaken.LANDING.offset(5, 0, 0); var original = city.getBlockState(custom);
			city.setBlockAndUpdate(custom, Blocks.GOLD_BLOCK.defaultBlockState()); ForsakenLanding.prepare(city);
			helper.assertTrue(city.getBlockState(custom).is(Blocks.GOLD_BLOCK) && city.getBlockState(Forsaken.EXIT).is(Forsaken.RETURN), "Subsequent arrivals preserve the initialized landing");
			city.setBlockAndUpdate(custom, original);
			var data = city.getDataStorage().computeIfAbsent(ForsakenLanding.TYPE);
			var encoded = ForsakenLanding.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
			helper.assertTrue(ForsakenLanding.CODEC.encodeStart(JsonOps.INSTANCE, ForsakenLanding.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow()).getOrThrow().equals(encoded), "Landing state survives restart serialization");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void bunkerTemplatesRetainResearchGateAndLootAcrossRotations(GameTestHelper helper) {
		var manager = helper.getLevel().getStructureTemplateManager();
		for (int variant = 0; variant < 3; variant++) for (var rotation : Rotation.values()) {
			var template = manager.getOrCreate(Darkspawn.id("landmarks/research_bunker_" + variant));
			var settings = new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(12, 0, 12));
			helper.assertTrue(template.getSize().getX() == 25, "Bunker template loads with its bounded footprint");
			helper.assertTrue(template.filterBlocks(BlockPos.ZERO, settings, Forsaken.ARCHIVE).size() == 1, "Every bunker includes recoverable research");
			var gates = template.filterBlocks(BlockPos.ZERO, settings, Forsaken.GATE);
			helper.assertTrue(gates.size() == 1 && !gates.getFirst().state().getValue(QuarantineGateBlock.ACTIVE), "Every bunker starts with an inactive gate");
			var chests = template.filterBlocks(BlockPos.ZERO, settings, Blocks.CHEST);
			helper.assertTrue(chests.size() == 1 && chests.getFirst().nbt().getStringOr("LootTable", "").equals("darkspawn:chests/research_bunker"), "Bunker supplies use lazy native loot");
		}
		helper.succeed();
	}
	@GameTest public void missingEntranceFallsBackToOutsideSpawn(GameTestHelper helper) {
		var gate = frame(helper, 0); var player = player(helper, gate.south());
		try {
			var city = helper.getLevel().getServer().getLevel(Forsaken.LEVEL); ForsakenLanding.prepare(city);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(city, Vec3.atBottomCenterOf(Forsaken.EXIT.south()), Vec3.ZERO, 0, 0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING)); ready(player);
			Forsaken.state(player).entrance = GlobalPos.of(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Darkspawn.id("missing_gate_world")), gate);
			helper.assertTrue(Forsaken.leave(player, Forsaken.EXIT) && player.level().dimension().equals(Level.OVERWORLD), "A missing saved dimension permits safe extraction to the native outside spawn");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void placedBunkerCanBeRepairedAndContainsInstructionBook(GameTestHelper helper) {
		var level = helper.getLevel(); var origin = helper.absolutePos(new BlockPos(1024, 5, 1024));
		var piece = new darkspawn.black.worldgen.BossLandmarkStructure.LandmarkPiece(level.getStructureTemplateManager(), Darkspawn.id("landmarks/research_bunker_0"), origin, Rotation.CLOCKWISE_90);
		var bounds = piece.getBoundingBox();
		for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) level.getChunk(x, z);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(1), bounds, new net.minecraft.world.level.ChunkPos(origin.getX() >> 4, origin.getZ() >> 4), origin);
		var gate = piece.template().filterBlocks(origin, piece.placeSettings(), Forsaken.GATE).getFirst().pos();
		helper.assertTrue(!Forsaken.validFrame(level, gate), "Placed bunker gate starts damaged");
		var right = level.getBlockState(gate).getValue(QuarantineGateBlock.FACING).getClockWise();
		int repaired = 0;
		for (int x : new int[] {-2, 2}) { var corner = gate.relative(right, x).above(4); if (level.isEmptyBlock(corner)) { level.setBlockAndUpdate(corner, Blocks.OBSIDIAN.defaultBlockState()); repaired++; } }
		helper.assertTrue(repaired == 1 && Forsaken.validFrame(level, gate), "One obsidian repairs the rotated frame using its actual orientation");
		var chestPos = piece.template().filterBlocks(origin, piece.placeSettings(), Blocks.CHEST).getFirst().pos();
		var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestPos);
		chest.unpackLootTable(null);
		int books = 0; for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).is(Items.WRITTEN_BOOK)) {
			books++; helper.assertTrue(chest.getItem(slot).get(DataComponents.WRITTEN_BOOK_CONTENT).pages().size() == 5, "Instruction book explains research, repairs, materials and extraction");
		}
		helper.assertTrue(books == 1, "Every bunker supplies one instruction book");
		helper.succeed();
	}
}
