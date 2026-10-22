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

public final class ShadowIncursionGameTest {
	private static int fixtureIndex;
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
            // Fixture Isolation: Keep this clearing away from concurrent Fungal Bloom terrain and cleanup.
			center = h.absolutePos(new BlockPos(256 * fixtureIndex++, 80, 4096));
			dragon = h.getLevel().getServer().getLevel(Level.END).getDragonFight();
			difficulty = h.getLevel().getDifficulty(); enabled = ServerTuning.regionalEvents;
			ServerTuning.regionalEvents = true;
			h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++) {
				h.getLevel().setBlockAndUpdate(center.offset(x, -1, z), Blocks.SCULK.defaultBlockState());
				for (int y = 0; y <= 4; y++) h.getLevel().setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
			}
            // Fixture Chunks: Entity queries require accessible chunks, beyond block-data loading.
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) h.getLevel().setChunkForced((center.getX()>>4)+x,(center.getZ()>>4)+z,true);
            h.getLevel().getChunkSource().tick(() -> true,false);
            for (int tasks=0;tasks<10000 && h.getLevel().getChunkSource().pollTask();tasks++) { }
			// Fixture Biome: Cover interpolation neighbors while staying below the native fill volume limit.
			var fill = FillBiomeCommand.fill(h.getLevel(), center.offset(-12, -8, -12), center.offset(12, 8, 12),
				h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DEEP_DARK));
			h.assertTrue(fill.left().isPresent() || h.getLevel().getBiome(center).is(Biomes.DEEP_DARK), "Fixture biome fill: " + fill);
		}
        void enable() {
            ServerTuning.regionalEvents=true;
            h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);
			h.getLevel().getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
            var end=h.getLevel().getServer().getLevel(Level.END);
            end.getDragonFight().init(end, 0L, BlockPos.ZERO);
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
			h.assertTrue(level.getBiome(p.blockPosition()).is(Biomes.DEEP_DARK), "Fixture has deep-dark biome");
			h.assertTrue(level.canSeeSky(p.blockPosition()), "Fixture has open sky");
			h.assertTrue(p.isAlive() && !p.isCreative() && !p.isSpectator(), "Fixture is alive in Survival");
			if (players.size() == 1) {
				var site = center.offset(12, 0, 0);
				h.assertTrue(level.hasChunkAt(site), "Fixture spawn chunk loaded");
				h.assertTrue(level.canSeeSky(site), "Fixture spawn site has open sky");
				h.assertTrue(level.getBlockState(site.below()).is(Blocks.SCULK), "Fixture spawn site has sculk ground");
				var probe = new RegionalMob(RegionalEcosystems.MOBS.get(RegionalKind.SHADOW_DRONE), level, RegionalKind.SHADOW_DRONE);
				probe.setPos(Vec3.atBottomCenterOf(site));
				h.assertTrue(level.noCollision(probe), "Fixture spawn site has no collision: " + probe.getBoundingBox());
				h.assertTrue(level.getWorldBorder().isWithinBounds(probe.getBoundingBox()), "Fixture spawn site inside border");
				probe.discard();
			}
			return p;
		}
		List<RegionalMob> sporelings() {
			return h.getLevel().getEntitiesOfClass(RegionalMob.class, new AABB(center).inflate(32), RegionalEncounters.SHADOW::combatant);
		}
		@Override public void close() {
			for (var p : players) {
				RegionalEncounters.SHADOW.leave(p);
				if (h.getLevel().getServer().getPlayerList().getPlayer(p.getUUID()) != null) h.getLevel().getServer().getPlayerList().remove(p);
				p.discard();
			}
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) h.getLevel().setChunkForced((center.getX()>>4)+x,(center.getZ()>>4)+z,false);
			h.getLevel().getServer().getLevel(Level.END).setDragonFight(dragon);
			h.getLevel().getServer().setDifficulty(difficulty, true); ServerTuning.regionalEvents = enabled;
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
				h.getLevel().setBlockAndUpdate(center.offset(x, -1, z), Blocks.AIR.defaultBlockState());
		}
	}
    @GameTest(maxTicks=100)
    public void warnedBurstKeepsBystandersAndTerrainSafe(GameTestHelper h) {
        var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
            var p=f.player(); var outsider=f.player();
            h.assertTrue(RegionalEncounters.SHADOW.form(p,false)==1,"Burst fixture starts");
            var drone=f.sporelings().getFirst(); drone.setPos(p.position().add(2,0,0));
            float outsiderHealth=outsider.getHealth();
            var floor=h.getLevel().getBlockState(f.center.below());
            for(int i=0;i<65;i++) { drone.commonTick(); drone.customServerAiStep(h.getLevel()); }
            h.assertTrue(p.getHealth()<p.getMaxHealth(),"Warned burst damages its participant");
            h.assertTrue(outsider.getHealth()==outsiderHealth && !outsider.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS),"Burst excludes bystanders and its darkness debuff");
            h.assertTrue(!drone.isRemoved(),"Event drone does not self-discard and invalidate the encounter");
            h.assertTrue(h.getLevel().getBlockState(f.center.below()).equals(floor),"Burst preserves terrain");
        }
        h.succeed();
        });
    }
	@GameTest(maxTicks=100)
	public void soloIsOptInProtectsBystandersAndPaysOnce(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var p = f.player(); var bystander = f.player();
			RegionalEncounters.SHADOW.offer(p);
			h.assertTrue(f.sporelings().isEmpty(), "Offer never spawns an encounter");
			h.assertTrue(RegionalEncounters.SHADOW.form(p, false) == 1 && f.sporelings().size() == 3, "Solo starts exactly three sporelings: " + f.messages);
			var first = f.sporelings().getFirst();
			first.setTarget(bystander);
			h.assertTrue(first.getTarget() == null && !first.canAttack(bystander), "Native AI cannot target nonparticipants");
			h.assertTrue(!first.hurtServer(h.getLevel(), bystander.damageSources().playerAttack(bystander), 1000), "Outsiders cannot kill hunt sporelings");
			h.assertTrue(!first.doHurtTarget(h.getLevel(), bystander), "Direct melee cannot bypass participation");
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF, 4));
			for (var sporeling : List.copyOf(f.sporelings())) sporeling.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 1000);
			h.assertTrue(f.sporelings().isEmpty() && RegionalEncounters.SHADOW.ledger(h.getLevel()).reward(p.getUUID()), "Victory cleans the pack and saves the claim");
			h.assertTrue(!RegionalEncounters.SHADOW.ledger(h.getLevel()).reward(bystander.getUUID()), "Bystanders receive no reward");
			h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(f.center).inflate(32)).isEmpty(), "Hunt sporelings drop no ordinary loot");
			h.assertTrue(RegionalEncounters.SHADOW.claim(p) == 1 && p.getInventory().countItem(Items.SCULK) == 4, "Exact reward delivered");
			h.assertTrue(RegionalEncounters.SHADOW.claim(p) == 0 && p.getInventory().countItem(Items.SCULK) == 4, "Repeated claim cannot duplicate");
			h.assertTrue(RegionalEncounters.SHADOW.form(p, false) == 0 && f.sporelings().isEmpty(), "Started cooldown prevents immediate repeat");
		}
		h.succeed();
        });
	}
	@GameTest(maxTicks=100)
	public void cooperativeRequiresOptInCapsPartyAndRewardsEachMember(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var leader = f.player(); var second = f.player(); var third = f.player(); var fourth = f.player(); var outsider = f.player();
			RegionalEncounters.SHADOW.form(leader, true);
			h.assertTrue(RegionalEncounters.SHADOW.start(leader) == 0 && f.sporelings().isEmpty(), "Cooperative hunts require two players");
			RegionalEncounters.SHADOW.join(second, leader); RegionalEncounters.SHADOW.join(third, leader); RegionalEncounters.SHADOW.join(fourth, leader); RegionalEncounters.SHADOW.join(outsider, leader);
			h.assertTrue(RegionalEncounters.SHADOW.start(second) == 0, "Only the leader starts");
			h.assertTrue(RegionalEncounters.SHADOW.start(leader) == 1 && f.sporelings().size() == 6, "Four participants still spawn only six sporelings; found " + f.sporelings().size() + ": " + f.messages);
			for (var sporeling : List.copyOf(f.sporelings())) {
				h.assertTrue(RegionalEncounters.SHADOW.canTarget(sporeling, second) && !RegionalEncounters.SHADOW.canTarget(sporeling, outsider), "Fifth player is excluded");
				sporeling.hurtServer(h.getLevel(), second.damageSources().playerAttack(second), 1000);
			}
			for (var p : List.of(leader, second, third, fourth))
				h.assertTrue(RegionalEncounters.SHADOW.claim(p) == 1 && p.getInventory().countItem(Items.SCULK) == 4, "Every opted-in supporter earns one fixed reward");
			h.assertTrue(RegionalEncounters.SHADOW.claim(outsider) == 0, "Party overflow cannot claim");
		}
		h.succeed();
        });
	}
	@GameTest(maxTicks=100)
	public void invalidTerrainAndProgressionNeverChargeOrSpawn(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var p = f.player();
			h.getLevel().getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.createDefault());
			h.assertTrue(RegionalEncounters.SHADOW.form(p, false) == 0 && f.sporelings().isEmpty(), "Dragon gate applies to manual starts");
			h.getLevel().getServer().getLevel(Level.END).setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
				h.getLevel().setBlockAndUpdate(f.center.offset(x, -1, z), Blocks.GLASS.defaultBlockState());
			h.assertTrue(RegionalEncounters.SHADOW.form(p, false) == 0 && f.sporelings().isEmpty(), "Pack validation is all-or-nothing on invalid ground");
			h.assertTrue(RegionalEncounters.SHADOW.ledger(h.getLevel()).ready(p.getUUID(), h.getLevel().getGameTime()), "Failed start charges no cooldown");
			ServerTuning.regionalEvents = false;
			h.assertTrue(RegionalEncounters.SHADOW.form(p, true) == 0, "Disabled event setting prevents lobbies");
		}
		h.succeed();
        });
	}
	@GameTest(maxTicks=100)
	public void disconnectCancelsPartyAndReloadedSporelingsAreDiscarded(GameTestHelper h) {
		var f = new Fixture(h);
        h.runAfterDelay(20, () -> { try (f) { f.enable();
			var leader = f.player(); var member = f.player();
			RegionalEncounters.SHADOW.form(leader, true); RegionalEncounters.SHADOW.join(member, leader);
			h.assertTrue(RegionalEncounters.SHADOW.start(leader) == 1, "Cooperative fixture starts: " + f.messages);
			var sporeling = f.sporelings().getFirst();
			var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
			sporeling.saveWithoutId(out);
			h.getLevel().getServer().getPlayerList().remove(member);
			RegionalEncounters.SHADOW.tick(h.getLevel());
			h.assertTrue(f.sporelings().isEmpty() && !RegionalEncounters.SHADOW.ledger(h.getLevel()).reward(leader.getUUID()), "Disconnect cancels the group with no reward");
			h.assertTrue(!RegionalEncounters.SHADOW.ledger(h.getLevel()).ready(leader.getUUID(), h.getLevel().getGameTime()), "Interruption preserves spent cooldown");
			var copy = new RegionalMob(RegionalEcosystems.MOBS.get(RegionalKind.SHADOW_DRONE), h.getLevel(), RegionalKind.SHADOW_DRONE);
			copy.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
			h.getLevel().addFreshEntity(copy);
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
					h.getLevel().setBlockAndUpdate(f.center.offset(x, -1, z), Blocks.DEEPSLATE.defaultBlockState());
				h.assertTrue(RegionalEncounters.SHADOW.form(p, false) == 1, "Cancellation fixture starts: " + f.messages);
				if (reason == 0) p.setPos(Vec3.atBottomCenterOf(f.center).add(49, 0, 0));
				else if (reason == 1) p.setHealth(0);
				else RegionalEncounters.SHADOW.leave(p);
				RegionalEncounters.SHADOW.tick(h.getLevel());
				h.assertTrue(f.sporelings().isEmpty() && !RegionalEncounters.SHADOW.ledger(h.getLevel()).reward(p.getUUID()),
					"Boundary, death and opt-out each clean the pack without rewards");
				h.assertTrue(!RegionalEncounters.SHADOW.ledger(h.getLevel()).ready(p.getUUID(), h.getLevel().getGameTime()),
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
			var p = f.player(); var saved = RegionalEncounters.SHADOW.ledger(h.getLevel());
			saved.won(p.getUUID());
			for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
			h.assertTrue(RegionalEncounters.SHADOW.claim(p) == 0 && saved.reward(p.getUUID()), "Full inventory retains entitlement");
			p.getInventory().setItem(9, ItemStack.EMPTY);
			var state = darkspawn.black.oneblock.OneBlock.state(p);
			state.ascensionInside = true;
			h.assertTrue(RegionalEncounters.SHADOW.claim(p) == 0 && saved.reward(p.getUUID()), "Active island possessions cannot receive outside rewards");
			state.ascensionInside = false;
			h.assertTrue(RegionalEncounters.SHADOW.claim(p) == 1 && p.getInventory().getItem(9).is(Items.SCULK), "Recovered outside profile can claim once");
		}
		h.succeed();
        });
	}
}
