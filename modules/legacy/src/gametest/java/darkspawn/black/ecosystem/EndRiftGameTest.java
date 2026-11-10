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

public final class EndRiftGameTest {
	private static int fixtureIndex;
	private static final class Fixture implements AutoCloseable {
		final GameTestHelper h;
        final net.minecraft.server.level.ServerLevel level;
		final BlockPos center;
		final List<ServerPlayer> players = new ArrayList<>();
		final List<String> messages = new ArrayList<>();
		final EnderDragonFight dragon;
		final Difficulty difficulty;
		final boolean enabled;
		Fixture(GameTestHelper h) {
			this.h = h;
            level = h.getLevel().getServer().getLevel(Level.END);
			center = h.absolutePos(new BlockPos(256 * fixtureIndex++, 80, 0));
			dragon = level.getServer().getLevel(Level.END).getDragonFight();
			difficulty = level.getDifficulty(); enabled = ServerTuning.regionalEvents;
			ServerTuning.regionalEvents = true;
			level.getServer().setDifficulty(Difficulty.NORMAL, true);
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++) {
				level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.END_STONE.defaultBlockState());
				for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
			}
            // Fixture Chunks: Entity queries require accessible chunks, beyond block-data loading.
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) level.setChunkForced((center.getX()>>4)+x,(center.getZ()>>4)+z,true);
            level.getChunkSource().tick(() -> true,false);
            for (int tasks=0;tasks<10000 && level.getChunkSource().pollTask();tasks++) { }
			// Fixture Biome: Cover interpolation neighbors while staying below the native fill volume limit.
			var fill = FillBiomeCommand.fill(level, center.offset(-12, -8, -12), center.offset(12, 8, 12),
				level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.END_HIGHLANDS));
			h.assertTrue(fill.left().isPresent() || level.getBiome(center).is(Biomes.END_HIGHLANDS), "Fixture biome fill: " + fill);
		}
        void enable() {
            ServerTuning.regionalEvents=true;
            level.getServer().setDifficulty(Difficulty.NORMAL,true);
			level.getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
            var end=level.getServer().getLevel(Level.END);
            end.getDragonFight().init(end, 0L, BlockPos.ZERO);
        }
		ServerPlayer player() {
			var level = this.level;
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
			p.teleport(new net.minecraft.world.level.portal.TeleportTransition(level, pos, Vec3.ZERO, 0, 0, net.minecraft.world.level.portal.TeleportTransition.PLACE_PORTAL_TICKET)); p.setOnGround(true);
			p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
			players.add(p);
			h.assertTrue(p.blockPosition().equals(BlockPos.containing(pos)), "Fixture player reaches clearing: " + p.position());
			h.assertTrue(level.getBiome(p.blockPosition()).is(Biomes.END_HIGHLANDS), "Fixture has taiga biome");
			h.assertTrue(p.isAlive() && !p.isCreative() && !p.isSpectator(), "Fixture is alive in Survival");
			if (players.size() == 1) {
				var site = center.offset(12, 0, 0);
				h.assertTrue(level.hasChunkAt(site), "Fixture spawn chunk loaded");
				h.assertTrue(level.getBlockState(site.below()).is(Blocks.END_STONE), "Fixture spawn site has grass ground");
				var probe = new RegionalMob(RegionalEcosystems.MOBS.get(RegionalKind.VOIDLING), level, RegionalKind.VOIDLING);
				probe.setPos(Vec3.atBottomCenterOf(site));
				h.assertTrue(level.noCollision(probe), "Fixture spawn site has no collision: " + probe.getBoundingBox());
				h.assertTrue(level.getWorldBorder().isWithinBounds(probe.getBoundingBox()), "Fixture spawn site inside border");
				probe.discard();
			}
			return p;
		}
		List<RegionalMob> sporelings() {
			return level.getEntitiesOfClass(RegionalMob.class, new AABB(center).inflate(32), RegionalEncounters.RIFT::combatant);
		}
		@Override public void close() {
			for (var p : players) {
				RegionalEncounters.RIFT.leave(p);
				if (level.getServer().getPlayerList().getPlayer(p.getUUID()) != null) level.getServer().getPlayerList().remove(p);
				p.discard();
			}
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) level.setChunkForced((center.getX()>>4)+x,(center.getZ()>>4)+z,false);
			level.getServer().getLevel(Level.END).setDragonFight(dragon);
			level.getServer().setDifficulty(difficulty, true); ServerTuning.regionalEvents = enabled;
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
				level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.AIR.defaultBlockState());
		}
	}
    private static int claim(ServerPlayer p) {
        p.teleport(new net.minecraft.world.level.portal.TeleportTransition(p.level().getServer().overworld(), p.position(), Vec3.ZERO, 0, 0,
            net.minecraft.world.level.portal.TeleportTransition.PLACE_PORTAL_TICKET));
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        return RegionalEncounters.RIFT.claim(p);
    }
	@GameTest(maxTicks=100)
	public void soloIsOptInProtectsBystandersAndPaysOnce(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var p = f.player(); var bystander = f.player();
			RegionalEncounters.RIFT.offer(p);
			h.assertTrue(f.sporelings().isEmpty(), "Offer never spawns an encounter");
			h.assertTrue(RegionalEncounters.RIFT.form(p, false) == 1 && f.sporelings().size() == 3, "Solo starts exactly three sporelings: " + f.messages);
			var first = f.sporelings().getFirst();
			first.setTarget(bystander);
			h.assertTrue(first.getTarget() == null && !first.canAttack(bystander), "Native AI cannot target nonparticipants");
			h.assertTrue(!first.hurtServer(f.level, bystander.damageSources().playerAttack(bystander), 1000), "Outsiders cannot kill hunt sporelings");
			h.assertTrue(!first.doHurtTarget(f.level, bystander), "Direct melee cannot bypass participation");
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF, 4));
			for (var sporeling : List.copyOf(f.sporelings())) sporeling.hurtServer(f.level, p.damageSources().playerAttack(p), 1000);
			h.assertTrue(f.sporelings().isEmpty() && RegionalEncounters.RIFT.ledger(f.level).reward(p.getUUID()), "Victory cleans the pack and saves the claim");
			h.assertTrue(!RegionalEncounters.RIFT.ledger(f.level).reward(bystander.getUUID()), "Bystanders receive no reward");
			h.assertTrue(f.level.getEntitiesOfClass(ItemEntity.class, new AABB(f.center).inflate(32)).isEmpty(), "Hunt sporelings drop no ordinary loot");
			h.assertTrue(claim(p) == 1 && p.getInventory().countItem(Items.CHORUS_FRUIT) == 4, "Exact reward delivered");
			h.assertTrue(claim(p) == 0 && p.getInventory().countItem(Items.CHORUS_FRUIT) == 4, "Repeated claim cannot duplicate");
			h.assertTrue(RegionalEncounters.RIFT.form(p, false) == 0 && f.sporelings().isEmpty(), "Started cooldown prevents immediate repeat");
		}
		h.succeed();
        });
	}
	@GameTest(maxTicks=100)
	public void cooperativeRequiresOptInCapsPartyAndRewardsEachMember(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var leader = f.player(); var second = f.player(); var third = f.player(); var fourth = f.player(); var outsider = f.player();
			RegionalEncounters.RIFT.form(leader, true);
			h.assertTrue(RegionalEncounters.RIFT.start(leader) == 0 && f.sporelings().isEmpty(), "Cooperative hunts require two players");
			RegionalEncounters.RIFT.join(second, leader); RegionalEncounters.RIFT.join(third, leader); RegionalEncounters.RIFT.join(fourth, leader); RegionalEncounters.RIFT.join(outsider, leader);
			h.assertTrue(RegionalEncounters.RIFT.start(second) == 0, "Only the leader starts");
			h.assertTrue(RegionalEncounters.RIFT.start(leader) == 1 && f.sporelings().size() == 6, "Four participants still spawn only six sporelings; found " + f.sporelings().size() + ": " + f.messages);
			for (var sporeling : List.copyOf(f.sporelings())) {
				h.assertTrue(RegionalEncounters.RIFT.canTarget(sporeling, second) && !RegionalEncounters.RIFT.canTarget(sporeling, outsider), "Fifth player is excluded");
				sporeling.hurtServer(f.level, second.damageSources().playerAttack(second), 1000);
			}
			for (var p : List.of(leader, second, third, fourth))
				h.assertTrue(claim(p) == 1 && p.getInventory().countItem(Items.CHORUS_FRUIT) == 4, "Every opted-in supporter earns one fixed reward");
			h.assertTrue(claim(outsider) == 0, "Party overflow cannot claim");
		}
		h.succeed();
        });
	}
	@GameTest(maxTicks=100)
	public void invalidTerrainAndProgressionNeverChargeOrSpawn(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var p = f.player();
			f.level.getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.createDefault());
			h.assertTrue(RegionalEncounters.RIFT.form(p, false) == 0 && f.sporelings().isEmpty(), "Dragon gate applies to manual starts");
			f.level.getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
				f.level.setBlockAndUpdate(f.center.offset(x, -1, z), Blocks.GLASS.defaultBlockState());
			h.assertTrue(RegionalEncounters.RIFT.form(p, false) == 0 && f.sporelings().isEmpty(), "Pack validation is all-or-nothing on invalid ground");
			h.assertTrue(RegionalEncounters.RIFT.ledger(f.level).ready(p.getUUID(), f.level.getGameTime()), "Failed start charges no cooldown");
			ServerTuning.regionalEvents = false;
			h.assertTrue(RegionalEncounters.RIFT.form(p, true) == 0, "Disabled event setting prevents lobbies");
		}
		h.succeed();
        });
	}
	@GameTest(maxTicks=100)
	public void disconnectCancelsPartyAndReloadedSporelingsAreDiscarded(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var leader = f.player(); var member = f.player();
			RegionalEncounters.RIFT.form(leader, true); RegionalEncounters.RIFT.join(member, leader);
			h.assertTrue(RegionalEncounters.RIFT.start(leader) == 1, "Cooperative fixture starts: " + f.messages);
			var sporeling = f.sporelings().getFirst();
			var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, f.level.registryAccess());
			sporeling.saveWithoutId(out);
			f.level.getServer().getPlayerList().remove(member);
			RegionalEncounters.RIFT.tick(f.level);
			h.assertTrue(f.sporelings().isEmpty() && !RegionalEncounters.RIFT.ledger(f.level).reward(leader.getUUID()), "Disconnect cancels the group with no reward");
			h.assertTrue(!RegionalEncounters.RIFT.ledger(f.level).ready(leader.getUUID(), f.level.getGameTime()), "Interruption preserves spent cooldown");
			var copy = new RegionalMob(RegionalEcosystems.MOBS.get(RegionalKind.VOIDLING), f.level, RegionalKind.VOIDLING);
			copy.load(TagValueInput.create(ProblemReporter.DISCARDING, f.level.registryAccess(), out.buildResult()));
			f.level.addFreshEntity(copy);
			h.assertTrue(copy.isRemoved(), "Orphan hunt sporeling cannot survive restart/unload");
		}
		h.succeed();
        });
	}
	@GameTest(maxTicks=100)
	public void boundaryDeathAndOptOutCancelWithoutReward(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			for (int reason = 0; reason < 3; reason++) {
				var p = f.player();
				if (reason == 1) for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
					f.level.setBlockAndUpdate(f.center.offset(x, -1, z), Blocks.END_STONE.defaultBlockState());
				h.assertTrue(RegionalEncounters.RIFT.form(p, false) == 1, "Cancellation fixture starts: " + f.messages);
				if (reason == 0) p.setPos(Vec3.atBottomCenterOf(f.center).add(49, 0, 0));
				else if (reason == 1) p.setHealth(0);
				else RegionalEncounters.RIFT.leave(p);
				RegionalEncounters.RIFT.tick(f.level);
				h.assertTrue(f.sporelings().isEmpty() && !RegionalEncounters.RIFT.ledger(f.level).reward(p.getUUID()),
					"Boundary, death and opt-out each clean the pack without rewards");
				h.assertTrue(!RegionalEncounters.RIFT.ledger(f.level).ready(p.getUUID(), f.level.getGameTime()),
					"Abandoning a started hunt preserves its cooldown");
			}
		}
		h.succeed();
        });
	}
	@GameTest(maxTicks=100)
	public void fullInventoryPreservesClaimAndIslandProfileCannotClaim(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var p = f.player(); var saved = RegionalEncounters.RIFT.ledger(f.level);
			saved.won(p.getUUID());
			for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
			h.assertTrue(claim(p) == 0 && saved.reward(p.getUUID()), "Full inventory retains entitlement");
			p.getInventory().setItem(9, ItemStack.EMPTY);
			var state = darkspawn.black.oneblock.OneBlock.state(p);
			state.ascensionInside = true;
			h.assertTrue(claim(p) == 0 && saved.reward(p.getUUID()), "Active island possessions cannot receive outside rewards");
			state.ascensionInside = false;
			h.assertTrue(claim(p) == 1 && p.getInventory().getItem(9).is(Items.CHORUS_FRUIT), "Recovered outside profile can claim once");
		}
		h.succeed();
        });
	}
}
