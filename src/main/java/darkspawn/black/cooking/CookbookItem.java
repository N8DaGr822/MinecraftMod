package darkspawn.black.cooking;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

/** Rebuild the native book on use, from the server's current recipes and this reader's discoveries. */
public final class CookbookItem extends Item {
	public CookbookItem(Properties properties) { super(properties); }
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			var pages = new ArrayList<Filterable<Component>>();
			pages.add(Filterable.passThrough(Component.literal("Darkspawn Cookbook\n\nUse a Cooking Station. Each listed ingredient costs one item.\n\nMeal buffs replace one another; potions remain independent.\n\nFeasts: six servings. Breaking a partly eaten feast loses it.")));
			var progress = Culinary.progress(player);
			pages.add(Filterable.passThrough(Component.literal("Your kitchen\n\nDistinct meals: " + progress.meals().size()
				+ "\nRecent variety: " + progress.variety() + "/8\n\n3 foods: +5% saturation\n5 foods: +10%\n8 foods: heal half a heart every 5s at full hunger.\n\nEat 20 meals to learn Hero Feast.")));
			level.getServer().getRecipeManager().getRecipes().stream()
				.filter(r -> r.value() instanceof CookingRecipe)
				.sorted(Comparator.comparing(r -> category(r.id().identifier().getPath()) + r.id().identifier().getPath()))
				.forEach(holder -> {
					CookingRecipe recipe = (CookingRecipe) holder.value();
					String name = holder.id().identifier().getPath();
					var dish = Cuisine.DISHES.get(name);
					Component title = recipe.resultStack().getHoverName();
					var text = Component.literal(category(name) + "\n").append(title).append("\n\n");
					if (!Cuisine.canCook(player, name)) {
						text.append("Locked recipe\nFind a scroll: " + dish.discovery() + "\n\nIngredients:\n??? + ");
						// Show a useful known ingredient without revealing the entire locked recipe.
						if (recipe.ingredients().size() > 1) text.append(new ItemStack(recipe.ingredients().get(1).items().findFirst().orElseThrow()).getHoverName());
					} else {
						for (var ingredient : recipe.ingredients()) {
							text.append("- ").append(new ItemStack(ingredient.items().findFirst().orElseThrow()).getHoverName()).append("\n");
						}
						if (recipe.resultStack().getItem() instanceof MealItem meal) text.append("\n").append(meal.effectDescription());
					}
					pages.add(Filterable.passThrough(text));
				});
			ItemStack book = player.getItemInHand(hand);
			book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Darkspawn Cookbook"), "Darkspawn", 0, pages, true));
			// Send the refreshed pages before the open packet, including when the book was already resolved.
			player.inventoryMenu.broadcastChanges();
			player.containerMenu.broadcastChanges();
			serverPlayer.openItemGui(book, hand);
		}
		return InteractionResult.SUCCESS;
	}
	private static String category(String name) {
		var dish = Cuisine.DISHES.get(name);
		return dish == null ? "Basics" : dish.category();
	}
}
