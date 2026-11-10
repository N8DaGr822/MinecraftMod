package darkspawn.black.equipment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;

public final class ArmorStyleRecipe extends SimpleSmithingRecipe {
	public static final MapCodec<ArmorStyleRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.template),
			Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
			Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition),
			ArmorStyles.STYLE_CODEC.optionalFieldOf("style", "classic").forGetter(recipe -> recipe.style)).apply(instance, ArmorStyleRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, ArmorStyleRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.template,
			Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.base,
			Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.addition,
			ByteBufCodecs.STRING_UTF8, recipe -> recipe.style, ArmorStyleRecipe::new);
	public static final RecipeSerializer<ArmorStyleRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
	private final Ingredient template;
	private final Ingredient base;
	private final Ingredient addition;
	private final String style;

	public ArmorStyleRecipe(Ingredient template, Ingredient base, Ingredient addition) {
		this(template, base, addition, "classic");
	}

	public ArmorStyleRecipe(Ingredient template, Ingredient base, Ingredient addition, String style) {
		super(new Recipe.CommonInfo(true));
		this.template = template;
		this.base = base;
		this.addition = addition;
		this.style = style;
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		return template.test(input.template()) && base.test(input.base()) && addition.test(input.addition())
				&& ArmorStyles.supports(input.base()) && !style.equals(ArmorStyles.style(input.base()));
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		return ArmorStyles.apply(input.base(), style);
	}

	@Override
	public Optional<Ingredient> templateIngredient() {
		return Optional.of(template);
	}

	@Override
	public Ingredient baseIngredient() {
		return base;
	}

	@Override
	public Optional<Ingredient> additionIngredient() {
		return Optional.of(addition);
	}

	@Override
	protected PlacementInfo createPlacementInfo() {
		return PlacementInfo.createFromOptionals(List.of(Optional.of(template), Optional.of(base), Optional.of(addition)));
	}

	@Override
	public RecipeSerializer<ArmorStyleRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(new SmithingRecipeDisplay(template.display(), base.display(), addition.display(),
				new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(ArmorStyles.apply(
					new ItemStack(base.items().findFirst().orElseThrow()), style))),
				new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)));
	}
}
