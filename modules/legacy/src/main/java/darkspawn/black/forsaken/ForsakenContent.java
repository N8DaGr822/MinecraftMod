package darkspawn.black.forsaken;

import darkspawn.black.Darkspawn;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ForsakenContent {
	public static final Item ALLOY = item("contaminated_alloy");
	public static final Item BIOFIBER = item("biofiber");
	public static final Item CORE = item("parasitic_core");
	public static final Item DATA = item("research_data");
	public static final Item MODULE = item("quarantine_module");
	public static final Item BLADE = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("pulse_blade"), new PulseBladeItem(new Item.Properties()
		.setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("pulse_blade"))).sword(net.minecraft.world.item.ToolMaterial.IRON, 3, -2.4F).durability(500)));
	public static final ContainmentBeaconBlock BEACON = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("containment_beacon"),
		new ContainmentBeaconBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id("containment_beacon")))
			.strength(3, 6).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL).lightLevel(state -> state.getValue(ContainmentBeaconBlock.ACTIVE) ? 12 : 0)
			.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)));
	public static final Item BEACON_ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("containment_beacon"),
		new net.minecraft.world.item.BlockItem(BEACON, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("containment_beacon"))).useBlockDescriptionPrefix()));
	public static final BlockEntityType<ContainmentBeaconBlockEntity> BEACON_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Darkspawn.id("containment_beacon"), new BlockEntityType<>(ContainmentBeaconBlockEntity::new, Set.of(BEACON)));
	public static final Item SCANNER = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("bio_scanner"),
		new NestScannerItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("bio_scanner"))).stacksTo(1)));
	public static final Map<ArmorType, Item> ARMOR = new EnumMap<>(ArmorType.class);
	public static final Map<Infected.Kind, EntityType<Infected>> MOBS = new EnumMap<>(Infected.Kind.class);
	public static final EntityType<SporeBolt> SPORE = projectile();
	public static final InfectedNestBlock NEST = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("infected_nest"),
		new InfectedNestBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id("infected_nest")))
			.strength(5, 6).sound(net.minecraft.world.level.block.SoundType.WART_BLOCK).lightLevel(state -> 5)
			.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)));
	public static final BlockEntityType<InfectedNestBlockEntity> NEST_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Darkspawn.id("infected_nest"), new BlockEntityType<>(InfectedNestBlockEntity::new, Set.of(NEST)));
	public static final Holder<MobEffect> CONTAMINATION = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
		Darkspawn.id("contamination"), new Contamination().addAttributeModifier(Attributes.MOVEMENT_SPEED,
			Darkspawn.id("contamination"), -.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	private static final class Contamination extends MobEffect {
		private Contamination() { super(MobEffectCategory.HARMFUL, 0x91AD47); }
	}
	private ForsakenContent() { }
	private static Item item(String name) {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
	}
	private static EntityType<SporeBolt> projectile() {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id("infected_spore"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<SporeBolt>of(SporeBolt::new, MobCategory.MISC)
			.sized(.25F, .25F).clientTrackingRange(4).updateInterval(1).build(key));
	}
	public static void initialize() {
		for (var kind : Infected.Kind.values()) {
			var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id(kind.id));
			var type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<Infected>of((entity, level) -> new Infected(entity, level, kind), MobCategory.MONSTER)
				.sized(kind.width, kind.height).clientTrackingRange(8).notInPeaceful().noLootTable().build(key));
			MOBS.put(kind, type); FabricDefaultAttributeRegistry.register(type, Infected.attributes(kind));
		}
		var material = new ArmorMaterial(18, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2),
			12, SoundEvents.ARMOR_EQUIP_IRON, 0, 0, ItemTags.REPAIRS_IRON_ARMOR, ResourceKey.create(EquipmentAssets.ROOT_ID, Darkspawn.id("quarantine")));
		for (var slot : new ArmorType[] {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS}) {
			var key = ResourceKey.create(Registries.ITEM, Darkspawn.id("quarantine_" + slot.getName()));
			ARMOR.put(slot, Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).humanoidArmor(material, slot))));
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			for (var item : new Item[] {ALLOY, BIOFIBER, CORE, DATA, MODULE, SCANNER}) entries.accept(item);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> { ARMOR.values().forEach(entries::accept); entries.accept(BLADE); });
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(BEACON_ITEM));
		ForsakenThreats.initialize();
	}
	public static int protection(LivingEntity entity) {
		return (int) ARMOR.entrySet().stream().filter(entry -> entity.getItemBySlot(entry.getKey().getSlot()).is(entry.getValue())).count();
	}
	public static void contaminate(LivingEntity entity) {
		if (entity.level().isClientSide() || !entity.level().dimension().equals(Forsaken.LEVEL) || !entity.isAlive()) return;
		int pieces = protection(entity);
		// Quarantine Protection: Each piece shortens exposure by 15 seconds; a sealed set prevents it entirely.
		if (pieces < 4) entity.addEffect(new MobEffectInstance(CONTAMINATION, 1200 - 300 * pieces));
	}
}
