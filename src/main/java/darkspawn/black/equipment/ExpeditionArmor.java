package darkspawn.black.equipment;

import darkspawn.black.Darkspawn;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ExpeditionArmor {
	public static final Map<ArmorType, Item> MOUNTAINEERING = new EnumMap<>(ArmorType.class);
	public static final Map<ArmorType, Item> SPELUNKING = new EnumMap<>(ArmorType.class);
	public static final Map<ArmorType, Item> DIVER = new EnumMap<>(ArmorType.class);
	public static final Map<ArmorType, Item> DESERT_WANDERER = new EnumMap<>(ArmorType.class);
	public static final Map<ArmorType, Item> ASHWALKER = new EnumMap<>(ArmorType.class);
	private ExpeditionArmor() { }

	public static void initialize() {
		register("mountaineering", MOUNTAINEERING, 18, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2));
		register("spelunking", SPELUNKING, 15, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 5, ArmorType.LEGGINGS, 4, ArmorType.BOOTS, 1));
		register("diver", DIVER, 18, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2));
		register("desert_wanderer", DESERT_WANDERER, 15, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 5, ArmorType.LEGGINGS, 4, ArmorType.BOOTS, 1));
		register("ashwalker", ASHWALKER, 20, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
			MOUNTAINEERING.values().forEach(entries::accept);
			SPELUNKING.values().forEach(entries::accept);
			DIVER.values().forEach(entries::accept);
			DESERT_WANDERER.values().forEach(entries::accept);
			ASHWALKER.values().forEach(entries::accept);
		});
	}

	private static void register(String family, Map<ArmorType, Item> items, int durability, Map<ArmorType, Integer> defense) {
		var material = new ArmorMaterial(durability, defense, 12, SoundEvents.ARMOR_EQUIP_IRON, 0, 0,
			ItemTags.REPAIRS_IRON_ARMOR, ResourceKey.create(EquipmentAssets.ROOT_ID, Darkspawn.id(family)));
		for (ArmorType type : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) {
			var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(family + "_" + type.getName()));
			var properties = new Item.Properties().setId(key).humanoidArmor(material, type);
			if (family.equals("spelunking") && type == ArmorType.BOOTS) {
				// Fall Protection: Multiply remaining fall damage so meals and boss powers retain their own modifiers.
				properties.attributes(material.createAttributes(type).withModifierAdded(Attributes.FALL_DAMAGE_MULTIPLIER,
					new AttributeModifier(Darkspawn.id("spelunking_boots"), -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.FEET));
			}
			if (family.equals("diver") && type == ArmorType.HELMET) {
				// Diving Air: Native oxygen bonus extends air probabilistically without granting water breathing.
				properties.attributes(material.createAttributes(type).withModifierAdded(Attributes.OXYGEN_BONUS,
					new AttributeModifier(Darkspawn.id("diver_air"), 2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
					.withModifierAdded(Attributes.SUBMERGED_MINING_SPEED,
						new AttributeModifier(Darkspawn.id("diver_mining"), 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HEAD));
			}
			items.put(type, Registry.register(BuiltInRegistries.ITEM, key, new Item(properties)));
		}
	}

	public static boolean wearsFullMountaineering(LivingEntity entity) {
		return !MOUNTAINEERING.isEmpty() && MOUNTAINEERING.entrySet().stream()
			.allMatch(entry -> entity.getItemBySlot(entry.getKey().getSlot()).is(entry.getValue()));
	}

	public static boolean hasHeadlamp(LivingEntity entity) {
		Item helmet = SPELUNKING.get(ArmorType.HELMET);
		Item divingHelmet = DIVER.get(ArmorType.HELMET);
		return entity.isAlive() && (helmet != null && entity.getItemBySlot(EquipmentSlot.HEAD).is(helmet)
			|| divingHelmet != null && entity.isUnderWater() && entity.getItemBySlot(EquipmentSlot.HEAD).is(divingHelmet));
	}

	public static int ashwalkerPieces(LivingEntity entity) {
		return (int) ASHWALKER.entrySet().stream()
			.filter(entry -> entity.getItemBySlot(entry.getKey().getSlot()).is(entry.getValue())).count();
	}
}
