package darkspawn.black.boss;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class AwakenedPatternGameTest {
	private static ServerPlayer player(GameTestHelper h) {
		var level = h.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "awakened-test"), false);
		var p = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());
		p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		p.hasChangedDimension();
		p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		p.setHealth(100);
		p.setNoGravity(true);
		return p;
	}

	private static void remove(ServerPlayer p) { p.level().getServer().getPlayerList().remove(p); p.discard(); }
	private static void move(ServerPlayer p, Vec3 pos) { p.teleportTo(pos.x, pos.y, pos.z); }
	private static Mob boss(GameTestHelper h, BossKind kind) {
		return switch (kind) {
			case ANCIENT_TREE_SPIRIT -> new AncientTreeSpirit(BossEntities.TREE_SPIRIT, h.getLevel());
			case MUTANT_WOLF -> new MutantWolf(BossEntities.MUTANT_WOLF, h.getLevel());
			default -> {
				var profile = BossProfile.valueOf(kind.name());
				yield new BiomeBoss(BossEntities.BIOME_BOSSES.get(profile), h.getLevel(), profile);
			}
		};
	}
	private static void begin(GameTestHelper h, AwakenedPattern pattern, Mob boss, BossKind kind, ServerPlayer p) {
		for (int i = 0; i < 3; i++) h.assertTrue(!pattern.begin(boss, kind, p), "First three opportunities retain normal attacks");
		h.assertTrue(pattern.begin(boss, kind, p), "Fourth awakened opportunity begins the special");
	}

	private static void ai(Mob boss, net.minecraft.server.level.ServerLevel level) {
		boss.commonTick();
		switch (boss) {
			case AncientTreeSpirit tree -> tree.customServerAiStep(level);
			case MutantWolf wolf -> wolf.customServerAiStep(level);
			case BiomeBoss biome -> biome.customServerAiStep(level);
			default -> throw new IllegalArgumentException("Unknown boss implementation");
		}
	}

	@GameTest(skyAccess = true)
	public void everyBossSchedulesSpecialAndCancelsItOnPhaseChange(GameTestHelper h) throws Exception {
		var p = player(h);
		p.getAbilities().invulnerable = true;
		var origin = Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)).add(0, 140, 512);
		try {
			for (var kind : BossKind.values()) {
				move(p, origin);
				var boss = boss(h, kind);
				boss.setPos(origin.add(24, 0, 0));
				boss.setNoGravity(true);
				boss.addTag(AwakenedRematches.TAG);
				h.getLevel().addFreshEntity(boss);
				try {
					var field = boss.getClass().getDeclaredField("awakenedPattern");
					field.setAccessible(true);
					var pattern = (AwakenedPattern) field.get(boss);
					for (int tick = 0; tick < 1800 && !pattern.active(); tick++) ai(boss, h.getLevel());
					h.assertTrue(pattern.active(), kind + " must schedule the special through real AI");
					boss.setHealth(boss.getMaxHealth() * .6F);
					ai(boss, h.getLevel());
					h.assertTrue(!pattern.active(), kind + " phase transition cancels pending special");
				} finally { boss.discard(); }
			}
		} finally { remove(p); }
		h.succeed();
	}

	@GameTest
	public void allSixteenPatternsWarnThenDealExactlyThreeHits(GameTestHelper h) {
		var p = player(h);
		var origin = Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)).add(0, 140, 0);
		try {
			for (var kind : BossKind.values()) {
				var boss = boss(h, kind);
				boss.setPos(origin.add(0, 0, -24));
				boss.addTag(AwakenedRematches.TAG);
				var pattern = new AwakenedPattern();
				move(p, origin); p.setHealth(100); p.setInvulnerableTime(0); p.damageCooldownTime = 0;
				begin(h, pattern, boss, kind, p);
				for (int tick = 1; tick <= 160; tick++) {
					int beat = Math.min(2, Math.max(0, (tick - 21) / 40));
					var zone = AwakenedPattern.zones(kind, beat).getFirst();
					move(p, origin.add(zone.x() + (zone.inner() + zone.outer()) / 2, 0, zone.z()));
					p.setInvulnerableTime(Math.max(0, p.getInvulnerableTime() - 1));
					// Simulated Player Ticks: The isolated pattern loop must advance native hurt cooldowns too.
					p.damageCooldownTime = Math.max(0, p.damageCooldownTime - 1);
					pattern.tick(h.getLevel(), boss, kind, List.of(p));
					int hits = tick >= 140 ? 3 : tick >= 100 ? 2 : tick >= 60 ? 1 : 0;
					h.assertTrue(p.getHealth() == 100 - hits * 8, kind + " hit timing at tick " + tick);
				}
				h.assertTrue(!pattern.active(), "Special terminates without persistent hazards");
				boss.discard();
			}
		} finally { remove(p); }
		h.succeed();
	}

	@GameTest
	public void safeCenterLateJoinAndAbandonmentAreProtected(GameTestHelper h) {
		var p = player(h);
		var late = player(h);
		var origin = Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)).add(0, 180, 0);
		var boss = boss(h, BossKind.ANCIENT_TREE_SPIRIT);
		boss.setPos(origin.add(0, 0, -24));
		boss.addTag(AwakenedRematches.TAG);
		try {
			move(p, origin); move(late, origin.add(7, 0, 0));
			var pattern = new AwakenedPattern();
			begin(h, pattern, boss, BossKind.ANCIENT_TREE_SPIRIT, p);
			for (int tick = 1; tick <= 60; tick++)
				pattern.tick(h.getLevel(), boss, BossKind.ANCIENT_TREE_SPIRIT, tick < 59 ? List.of(p) : List.of(p, late));
			h.assertTrue(p.getHealth() == 100 && late.getHealth() == 100, "Safe center and late arrival avoid the first impact");
			move(late, origin.add(11, 0, 0));
			for (int tick = 61; tick <= 100; tick++) pattern.tick(h.getLevel(), boss, BossKind.ANCIENT_TREE_SPIRIT, List.of(p, late));
			h.assertTrue(late.getHealth() == 92 && p.getHealth() == 100, "Late player becomes eligible only after a full warning");
			pattern.tick(h.getLevel(), boss, BossKind.ANCIENT_TREE_SPIRIT, List.of());
			h.assertTrue(!pattern.active(), "Abandonment cancels pending damage");
			for (int tick = 0; tick < 160; tick++) pattern.tick(h.getLevel(), boss, BossKind.ANCIENT_TREE_SPIRIT, List.of(p, late));
			h.assertTrue(late.getHealth() == 92, "Returning alone cannot resume a cancelled impact");
		} finally { boss.discard(); remove(p); remove(late); }
		h.succeed();
	}

	@GameTest
	public void normalBossesNeverStartAndReloadCancelsAwakenedState(GameTestHelper h) throws Exception {
		var p = player(h);
		move(p, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)).add(0, 140, 0));
		try {
			for (var kind : BossKind.values()) {
				var boss = boss(h, kind);
				boss.setPos(p.position().add(0, 0, -24));
				var field = boss.getClass().getDeclaredField("awakenedPattern");
				field.setAccessible(true);
				var pattern = (AwakenedPattern) field.get(boss);
				for (int i = 0; i < 12; i++) h.assertTrue(!pattern.begin(boss, kind, p), "Normal fight never starts a special");
				boss.addTag(AwakenedRematches.TAG);
				begin(h, pattern, boss, kind, p);
				pattern.tick(h.getLevel(), boss, kind, List.of(p));
				var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
				boss.saveWithoutId(out);
				var restored = boss(h, kind);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
				h.assertTrue(!((AwakenedPattern) field.get(restored)).active() && restored.entityTags().contains(AwakenedRematches.TAG),
					"Reload preserves awakened identity but discards pending special");
				h.assertTrue(restored.getHealth() == boss.getHealth(), "Reload preserves health");
				boss.discard(); restored.discard();
			}
		} finally { remove(p); }
		h.succeed();
	}
}
