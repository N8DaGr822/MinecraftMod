package darkspawn.black.cooking;

import darkspawn.black.Darkspawn;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class Cooking {
	public static final Block STATION = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("cooking_station"),
			new CookingStationBlock(BlockBehaviour.Properties.of()
					.setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id("cooking_station")))
					.strength(2.5F).sound(SoundType.WOOD)));
	public static final Item STATION_ITEM = register("cooking_station", properties -> new BlockItem(STATION, properties.useBlockDescriptionPrefix()));
	public static final MenuType<CookingMenu> MENU = Registry.register(BuiltInRegistries.MENU, Darkspawn.id("cooking_station"),
			new MenuType<>(CookingMenu::new, FeatureFlags.VANILLA_SET));
	public static final RecipeType<CookingRecipe> RECIPE_TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE,
			Darkspawn.id("cooking"), new RecipeType<>() {
				@Override public String toString() { return "darkspawn:cooking"; }
			});
	public static final Item SALT = register("salt", Item::new);
	public static final Item WILD_HERBS = register("wild_herbs", Item::new);
	public static final Item BUTTER = register("butter", Item::new);
	public static final Item CHEESE = register("cheese", properties -> new Item(properties.food(new FoodProperties.Builder()
			.nutrition(3).saturationModifier(0.5F).build())));
	public static final MealItem HERB_STEAK = meal("herb_steak", 8, 0.8F, MealEffects.HERBAL, 180, false);
	public static final MealItem HONEY_PORK = meal("honey_pork", 8, 0.8F, MealEffects.SWEET, 15, false);
	public static final MealItem BEEF_WELLINGTON = meal("beef_wellington", 10, 0.8F, MealEffects.SAVORY, 120, false);
	public static final MealItem FISH_CHOWDER = meal("fish_chowder", 9, 0.7F, MealEffects.SEAFOOD, 180, true);
	public static final MealItem CHICKEN_CORDON_BLEU = meal("chicken_cordon_bleu", 9, 0.8F, MealEffects.HEARTY, 120, false);
	public static final MealItem WOODLAND_STEW = meal("woodland_stew", 8, 0.8F, MealEffects.SWEET, 30, true);

	private Cooking() {
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory) {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	private static MealItem meal(String name, int nutrition, float saturation, Holder<MobEffect> effect,
			int seconds, boolean bowl) {
		return register(name, properties -> {
			properties.stacksTo(16).food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build());
			if (bowl) {
				properties.usingConvertsTo(Items.BOWL);
			}
			return new MealItem(properties, effect, seconds * 20);
		});
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Darkspawn.id("cooking"), CookingRecipe.SERIALIZER);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(STATION_ITEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			entries.accept(SALT);
			entries.accept(WILD_HERBS);
			entries.accept(CHEESE);
			entries.accept(BUTTER);
			entries.accept(HERB_STEAK);
			entries.accept(HONEY_PORK);
			entries.accept(BEEF_WELLINGTON);
			entries.accept(FISH_CHOWDER);
			entries.accept(CHICKEN_CORDON_BLEU);
			entries.accept(WOODLAND_STEW);
		});
	}
}
