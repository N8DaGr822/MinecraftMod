package darkspawn.black.legendaryarmor;

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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

public final class LegendaryArmorGameTest {
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
		LegendaryArmor.update(player);
		return removed;
	}

	@GameTest public void coresUpgradeNetheriteWithoutBossesButAreNotCraftable(GameTestHelper helper) {
		var level = helper.getLevel();
		var recipes = level.getServer().getRecipeManager();
		for (String name : LegendaryArmor.NAMES) {
			// Soft Bosses: Core recipes name boss essences and load only when a mod registers them.
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, DarkspawnCore.id(name + "_core"))).isEmpty(), "Without boss essences the " + name + " core is creative-only");
			for (var piece : LegendaryArmor.SETS.get(name).entrySet()) {
				var original = new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("netherite_" + piece.getKey().getName())));
				original.enchant(level.registryAccess().getOrThrow(Enchantments.PROTECTION), 2);
				original.setDamageValue(17); original.set(DataComponents.CUSTOM_NAME, Component.literal("Heirloom"));
				var input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), original, new ItemStack(LegendaryArmor.CORES.get(name)));
				var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level).orElseThrow().value();
				var result = recipe.assemble(input);
				helper.assertTrue(result.is(piece.getValue()) && result.isEnchanted() && result.getDamageValue() == 17 && result.getHoverName().getString().equals("Heirloom"), "Legendary conversion preserves existing investment");
				helper.assertTrue(result.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(DarkspawnCore.id(name)), "Legendary conversion selects its own worn model");
				helper.assertTrue(!recipe.matches(new SmithingRecipeInput(input.template(), result, input.addition()), level), "Legendary pieces cannot be recursively converted");
				helper.assertTrue(result.is(ItemTags.ARMOR_ENCHANTABLE), "Legendary armor supports native enchantments");
			}
		}
		helper.succeed();
	}

	private void legendaryArmor(ServerPlayer player, String name) {
		LegendaryArmor.SETS.get(name).forEach((type, item) -> player.setItemSlot(type.getSlot(), new ItemStack(item)));
	}

	@GameTest public void legendaryTitanVoidAndDragonBonusesRequireWornFullSets(GameTestHelper helper) {
		var player = player(helper);
		var target = (net.minecraft.world.entity.Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_golem")), new BlockPos(3, 2, 2));
		try {
			legendaryArmor(player, "titan"); player.doTick(); LegendaryArmor.update(player); LegendaryArmor.update(player);
			helper.assertTrue(player.getMaxHealth() == 24 && player.getHealth() == 20, "Titan adds two hearts capacity without healing");
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - .8) < .001, "Titan adds forty points to native netherite resistance");
			player.setHealth(24); takeOffArmor(player);
			helper.assertTrue(player.getMaxHealth() == 20 && player.getHealth() == 20, "Removing Titan immediately removes bonus capacity");
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
