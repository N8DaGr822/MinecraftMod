package darkspawn.black.expeditiongear;

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

public final class ExpeditionGearGameTest {
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
		return removed;
	}
	private static void putOnArmor(ServerPlayer player, java.util.List<ItemStack> armor) {
		for (int i = 0; i < ARMOR_TYPES.length; i++) player.setItemSlot(ARMOR_TYPES[i].getSlot(), armor.get(i));
	}

	@GameTest public void paddedBootsMultiplyFallDamageAndUnequipCleanly(GameTestHelper helper) {
		var player = player(helper);
		try {
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ExpeditionArmor.SPELUNKING.get(ArmorType.BOOTS)));
			player.doTick();
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) - .75) < .0001, "Boots multiply remaining fall damage");
			player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			player.doTick();
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) - 1) < .0001, "Unequipping removes the boot modifier");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void mountainSuitAndBootsOnlyProtectWhileEquipped(GameTestHelper helper) {
		var player = player(helper);
		try {
			helper.assertTrue(player.canFreeze(), "Baseline player can freeze");
			for (var entry : ExpeditionArmor.MOUNTAINEERING.entrySet()) player.setItemSlot(entry.getKey().getSlot(), new ItemStack(entry.getValue()));
			helper.assertTrue(!player.canFreeze() && PowderSnowBlock.canEntityWalkOnPowderSnow(player), "Full suit prevents freezing and boots walk on powder snow");
			player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			helper.assertTrue(player.canFreeze() && PowderSnowBlock.canEntityWalkOnPowderSnow(player), "Incomplete suit loses immunity but retains boot function");
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ExpeditionArmor.MOUNTAINEERING.get(ArmorType.CHESTPLATE)));
			var suit = takeOffArmor(player);
			helper.assertTrue(player.canFreeze() && !PowderSnowBlock.canEntityWalkOnPowderSnow(player), "Removed suit grants no snow or freezing protection");
			putOnArmor(player, suit);
			helper.assertTrue(!player.canFreeze(), "Wearing the suit again restores protection");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void headlampDoesNotOverwritePotionsAndStopsWhenRemoved(GameTestHelper helper) {
		var player = player(helper);
		try {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600));
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ExpeditionArmor.SPELUNKING.get(ArmorType.HELMET)));
			helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration(), "Lamp avoids the short-duration night vision flicker");
			helper.assertTrue(player.getActiveEffectsMap().get(MobEffects.NIGHT_VISION).getDuration() == 600, "Original potion timer remains unchanged");
			var helmet = takeOffArmor(player);
			helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 600, "Removing helmet restores the existing potion");
			player.removeAllEffects();
			helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Removed headlamp grants no vision");
			putOnArmor(player, helmet);
			helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION) && player.getActiveEffectsMap().isEmpty(), "Equipped lamp works without creating persistent potion effects");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void divingHelmetBonusesStopWhenRemoved(GameTestHelper helper) {
		var player = player(helper);
		try {
			double mining = player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ExpeditionArmor.DIVER.get(ArmorType.HELMET)));
			player.doTick();
			helper.assertTrue(player.getAttributeValue(Attributes.OXYGEN_BONUS) == 2, "Diver supplies native oxygen bonus");
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED) - mining * 2.5) < .0001, "Diver improves underwater mining");
			helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION) && !player.hasEffect(MobEffects.WATER_BREATHING), "Dry helmet grants neither vision nor infinite air");
			for (int y = 2; y <= 5; y++) helper.setBlock(new BlockPos(1, y, 2), net.minecraft.world.level.block.Blocks.WATER);
			player.doTick(); player.doTick();
			helper.assertTrue(player.isUnderWater() && player.hasEffect(MobEffects.NIGHT_VISION), "Submerged helmet supplies vision");
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600));
			takeOffArmor(player);
			player.doTick();
			helper.assertTrue(player.getAttributeValue(Attributes.OXYGEN_BONUS) == 0 && Math.abs(player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED) - mining) < .0001, "Removed suit contributes no attributes");
			helper.assertTrue(!player.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration(), "Removing helmet preserves the original timed potion");
			player.removeAllEffects();
			helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Removed diving helmet supplies no vision");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void ashwalkerReducesFireButNotOrdinaryDamageOrLavaToZero(GameTestHelper helper) {
		var player = player(helper);
		try {
			for (var entry : ExpeditionArmor.ASHWALKER.entrySet()) player.setItemSlot(entry.getKey().getSlot(), new ItemStack(entry.getValue()));
			player.doTick();
			// Damage Isolation: Remove defense attributes so this fixture measures only fire protection.
			player.getAttribute(Attributes.ARMOR).removeModifiers();
			player.damageCooldownTime = 0;
			helper.assertTrue(player.hurtServer(helper.getLevel(), player.damageSources().inFire(), 10), "Fire damage still applies");
			helper.assertTrue(Math.abs(player.getHealth() - 14) < .001, "Four plates reduce fire by forty percent");
			player.setHealth(20); player.damageCooldownTime = 0;
			player.hurtServer(helper.getLevel(), player.damageSources().generic(), 10);
			helper.assertTrue(Math.abs(player.getHealth() - 10) < .001, "Ordinary damage receives no fire reduction");
			player.setHealth(20); player.damageCooldownTime = 0;
			player.hurtServer(helper.getLevel(), player.damageSources().lava(), 10);
			helper.assertTrue(Math.abs(player.getHealth() - 14) < .001, "Lava remains damaging");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			player.setHealth(20); player.damageCooldownTime = 0;
			player.hurtServer(helper.getLevel(), player.damageSources().inFire(), 10);
			helper.assertTrue(Math.abs(player.getHealth() - 13) < .001, "Three plates reduce fire by thirty percent");
			player.setHealth(20); player.damageCooldownTime = 0;
			for (var type : ExpeditionArmor.ASHWALKER.keySet()) player.setItemSlot(type.getSlot(), ItemStack.EMPTY);
			player.hurtServer(helper.getLevel(), player.damageSources().inFire(), 10);
			helper.assertTrue(Math.abs(player.getHealth() - 10) < .001, "Unequipped suit supplies no protection");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void smithingRecipesAndTagsWorkForEveryPiece(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String family : new String[] { "mountaineering", "spelunking", "diver", "desert_wanderer", "ashwalker" }) for (ArmorType type : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) {
			var original = new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_" + type.getName())));
			original.enchant(level.registryAccess().getOrThrow(Enchantments.PROTECTION), 2);
			original.set(DataComponents.CUSTOM_NAME, Component.literal("Family Gear"));
			original.setDamageValue(30);
			var addition = switch (family) {
				case "diver" -> Items.PRISMARINE_SHARD;
				case "desert_wanderer" -> Items.RABBIT_HIDE;
				case "ashwalker" -> Items.MAGMA_CREAM;
				case "mountaineering" -> BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blue_wool"));
				default -> type == ArmorType.HELMET ? Items.GLOWSTONE_DUST : type == ArmorType.BOOTS ? Items.SLIME_BALL : Items.COPPER_INGOT;
			};
			var input = new SmithingRecipeInput(new ItemStack(Items.LEATHER), original, new ItemStack(addition));
			var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level).orElseThrow().value();
			var result = recipe.assemble(input);
			helper.assertTrue(result.is(BuiltInRegistries.ITEM.getValue(DarkspawnCore.id(family + "_" + type.getName()))) && result.getDamageValue() == 30 && result.isEnchanted(), "Native smithing converts the correct item and preserves wear/enchantments");
			helper.assertTrue(result.is(ItemTags.ARMOR_ENCHANTABLE) && result.is(ItemTags.DURABILITY_ENCHANTABLE) && result.is(ItemTags.EQUIPPABLE_ENCHANTABLE), "Enchantment tags support protection, durability, and binding");
			helper.assertTrue(!recipe.matches(new SmithingRecipeInput(input.template(), result, input.addition()), level), "Converted armor cannot be repeatedly upgraded");
		}
		helper.succeed();
	}
}
