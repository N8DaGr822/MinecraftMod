package darkspawn.black.cooking;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

public final class CookingRecipe implements Recipe<CraftingInput> {
	public static final MapCodec<CookingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Ingredient.CODEC.listOf(1, 4).fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)).apply(instance, CookingRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, CookingRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.ingredients,
			ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result, CookingRecipe::new);
	public static final RecipeSerializer<CookingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
	private final List<Ingredient> ingredients;
	private final ItemStackTemplate result;
	private final ShapelessRecipe matchingRecipe;

	public CookingRecipe(List<Ingredient> ingredients, ItemStackTemplate result) {
		this.ingredients = List.copyOf(ingredients);
		this.result = result;
		// Recipe Matching: Reuse vanilla's ingredient assignment, including tags and duplicate ingredients.
		this.matchingRecipe = new ShapelessRecipe(new Recipe.CommonInfo(false),
				new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""), result, this.ingredients);
	}

	@Override public boolean matches(CraftingInput input, Level level) { return matchingRecipe.matches(input, level); }
	public List<Ingredient> ingredients() { return ingredients; }
	public ItemStack resultStack() { return result.create(); }
	@Override public ItemStack assemble(CraftingInput input) { return result.create(); }
	@Override public boolean isSpecial() { return true; }
	@Override public boolean showNotification() { return false; }
	@Override public String group() { return ""; }
	@Override public RecipeSerializer<CookingRecipe> getSerializer() { return SERIALIZER; }
	@Override public RecipeType<CookingRecipe> getType() { return Cooking.RECIPE_TYPE; }
	@Override public PlacementInfo placementInfo() { return matchingRecipe.placementInfo(); }
	@Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
