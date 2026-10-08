package darkspawn.black.cooking;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class CookingGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "cooking-test"), false);
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

	private static void removePlayer(ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
		player.discard();
	}

	private static int count(ServerPlayer player, Item item) {
		int total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) { total += stack.getCount(); }
		}
		return total;
	}

	@GameTest
	public void normalAndShiftClicksConsumeExactlyOneSetPerMeal(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			CookingMenu menu = new CookingMenu(1, player.getInventory());
			menu.getSlot(2).set(new ItemStack(Items.COOKED_BEEF, 3));
			menu.getSlot(3).set(new ItemStack(Items.DANDELION, 3));
			helper.assertTrue(menu.getSlot(4).getItem().is(Cooking.HERB_STEAK), "Reordered inputs must preview Herb Steak");
			menu.clicked(4, 0, ContainerInput.PICKUP, player);
			helper.assertTrue(menu.getCarried().is(Cooking.HERB_STEAK) && menu.getCarried().getCount() == 1, "Click must produce one meal");
			helper.assertTrue(menu.getSlot(2).getItem().getCount() == 2 && menu.getSlot(3).getItem().getCount() == 2, "Click must consume one of each ingredient");
			menu.setCarried(ItemStack.EMPTY);
			menu.clicked(4, 0, ContainerInput.QUICK_MOVE, player);
			helper.assertTrue(count(player, Cooking.HERB_STEAK) == 2, "Shift-click must craft exactly the remaining two meals");
			helper.assertTrue(menu.getSlot(2).getItem().isEmpty() && menu.getSlot(3).getItem().isEmpty() && menu.getSlot(4).getItem().isEmpty(), "Exhausted ingredients must clear output");
			menu.clicked(4, 0, ContainerInput.QUICK_MOVE, player);
			helper.assertTrue(count(player, Cooking.HERB_STEAK) == 2, "Empty output must not duplicate meals");
		} finally { removePlayer(player); }
		helper.succeed();
	}

	@GameTest
	public void craftingAndEatingReturnBucketsBottlesAndBowls(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			CookingMenu menu = new CookingMenu(1, player.getInventory());
			menu.getSlot(3).set(new ItemStack(Items.WATER_BUCKET));
			menu.clicked(4, 0, ContainerInput.QUICK_MOVE, player);
			helper.assertTrue(count(player, Cooking.SALT) == 1 && menu.getSlot(3).getItem().is(Items.BUCKET), "Salt must return its bucket to the original slot");
			menu.getSlot(3).set(ItemStack.EMPTY);
			menu.getSlot(1).set(new ItemStack(Cooking.SALT));
			menu.getSlot(3).set(new ItemStack(Items.MILK_BUCKET));
			menu.clicked(4, 0, ContainerInput.QUICK_MOVE, player);
			helper.assertTrue(count(player, Cooking.CHEESE) == 1 && menu.getSlot(3).getItem().is(Items.BUCKET), "Cheese must consume salt and return its milk bucket");
			menu.getSlot(3).set(ItemStack.EMPTY);
			menu.getSlot(1).set(new ItemStack(Items.COOKED_PORKCHOP));
			menu.getSlot(2).set(new ItemStack(Items.HONEY_BOTTLE, 2));
			menu.clicked(4, 0, ContainerInput.QUICK_MOVE, player);
			helper.assertTrue(count(player, Cooking.HONEY_PORK) == 1 && count(player, Items.GLASS_BOTTLE) == 1, "Stacked honey must return one bottle without losing the remaining honey");
			helper.assertTrue(menu.getSlot(2).getItem().is(Items.HONEY_BOTTLE) && menu.getSlot(2).getItem().getCount() == 1, "Unused honey must remain");
			ItemStack bowl = new ItemStack(Cooking.FISH_CHOWDER).finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(bowl.is(Items.BOWL) && bowl.getCount() == 1, "Eating chowder must return one bowl");
		} finally { removePlayer(player); }
		helper.succeed();
	}

	@GameTest
	public void fullInventoryAndInvalidRecipesCannotLoseOrCreateItems(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			CookingMenu menu = new CookingMenu(1, player.getInventory());
			for (int i = 0; i < 36; i++) { player.getInventory().setItem(i, new ItemStack(Items.STONE, 64)); }
			menu.getSlot(0).set(new ItemStack(Items.COOKED_BEEF));
			menu.getSlot(1).set(new ItemStack(Items.DANDELION));
			menu.clicked(4, 0, ContainerInput.QUICK_MOVE, player);
			helper.assertTrue(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().getCount() == 1, "Full inventory must not consume ingredients");
			menu.getSlot(2).set(new ItemStack(Items.DIRT));
			helper.assertTrue(menu.getSlot(4).getItem().isEmpty(), "Extra ingredient must invalidate the preview");
			menu.clicked(4, 0, ContainerInput.PICKUP, player);
			helper.assertTrue(menu.getCarried().isEmpty(), "An invalid recipe must not yield an old preview");
			helper.assertTrue(!menu.getSlot(4).mayPlace(new ItemStack(Items.DIRT)), "Output must reject inserts");
		} finally { removePlayer(player); }
		helper.succeed();
	}

	@GameTest
	public void stationValidityAndClosingPreserveInputsWithoutGrantingPreview(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
			helper.getLevel().setBlockAndUpdate(pos, Cooking.STATION.defaultBlockState());
			var provider = helper.getLevel().getBlockState(pos).getMenuProvider(helper.getLevel(), pos);
			CookingMenu menu = (CookingMenu) provider.createMenu(1, player.getInventory(), player);
			helper.assertTrue(menu.stillValid(player), "Placed cooking station must open a usable menu");
			ItemStack named = new ItemStack(Items.COOKED_BEEF, 2);
			named.set(DataComponents.CUSTOM_NAME, Component.literal("Dinner"));
			menu.getSlot(0).set(named);
			menu.getSlot(1).set(new ItemStack(Items.DANDELION));
			CookingMenu secondMenu = new CookingMenu(2, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), pos));
			helper.assertTrue(secondMenu.getSlot(0).getItem().isEmpty(), "Separate sessions must not share inputs");
			helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			helper.assertTrue(!menu.stillValid(player), "Breaking the station must invalidate its menu");
			menu.removed(player);
			helper.assertTrue(count(player, Items.COOKED_BEEF) == 2 && count(player, Items.DANDELION) == 1, "Closing must return unconsumed ingredients");
			helper.assertTrue(count(player, Cooking.HERB_STEAK) == 0 && menu.getSlot(4).getItem().isEmpty(), "Closing must discard the uncrafted preview");
			boolean keptName = false;
			for (int i = 0; i < 36; i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.is(Items.COOKED_BEEF)) { keptName = Component.literal("Dinner").equals(stack.get(DataComponents.CUSTOM_NAME)); }
			}
			helper.assertTrue(keptName, "Returning ingredients must preserve components");
			menu.removed(player);
			helper.assertTrue(count(player, Items.COOKED_BEEF) == 2, "Repeated close must not duplicate inputs");
		} finally { removePlayer(player); }
		helper.succeed();
	}

	@GameTest
	public void mealsReplaceEachOtherAndPreservePotionsAndAbsorption(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600));
			player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
			float absorption = player.getAbsorptionAmount();
			new ItemStack(Cooking.CHICKEN_CORDON_BLEU).finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(player.getAbsorptionAmount() == absorption + 4, "Hearty meal must add exactly two golden hearts");
			new ItemStack(Cooking.HERB_STEAK).finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(player.getAbsorptionAmount() == absorption && player.hasEffect(MobEffects.ABSORPTION), "Replacement must preserve potion absorption");
			helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 3600, "Vision must use the longer meal timer");
			new ItemStack(Cooking.FISH_CHOWDER).finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(!player.hasEffect(MealEffects.HERBAL) && player.hasEffect(MealEffects.SEAFOOD), "New meal must replace the old meal");
			helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 600, "Underlying night vision potion must remain unchanged");
			helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING), "Seafood must satisfy vanilla breathing checks");
			float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			new ItemStack(Cooking.BEEF_WELLINGTON).finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage + 3, "Wellington must grant +3 attack damage");
			helper.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING), "Replacing seafood must remove meal breathing");
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600));
			new ItemStack(Cooking.HONEY_PORK).finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage + 3 && player.hasEffect(MobEffects.STRENGTH), "Replacing Wellington must preserve potion strength");
			helper.assertTrue(MealEffects.ALL.stream().filter(player::hasEffect).count() == 1, "Only one primary meal buff may remain");
			player.setHealth(10);
			MealEffects.SWEET.value().applyEffectTick(helper.getLevel(), player, 0);
			helper.assertTrue(player.getHealth() == 11, "Honey Pork must regenerate health");
			new ItemStack(Items.MILK_BUCKET).finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(MealEffects.ALL.stream().noneMatch(player::hasEffect), "Milk must clear meal buffs normally");
		} finally { removePlayer(player); }
		helper.succeed();
	}

	@GameTest
	public void mealPersistsInSaveDataAndExpiresOnServerTicks(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			MealEffects.apply(player, MealEffects.SAVORY, 20);
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			player.saveWithoutId(output);
			player.removeAllEffects();
			player.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
			helper.assertTrue(player.hasEffect(MealEffects.SAVORY) && player.getEffect(MealEffects.SAVORY).getDuration() == 20, "Save/load must retain the meal and remaining duration");
			// Mock Connection: Embedded channels do not tick a network listener, which normally calls doTick.
			for (int i = 0; i < 25; i++) { player.doTick(); }
			helper.assertTrue(!player.hasEffect(MealEffects.SAVORY), "Meal must expire on server ticks");
			helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(darkspawn.black.Darkspawn.id("meal_strength")) == null, "Expiration must remove the meal's attribute modifier");
		} finally { removePlayer(player); }
		helper.succeed();
	}
}
