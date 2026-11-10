package darkspawn.black.farming;

import darkspawn.black.core.DarkspawnCore;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public final class Agriculture {
	public static final Crop ONION = crop("onion", 2);
	public static final Crop GARLIC = crop("garlic", 1);
	public static final Crop TOMATO = crop("tomato", 3);
	public static final Crop RICE = crop("rice", 0);
	public static final Crop CORN = crop("corn", 3);
	public static final Crop PEPPER = crop("pepper", 2);
	public static final Crop FROST_GARLIC = crop("frost_garlic", 1);
	public static final Crop JUNGLE_PEPPER = crop("jungle_pepper", 2);
	public static final Crop MARSH_RICE = crop("marsh_rice", 0);
	public static final Item WILD_HERBS = item("wild_herbs");
	// Regional Variants: Ripening in these biomes can turn the base crop into its regional cousin.
	public static final TagKey<Biome> FROST_GARLIC_BIOMES = biomes("frost_garlic_biomes");
	public static final TagKey<Biome> JUNGLE_PEPPER_BIOMES = biomes("jungle_pepper_biomes");
	public static final TagKey<Biome> MARSH_RICE_BIOMES = biomes("marsh_rice_biomes");
	public static final List<Crop> STANDARD_CROPS = List.of(ONION, GARLIC, TOMATO, RICE, CORN, PEPPER);
	public static final List<Crop> CROPS = List.of(ONION, GARLIC, TOMATO, RICE, CORN, PEPPER, FROST_GARLIC, JUNGLE_PEPPER, MARSH_RICE);

	private Agriculture() {
	}

	private static Item item(String name) {
		var key = ResourceKey.create(Registries.ITEM, DarkspawnCore.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
	}

	private static TagKey<Biome> biomes(String name) {
		return TagKey.create(Registries.BIOME, DarkspawnCore.id(name));
	}

	private static Crop crop(String name, int nutrition) {
		var blockKey = ResourceKey.create(Registries.BLOCK, DarkspawnCore.id(name + "_crop"));
		CropBlock block = Registry.register(BuiltInRegistries.BLOCK, blockKey, new IngredientCropBlock(
				BlockBehaviour.Properties.of().setId(blockKey).mapColor(MapColor.PLANT).noCollision()
						.randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED)));
		var seedKey = ResourceKey.create(Registries.ITEM, DarkspawnCore.id(name + "_seeds"));
		BlockItem seeds = Registry.register(BuiltInRegistries.ITEM, seedKey, new BlockItem(block,
				new Item.Properties().setId(seedKey).compostable(ContextIntProviders.COMPOSTABLE_LOW)));
		seeds.registerBlocks(Item.BY_BLOCK, seeds);
		var ingredientKey = ResourceKey.create(Registries.ITEM, DarkspawnCore.id(name));
		var properties = new Item.Properties().setId(ingredientKey).compostable(ContextIntProviders.COMPOSTABLE_MEDIUM);
		if (nutrition > 0) {
			properties.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F).build());
		}
		Item ingredient = Registry.register(BuiltInRegistries.ITEM, ingredientKey, new Item(properties));
		return new Crop(block, ingredient, seeds);
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries ->
				CROPS.forEach(crop -> entries.accept(crop.seeds())));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			entries.accept(WILD_HERBS);
			CROPS.forEach(crop -> entries.accept(crop.ingredient()));
		});
	}

	public record Crop(CropBlock block, Item ingredient, Item seeds) {
	}

	private static final class IngredientCropBlock extends CropBlock {
		private IngredientCropBlock(BlockBehaviour.Properties properties) { super(properties); }

		// Crop Identity: Pick-block returns this crop's registered seeds instead of wheat seeds.
		@Override protected ItemLike getBaseSeedId() { return asItem(); }

		@Override protected void randomTick(net.minecraft.world.level.block.state.BlockState state,
				net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
			super.randomTick(state, level, pos, random);
			// Only newly ripened crops mutate. Mature vanilla crops do not receive random ticks.
			if (!isMaxAge(state) && isMaxAge(level.getBlockState(pos))) {
				Crop variant = regionalVariant(this, level.getBiome(pos));
				if (variant != null && random.nextInt(4) == 0) level.setBlock(pos, variant.block().getStateForAge(7), 2);
			}
		}
	}

	public static Crop regionalVariant(CropBlock crop, net.minecraft.core.Holder<Biome> biome) {
		if (crop == GARLIC.block() && biome.is(FROST_GARLIC_BIOMES)) return FROST_GARLIC;
		if (crop == PEPPER.block() && biome.is(JUNGLE_PEPPER_BIOMES)) return JUNGLE_PEPPER;
		if (crop == RICE.block() && biome.is(MARSH_RICE_BIOMES)) return MARSH_RICE;
		return null;
	}
}
