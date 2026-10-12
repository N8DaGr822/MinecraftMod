package darkspawn.black.equipment;

import com.mojang.authlib.GameProfile;
import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossEmpowerment;
import darkspawn.black.boss.EquipmentPowers;
import darkspawn.black.cooking.MealEffects;
import darkspawn.black.inventory.ArmorLoadoutPayload;
import darkspawn.black.inventory.ArmorLoadouts;
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

public final class ExpeditionArmorGameTest {
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

	@GameTest public void climateProtectionRequiresWornGearAndEnchantmentsStayLimited(GameTestHelper helper) {
		var player = player(helper);
		try {
			var warmth = player.registryAccess().getOrThrow(Temperature.WARMTH);
			var cooling = player.registryAccess().getOrThrow(Temperature.COOLING);
			styleArmor(player, "classic");
			for (ArmorType slot : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) player.getItemBySlot(slot.getSlot()).enchant(warmth, 2);
			helper.assertTrue(Temperature.protectedClimate(player, -110) == -86, "Enchanted ordinary armor still reaches dangerous cold");
			helper.assertTrue(!net.minecraft.world.item.enchantment.Enchantment.areCompatible(warmth, cooling), "Warmth and Cooling cannot share a piece");
			for (var piece : ExpeditionArmor.MOUNTAINEERING.entrySet()) {
				var item = new ItemStack(piece.getValue()); helper.assertTrue(warmth.value().canEnchant(item), "Expedition pieces accept native climate enchants");
				player.setItemSlot(piece.getKey().getSlot(), item);
			}
			helper.assertTrue(Temperature.protectedClimate(player, -110) == -30 && Temperature.protectedClimate(player, 110) == 130, "Mountain clothing protects cold but worsens heat");
			helper.assertTrue(Temperature.protectedClimate(player, -170) == -90, "Unenchanted mountain suit alone cannot solve extreme cold");
			for (var piece : ExpeditionArmor.MOUNTAINEERING.entrySet()) player.getItemBySlot(piece.getKey().getSlot()).enchant(warmth, 2);
			helper.assertTrue(Temperature.protectedClimate(player, -170) == -66, "Enchantments make extreme expeditions survivable but still strenuous");
			MealEffects.apply(player, MealEffects.WARMING, 600);
			helper.assertTrue(Temperature.protectedClimate(player, -170) == -51, "Existing warming food helps without replacing clothing");
			player.removeAllEffects(); Temperature.state(player).restore(-70, 10, 0);
			ArmorLoadouts.state(player).unlocked = 2; swap(player, 1);
			helper.assertTrue(Temperature.state(player).exposure == -70 && Temperature.protectedClimate(player, -110) == -110, "Loadout swap preserves exposure and stored protection is inactive");
			for (var piece : ExpeditionArmor.DESERT_WANDERER.entrySet()) player.setItemSlot(piece.getKey().getSlot(), new ItemStack(piece.getValue()));
			helper.assertTrue(Temperature.protectedClimate(player, 110) == 30, "Desert suit supports sustained dry heat");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			helper.assertTrue(Temperature.protectedClimate(player, 110) == 50, "Removing one piece removes exactly its protection");
			for (var piece : ExpeditionArmor.DIVER.entrySet()) player.setItemSlot(piece.getKey().getSlot(), new ItemStack(piece.getValue()));
			helper.assertTrue(Temperature.protectedClimate(player, -60) == -20, "Wet divers benefit from their worn suit");
			Temperature.state(player).wetSeconds = 0;
			helper.assertTrue(Temperature.protectedClimate(player, -60) == -60, "Dry diving suit cannot replace mountain insulation");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void temperatureDamageAndModifiersRecoverWithoutErasingOtherEffects(GameTestHelper helper) {
		var player = player(helper);
		try {
			for (var piece : ExpeditionArmor.MOUNTAINEERING.entrySet()) player.setItemSlot(piece.getKey().getSlot(), new ItemStack(piece.getValue()));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600));
			Temperature.state(player).restore(-100, 0, 4);
			Temperature.tick(player);
			helper.assertTrue(player.getHealth() == 19 && Temperature.state(player).exposure == -98, "Climate damage bypasses armor and powder-snow immunity while recovery remains gradual");
			helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Darkspawn.id("temperature_movement")) != null, "Severe exposure adds its own slowdown");
			Temperature.state(player).restore(0, 0, 0); Temperature.tick(player);
			helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Darkspawn.id("temperature_movement")) == null && player.hasEffect(MobEffects.SLOWNESS), "Recovery removes only climate-owned penalties");
			Temperature.state(player).restore(100, 0, 4); player.damageCooldownTime = 0;
			Temperature.tick(player);
			helper.assertTrue(player.getHealth() == 18, "Heat and cold both damage through the normal server damage path");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void temperaturePersistsThroughSavingButDeathClearsExposure(GameTestHelper helper) {
		var player = player(helper); var next = player(helper);
		try {
			Temperature.state(player).restore(-91, 27, 3);
			var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			player.saveWithoutId(output); Temperature.state(player).restore(0, 0, 0);
			player.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
			helper.assertTrue(Temperature.state(player).exposure == -91 && Temperature.state(player).wetSeconds == 27 && Temperature.state(player).dangerSeconds == 3, "Saved exposure, wetness and damage cadence survive reconnect");
			helper.assertTrue(Temperature.state(next).exposure == 0, "Players have independent climate state");
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, next, false);
			helper.assertTrue(Temperature.state(next).exposure == 0, "Death respawn starts comfortable");
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, next, true);
			helper.assertTrue(Temperature.state(next).exposure == -91 && Temperature.state(next).wetSeconds == 27 && Temperature.state(next).dangerSeconds == 3, "Living transfers retain climate state");
		} finally { remove(player); remove(next); }
		helper.succeed();
	}

	@GameTest(skyAccess = true) public void temperatureSamplesBiomeShelterWetnessAndCampfires(GameTestHelper helper) {
		var player = player(helper); var level = helper.getLevel(); var pos = player.blockPosition();
		var originalBiome = level.getBiome(pos).unwrapKey().orElseThrow(); long originalTime = level.getOverworldClockTime();
		helper.setBiome(net.minecraft.world.level.biome.Biomes.DESERT); helper.setTime(6000);
		helper.runAfterDelay(5, () -> {
			try {
				float dry = Temperature.climate(player);
				Temperature.state(player).wetSeconds = 30;
				helper.assertTrue(Temperature.climate(player) == dry - 20, "Wetness cools the sampled environment");
				Temperature.state(player).wetSeconds = 0;
				level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
				helper.assertTrue(Temperature.climate(player) == Math.min(170, dry + 55), "Lit campfire contributes bounded local heat");
				level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
				helper.assertTrue(Temperature.climate(player) == dry, "Extinguished fire provides no heat");
				level.setBlockAndUpdate(pos.above(14), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
			} catch (RuntimeException | Error failure) { remove(player); helper.setBiome(originalBiome); helper.setTime(originalTime); throw failure; }
			// Shelter Sampling: Sky visibility follows asynchronous lighting, not a fixed number of server ticks.
			helper.startSequence().thenWaitUntil(() -> helper.assertTrue(!level.canSeeSky(player.blockPosition()), "Roof lighting has reached the player")).thenExecute(() -> {
				try {
					helper.assertTrue(Math.abs(Temperature.climate(player)) < TemperatureState.WARNING, "Deep cover moderates desert climate for spelunking");
					Temperature.state(player).wetSeconds = 1; Temperature.tick(player);
					helper.assertTrue(Temperature.state(player).wetSeconds == 0, "Sheltered players dry naturally");
				} finally { level.setBlockAndUpdate(pos.above(14), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()); remove(player); helper.setBiome(originalBiome); helper.setTime(originalTime); }
			}).thenSucceed();
		});
	}
	private static void styleArmor(ServerPlayer player, String style) {
		for (ArmorType type : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) {
			String metal = type == ArmorType.HELMET || type == ArmorType.LEGGINGS ? "iron" : "diamond";
			var item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(metal + "_" + type.getName()));
			player.setItemSlot(type.getSlot(), ArmorStyles.apply(new ItemStack(item), style));
		}
	}
	private static void swap(ServerPlayer player, int target) {
		player.setOnGround(true);
		player.inventoryMenu.broadcastFullState();
		ArmorLoadouts.request(player, new ArmorLoadoutPayload(target, ArmorLoadouts.state(player).revision, player.inventoryMenu.getStateId()));
		for (int i = 0; i < ArmorLoadouts.SWITCH_TICKS; i++) ArmorLoadouts.tick(player);
	}

	@GameTest public void mountainSuitAndBootsOnlyProtectWhileEquipped(GameTestHelper helper) {
		var player = player(helper);
		try {
			ArmorLoadouts.state(player).unlocked = 2;
			helper.assertTrue(player.canFreeze(), "Baseline player can freeze");
			for (var entry : ExpeditionArmor.MOUNTAINEERING.entrySet()) player.setItemSlot(entry.getKey().getSlot(), new ItemStack(entry.getValue()));
			helper.assertTrue(!player.canFreeze() && PowderSnowBlock.canEntityWalkOnPowderSnow(player), "Full suit prevents freezing and boots walk on powder snow");
			player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			helper.assertTrue(player.canFreeze() && PowderSnowBlock.canEntityWalkOnPowderSnow(player), "Incomplete suit loses immunity but retains boot function");
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ExpeditionArmor.MOUNTAINEERING.get(ArmorType.CHESTPLATE)));
			swap(player, 1);
			helper.assertTrue(ArmorLoadouts.state(player).active == 1 && player.canFreeze() && !PowderSnowBlock.canEntityWalkOnPowderSnow(player), "Stored suit grants no snow or freezing protection");
			swap(player, 0);
			helper.assertTrue(!player.canFreeze(), "Switching back restores protection");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void headlampDoesNotOverwritePotionsOrMealsAndStopsOnSwap(GameTestHelper helper) {
		var player = player(helper);
		try {
			ArmorLoadouts.state(player).unlocked = 2;
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600));
			MealEffects.apply(player, MealEffects.HERBAL, 1200);
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ExpeditionArmor.SPELUNKING.get(ArmorType.HELMET)));
			helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration(), "Lamp avoids the short-duration night vision flicker");
			helper.assertTrue(player.getActiveEffectsMap().get(MobEffects.NIGHT_VISION).getDuration() == 600 && player.getEffect(MealEffects.HERBAL).getDuration() == 1200, "Original potion and meal timers remain unchanged");
			swap(player, 1);
			helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 1200, "Removing helmet restores longest existing vision source");
			player.removeAllEffects();
			helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Stored headlamp grants no vision");
			swap(player, 0);
			helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION) && player.getActiveEffectsMap().isEmpty(), "Equipped lamp works without creating persistent potion effects");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void paddedBootsCombineWithBossAndMealModifiersAndUnequipCleanly(GameTestHelper helper) {
		var player = player(helper);
		try {
			var boots = BossEmpowerment.apply(new ItemStack(ExpeditionArmor.SPELUNKING.get(ArmorType.BOOTS)), "armor_highland");
			player.setItemSlot(EquipmentSlot.FEET, boots);
			player.doTick();
			EquipmentPowers.update(player);
			MealEffects.apply(player, MealEffects.SUREFOOTED, 600);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) - .75 * .75 * .65) < .0001, "Boots multiply remaining damage alongside existing sources");
			player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			player.doTick();
			EquipmentPowers.update(player);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) - .65) < .0001, "Unequipping removes only armor bonuses, preserving meal");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void divingHelmetBonusesStopWhenStored(GameTestHelper helper) {
		var player = player(helper);
		try {
			ArmorLoadouts.state(player).unlocked = 2;
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
			swap(player, 1);
			player.doTick();
			helper.assertTrue(player.getAttributeValue(Attributes.OXYGEN_BONUS) == 0 && Math.abs(player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED) - mining) < .0001, "Stored suit contributes no attributes");
			helper.assertTrue(!player.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration(), "Removing helmet preserves the original timed potion");
			player.removeAllEffects();
			helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Stored diving helmet supplies no vision");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void ashwalkerReducesFireButNotOrdinaryDamageOrLavaToZero(GameTestHelper helper) {
		var player = player(helper);
		try {
			ArmorLoadouts.state(player).unlocked = 2;
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

	@GameTest public void matchingStyleBonusesStackOnceAndStopWithPartialOrStoredSets(GameTestHelper helper) {
		var player = player(helper);
		try {
			ArmorLoadouts.state(player).unlocked = 2;
			styleArmor(player, "war_chief"); player.doTick();
			MealEffects.apply(player, MealEffects.SAVORY, 600);
			ArmorStyleBonuses.update(player); ArmorStyleBonuses.update(player);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 5) < .001, "War Chief adds one damage once alongside the three-damage meal and base attack");
			player.setItemSlot(EquipmentSlot.HEAD, ArmorStyles.apply(new ItemStack(Items.IRON_HELMET), "knight"));
			ArmorStyleBonuses.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4, "Mixed styles remove only the set bonus");
			styleArmor(player, "berserker"); player.doTick(); ArmorStyleBonuses.update(player);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - 4 * 1.10) < .001, "Berserker raises native recharge rate");
			swap(player, 1); ArmorStyleBonuses.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_SPEED) == 4, "Stored armor grants no attack speed");
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
			ArmorLoadouts.state(player).unlocked = 2;
			var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
			var other = Darkspawn.id("test_external_speed");
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
			original.set(BossEmpowerment.POWER, "armor_highland");
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
				helper.assertTrue(ItemStack.isSameItemSameComponents(original, ArmorStyles.apply(current, "classic")), "Trims, names, enchants, damage, powers and stats survive each style change");
				helper.assertTrue(!recipe.matches(new SmithingRecipeInput(input.template(), current, input.addition()), level), "Already-applied style is rejected without consuming ingredients");
			}
		}
		var invalid = new SmithingRecipeInput(new ItemStack(Items.PAPER), new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.BONE));
		helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING, invalid, level).isEmpty(), "Styles do not accept unsupported armor");
		helper.succeed();
	}

	@GameTest public void stylesSurviveLoadoutsAndDoNotReplaceNetheriteOrExpeditionEquipment(GameTestHelper helper) {
		var player = player(helper);
		try {
			var level = helper.getLevel();
			ArmorLoadouts.state(player).unlocked = 2;
			var styled = ArmorStyles.apply(new ItemStack(Items.DIAMOND_HELMET), "spartan");
			player.setItemSlot(EquipmentSlot.HEAD, styled);
			swap(player, 1); swap(player, 0);
			helper.assertTrue(ItemStack.isSameItemSameComponents(styled, player.getItemBySlot(EquipmentSlot.HEAD)), "Loadout round trip preserves the style component");
			var upgrade = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), styled, new ItemStack(Items.NETHERITE_INGOT));
			var netherite = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, upgrade, level).orElseThrow().value().assemble(upgrade);
			helper.assertTrue(netherite.is(Items.NETHERITE_HELMET) && !ArmorStyles.supports(netherite), "Netherite uses its own rendering and stats");
			helper.assertTrue(netherite.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(net.minecraft.resources.Identifier.withDefaultNamespace("netherite")), "Style never pins the equipped material to diamond");
			var conversion = new SmithingRecipeInput(new ItemStack(Items.LEATHER), ArmorStyles.apply(new ItemStack(Items.IRON_HELMET), "viking"), new ItemStack(Items.PRISMARINE_SHARD));
			var diver = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, conversion, level).orElseThrow().value().assemble(conversion);
			helper.assertTrue(diver.is(ExpeditionArmor.DIVER.get(ArmorType.HELMET)) && !ArmorStyles.supports(diver), "Expedition conversion selects its own helmet");
			helper.assertTrue(diver.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(darkspawn.black.Darkspawn.id("diver")), "Style does not override expedition visuals");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void smithingRecipesTagsAndEquipmentPowersWorkForEveryPiece(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String family : new String[] { "mountaineering", "spelunking", "diver", "desert_wanderer", "ashwalker" }) for (ArmorType type : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) {
			var original = new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_" + type.getName())));
			original.enchant(level.registryAccess().getOrThrow(Enchantments.PROTECTION), 2);
			original.set(DataComponents.CUSTOM_NAME, Component.literal("Family Gear"));
			original.set(BossEmpowerment.POWER, "armor_tide");
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
			helper.assertTrue(result.is(BuiltInRegistries.ITEM.getValue(Darkspawn.id(family + "_" + type.getName()))) && result.getDamageValue() == 30 && result.isEnchanted(), "Native smithing converts the correct item and preserves wear/enchantments");
			helper.assertTrue(result.is(ItemTags.ARMOR_ENCHANTABLE) && result.is(ItemTags.DURABILITY_ENCHANTABLE) && result.is(ItemTags.EQUIPPABLE_ENCHANTABLE), "Enchantment tags support protection, durability, and binding");
			var empower = new SmithingRecipeInput(new ItemStack(Items.AMETHYST_SHARD), result, new ItemStack(BuiltInRegistries.ITEM.getValue(Darkspawn.id("mountain_titan_essence"))));
			helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING, empower, level).isPresent(), "Existing boss armor recipes accept new gear");
			helper.assertTrue(!recipe.matches(new SmithingRecipeInput(input.template(), result, input.addition()), level), "Converted armor cannot be repeatedly upgraded");
		}
		helper.succeed();
	}
	private void legendaryArmor(ServerPlayer player, String name) {
		LegendaryArmor.SETS.get(name).forEach((type, item) -> player.setItemSlot(type.getSlot(), new ItemStack(item)));
	}

	@GameTest public void legendarySmithingRequiresBossCoresAndPreservesArmor(GameTestHelper helper) {
		var level = helper.getLevel();
		var pairs = java.util.Map.of("dragonlord", java.util.List.of("ice_wyrm", "netherborn"), "phoenix", java.util.List.of("thunder_bird", "netherborn"),
			"voidwalker", java.util.List.of("void_eye", "shadow_creeper_queen"), "titan", java.util.List.of("mountain_titan", "fossil_tyrant"), "soul_reaper", java.util.List.of("soulbound_colossus", "baba_yaga"));
		for (String name : LegendaryArmor.NAMES) {
			var ingredients = new java.util.ArrayList<ItemStack>();
			pairs.get(name).forEach(boss -> ingredients.add(new ItemStack(BuiltInRegistries.ITEM.getValue(Darkspawn.id(boss + "_essence")))));
			ingredients.add(new ItemStack(Items.NETHERITE_INGOT));
			var coreInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 1, ingredients);
			var coreRecipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, coreInput, level).orElseThrow().value();
			helper.assertTrue(coreRecipe.assemble(coreInput).is(LegendaryArmor.CORES.get(name)), "Two boss essences and a netherite ingot forge one core");
			for (var piece : LegendaryArmor.SETS.get(name).entrySet()) {
				var original = new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("netherite_" + piece.getKey().getName())));
				original.enchant(level.registryAccess().getOrThrow(Enchantments.PROTECTION), 2);
				original.setDamageValue(17); original.set(DataComponents.CUSTOM_NAME, Component.literal("Heirloom")); original.set(BossEmpowerment.POWER, "armor_tide");
				var input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), original, new ItemStack(LegendaryArmor.CORES.get(name)));
				var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level).orElseThrow().value();
				var result = recipe.assemble(input);
				helper.assertTrue(result.is(piece.getValue()) && result.isEnchanted() && result.getDamageValue() == 17 && result.getHoverName().getString().equals("Heirloom") && "armor_tide".equals(result.get(BossEmpowerment.POWER)), "Legendary conversion preserves existing investment");
				helper.assertTrue(result.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(Darkspawn.id(name)), "Legendary conversion selects its own worn model");
				helper.assertTrue(!recipe.matches(new SmithingRecipeInput(new ItemStack(Items.PAPER), original, input.addition()), level), "Cheap style paper cannot create legendary armor");
				helper.assertTrue(!recipe.matches(new SmithingRecipeInput(input.template(), result, input.addition()), level), "Legendary pieces cannot be recursively converted");
				helper.assertTrue(result.is(ItemTags.ARMOR_ENCHANTABLE), "Legendary armor supports native enchantments");
			}
		}
		helper.succeed();
	}

	@GameTest public void legendaryTitanVoidAndDragonBonusesRequireWornFullSets(GameTestHelper helper) {
		var player = player(helper);
		var target = (net.minecraft.world.entity.Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_golem")), new BlockPos(3, 2, 2));
		try {
			ArmorLoadouts.state(player).unlocked = 2;
			legendaryArmor(player, "titan"); player.doTick(); LegendaryArmor.update(player); LegendaryArmor.update(player);
			helper.assertTrue(player.getMaxHealth() == 24 && player.getHealth() == 20, "Titan adds two hearts capacity without healing");
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - .8) < .001, "Titan adds forty points to native netherite resistance");
			player.setHealth(24); swap(player, 1);
			helper.assertTrue(player.getMaxHealth() == 20 && player.getHealth() == 20, "Storing Titan immediately removes bonus capacity");
			legendaryArmor(player, "voidwalker"); player.doTick(); LegendaryArmor.update(player);
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) - .6) < .001, "Voidwalker reduces remaining fall damage forty percent");
			var pearl = new net.minecraft.world.damagesource.DamageSource(helper.getLevel().registryAccess().getOrThrow(net.minecraft.world.damagesource.DamageTypes.ENDER_PEARL));
			player.hurtServer(helper.getLevel(), pearl, 5);
			helper.assertTrue(player.getHealth() == 20, "Native pearl damage is prevented");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); LegendaryArmor.update(player);
			helper.assertTrue(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) == 1 && LegendaryArmor.damage(player, pearl, 5) == 5, "Partial Voidwalker grants neither bonus");
			legendaryArmor(player, "dragonlord"); player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD)); target.setNoAi(true);
			target.setHealth(100); target.damageCooldownTime = 0; target.hurtServer(helper.getLevel(), player.getMainHandItem().getDamageSource(player), 10);
			helper.assertTrue(Math.abs(target.getHealth() - 87.5) < .001, "Dragonlord increases damage to high-health enemies");
			target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(39); target.setHealth(39); target.damageCooldownTime = 0;
			target.hurtServer(helper.getLevel(), player.getMainHandItem().getDamageSource(player), 10);
			helper.assertTrue(target.getHealth() == 29, "Ordinary low-health creatures receive no extra damage");
		} finally { remove(player); target.discard(); }
		helper.succeed();
	}

	@GameTest public void phoenixSavesOnceAndCooldownSurvivesSaveAndRespawn(GameTestHelper helper) {
		var player = player(helper); var restored = player(helper);
		try {
			legendaryArmor(player, "phoenix"); player.setHealth(20);
			player.hurtServer(helper.getLevel(), player.damageSources().magic(), 100);
			helper.assertTrue(player.isAlive() && player.getHealth() == 2 && player.getEffect(MobEffects.REGENERATION).getAmplifier() == 1, "Phoenix saves a fatal hit at one heart with Regeneration II");
			long ready = ((LegendaryArmorPlayer) player).darkspawn$phoenixReady();
			helper.assertTrue(ready == helper.getLevel().getServer().overworld().getGameTime() + 12000, "Phoenix reserves ten minutes before rescue");
			((LegendaryArmorPlayer) player).darkspawn$reaperReady(ready - 100);
			var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			player.saveWithoutId(output);
			player.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
			helper.assertTrue(((LegendaryArmorPlayer) player).darkspawn$phoenixReady() == ready, "Cooldown persists through saved player data");
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, restored, false);
			helper.assertTrue(((LegendaryArmorPlayer) restored).darkspawn$phoenixReady() == ready && ((LegendaryArmorPlayer) restored).darkspawn$reaperReady() == ready - 100, "Both cooldowns survive respawn");
			legendaryArmor(restored, "titan"); legendaryArmor(restored, "phoenix"); restored.damageCooldownTime = 0;
			restored.setHealth(2);
			boolean hit = restored.hurtServer(helper.getLevel(), restored.damageSources().magic(), 100);
			helper.assertTrue(!restored.isAlive(), "Respawn and equipment changes cannot bypass Phoenix cooldown; hit=" + hit + ", health=" + restored.getHealth() + ", ready=" + ((LegendaryArmorPlayer) restored).darkspawn$phoenixReady());
		} finally { remove(player); remove(restored); }
		helper.succeed();
	}

	@GameTest public void soulReaperHealsOnlyHostileKillsAndHonorsCooldown(GameTestHelper helper) {
		var player = player(helper);
		try {
			legendaryArmor(player, "soul_reaper"); player.setHealth(10);
			for (int i = 0; i < 2; i++) {
				var zombie = (net.minecraft.world.entity.Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("zombie")), new BlockPos(3, 2, 2));
				zombie.setNoAi(true); zombie.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1000); zombie.discard();
			}
			helper.assertTrue(player.getHealth() == 12, "Two rapid hostile kills heal only once");
			((LegendaryArmorPlayer) player).darkspawn$reaperReady(0);
			var cow = (net.minecraft.world.entity.Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cow")), new BlockPos(3, 2, 2));
			cow.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1000); cow.discard();
			helper.assertTrue(player.getHealth() == 12, "Passive animals cannot farm kill healing");
		} finally { remove(player); }
		helper.succeed();
	}

}
