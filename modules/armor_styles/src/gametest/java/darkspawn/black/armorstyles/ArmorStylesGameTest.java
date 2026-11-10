package darkspawn.black.armorstyles;

import com.mojang.authlib.GameProfile;
import darkspawn.black.core.DarkspawnCore;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.phys.Vec3;

public final class ArmorStylesGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "expedition-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		Vec3 pos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 2)));
		player.teleportTo(pos.x, pos.y, pos.z);
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		player.setOnGround(true);
		return player;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }

	private static final ArmorType[] ARMOR_TYPES = { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS };
	private static java.util.List<ItemStack> takeOffArmor(ServerPlayer player) {
		var removed = new java.util.ArrayList<ItemStack>();
		for (ArmorType type : ARMOR_TYPES) { removed.add(player.getItemBySlot(type.getSlot())); player.setItemSlot(type.getSlot(), ItemStack.EMPTY); }
		ArmorStyleBonuses.update(player);
		return removed;
	}

	private static void styleArmor(ServerPlayer player, String style) {
		for (ArmorType type : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) {
			String metal = type == ArmorType.HELMET || type == ArmorType.LEGGINGS ? "iron" : "diamond";
			var item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(metal + "_" + type.getName()));
			player.setItemSlot(type.getSlot(), ArmorStyles.apply(new ItemStack(item), style));
		}
	}

	@GameTest public void matchingStyleBonusesStackOnceAndStopWithPartialOrRemovedSets(GameTestHelper helper) {
		var player = player(helper);
		try {
			styleArmor(player, "war_chief"); player.doTick();
			ArmorStyleBonuses.update(player); ArmorStyleBonuses.update(player);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 2) < .001, "War Chief adds one damage once to the base attack");
			player.setItemSlot(EquipmentSlot.HEAD, ArmorStyles.apply(new ItemStack(Items.IRON_HELMET), "knight"));
			ArmorStyleBonuses.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1, "Mixed styles remove only the set bonus");
			styleArmor(player, "berserker"); player.doTick(); ArmorStyleBonuses.update(player);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - 4 * 1.10) < .001, "Berserker raises native recharge rate");
			takeOffArmor(player); ArmorStyleBonuses.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_SPEED) == 4, "Removed armor grants no attack speed");
			styleArmor(player, "knight"); player.doTick(); ArmorStyleBonuses.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 6, "Mixed iron/diamond Knight adds two toughness to diamond chest and boots");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); ArmorStyleBonuses.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 4, "Missing piece removes set toughness while keeping material toughness");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void utilityStylesRespectHealthCapsAndRemoveOnlyOwnedBonuses(GameTestHelper helper) {
		var player = player(helper);
		try {
			var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
			var other = DarkspawnCore.id("test_external_speed");
			speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(other, .02, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
			double baseline = speed.getValue();
			styleArmor(player, "pathfinder"); player.doTick(); ArmorStyleBonuses.update(player); ArmorStyleBonuses.update(player);
			helper.assertTrue(Math.abs(speed.getValue() - baseline * 1.05) < .00001, "Pathfinder multiplies current speed once alongside external bonuses");
			styleArmor(player, "juggernaut"); player.doTick(); ArmorStyleBonuses.update(player);
			helper.assertTrue(Math.abs(speed.getValue() - baseline) < .00001 && speed.getModifier(other) != null, "Changing sets preserves external speed");
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - .15) < .00001, "Juggernaut adds fifteen percentage points of resistance");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); ArmorStyleBonuses.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0, "Incomplete set removes resistance");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void weaponStylesOnlyIncreaseEligibleDirectMeleeDamage(GameTestHelper helper) {
		var player = player(helper);
		var target = (net.minecraft.world.entity.Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_golem")), new BlockPos(3, 2, 2));
		try {
			target.setNoAi(true);
			for (String style : java.util.List.of("spartan", "viking", "samurai")) {
				styleArmor(player, style);
				var weapon = style.equals("spartan") ? BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_spear")) : style.equals("samurai") ? Items.IRON_SWORD : Items.IRON_AXE;
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
				target.setHealth(100); target.damageCooldownTime = 0;
				target.hurtServer(helper.getLevel(), player.getMainHandItem().getDamageSource(player), 10);
				helper.assertTrue(Math.abs(target.getHealth() - 89) < .001, "Eligible native melee source gets ten percent damage");
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
				target.setHealth(100); target.damageCooldownTime = 0;
				target.hurtServer(helper.getLevel(), player.getMainHandItem().getDamageSource(player), 10);
				helper.assertTrue(Math.abs(target.getHealth() - 90) < .001, "Wrong weapon receives no bonus");
				helper.assertTrue(ArmorStyleBonuses.meleeDamage(player.damageSources().thorns(player), 10) == 10, "Retaliation never receives weapon bonuses");
			}
		} finally { remove(player); target.discard(); }
		helper.succeed();
	}

	@GameTest public void specialistBonusesRespectWeaponsProjectilesAndMiningTools(GameTestHelper helper) {
		var player = player(helper);
		var target = (net.minecraft.world.entity.Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_golem")), new BlockPos(3, 2, 2));
		try {
			target.setNoAi(true);
			for (String style : java.util.List.of("reaver", "pirate")) {
				styleArmor(player, style);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(style.equals("reaver") ? Items.MACE : Items.TRIDENT));
				target.setHealth(100); target.damageCooldownTime = 0;
				target.hurtServer(helper.getLevel(), player.getMainHandItem().getDamageSource(player), 10);
				helper.assertTrue(Math.abs(target.getHealth() - 89) < .001, "Specialist weapon gains ten percent melee damage");
				if (style.equals("reaver")) {
					target.setHealth(100); target.damageCooldownTime = 0;
					target.hurtServer(helper.getLevel(), player.damageSources().mace(player), 10);
					helper.assertTrue(Math.abs(target.getHealth() - 89) < .001, "Native mace smash also gains ten percent damage");
				}
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
				helper.assertTrue(ArmorStyleBonuses.meleeDamage(player.getMainHandItem().getDamageSource(player), 10) == 10, "Sword receives no specialist bonus");
			}
			var trident = new net.minecraft.world.entity.projectile.arrow.ThrownTrident(helper.getLevel(), player, new ItemStack(Items.TRIDENT));
			var source = player.damageSources().trident(trident, player);
			target.setHealth(100); target.damageCooldownTime = 0;
			target.hurtServer(helper.getLevel(), source, 10);
			helper.assertTrue(Math.abs(target.getHealth() - 89) < .001, "Thrown trident gains exactly one bonus even after changing held item");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			helper.assertTrue(ArmorStyleBonuses.tridentDamage(source, 10) == 10, "Removing Pirate before impact removes its bonus");
			var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
			float base = player.getDestroySpeed(stone);
			styleArmor(player, "conquistador");
			helper.assertTrue(Math.abs(player.getDestroySpeed(stone) - base * 1.10F) < .001, "Native pickaxe mining speed receives ten percent");
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
			float axe = player.getDestroySpeed(stone);
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			helper.assertTrue(player.getDestroySpeed(stone) == axe, "Other tools receive no mining bonus");
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
			helper.assertTrue(player.getDestroySpeed(stone) == base, "Partial set has normal mining speed");
		} finally { remove(player); target.discard(); }
		helper.succeed();
	}

	@GameTest public void rangerSupportsBothLaunchWeaponsAndCrossbowRockets(GameTestHelper helper) {
		var player = player(helper);
		var target = (net.minecraft.world.entity.Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_golem")), new BlockPos(3, 2, 2));
		try {
			target.setNoAi(true);
			for (String style : java.util.List.of("bow", "crossbow")) {
				styleArmor(player, "ranger");
				var launchWeapon = style.equals("bow") ? Items.BOW : Items.CROSSBOW;
				var otherWeapon = style.equals("bow") ? Items.CROSSBOW : Items.BOW;
				var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(launchWeapon));
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(otherWeapon));
				var source = player.damageSources().arrow(arrow, player);
				target.setHealth(100); target.damageCooldownTime = 0;
				target.hurtServer(helper.getLevel(), source, 10);
				helper.assertTrue(Math.abs(target.getHealth() - 89) < .001, "Arrow uses stored launch weapon even after switching hands");
				var wrongArrow = new net.minecraft.world.entity.projectile.arrow.Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(otherWeapon));
				helper.assertTrue(ArmorStyleBonuses.rangedDamage(player.damageSources().arrow(wrongArrow, player), 10) == 11, "Both bow and crossbow launch weapons qualify");
				player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
				helper.assertTrue(ArmorStyleBonuses.rangedDamage(source, 10) == 10, "Partial set at impact grants no arrow bonus");
			}
			styleArmor(player, "ranger");
			var rocket = new net.minecraft.world.entity.projectile.FireworkRocketEntity(helper.getLevel(), new ItemStack(Items.FIREWORK_ROCKET), player, 0, 2, 0, true);
			target.setHealth(100); target.damageCooldownTime = 0;
			target.hurtServer(helper.getLevel(), player.damageSources().fireworks(rocket, player), 10);
			helper.assertTrue(Math.abs(target.getHealth() - 89) < .001, "Crossbow rocket damage receives ten percent");
			var handRocket = new net.minecraft.world.entity.projectile.FireworkRocketEntity(helper.getLevel(), new ItemStack(Items.FIREWORK_ROCKET), player, 0, 2, 0, false);
			helper.assertTrue(ArmorStyleBonuses.rangedDamage(player.damageSources().fireworks(handRocket, player), 10) == 10, "Hand-launched rockets do not receive crossbow bonus");
		} finally { remove(player); target.discard(); }
		helper.succeed();
	}

	@GameTest(maxTicks = 400) public void paladinHealingAndShadowbladeCooldownUseOnlyWornSets(GameTestHelper helper) {
		var player = player(helper);
		try {
			styleArmor(player, "paladin"); player.setHealth(10); player.heal(4);
			helper.assertTrue(Math.abs(player.getHealth() - 14.4) < .001, "Paladin scales native healing by ten percent");
			player.setHealth(19); player.heal(4);
			helper.assertTrue(player.getHealth() == 20, "Healing still respects maximum health");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); player.setHealth(10); player.heal(4);
			helper.assertTrue(player.getHealth() == 14, "Partial Paladin grants normal healing");
			styleArmor(player, "shadowblade"); player.setHealth(20);
			player.hurtServer(helper.getLevel(), player.damageSources().magic(), 1);
			var invisibility = net.minecraft.world.effect.MobEffects.INVISIBILITY;
			helper.assertTrue(player.hasEffect(invisibility) && player.getEffect(invisibility).getDuration() == 40, "Taking damage grants two seconds of invisibility");
			player.removeEffect(invisibility);
			styleArmor(player, "knight"); styleArmor(player, "shadowblade");
			player.damageCooldownTime = 0;
			player.hurtServer(helper.getLevel(), player.damageSources().magic(), 1);
			helper.assertTrue(!player.hasEffect(invisibility), "Changing armor cannot bypass the fifteen-second cooldown");
		} catch (Throwable error) { remove(player); throw error; }
		helper.runAfterDelay(301, () -> {
			try {
				player.damageCooldownTime = 0;
				player.hurtServer(helper.getLevel(), player.damageSources().magic(), 1);
				helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY), "Shadowblade reactivates after fifteen seconds");
			} finally { remove(player); }
			helper.succeed();
		});
	}

	@GameTest public void classicArmorWearIsReducedOnlyForTheWornFullSet(GameTestHelper helper) {
		var player = player(helper);
		try {
			styleArmor(player, "classic");
			var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
			helmet.hurtAndBreak(5, player, EquipmentSlot.HEAD);
			helper.assertTrue(helmet.getDamageValue() == 4, "Five points of native armor wear become four before enchantments");
			var spare = new ItemStack(Items.IRON_HELMET);
			spare.hurtAndBreak(4, player, EquipmentSlot.HEAD);
			helper.assertTrue(spare.getDamageValue() == 4, "Spare armor receives no reduction");
			player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			helmet.hurtAndBreak(4, player, EquipmentSlot.HEAD);
			helper.assertTrue(helmet.getDamageValue() == 8, "Partial Classic set receives normal wear");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void cosmeticSmithingPreservesNativeArmorAndRejectsRepeatStyles(GameTestHelper helper) {
		var level = helper.getLevel();
		for (var item : java.util.List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
				Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)) {
			var original = new ItemStack(item);
			original.setDamageValue(17);
			original.enchant(level.registryAccess().getOrThrow(Enchantments.PROTECTION), 2);
			original.set(DataComponents.CUSTOM_NAME, Component.literal("Family Armor"));
			original.set(DataComponents.TRIM, new net.minecraft.world.item.equipment.trim.ArmorTrim(
				level.registryAccess().getOrThrow(net.minecraft.world.item.equipment.trim.TrimMaterials.GOLD),
				level.registryAccess().getOrThrow(net.minecraft.world.item.equipment.trim.TrimPatterns.SENTRY)));
			var current = original;
			var styleAdditions = java.util.List.of(Items.BONE, Items.FEATHER, Items.GOLD_INGOT, Items.IRON_INGOT, Items.RABBIT_HIDE, Items.BAMBOO, Items.AMETHYST_SHARD, Items.OBSIDIAN, Items.STRING, Items.FLINT, Items.NAUTILUS_SHELL, Items.COPPER_INGOT, Items.LEATHER, Items.INK_SAC, Items.IRON_NUGGET);
			var styles = java.util.List.of("viking", "spartan", "war_chief", "knight", "berserker", "samurai", "paladin", "juggernaut", "pathfinder", "reaver", "pirate", "conquistador", "ranger", "shadowblade", "classic");
			for (int index = 0; index < styles.size(); index++) {
				var addition = styleAdditions.get(index);
				String style = styles.get(index);
				var input = new SmithingRecipeInput(new ItemStack(Items.PAPER), current, new ItemStack(addition));
				var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level).orElseThrow().value();
				current = recipe.assemble(input);
				helper.assertTrue(ArmorStyles.style(current).equals(style) && current.is(item), "Recipe changes only the intended style");
				helper.assertTrue(ItemStack.isSameItemSameComponents(original, ArmorStyles.apply(current, "classic")), "Trims, names, enchants, damage and stats survive each style change");
				helper.assertTrue(!recipe.matches(new SmithingRecipeInput(input.template(), current, input.addition()), level), "Already-applied style is rejected without consuming ingredients");
			}
		}
		var invalid = new SmithingRecipeInput(new ItemStack(Items.PAPER), new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.BONE));
		helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING, invalid, level).isEmpty(), "Styles do not accept unsupported armor");
		helper.succeed();
	}
}
