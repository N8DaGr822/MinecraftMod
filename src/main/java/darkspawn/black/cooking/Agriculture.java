package darkspawn.black.cooking;

import darkspawn.black.Darkspawn;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
	public static final List<Crop> CROPS = List.of(ONION, GARLIC, TOMATO, RICE, CORN, PEPPER);

	private Agriculture() {
	}

	private static Crop crop(String name, int nutrition) {
		var blockKey = ResourceKey.create(Registries.BLOCK, Darkspawn.id(name + "_crop"));
		CropBlock block = Registry.register(BuiltInRegistries.BLOCK, blockKey, new IngredientCropBlock(
				BlockBehaviour.Properties.of().setId(blockKey).mapColor(MapColor.PLANT).noCollision()
						.randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED)));
		var seedKey = ResourceKey.create(Registries.ITEM, Darkspawn.id(name + "_seeds"));
		BlockItem seeds = Registry.register(BuiltInRegistries.ITEM, seedKey, new BlockItem(block,
				new Item.Properties().setId(seedKey).compostable(ContextIntProviders.COMPOSTABLE_LOW)));
		seeds.registerBlocks(Item.BY_BLOCK, seeds);
		var ingredientKey = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
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
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries ->
				CROPS.forEach(crop -> entries.accept(crop.ingredient())));
	}

	public record Crop(CropBlock block, Item ingredient, Item seeds) {
	}

	private static final class IngredientCropBlock extends CropBlock {
		private IngredientCropBlock(BlockBehaviour.Properties properties) { super(properties); }

		// Crop Identity: Pick-block returns this crop's registered seeds instead of wheat seeds.
		@Override protected ItemLike getBaseSeedId() { return asItem(); }
	}
}
