package darkspawn.black.client;

import darkspawn.black.cooking.CookingMenu;
import darkspawn.black.cooking.MealItem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CookingScreen extends AbstractContainerScreen<CookingMenu> {
	public CookingScreen(CookingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 230, 206);
		inventoryLabelX = 34;
		inventoryLabelY = 112;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		darkspawn.black.client.UiTheme.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		for (Slot slot : menu.slots) {
			int x = leftPos + slot.x;
			int y = topPos + slot.y;
			graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF566F73);
			graphics.fill(x, y, x + 17, y + 17, 0xFFF4EBDA);
			graphics.fill(x, y, x + 16, y + 16, 0xFFB0C1B9);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		graphics.text(font, Component.translatable("container.darkspawn.ingredients"), 20, 22, darkspawn.black.client.UiTheme.INK, false);
		graphics.text(font, "->", 146, 40, darkspawn.black.client.UiTheme.INK, false);
		ItemStack output = menu.getSlot(CookingMenu.RESULT_SLOT).getItem();
		if (output.isEmpty()) {
			fittedLabel(graphics, Component.translatable(menu.isRecipeLocked()
				? "container.darkspawn.recipe_locked" : "container.darkspawn.cooking_hint"), 68, darkspawn.black.client.UiTheme.INK, mouseX, mouseY);
		} else {
			fittedLabel(graphics, output.getHoverName(), 62, darkspawn.black.client.UiTheme.INK, mouseX, mouseY);
			fittedLabel(graphics, MealItem.nutritionDescription(output), 76, darkspawn.black.client.UiTheme.INK, mouseX, mouseY);
			if (output.getItem() instanceof MealItem meal) {
				fittedLabel(graphics, meal.effectDescription(), 90, 0xFF31552C, mouseX, mouseY);
			}
		}
	}
	private void fittedLabel(GuiGraphicsExtractor graphics, Component label, int y, int color, int mouseX, int mouseY) {
		// Cooking Labels: Bound long names/translations without shrinking text; hover retains the complete description.
		int available = imageWidth - 16;
		boolean clipped = font.width(label) > available;
		Component text = clipped ? Component.literal(font.plainSubstrByWidth(label.getString(), available - font.width("…")) + "…").withStyle(label.getStyle()) : label;
		graphics.text(font, text, 8, y, color, false);
		if (clipped && mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8
			&& mouseY >= topPos + y && mouseY < topPos + y + font.lineHeight) {
			graphics.setTooltipForNextFrame(font, java.util.List.of(label), java.util.Optional.empty(), mouseX, mouseY, null, true);
		}
	}
}
