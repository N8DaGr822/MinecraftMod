package darkspawn.black.equipment;

import darkspawn.black.Darkspawn;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class LegendaryArmor {
	public static final List<String> NAMES = List.of("dragonlord", "phoenix", "voidwalker", "titan", "soul_reaper");
	public static final Map<String, Map<ArmorType, Item>> SETS = new LinkedHashMap<>();
	public static final Map<String, Item> CORES = new LinkedHashMap<>();
	private LegendaryArmor() { }

	public static void initialize() {
		for (String name : NAMES) {
			var coreKey = ResourceKey.create(Registries.ITEM, Darkspawn.id(name + "_core"));
			CORES.put(name, Registry.register(BuiltInRegistries.ITEM, coreKey, new Item(new Item.Properties().setId(coreKey).rarity(Rarity.EPIC).fireResistant())));
			var material = new ArmorMaterial(37, Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
				15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3, .1F, ItemTags.REPAIRS_NETHERITE_ARMOR, ResourceKey.create(EquipmentAssets.ROOT_ID, Darkspawn.id(name)));
			var pieces = new EnumMap<ArmorType, Item>(ArmorType.class);
			for (ArmorType slot : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) {
				var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name + "_" + slot.getName()));
				pieces.put(slot, Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).humanoidArmor(material, slot).rarity(Rarity.EPIC).fireResistant())));
			}
			SETS.put(name, pieces);
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
			CORES.values().forEach(entries::accept);
			SETS.values().forEach(pieces -> pieces.values().forEach(entries::accept));
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(LegendaryArmor::update));
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			var old = (LegendaryArmorPlayer) oldPlayer; var next = (LegendaryArmorPlayer) newPlayer;
			next.darkspawn$phoenixReady(old.darkspawn$phoenixReady()); next.darkspawn$reaperReady(old.darkspawn$reaperReady());
		});
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damage) -> {
			if (!(entity instanceof ServerPlayer player) || !fullSet(player).equals("phoenix") || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			var cooldown = (LegendaryArmorPlayer) player; long now = time(player);
			if (cooldown.darkspawn$phoenixReady() > now) return true;
			cooldown.darkspawn$phoenixReady(now + 12000);
			player.setHealth(2);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
			return false;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Enemy) || !(source.getEntity() instanceof ServerPlayer player) || !player.isAlive() || !fullSet(player).equals("soul_reaper")) return;
			var cooldown = (LegendaryArmorPlayer) player; long now = time(player);
			if (cooldown.darkspawn$reaperReady() > now) return;
			cooldown.darkspawn$reaperReady(now + 100);
			player.heal(2);
		});
	}

	private static long time(ServerPlayer player) { return player.level().getServer().overworld().getGameTime(); }

	public static String family(ItemStack stack) {
		for (var entry : SETS.entrySet()) if (entry.getValue().containsValue(stack.getItem())) return entry.getKey();
		return "";
	}

	public static String fullSet(LivingEntity wearer) {
		if (wearer.isSpectator()) return "";
		// Fatal-Hit Check: Phoenix must still identify worn equipment after health has reached zero.
		for (var entry : SETS.entrySet()) if (entry.getValue().entrySet().stream().allMatch(piece -> wearer.getItemBySlot(piece.getKey().getSlot()).is(piece.getValue()))) return entry.getKey();
		return "";
	}

	public static void update(ServerPlayer player) {
		String set = player.isAlive() ? fullSet(player) : "";
		boolean hadTitan = player.getAttribute(Attributes.MAX_HEALTH).getModifier(Darkspawn.id("legendary_titan_health")) != null;
		modifier(player, Attributes.MAX_HEALTH, "legendary_titan_health", set.equals("titan") ? 4 : 0, AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.KNOCKBACK_RESISTANCE, "legendary_titan_knockback", set.equals("titan") ? .4 : 0, AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, "legendary_void_fall", set.equals("voidwalker") ? -.4 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		if (hadTitan && !set.equals("titan") && player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
	}

	private static void modifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> type, String name, double amount, AttributeModifier.Operation operation) {
		var attribute = player.getAttribute(type); if (attribute == null) return;
		var id = Darkspawn.id(name); var old = attribute.getModifier(id);
		if (old != null && old.amount() == amount && old.operation() == operation) return;
		attribute.removeModifier(id);
		if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
	}

	public static float damage(LivingEntity target, DamageSource source, float amount) {
		if (amount <= 0) return amount;
		if (target instanceof ServerPlayer player && source.is(DamageTypes.ENDER_PEARL) && fullSet(player).equals("voidwalker")) return 0;
		if (target instanceof net.minecraft.world.entity.Mob && (target.getMaxHealth() >= 40 || target.getType().builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE, Darkspawn.id("legendary_bosses")))) && source.getEntity() instanceof ServerPlayer player && player.isAlive() && fullSet(player).equals("dragonlord")
				&& (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.SPEAR) || source.is(DamageTypes.MACE_SMASH)
					|| source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile)) return amount * 1.25F;
		return amount;
	}
}
