package darkspawn.black.client;

import darkspawn.black.forsaken.QuarantineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class QuarantineScreen extends AbstractContainerScreen<QuarantineMenu> {
	public QuarantineScreen(QuarantineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 210, 194); inventoryLabelX = 24; inventoryLabelY = 100;
	}
	@Override protected void init() {
		super.init();
		for (int action = 0; action < 2; action++) {
			int choice = action;
			var costs = action == 0 ? QuarantineMenu.GATE_COST : QuarantineMenu.KEY_COST;
			var tooltip = Component.translatable("quarantine.darkspawn.costs");
			for (var cost : costs) tooltip.append(Component.literal("\n" + cost.count() + " × ")).append(cost.item().getDefaultInstance().getHoverName());
			addRenderableWidget(Button.builder(Component.translatable(action == 0 ? "block.darkspawn.quarantine_gate" : "item.darkspawn.containment_key"),
				button -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, choice); })
				.bounds(leftPos + 15, topPos + 31 + action * 26, 180, 20).tooltip(Tooltip.create(tooltip)).build());
		}
	}
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF334447);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFD8DEDD);
		for (var slot : menu.slots) {
			graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF596363);
			graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF8B9995);
		}
	}
}
