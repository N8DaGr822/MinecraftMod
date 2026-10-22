package darkspawn.black.ecosystem;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.ServerTuning;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WolfHuntGameTest {
	private static final class Fixture implements AutoCloseable {
		final GameTestHelper h;
		final BlockPos center;
		final List<ServerPlayer> players = new ArrayList<>();
		final List<String> messages = new ArrayList<>();
		final EnderDragonFight dragon;
		final Difficulty difficulty;
		final boolean enabled;
		Fixture(GameTestHelper h) {
			this.h = h;
			center = h.absolutePos(new BlockPos(0, 80, 0));
			dragon = h.getLevel().getServer().getLevel(Level.END).getDragonFight();
			difficulty = h.getLevel().getDifficulty(); enabled = ServerTuning.regionalEvents;
			ServerTuning.regionalEvents = true;
			h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
			h.getLevel().getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++) {
				h.getLevel().setBlockAndUpdate(center.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
				for (int y = 0; y <= 4; y++) h.getLevel().setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
			}
			// Fixture Biome: Cover interpolation neighbors while staying below the native fill volume limit.
			var fill = FillBiomeCommand.fill(h.getLevel(), center.offset(-12, -8, -12), center.offset(12, 8, 12),
				h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.TAIGA));
			h.assertTrue(fill.left().isPresent() || h.getLevel().getBiome(center).is(Biomes.TAIGA), "Fixture biome fill: " + fill);
		}
		ServerPlayer player() {
			var level = h.getLevel();
			var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "hunt-" + players.size()), false);
			var p = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
				@Override public GameType gameMode() { return GameType.SURVIVAL; }
				@Override public boolean isClientAuthoritative() { return false; }
				@Override public void sendSystemMessage(net.minecraft.network.chat.Component text) { messages.add(text.getString()); }
			};
			var connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
			level.getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
			GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());
			var pos = Vec3.atBottomCenterOf(center).add(players.size(), 0, 0);
			p.teleportTo(pos.x, pos.y, pos.z); p.setOnGround(true);
			p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
			players.add(p);
			h.assertTrue(p.blockPosition().equals(BlockPos.containing(pos)), "Fixture player reaches clearing: " + p.position());
			h.assertTrue(level.getBiome(p.blockPosition()).is(darkspawn.black.boss.MutantWolfSummonItem.TAIGAS), "Fixture has taiga biome");
			h.assertTrue(level.canSeeSky(p.blockPosition()), "Fixture has open sky");
			h.assertTrue(p.isAlive() && !p.isCreative() && !p.isSpectator(), "Fixture is alive in Survival");
			if (players.size() == 1) {
				var site = center.offset(12, 0, 0);
				h.assertTrue(level.hasChunkAt(site), "Fixture spawn chunk loaded");
				h.assertTrue(level.canSeeSky(site), "Fixture spawn site has open sky");
				h.assertTrue(level.getBlockState(site.below()).is(Blocks.GRASS_BLOCK), "Fixture spawn site has grass ground");
				var probe = new TaigaWolf(TaigaEcosystem.WOLVES.get(TaigaWolf.Kind.DIRE), level, TaigaWolf.Kind.DIRE);
				probe.setPos(Vec3.atBottomCenterOf(site));
				h.assertTrue(level.noCollision(probe), "Fixture spawn site has no collision: " + probe.getBoundingBox());
				h.assertTrue(level.getWorldBorder().isWithinBounds(probe.getBoundingBox()), "Fixture spawn site inside border");
				probe.discard();
			}
			return p;
		}
		List<TaigaWolf> wolves() {
			return h.getLevel().getEntitiesOfClass(TaigaWolf.class, new AABB(center).inflate(32), WolfHunt::combatant);
		}
		@Override public void close() {
			for (var p : players) {
				WolfHunt.leave(p);
				if (h.getLevel().getServer().getPlayerList().getPlayer(p.getUUID()) != null) h.getLevel().getServer().getPlayerList().remove(p);
				p.discard();
			}
			h.getLevel().getServer().getLevel(Level.END).setDragonFight(dragon);
			h.getLevel().getServer().setDifficulty(difficulty, true); ServerTuning.regionalEvents = enabled;
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
				h.getLevel().setBlockAndUpdate(center.offset(x, -1, z), Blocks.AIR.defaultBlockState());
		}
	}
	@GameTest
	public void soloIsOptInProtectsBystandersAndPaysOnce(GameTestHelper h) {
		try (var f = new Fixture(h)) {
			var p = f.player(); var bystander = f.player();
			WolfHunt.offer(p);
			h.assertTrue(f.wolves().isEmpty(), "Offer never spawns an encounter");
			h.assertTrue(WolfHunt.form(p, false) == 1 && f.wolves().size() == 3, "Solo starts exactly three wolves: " + f.messages);
			var first = f.wolves().getFirst();
			first.setTarget(bystander);
			h.assertTrue(first.getTarget() == null && !first.canAttack(bystander), "Native AI cannot target nonparticipants");
			h.assertTrue(!first.hurtServer(h.getLevel(), bystander.damageSources().playerAttack(bystander), 1000), "Outsiders cannot kill hunt wolves");
			h.assertTrue(!first.doHurtTarget(h.getLevel(), bystander), "Direct melee cannot bypass participation");
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF, 4));
			h.assertTrue(!first.tame(p) && p.getMainHandItem().getCount() == 4, "Event wolves cannot be captured");
			for (var wolf : List.copyOf(f.wolves())) wolf.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 1000);
			h.assertTrue(f.wolves().isEmpty() && WolfHuntLedger.get(h.getLevel()).reward(p.getUUID()), "Victory cleans the pack and saves the claim");
			h.assertTrue(!WolfHuntLedger.get(h.getLevel()).reward(bystander.getUUID()), "Bystanders receive no reward");
			h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(f.center).inflate(32)).isEmpty(), "Hunt wolves drop no ordinary loot");
			h.assertTrue(WolfHunt.claim(p) == 1 && p.getInventory().countItem(Items.EMERALD) == 4, "Exact reward delivered");
			h.assertTrue(WolfHunt.claim(p) == 0 && p.getInventory().countItem(Items.EMERALD) == 4, "Repeated claim cannot duplicate");
			h.assertTrue(WolfHunt.form(p, false) == 0 && f.wolves().isEmpty(), "Started cooldown prevents immediate repeat");
		}
		h.succeed();
	}
	@GameTest
	public void cooperativeRequiresOptInCapsPartyAndRewardsEachMember(GameTestHelper h) {
		try (var f = new Fixture(h)) {
			var leader = f.player(); var second = f.player(); var third = f.player(); var fourth = f.player(); var outsider = f.player();
			WolfHunt.form(leader, true);
			h.assertTrue(WolfHunt.start(leader) == 0 && f.wolves().isEmpty(), "Cooperative hunts require two players");
			WolfHunt.join(second, leader); WolfHunt.join(third, leader); WolfHunt.join(fourth, leader); WolfHunt.join(outsider, leader);
			h.assertTrue(WolfHunt.start(second) == 0, "Only the leader starts");
			h.assertTrue(WolfHunt.start(leader) == 1 && f.wolves().size() == 6, "Four participants still spawn only six wolves: " + f.messages);
			for (var wolf : List.copyOf(f.wolves())) {
				h.assertTrue(WolfHunt.canTarget(wolf, second) && !WolfHunt.canTarget(wolf, outsider), "Fifth player is excluded");
				wolf.hurtServer(h.getLevel(), second.damageSources().playerAttack(second), 1000);
			}
			for (var p : List.of(leader, second, third, fourth))
				h.assertTrue(WolfHunt.claim(p) == 1 && p.getInventory().countItem(Items.EMERALD) == 4, "Every opted-in supporter earns one fixed reward");
			h.assertTrue(WolfHunt.claim(outsider) == 0, "Party overflow cannot claim");
		}
		h.succeed();
	}
	@GameTest
	public void invalidTerrainAndProgressionNeverChargeOrSpawn(GameTestHelper h) {
		try (var f = new Fixture(h)) {
			var p = f.player();
			h.getLevel().getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.createDefault());
			h.assertTrue(WolfHunt.form(p, false) == 0 && f.wolves().isEmpty(), "Dragon gate applies to manual starts");
			h.getLevel().getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
				h.getLevel().setBlockAndUpdate(f.center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
			h.assertTrue(WolfHunt.form(p, false) == 0 && f.wolves().isEmpty(), "Pack validation is all-or-nothing on invalid ground");
			h.assertTrue(WolfHuntLedger.get(h.getLevel()).ready(p.getUUID(), h.getLevel().getGameTime()), "Failed start charges no cooldown");
			ServerTuning.regionalEvents = false;
			h.assertTrue(WolfHunt.form(p, true) == 0, "Disabled event setting prevents lobbies");
		}
		h.succeed();
	}
	@GameTest
	public void disconnectCancelsPartyAndReloadedWolvesAreDiscarded(GameTestHelper h) {
		try (var f = new Fixture(h)) {
			var leader = f.player(); var member = f.player();
			WolfHunt.form(leader, true); WolfHunt.join(member, leader);
			h.assertTrue(WolfHunt.start(leader) == 1, "Cooperative fixture starts: " + f.messages);
			var wolf = f.wolves().getFirst();
			var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
			wolf.saveWithoutId(out);
			h.getLevel().getServer().getPlayerList().remove(member);
			WolfHunt.tick(h.getLevel());
			h.assertTrue(f.wolves().isEmpty() && !WolfHuntLedger.get(h.getLevel()).reward(leader.getUUID()), "Disconnect cancels the group with no reward");
			h.assertTrue(!WolfHuntLedger.get(h.getLevel()).ready(leader.getUUID(), h.getLevel().getGameTime()), "Interruption preserves spent cooldown");
			var copy = new TaigaWolf(TaigaEcosystem.WOLVES.get(TaigaWolf.Kind.DIRE), h.getLevel(), TaigaWolf.Kind.DIRE);
			copy.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
			h.getLevel().addFreshEntity(copy);
			h.assertTrue(copy.isRemoved(), "Orphan hunt wolf cannot survive restart/unload");
		}
		h.succeed();
	}
	@GameTest
	public void boundaryDeathAndOptOutCancelWithoutReward(GameTestHelper h) {
		try (var f = new Fixture(h)) {
			for (int reason = 0; reason < 3; reason++) {
				var p = f.player();
				if (reason == 1) for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
					h.getLevel().setBlockAndUpdate(f.center.offset(x, -1, z), Blocks.PODZOL.defaultBlockState());
				h.assertTrue(WolfHunt.form(p, false) == 1, "Cancellation fixture starts: " + f.messages);
				if (reason == 0) p.setPos(Vec3.atBottomCenterOf(f.center).add(49, 0, 0));
				else if (reason == 1) p.setHealth(0);
				else WolfHunt.leave(p);
				WolfHunt.tick(h.getLevel());
				h.assertTrue(f.wolves().isEmpty() && !WolfHuntLedger.get(h.getLevel()).reward(p.getUUID()),
					"Boundary, death and opt-out each clean the pack without rewards");
				h.assertTrue(!WolfHuntLedger.get(h.getLevel()).ready(p.getUUID(), h.getLevel().getGameTime()),
					"Abandoning a started hunt preserves its cooldown");
			}
		}
		h.succeed();
	}
	@GameTest
	public void fullInventoryPreservesClaimAndIslandProfileCannotClaim(GameTestHelper h) {
		try (var f = new Fixture(h)) {
			var p = f.player(); var saved = WolfHuntLedger.get(h.getLevel());
			saved.won(p.getUUID());
			for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
			h.assertTrue(WolfHunt.claim(p) == 0 && saved.reward(p.getUUID()), "Full inventory retains entitlement");
			p.getInventory().setItem(9, ItemStack.EMPTY);
			var state = darkspawn.black.oneblock.OneBlock.state(p);
			state.ascensionInside = true;
			h.assertTrue(WolfHunt.claim(p) == 0 && saved.reward(p.getUUID()), "Active island possessions cannot receive outside rewards");
			state.ascensionInside = false;
			h.assertTrue(WolfHunt.claim(p) == 1 && p.getInventory().getItem(9).is(Items.EMERALD), "Recovered outside profile can claim once");
		}
		h.succeed();
	}
}
