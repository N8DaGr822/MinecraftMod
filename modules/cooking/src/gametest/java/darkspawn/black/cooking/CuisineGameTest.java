package darkspawn.black.cooking;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class CuisineGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "cuisine-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.getAbilities().invulnerable = true;
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.teleportTo(pos.x, pos.y, pos.z);
		return player;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	@GameTest public void sixServingsAreSharedAndFinalServingCannotBeReused(GameTestHelper helper) {
		var first = player(helper); var second = player(helper);
		try {
			var pos = helper.absolutePos(new BlockPos(1, 1, 1));
			FeastBlock feast = (FeastBlock) ((BlockItem) Cuisine.DISHES.get("hunters_feast").item()).getBlock();
			helper.getLevel().setBlock(pos, feast.defaultBlockState(), 3);
			first.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 2));
			for (int i = 0; i < 6; i++) {
				var diner = i % 2 == 0 ? first : second;
				diner.getFoodData().setFoodLevel(0);
				MealEffects.apply(diner, MealEffects.HERBAL, 100);
				feast.serve(helper.getLevel(), pos, diner);
				helper.assertTrue(diner.getFoodData().getFoodLevel() == 8 && diner.hasEffect(MealEffects.FEAST) && !diner.hasEffect(MealEffects.HERBAL), "Serving replaces only meal and restores hunger");
				if (i < 5) helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(FeastBlock.SERVINGS) == 5 - i, "Serving count must decrement once");
			}
			helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Final serving removes the feast");
			first.getFoodData().setFoodLevel(0);
			feast.serve(helper.getLevel(), pos, first);
			helper.assertTrue(first.getFoodData().getFoodLevel() == 0, "A stale use cannot create a seventh serving");
			helper.assertTrue(first.getEffect(MobEffects.SPEED).getAmplifier() == 2, "Serving preserves the potion");
		} finally { remove(first); remove(second); }
		helper.succeed();
	}
	@GameTest public void eatingTracksActualFoodAndPersistsDiscoveries(GameTestHelper helper) {
		var player = player(helper);
		try {
			for (var item : List.of(Items.APPLE, Items.CARROT, Cuisine.DISHES.get("berry_pie").item())) {
				player.getFoodData().setFoodLevel(0);
				ItemStack food = new ItemStack(item);
				food.finishUsingItem(helper.getLevel(), player);
			}
			helper.assertTrue(Culinary.progress(player).variety() == 3 && Culinary.progress(player).meals().size() == 1, "Consumption must record food once, and distinguish meals");
			Culinary.progress(player).unlock("shadow_stew");
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			player.saveWithoutId(output);
			Culinary.progress(player).restore(List.of(), List.of(), List.of());
			player.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
			helper.assertTrue(Culinary.progress(player).knows("shadow_stew") && Culinary.progress(player).variety() == 3, "Player save/load preserves discoveries and dietary window");
			for (int i = 0; i < 8; i++) new ItemStack(Items.APPLE).finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(Culinary.progress(player).variety() == 1, "Repeated apples must displace past variety");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void chefHasJobSiteAllLevelsAndPersistentRestockableTrades(GameTestHelper helper) {
		var level = helper.getLevel();
		Villager chef = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
		try {
			var profession = level.registryAccess().getOrThrow(VillageKitchen.CHEF).value();
			helper.assertTrue(profession.acquirableJobSite().test(PoiTypes.forState(Cooking.STATION.defaultBlockState()).orElseThrow()), "Cooking Station must be a Chef job site");
			for (int tier = 1; tier <= 5; tier++) helper.assertTrue(level.registryAccess().getOrThrow(profession.getTrades(tier)) != null, "All Chef trade levels must resolve");
			chef.setVillagerData(chef.getVillagerData().withProfession(level.registryAccess(), VillageKitchen.CHEF));
			helper.assertTrue(chef.getOffers().size() == 2, "Novice Chef has two trades");
			chef.getOffers().forEach(offer -> offer.setToOutOfStock());
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			chef.saveWithoutId(output);
			chef.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
			helper.assertTrue(chef.getOffers().stream().allMatch(offer -> offer.isOutOfStock()), "Chef stock persists");
			chef.restock();
			helper.assertTrue(chef.getOffers().stream().noneMatch(offer -> offer.isOutOfStock()), "Chef stock renews through native restocking");
		} finally { chef.discard(); }
		helper.succeed();
	}
	@GameTest public void ironStomachShortensPoisonAndMealReplacementPreservesPotions(GameTestHelper helper) {
		var player = player(helper);
		try {
			MealEffects.apply(player, MealEffects.IRON_STOMACH, 1200);
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
			helper.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 100, "Iron Stomach halves incoming poison duration");
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000, 0));
			MealEffects.apply(player, MealEffects.FIREPROOF, 500);
			MealEffects.apply(player, MealEffects.MINER, 500);
			helper.assertTrue(!player.hasEffect(MealEffects.IRON_STOMACH) && !player.hasEffect(MealEffects.FIREPROOF)
				&& player.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() == 1000, "Meal ownership must leave the potion alone");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void vanillaDishesCookAloneAndForeignIngredientRecipesFollowTheirMods(GameTestHelper helper) {
		var recipes = helper.getLevel().getServer().getRecipeManager();
		java.util.function.Predicate<String> present = name -> recipes.byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, darkspawn.black.core.DarkspawnCore.id(name))).isPresent();
		java.util.function.Predicate<String> registered = name -> net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(darkspawn.black.core.DarkspawnCore.id(name));
		for (String name : List.of("salt", "butter", "cheese", "herb_steak", "honey_pork", "fish_chowder", "berry_pie", "cooking_station"))
			helper.assertTrue(present.test(name), name + " needs only vanilla ingredients and must always load");
		helper.assertTrue(present.test("woodland_stew") == registered.test("wild_herbs"), "Woodland stew follows farming's wild herbs");
		helper.assertTrue(present.test("pike_chowder") == registered.test("pike"), "Pike chowder follows fishing");
		helper.assertTrue(present.test("truffle_risotto") == registered.test("woodland_truffle"), "Truffle risotto follows husbandry");
		helper.assertTrue(present.test("shadow_stew") == registered.test("royal_jelly"), "Boss dishes follow their boss ingredient");
		helper.assertTrue(Cuisine.DISHES.containsKey("shadow_stew") && Cuisine.SCROLLS.containsKey("shadow_stew"), "Boss meals and scrolls stay registered for stable ids");
		var player = player(helper);
		try {
			helper.assertTrue(darkspawn.black.core.hook.ClimateProtection.total(player, false).cold() == 0, "No meal, no climate protection");
			MealEffects.apply(player, MealEffects.WARMING, 200);
			helper.assertTrue(darkspawn.black.core.hook.ClimateProtection.total(player, false).cold() == 15, "The warming meal reports cold protection through core");
		} finally { remove(player); }
		helper.succeed();
	}
}
