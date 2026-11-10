package darkspawn.black.equipment;

import darkspawn.black.Darkspawn;
import darkspawn.black.cooking.MealEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.Heightmap;

public final class Temperature {
	public static final ResourceKey<Enchantment> WARMTH = ResourceKey.create(Registries.ENCHANTMENT, Darkspawn.id("warmth"));
	public static final ResourceKey<Enchantment> COOLING = ResourceKey.create(Registries.ENCHANTMENT, Darkspawn.id("cooling"));
	public static final ResourceKey<DamageType> COLD = ResourceKey.create(Registries.DAMAGE_TYPE, Darkspawn.id("hypothermia"));
	public static final ResourceKey<DamageType> HEAT = ResourceKey.create(Registries.DAMAGE_TYPE, Darkspawn.id("heatstroke"));
	private static final ArmorType[] SLOTS = { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS };
	private Temperature() { }

	public static TemperatureState state(Player player) { return ((TemperaturePlayer) player).darkspawn$temperature(); }

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(TemperaturePayload.TYPE, TemperaturePayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) server.getPlayerList().getPlayers().forEach(Temperature::tick);
		});
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, next, alive) -> {
			// Respawn: Death clears exposure; living transfers (including the End exit) preserve it.
			if (alive) { var old = state(oldPlayer); state(next).restore(old.exposure, old.wetSeconds, old.dangerSeconds); }
		});
	}

	public static void tick(ServerPlayer player) {
		if (!player.isAlive()) return;
		var state = state(player);
		if (player.isCreative() || player.isSpectator()) {
			state.restore(0, 0, 0); state.target = 0;
		} else {
			boolean soaked = player.isInWater() || player.level().isRainingAt(player.blockPosition());
			state.wetSeconds = soaked ? 30 : Math.max(0, state.wetSeconds - 1);
			float target = protectedClimate(player, climate(player));
			if (state.step(target)) player.hurtServer(player.level(), new DamageSource(player.registryAccess().getOrThrow(state.exposure < 0 ? COLD : HEAT)), 1);
		}
		boolean strained = Math.abs(state.exposure) >= TemperatureState.STRAIN;
		modifier(player, Attributes.MOVEMENT_SPEED, "temperature_movement", strained ? -.1 : 0);
		modifier(player, Attributes.BLOCK_BREAK_SPEED, "temperature_mining", strained ? -.15 : 0);
		if (ServerPlayNetworking.canSend(player, TemperaturePayload.TYPE)) ServerPlayNetworking.send(player, new TemperaturePayload(state.exposure, state.target, state.wetSeconds));
	}

	public static float climate(ServerPlayer player) {
		var level = player.level(); var pos = player.blockPosition();
		boolean nether = level.dimension() == Level.NETHER;
		boolean sky = level.canSeeSky(pos);
		int cover = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - pos.getY();
		float biome = level.getBiome(pos).value().getBaseTemperature();
		float climate = baseClimate(biome, nether, level.dimension() == Level.END, pos.getY(), sky, cover, level.isDarkOutside(), level.isRaining());
		if (state(player).wetSeconds > 0) climate -= 20;
		boolean campfire = false, lava = false;
		// Local Heat: A bounded scan once per second never loads surrounding chunks or stacks many fires.
		for (var nearby : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
			if (!level.hasChunkAt(nearby)) continue;
			var block = level.getBlockState(nearby);
			if (block.getBlock() instanceof CampfireBlock && block.getValue(CampfireBlock.LIT)) campfire = true;
			if (block.is(Blocks.MAGMA_BLOCK) || level.getFluidState(nearby).is(FluidTags.LAVA)) lava = true;
		}
		if (campfire) climate += 55;
		if (lava || player.isOnFire()) climate += 30;
		return Math.clamp(climate, -170, 170);
	}

	static float baseClimate(float biome, boolean nether, boolean end, int y, boolean sky, int cover, boolean night, boolean rain) {
		if (nether) return 140;
		if (end) return -30;
		float climate = biome <= .15F ? -110 : biome <= .4F ? -30 : biome >= 1.5F ? 110 : 0;
		if (!sky && cover >= 12) return climate * .2F;
		if (sky) {
			climate -= Math.clamp((y - 128) * .5F, 0, 40);
			if (night) climate -= biome >= 1.5F ? 65 : 15;
			if (rain) climate -= 15;
		} else climate *= climate < 0 ? .65F : .7F;
		return climate;
	}

	public static float protectedClimate(ServerPlayer player, float climate) {
		int cold = 0, heat = 0, warmth = 0, cooling = 0, mountain = 0, diver = 0;
		var warmEnchant = player.registryAccess().getOrThrow(WARMTH);
		var coolEnchant = player.registryAccess().getOrThrow(COOLING);
		for (ArmorType slot : SLOTS) {
			var item = player.getItemBySlot(slot.getSlot());
			if (item.is(ExpeditionArmor.MOUNTAINEERING.get(slot))) { cold += 20; mountain++; }
			if (item.is(ExpeditionArmor.DESERT_WANDERER.get(slot))) heat += 20;
			if (item.is(ExpeditionArmor.ASHWALKER.get(slot))) heat += player.level().dimension() == Level.NETHER ? 25 : 15;
			if (item.is(ExpeditionArmor.DIVER.get(slot))) diver++;
			warmth += Math.clamp(EnchantmentHelper.getItemEnchantmentLevel(warmEnchant, item), 0, 2) * 3;
			cooling += Math.clamp(EnchantmentHelper.getItemEnchantmentLevel(coolEnchant, item), 0, 2) * 3;
		}
		if (state(player).wetSeconds > 0) cold += diver * 10;
		if (player.hasEffect(MealEffects.WARMING)) cold += 15;
		if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) heat += 20;
		// Specialist Gear: Enchantments contribute at most 24 points; insulation has a cost in hot regions.
		if (climate > 0) return Math.max(0, climate + mountain * 5 - heat - cooling);
		return Math.min(0, climate + cold + warmth);
	}

	private static void modifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> type, String name, double amount) {
		var attribute = player.getAttribute(type); if (attribute == null) return;
		var id = Darkspawn.id(name); var old = attribute.getModifier(id);
		if (old != null && old.amount() == amount) return;
		attribute.removeModifier(id);
		if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}
}
