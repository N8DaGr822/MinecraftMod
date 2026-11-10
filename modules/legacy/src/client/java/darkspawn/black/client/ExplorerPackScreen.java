package darkspawn.black.client;

import darkspawn.black.inventory.ExplorerPackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ExplorerPackScreen extends AbstractContainerScreen<ExplorerPackMenu> {
	public ExplorerPackScreen(ExplorerPackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		inventoryLabelY = 72;
	}
	@Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		graphics.text(font, Component.translatable("screen.darkspawn.explorer_pack.contents"), 8, 18, UiTheme.INK, false);
	}
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		UiTheme.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		for (var slot : menu.slots) {
			int x = leftPos + slot.x, y = topPos + slot.y;
			graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF566F73);
			graphics.fill(x, y, x + 16, y + 16, 0xFFB0C1B9);
		}
	}
}
