package darkspawn.black.ecosystem;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.boss.BossBolt;
import darkspawn.black.boss.BossItems;
import darkspawn.black.cooking.Cooking;
import darkspawn.black.cooking.CookingMenu;
import darkspawn.black.cooking.MealEffects;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class ForestGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "forest-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.teleportTo(pos.x, pos.y, pos.z);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return player;
	}

	private static void removePlayer(ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
		player.discard();
	}

	private static ForestMob mob(GameTestHelper helper, ForestMob.Kind kind, Vec3 position) {
		var mob = new ForestMob(ForestEcosystem.MOBS.get(kind), helper.getLevel(), kind);
		mob.setPos(position);
		helper.getLevel().addFreshEntity(mob);
		return mob;
	}

	private static void tickStrike(GameTestHelper helper, ForestMob mob, int ticks) {
		for (int i = 0; i < ticks; i++) { mob.commonTick(); mob.customServerAiStep(helper.getLevel()); }
	}

	private static int count(ServerPlayer player, Item item) {
		int total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) { total += stack.getCount(); }
		}
		return total;
	}

	@GameTest(skyAccess = true)
	public void shearingIsNeutralAndCooldownSurvivesSaveLoad(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ServerPlayer secondPlayer = player(helper);
		ForestMob barkling = mob(helper, ForestMob.Kind.BARKLING, player.position().add(1, 0, 0));
		try {
			ItemStack shears = new ItemStack(Items.SHEARS);
			player.setItemInHand(InteractionHand.MAIN_HAND, shears);
			barkling.mobInteract(player, InteractionHand.MAIN_HAND);
			barkling.mobInteract(player, InteractionHand.MAIN_HAND);
			secondPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
			barkling.mobInteract(secondPlayer, InteractionHand.MAIN_HAND);
			helper.assertTrue(count(secondPlayer, Cooking.WILD_HERBS) == 0, "A second player must share this creature's harvest cooldown");
			helper.assertTrue(count(player, Cooking.WILD_HERBS) == 1 && shears.getDamageValue() == 1, "Repeated harvest must grant one herb and consume one durability");
			helper.assertTrue(barkling.getTarget() == null && !barkling.isPreventingPlayerRest(helper.getLevel(), player), "Harvest must leave the Barkling neutral");
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			barkling.saveWithoutId(output);
			var saved = output.buildResult();
			barkling.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
			barkling.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(count(player, Cooking.WILD_HERBS) == 1 && !barkling.herbsReady(), "Reload must not bypass herb regrowth");
			// Saved Clock Boundary: Expire this creature's cooldown without advancing every test's world clock.
			saved.putLong("darkspawn_herbs_ready_at", helper.getLevel().getGameTime());
			barkling.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
			barkling.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(count(player, Cooking.WILD_HERBS) == 2 && shears.getDamageValue() == 2, "Herbs must be renewable when the saved cooldown expires");
		} finally { barkling.discard(); removePlayer(player); removePlayer(secondPlayer); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void fullInventoryDropsHerbsAndAngryBarklingsRefuseHarvest(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ForestMob barkling = mob(helper, ForestMob.Kind.BARKLING, player.position().add(1, 0, 0));
		try {
			for (int i = 0; i < 36; i++) { player.getInventory().setItem(i, new ItemStack(Items.STONE, 64)); }
			ItemStack shears = new ItemStack(Items.SHEARS);
			player.setItemInHand(InteractionHand.MAIN_HAND, shears);
			barkling.mobInteract(player, InteractionHand.MAIN_HAND);
			var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, barkling.getBoundingBox().inflate(2), item -> item.getItem().is(Cooking.WILD_HERBS));
			helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "Full inventory must drop exactly one harvest");
			drops.forEach(ItemEntity::discard);
			barkling.setTarget(player);
			barkling.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(shears.getDamageValue() == 1 && barkling.isPreventingPlayerRest(helper.getLevel(), player), "Provoked Barkling must refuse harvesting and prevent rest");
			barkling.performRangedAttack(player, 1);
			var sticks = helper.getLevel().getEntitiesOfClass(BossBolt.class, barkling.getBoundingBox().inflate(2), bolt -> bolt.getOwner() == barkling);
			helper.assertTrue(sticks.size() == 1 && sticks.getFirst().getItem().is(Items.STICK), "Retaliation must use a thrown stick");
			sticks.forEach(BossBolt::discard);
		} finally { barkling.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void barklingsHideUntilProvokedAndHollowedHaveWoodenWeaknesses(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ForestMob barkling = mob(helper, ForestMob.Kind.BARKLING, player.position().add(1, 0, 0));
		ForestMob hollowed = mob(helper, ForestMob.Kind.HOLLOWED, player.position().add(2, 0, 0));
		try {
			barkling.setNoGravity(true);
			for (int i = 0; i < 12; i++) { barkling.commonTick(); barkling.tick(); }
			helper.assertTrue(barkling.getTarget() == null && barkling.hiding(), "Unprovoked Barkling must freeze without targeting the player");
			barkling.hurtServer(helper.getLevel(), barkling.damageSources().playerAttack(player), 1);
			for (int i = 0; i < 12; i++) { barkling.commonTick(); barkling.tick(); }
			helper.assertTrue(barkling.getTarget() == player && !barkling.hiding(), "Hurt-by-target AI must break hiding and retaliate");
			hollowed.hurtServer(helper.getLevel(), hollowed.damageSources().playerAttack(player), 6);
			float normalDamage = hollowed.getMaxHealth() - hollowed.getHealth();
			hollowed.setHealth(hollowed.getMaxHealth());
			hollowed.damageCooldownTime = 0;
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
			hollowed.hurtServer(helper.getLevel(), hollowed.damageSources().playerAttack(player), 6);
			helper.assertTrue(hollowed.getMaxHealth() - hollowed.getHealth() > normalDamage, "An axe must overcome more wooden armor than an equal normal hit");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			hollowed.setHealth(hollowed.getMaxHealth());
			hollowed.damageCooldownTime = 0;
			hollowed.hurtServer(helper.getLevel(), hollowed.damageSources().inFire(), 6);
			helper.assertTrue(hollowed.getMaxHealth() - hollowed.getHealth() > normalDamage, "Fire must exploit the wooden weakness");
			helper.assertTrue(hollowed.doHurtTarget(helper.getLevel(), player) && player.hasEffect(MobEffects.SLOWNESS), "Hollowed melee must briefly slow its victim");
		} finally { barkling.discard(); hollowed.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void rootWarningsAllowDodgingAndReloadCancelsUnseenDamage(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ForestMob roots = mob(helper, ForestMob.Kind.ROOTCRAWLER, player.position().add(2, 0, 0));
		try {
			float health = player.getHealth();
			helper.assertTrue(roots.doHurtTarget(helper.getLevel(), player), "Nearby target should begin a root warning");
			tickStrike(helper, roots, 24);
			helper.assertTrue(player.getHealth() == health && roots.windup() == 1, "Roots must not hurt during the warning");
			player.setPos(player.position().add(0, 0, 4));
			tickStrike(helper, roots, 1);
			helper.assertTrue(player.getHealth() == health, "Moving out of the marked spot must dodge the strike");
			tickStrike(helper, roots, 80);
			helper.assertTrue(roots.doHurtTarget(helper.getLevel(), player), "Cooldown must allow another warning");
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			roots.saveWithoutId(output);
			roots.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
			tickStrike(helper, roots, 25);
			helper.assertTrue(roots.windup() == 0 && player.getHealth() == health, "Reload must cancel the pending strike");
			tickStrike(helper, roots, 40);
			helper.assertTrue(roots.doHurtTarget(helper.getLevel(), player), "Reload recovery must eventually permit a fresh warning");
			tickStrike(helper, roots, 25);
			helper.assertTrue(player.getHealth() < health && player.hasEffect(MobEffects.SLOWNESS), "Standing in the warning must take damage and brief root slowing");
		} finally { roots.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void rootsRespectRangeHeightAndObstructions(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		Vec3 origin = player.position();
		ForestMob roots = mob(helper, ForestMob.Kind.ANCIENT_ENT, origin.add(2, 0, 0));
		try {
			float health = player.getHealth();
			player.setPos(origin.add(12, 0, 0));
			helper.assertTrue(!roots.doHurtTarget(helper.getLevel(), player), "Root warning must not target distant players");
			player.setPos(origin);
			helper.assertTrue(roots.doHurtTarget(helper.getLevel(), player), "In-range target must start warning");
			player.setPos(origin.add(0, 4, 0));
			tickStrike(helper, roots, 25);
			helper.assertTrue(player.getHealth() == health, "Ground roots must not hit players above their height limit");
			tickStrike(helper, roots, 80);
			player.setPos(origin);
			helper.assertTrue(roots.doHurtTarget(helper.getLevel(), player), "Next warning must start");
			for (int y = 0; y < 5; y++) { helper.getLevel().setBlockAndUpdate(BlockPos.containing(origin.add(1, y, 0)), Blocks.STONE.defaultBlockState()); }
			tickStrike(helper, roots, 25);
			helper.assertTrue(player.getHealth() == health, "Roots must not hit through an obstruction");
		} finally { roots.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void entRewardsRequirePlayerKillsAndWoodlandStewCompletesTheLoop(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ForestMob ent = mob(helper, ForestMob.Kind.ANCIENT_ENT, player.position().add(1, 0, 0));
		try {
			ent.dropCustomDeathLoot(helper.getLevel(), ent.damageSources().generic(), false);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, ent.getBoundingBox().inflate(2)).isEmpty(), "Environmental death must not grant culinary or summon rewards");
			ent.dropCustomDeathLoot(helper.getLevel(), ent.damageSources().playerAttack(player), true);
			var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, ent.getBoundingBox().inflate(2));
			helper.assertTrue(drops.size() == 2 && drops.stream().anyMatch(item -> item.getItem().is(BossItems.ANCIENT_HEARTWOOD))
					&& drops.stream().anyMatch(item -> item.getItem().is(Cooking.WILD_HERBS)), "Herald must drop herbs and the existing summon, without boss rewards");
			drops.forEach(ItemEntity::discard);
			CookingMenu menu = new CookingMenu(1, player.getInventory());
			menu.getSlot(0).set(new ItemStack(Items.BOWL));
			menu.getSlot(1).set(new ItemStack(Cooking.WILD_HERBS));
			menu.getSlot(2).set(new ItemStack(Items.CARROT));
			menu.getSlot(3).set(new ItemStack(Items.BROWN_MUSHROOM));
			menu.clicked(4, 0, ContainerInput.PICKUP, player);
			helper.assertTrue(menu.getCarried().is(Cooking.WOODLAND_STEW), "Four forest ingredients must produce Woodland Stew");
			for (int i = 0; i < 4; i++) { helper.assertTrue(menu.getSlot(i).getItem().isEmpty(), "Cooking must consume each ingredient once"); }
			MealEffects.apply(player, MealEffects.SAVORY, 600);
			player.getFoodData().setFoodLevel(8);
			player.getFoodData().setSaturation(0);
			ItemStack bowl = menu.getCarried().finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(bowl.is(Items.BOWL) && player.getFoodData().getFoodLevel() == 16, "Eating must restore eight hunger and return the bowl");
			helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 12.8F) < 0.01F, "Stew must provide 12.8 saturation");
			helper.assertTrue(!player.hasEffect(MealEffects.SAVORY) && player.getEffect(MealEffects.SWEET).getDuration() == 600, "Stew must replace the old meal with thirty seconds of regeneration");
		} finally { ent.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void naturalSpawningRequiresGlobalDragonForestAndSurface(GameTestHelper helper) {
		var level = helper.getLevel();
		var end = level.getServer().getLevel(Level.END);
		helper.assertTrue(end != null, "End dimension must be available for the global progression gate");
		var originalFight = end.getDragonFight();
		var originalDifficulty = level.getDifficulty();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		var originalBiome = level.getBiome(pos).unwrapKey().orElseThrow();
		try {
			helper.setBiome(Biomes.FOREST);
			level.setBlockAndUpdate(pos.below(), Blocks.MOSS_BLOCK.defaultBlockState());
			end.setDragonFight(EnderDragonFight.createDefault());
			for (var entry : ForestEcosystem.MOBS.entrySet()) {
				helper.assertTrue(!ForestEcosystem.canSpawn(entry.getKey(), entry.getValue(), level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Pre-dragon world must reject " + entry.getKey());
			}
			end.setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			for (var entry : ForestEcosystem.MOBS.entrySet()) {
				helper.assertTrue(ForestEcosystem.canSpawn(entry.getKey(), entry.getValue(), level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Unlocked forest surface must permit " + entry.getKey());
				helper.assertTrue(!ForestEcosystem.canSpawn(entry.getKey(), entry.getValue(), end, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Forest ecosystem must reject the End");
			}
			var type = ForestEcosystem.MOBS.get(ForestMob.Kind.HOLLOWED);
			helper.assertTrue(!ForestEcosystem.canSpawn(ForestMob.Kind.HOLLOWED, type, level, EntitySpawnReason.NATURAL, pos, RandomSource.create(1)), "Ordinary daytime spawning must enforce hostile light rules");
			helper.assertTrue(ForestEcosystem.canSpawn(ForestMob.Kind.BARKLING, ForestEcosystem.MOBS.get(ForestMob.Kind.BARKLING), level, EntitySpawnReason.NATURAL, pos, RandomSource.create(1)), "Barklings must allow ordinary bright forest spawning");
			level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
			for (var entry : ForestEcosystem.MOBS.entrySet()) {
				helper.assertTrue(ForestEcosystem.canSpawn(entry.getKey(), entry.getValue(), level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)) == (entry.getKey() == ForestMob.Kind.BARKLING), "Peaceful mode must allow only the neutral Barkling");
			}
			level.getServer().setDifficulty(originalDifficulty, true);
			helper.setBiome(Biomes.PLAINS);
			helper.assertTrue(!ForestEcosystem.canSpawn(ForestMob.Kind.HOLLOWED, type, level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Wrong biome must reject spawns");
			helper.setBiome(Biomes.FOREST);
			level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
			helper.assertTrue(!ForestEcosystem.canSpawn(ForestMob.Kind.HOLLOWED, type, level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Stone floor must reject spawns");
			level.setBlockAndUpdate(pos.below(), Blocks.MOSS_BLOCK.defaultBlockState());
			level.setBlockAndUpdate(pos.above(4), Blocks.STONE.defaultBlockState());
			helper.assertTrue(!ForestEcosystem.canSpawn(ForestMob.Kind.HOLLOWED, type, level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Underground floor must reject spawns");
		} finally { end.setDragonFight(originalFight); level.getServer().setDifficulty(originalDifficulty, true); helper.setBiome(originalBiome); }
		helper.succeed();
	}
}
