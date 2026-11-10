package darkspawn.black.cooking;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class RecipeScrollItem extends Item {
	private final String recipe;
	public RecipeScrollItem(Properties properties, String recipe) { super(properties); this.recipe = recipe; }
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			if (!Culinary.progress(player).unlock(recipe)) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.darkspawn.recipe_known"));
				return InteractionResult.FAIL;
			}
			player.getItemInHand(hand).consume(1, player);
			if (player.containerMenu instanceof CookingMenu menu) menu.slotsChanged(null);
			serverPlayer.sendOverlayMessage(Component.translatable("message.darkspawn.recipe_learned",
				Cuisine.DISHES.get(recipe).item().getDefaultInstance().getHoverName()));
			Culinary.award(serverPlayer, "recipe_discovery", "earned");
		}
		return InteractionResult.SUCCESS;
	}
}
