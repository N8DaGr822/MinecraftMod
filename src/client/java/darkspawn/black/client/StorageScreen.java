package darkspawn.black.client;

import darkspawn.black.storage.StorageActionPayload;
import darkspawn.black.storage.StorageMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class StorageScreen extends AbstractContainerScreen<StorageMenu> {
	private final Map<Integer, Button> buttons = new HashMap<>();
	private EditBox search;

	public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 300, 240);
		inventoryLabelX = 60; inventoryLabelY = 146;
	}
	@Override protected void init() {
		super.init(); buttons.clear();
		search = addRenderableWidget(new EditBox(font, leftPos + 8, topPos + 23, 136, 18, Component.translatable("storage.darkspawn.search")));
		search.setMaxLength(64); search.setHint(Component.translatable("storage.darkspawn.search"));
		button(StorageMenu.SEARCH, "refresh", 148, 22, 70);
		button(StorageMenu.ORDER, "order", 222, 22, 70);
		button(StorageMenu.PREVIOUS, "previous", 60, 43, 22);
		button(StorageMenu.NEXT, "next", 200, 43, 22);
		button(StorageMenu.TAB, "tab", 230, 43, 62);
		button(StorageMenu.DEPOSIT, "deposit", 230, 65, 62);
		button(StorageMenu.RESORT, "resort", 230, 85, 62);
		button(StorageMenu.TRASH, "trash", 230, 105, 62);
		button(StorageMenu.UPGRADE, "upgrade", 230, 125, 62);
		containerTick();
	}
	private void button(int action, String label, int x, int y, int width) {
		Button button = Button.builder(Component.translatable("storage.darkspawn." + label), ignored ->
			ClientPlayNetworking.send(new StorageActionPayload(menu.containerId, menu.getStateId(), action, search.getValue())))
			.bounds(leftPos + x, topPos + y, width, 18)
			.tooltip(Tooltip.create(Component.translatable("storage.darkspawn." + label + "_hint"))).build();
		buttons.put(action, addRenderableWidget(button));
	}
	@Override protected void containerTick() {
		super.containerTick();
		search.visible = menu.isBank();
		buttons.forEach((action, button) -> {
			button.visible = action == StorageMenu.DEPOSIT || action == StorageMenu.ORDER || menu.isBank();
			button.active = menu.getCarried().isEmpty();
		});
		buttons.get(StorageMenu.PREVIOUS).active &= menu.page() > 0;
		buttons.get(StorageMenu.ORDER).setMessage(Component.translatable(menu.isBank() ? "storage.darkspawn.order" : "storage.darkspawn.sort_chest"));
		buttons.get(StorageMenu.ORDER).setTooltip(Tooltip.create(Component.translatable(menu.isBank() ? "storage.darkspawn.order_hint" : "storage.darkspawn.sort_chest_hint")));
		buttons.get(StorageMenu.TAB).setMessage(Component.translatable(menu.trashView() ? "storage.darkspawn.view_bank" : "storage.darkspawn.view_trash"));
		buttons.get(StorageMenu.NEXT).active &= menu.page() + 1 < menu.pages();
		buttons.get(StorageMenu.RESORT).active &= !menu.resorting() && !menu.trashView();
		buttons.get(StorageMenu.TRASH).active &= menu.trashView();
		buttons.get(StorageMenu.TRASH).setMessage(Component.translatable(menu.trashArmed() ? "storage.darkspawn.confirm" : "storage.darkspawn.trash"));
		buttons.get(StorageMenu.UPGRADE).active &= menu.tier() < 3;
		buttons.get(StorageMenu.UPGRADE).setTooltip(Tooltip.create(Component.translatable("storage.darkspawn.upgrade_cost", 1 << menu.tier())));
	}
	@Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (search.visible && search.isFocused() && !event.isEscape()) {
			if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN) {
				ClientPlayNetworking.send(new StorageActionPayload(menu.containerId, menu.getStateId(), StorageMenu.SEARCH, search.getValue()));
			} else { search.keyPressed(event); }
			return true;
		}
		return super.keyPressed(event);
	}
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF37312B);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6BFAF);
		for (Slot slot : menu.slots) {
			if (!slot.isActive()) { continue; }
			int x = leftPos + slot.x, y = topPos + slot.y;
			graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF625C54);
			graphics.fill(x, y, x + 16, y + 16, slot.index >= StorageMenu.FILTER_START && slot.index < StorageMenu.PLAYER_START ? 0xFF658A84 : 0xFF91897C);
		}
	}
	@Override protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractSlots(graphics, mouseX, mouseY);
		// Bank Counts: Draw after item icons so the aggregate quantity remains readable.
		if (menu.isBank()) {
			for (int i = 0; i < StorageMenu.PAGE_SIZE; i++) {
				int count = menu.quantity(i);
				if (count == 0) { continue; }
				String label = count >= 1_000_000 ? "1M" : count >= 1000 ? count / 1000 + "k" : Integer.toString(count);
				Slot slot = menu.getSlot(i);
				graphics.pose().pushMatrix();
				graphics.pose().scale(0.75F, 0.75F);
				graphics.text(font, label, (int) ((slot.x + 17) / 0.75F) - font.width(label), (int) ((slot.y + 10) / 0.75F), 0xFFFFFFFF, true);
				graphics.pose().popMatrix();
			}
		}
	}
	@Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		if (menu.kind() != 0) { graphics.text(font, Component.translatable("storage.darkspawn.filters"), 8, 51, 0xFF40372D, false); }
		if (menu.isBank()) {
			graphics.text(font, Component.translatable(menu.trashView() ? "storage.darkspawn.trash_page" : "storage.darkspawn.bank_page", menu.page() + 1, menu.pages()), 88, 48, 0xFF40372D, false);
			graphics.text(font, Component.translatable("storage.darkspawn.capacity", menu.variants(), 256 << menu.tier()), 60, 120, 0xFF40372D, false);
			graphics.text(font, Component.translatable(menu.resorting() ? "storage.darkspawn.resorting" : "storage.darkspawn.withdraw_hint"), 60, 132, 0xFF40372D, false);
		} else {
			graphics.text(font, Component.translatable(menu.kind() == 0 ? "storage.darkspawn.dump_hint" : "storage.darkspawn.filter_hint"), 8, 25, 0xFF40372D, false);
			graphics.text(font, Component.translatable("storage.darkspawn.direction_hint"), 8, 125, 0xFF40372D, false);
		}
	}
	@Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (hoveredSlot != null && hoveredSlot.index < StorageMenu.PLAYER_START) {
			var lines = new ArrayList<Component>();
			if (hoveredSlot.hasItem()) { lines.addAll(getTooltipFromContainerItem(hoveredSlot.getItem())); }
			if (hoveredSlot.index >= StorageMenu.FILTER_START) {
				lines.add(Component.translatable(menu.isBank() ? "storage.darkspawn.trash_filter_hint" : "storage.darkspawn.ghost_hint"));
			} else if (menu.isBank() && hoveredSlot.hasItem()) {
				lines.add(Component.translatable("storage.darkspawn.quantity", menu.quantity(hoveredSlot.index)));
				lines.add(Component.translatable("storage.darkspawn.withdraw_detail"));
			}
			if (!lines.isEmpty()) { graphics.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mouseX, mouseY, null, true); return; }
		}
		super.extractTooltip(graphics, mouseX, mouseY);
	}
}
