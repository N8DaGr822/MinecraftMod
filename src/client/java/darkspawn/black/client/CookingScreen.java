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
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF37312B);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6BFAF);
		for (Slot slot : menu.slots) {
			int x = leftPos + slot.x;
			int y = topPos + slot.y;
			graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF625C54);
			graphics.fill(x, y, x + 17, y + 17, 0xFFF4EBDA);
			graphics.fill(x, y, x + 16, y + 16, 0xFF91897C);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		graphics.text(font, Component.translatable("container.darkspawn.ingredients"), 20, 22, 0xFF40372D, false);
		graphics.text(font, "->", 146, 40, 0xFF40372D, false);
		ItemStack output = menu.getSlot(CookingMenu.RESULT_SLOT).getItem();
		if (output.isEmpty()) {
			graphics.text(font, Component.translatable("container.darkspawn.cooking_hint"), 8, 68, 0xFF40372D, false);
		} else {
			graphics.text(font, output.getHoverName(), 8, 62, 0xFF40372D, false);
			graphics.text(font, MealItem.nutritionDescription(output), 8, 76, 0xFF40372D, false);
			if (output.getItem() instanceof MealItem meal) {
				graphics.text(font, meal.effectDescription(), 8, 90, 0xFF31552C, false);
			}
		}
	}
}
