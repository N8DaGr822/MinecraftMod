package darkspawn.black.food;

import darkspawn.black.cooking.*;

import com.mojang.authlib.GameProfile;
import darkspawn.black.boss.*;
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

public final class CuisineIntegrationGameTest {
	@GameTest public void newEquipmentPowersRespectBlocksAndDoNotStack(GameTestHelper helper) {
		var player = player(helper);
		var target = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(2,2,2));
		var victim = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(3,2,2));
		try {
			double base = player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
			player.setItemSlot(EquipmentSlot.FEET, BossEmpowerment.apply(new ItemStack(Items.DIAMOND_BOOTS), "armor_highland"));
			player.setItemSlot(EquipmentSlot.HEAD, BossEmpowerment.apply(new ItemStack(Items.DIAMOND_HELMET), "armor_highland"));
			EquipmentPowers.update(player);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER)-base+.25)<.001,"Highland pieces do not stack");
			player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY); player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); EquipmentPowers.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER)==base,"Highland modifier removed on unequip");
			player.setItemInHand(InteractionHand.MAIN_HAND, BossEmpowerment.apply(new ItemStack(Items.SHIELD),"shield_frostguard"));
			player.startUsingItem(InteractionHand.MAIN_HAND);
			EquipmentPowers.shieldBlock(player,target,0);
			helper.assertTrue(!target.hasEffect(MobEffects.WEAKNESS),"Failed blocks grant no retaliation");
			EquipmentPowers.shieldBlock(player,target,4);
			helper.assertTrue(target.hasEffect(MobEffects.WEAKNESS),"Frostguard weakens on successful block");
			var bow=BossEmpowerment.apply(new ItemStack(Items.BOW),"bow_venom");
			var arrow=new Arrow(helper.getLevel(),player,new ItemStack(Items.ARROW),bow);
			var source=player.damageSources().arrow(arrow,player);
			EquipmentPowers.arrowHit(victim,source,4,true);
			helper.assertTrue(!victim.hasEffect(MobEffects.POISON),"Blocked venom arrows cannot poison");
			victim.hurtServer(helper.getLevel(),source,4);
			helper.assertTrue(victim.hasEffect(MobEffects.POISON),"Venomshot retains firing weapon and poisons");
			arrow.discard();
		} finally { target.discard(); victim.discard(); remove(player); }
		helper.succeed();
	}
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
	private static void fillShadow(CookingMenu menu) {
		menu.getSlot(0).set(new ItemStack(darkspawn.black.boss.BossCuisine.INGREDIENTS.get(BossKind.SHADOW_CREEPER_QUEEN), 2));
		menu.getSlot(1).set(new ItemStack(darkspawn.black.farming.Agriculture.RICE.ingredient(), 2));
		menu.getSlot(2).set(new ItemStack(Cooking.BUTTER, 2));
		menu.getSlot(3).set(new ItemStack(darkspawn.black.farming.Agriculture.WILD_HERBS, 2));
	}
	@GameTest public void lockedCookingAndDuplicateScrollsCannotConsumeOrDuplicate(GameTestHelper helper) {
		var player = player(helper);
		try {
			CookingMenu menu = new CookingMenu(10, player.getInventory());
			fillShadow(menu);
			helper.assertTrue(menu.getSlot(4).getItem().isEmpty(), "Locked recipe must not produce output");
			menu.clicked(4, 0, ContainerInput.QUICK_MOVE, player);
			helper.assertTrue(menu.getSlot(0).getItem().getCount() == 2, "Locked attempts must preserve ingredients");
			Item scroll = Cuisine.SCROLLS.get("shadow_stew");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(scroll, 2));
			scroll.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
			helper.assertTrue(player.getMainHandItem().getCount() == 1, "Learning consumes exactly one scroll");
			scroll.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
			helper.assertTrue(player.getMainHandItem().getCount() == 1, "Duplicate learning consumes nothing");
			menu.slotsChanged(null);
			helper.assertTrue(menu.getSlot(4).getItem().is(Cuisine.DISHES.get("shadow_stew").item()), "Learned recipe must work");
			menu.clicked(4, 1, ContainerInput.PICKUP, player);
			helper.assertTrue(menu.getCarried().getCount() == 1 && menu.getSlot(0).getItem().getCount() == 1, "Right click crafts exactly once");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void cookbookShowsPersonalLockedAndUnlockedRecipes(GameTestHelper helper) {
		var player = player(helper);
		try {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Cuisine.COOKBOOK));
			Cuisine.COOKBOOK.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
			var book = player.getMainHandItem().get(DataComponents.WRITTEN_BOOK_CONTENT);
			helper.assertTrue(book != null && book.pages().size() > 40, "Cookbook must include all station recipes");
			String locked = book.getPages(false).stream().map(c -> c.getString()).filter(s -> s.contains("Shadow Stew")).findFirst().orElseThrow();
			helper.assertTrue(locked.contains("Locked") && locked.contains("Ancient City"), "Locked preview must include discovery hint");
			Culinary.progress(player).unlock("shadow_stew");
			Cuisine.COOKBOOK.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
			String known = player.getMainHandItem().get(DataComponents.WRITTEN_BOOK_CONTENT).getPages(false).stream()
				.map(c -> c.getString()).filter(s -> s.contains("Shadow Stew")).findFirst().orElseThrow();
			helper.assertTrue(!known.contains("Locked") && known.contains("Royal Jelly"), "Reopened book must show learned ingredients");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void armorDoesNotStackAndUnequippingRemovesItsBonus(GameTestHelper helper) {
		var player = player(helper);
		try {
			double base = player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
			player.setItemSlot(EquipmentSlot.HEAD, BossEmpowerment.apply(new ItemStack(Items.DIAMOND_HELMET), "armor_tide"));
			player.setItemSlot(EquipmentSlot.CHEST, BossEmpowerment.apply(new ItemStack(Items.DIAMOND_CHESTPLATE), "armor_tide"));
			EquipmentPowers.update(player);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) - base - 0.35) < 0.001, "Two pieces grant only one bonus");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			EquipmentPowers.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) == base, "Unequipping removes owned bonus");
			player.setItemSlot(EquipmentSlot.FEET, BossEmpowerment.apply(new ItemStack(Items.DIAMOND_BOOTS), "armor_winter"));
			player.setTicksFrozen(200); EquipmentPowers.update(player);
			helper.assertTrue(player.getTicksFrozen() == 0, "Winter armor clears freezing");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void arrowsKeepFiringPowerAndBlockedShotsDoNothing(GameTestHelper helper) {
		var player = player(helper);
		var target = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2));
		try {
			var bow = BossEmpowerment.apply(new ItemStack(Items.BOW), "bow_voidmark");
			var arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), bow);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			var source = player.damageSources().arrow(arrow, player);
			EquipmentPowers.arrowHit(target, source, 4, true);
			helper.assertTrue(!target.hasEffect(MobEffects.GLOWING), "Blocked shots cannot apply powers");
			target.hurtServer(helper.getLevel(), source, 4);
			helper.assertTrue(target.hasEffect(MobEffects.GLOWING) && target.hasEffect(MobEffects.SLOWNESS), "Arrow retains its original weapon power after hand changes");
			arrow.discard();
		} finally { target.discard(); remove(player); }
		helper.succeed();
	}
	@GameTest public void smithingPreservesComponentsAndRejectsWrongEquipment(GameTestHelper helper) {
		var level = helper.getLevel();
		ItemStack original = new ItemStack(Items.BOW);
		original.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Family Bow"));
		original.setDamageValue(37);
		original.enchant(level.registryAccess().getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER), 3);
		var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(Items.AMETHYST_SHARD), original,
			new ItemStack(BossItems.ESSENCES.get(BossProfile.THUNDER_BIRD)));
		var recipe = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, level).orElseThrow().value();
		ItemStack result = recipe.assemble(input);
		helper.assertTrue("bow_stormshot".equals(result.get(BossEmpowerment.POWER)) && result.getDamageValue() == 37
			&& result.getHoverName().getString().equals("Family Bow") && result.isEnchanted(), "Smithing preserves damage, name and enchantments");
		helper.assertTrue(!recipe.matches(new net.minecraft.world.item.crafting.SmithingRecipeInput(input.template(), result, input.addition()), level), "Same power cannot be applied again");
		helper.assertTrue(!recipe.matches(new net.minecraft.world.item.crafting.SmithingRecipeInput(input.template(), new ItemStack(Items.SHIELD), input.addition()), level), "Bow recipe rejects shield");
		helper.assertTrue(original.get(BossEmpowerment.POWER) == null, "Preview never mutates the base item");
		helper.succeed();
	}
	@GameTest public void culinaryRewardsRemainAvailableAfterHeartAndAreParticipantOwned(GameTestHelper helper) {
		var first = player(helper); var second = player(helper);
		var boss = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, helper.getLevel());
		boss.setPos(first.position());
		var participants = java.util.Set.of(first.getUUID(), second.getUUID());
		try {
			darkspawn.black.health.BossHearts.progress(first).consume(BossKind.ANCIENT_TREE_SPIRIT);
			var area = first.getBoundingBox().inflate(4);
			darkspawn.black.boss.BossCuisine.grant(boss, helper.getLevel(), java.util.Set.of(), BossKind.ANCIENT_TREE_SPIRIT, 160);
			var ingredient = darkspawn.black.boss.BossCuisine.INGREDIENTS.get(BossKind.ANCIENT_TREE_SPIRIT);
			var trophy = darkspawn.black.boss.BossCuisine.TROPHIES.get(BossKind.ANCIENT_TREE_SPIRIT);
			darkspawn.black.boss.BossCuisine.grant(boss, helper.getLevel(), participants, BossKind.ANCIENT_TREE_SPIRIT, 160);
			var rewards = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
				e -> e.getItem().is(ingredient) || e.getItem().is(trophy));
			helper.assertTrue(rewards.stream().filter(e -> e.getItem().is(ingredient)).count() == 2
				&& rewards.stream().filter(e -> e.getItem().is(trophy)).count() == 2, "Each contributor receives repeatable culinary loot, including heart owners");
			for (var reward : rewards) {
				var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
				reward.saveWithoutId(output);
				helper.assertTrue(output.buildResult().contains("Owner"), "Rewards must reserve pickup for their participant");
				reward.discard();
			}
			var adv = helper.getLevel().getServer().getAdvancements().get(darkspawn.black.Darkspawn.id("progress/defeat_ancient_tree_spirit"));
			helper.assertTrue(first.getAdvancements().getOrStartProgress(adv).isDone() && second.getAdvancements().getOrStartProgress(adv).isDone(), "Both contributors earn the defeat advancement");
		} finally { boss.discard(); remove(first); remove(second); }
		helper.succeed();
	}
}
