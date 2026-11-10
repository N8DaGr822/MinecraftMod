package darkspawn.black.climate;

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
import darkspawn.black.core.hook.ClimateProtection;

public final class ClimateGameTest {
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

	private static final java.util.Map<EquipmentSlot, net.minecraft.world.item.Item> IRON = java.util.Map.of(EquipmentSlot.HEAD, Items.IRON_HELMET,
		EquipmentSlot.CHEST, Items.IRON_CHESTPLATE, EquipmentSlot.LEGS, Items.IRON_LEGGINGS, EquipmentSlot.FEET, Items.IRON_BOOTS);

	@GameTest public void enchantmentsFireResistanceAndProvidersAdjustProtectedClimate(GameTestHelper helper) {
		var player = player(helper);
		try {
			var warmth = player.registryAccess().getOrThrow(Temperature.WARMTH);
			var cooling = player.registryAccess().getOrThrow(Temperature.COOLING);
			helper.assertTrue(!net.minecraft.world.item.enchantment.Enchantment.areCompatible(warmth, cooling), "Warmth and Cooling cannot share a piece");
			IRON.forEach((slot, item) -> { var stack = new ItemStack(item); stack.enchant(warmth, 2); player.setItemSlot(slot, stack); });
			helper.assertTrue(warmth.value().canEnchant(player.getItemBySlot(EquipmentSlot.HEAD)), "Ordinary armor accepts climate enchantments");
			helper.assertTrue(Temperature.protectedClimate(player, -110) == -86, "Warmth II on four pieces still leaves dangerous cold");
			helper.assertTrue(Temperature.protectedClimate(player, 110) == 110, "Warmth does not cool");
			IRON.forEach((slot, item) -> { var stack = new ItemStack(item); stack.enchant(cooling, 2); player.setItemSlot(slot, stack); });
			helper.assertTrue(Temperature.protectedClimate(player, 110) == 86 && Temperature.protectedClimate(player, -110) == -110, "Cooling only helps in heat");
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600));
			helper.assertTrue(Temperature.protectedClimate(player, 110) == 66, "Fire resistance adds heat protection");
			player.removeAllEffects();
			IRON.keySet().forEach(slot -> player.setItemSlot(slot, ItemStack.EMPTY));
			// Provider Hook: Other mods contribute through core; this provider only answers for the tagged test player.
			ClimateProtection.register((other, wet) -> other.entityTags().contains("darkspawn_climate_test")
				? new ClimateProtection.Protection(wet ? 30 : 10, -15) : ClimateProtection.Protection.NONE);
			player.addTag("darkspawn_climate_test");
			Temperature.state(player).wetSeconds = 0;
			helper.assertTrue(Temperature.protectedClimate(player, -110) == -100 && Temperature.protectedClimate(player, 110) == 125, "Providers add cold protection and may worsen heat");
			Temperature.state(player).wetSeconds = 10;
			helper.assertTrue(Temperature.protectedClimate(player, -110) == -80, "Providers see the climate mod's wetness");
			player.removeTag("darkspawn_climate_test");
			helper.assertTrue(Temperature.protectedClimate(player, -110) == -110, "Provider stops when its condition is gone");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void temperatureDamageAndModifiersRecoverWithoutErasingOtherEffects(GameTestHelper helper) {
		var player = player(helper);
		try {
			IRON.forEach((slot, item) -> player.setItemSlot(slot, new ItemStack(item)));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600));
			Temperature.state(player).restore(-100, 0, 4);
			Temperature.tick(player);
			helper.assertTrue(player.getHealth() == 19 && Temperature.state(player).exposure == -98, "Climate damage bypasses armor while recovery remains gradual");
			helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DarkspawnCore.id("temperature_movement")) != null, "Severe exposure adds its own slowdown");
			Temperature.state(player).restore(0, 0, 0); Temperature.tick(player);
			helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DarkspawnCore.id("temperature_movement")) == null && player.hasEffect(MobEffects.SLOWNESS), "Recovery removes only climate-owned penalties");
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

	// Shelter Timing: Allow asynchronous skylight updates to finish during the full server suite.
	@GameTest(skyAccess = true, maxTicks = 100) public void temperatureSamplesBiomeShelterWetnessAndCampfires(GameTestHelper helper) {
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
}
