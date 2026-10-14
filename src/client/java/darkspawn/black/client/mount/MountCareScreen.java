package darkspawn.black.client.mount;

import darkspawn.black.mount.MountMenu;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MountCareScreen extends AbstractContainerScreen<MountMenu> {
    private final Map<Integer, Button> buttons = new HashMap<>();
    public MountCareScreen(MountMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 196, 242); inventoryLabelX = 17; inventoryLabelY = 148;
    }
    @Override protected void init() {
        super.init(); buttons.clear();
        button(MountMenu.FOLLOW, "follow", 8, 22, 86); button(MountMenu.STAY, "stay", 102, 22, 86);
        button(MountMenu.BREED, "breed", 8, 44, 180);
        button(MountMenu.NEXT_PLAYER, "next_player", 8, 100, 180);
        button(MountMenu.OFFER, "offer", 8, 122, 86); button(MountMenu.CANCEL, "cancel", 102, 122, 86);
        button(MountMenu.ACCEPT, "accept", 8, 122, 180);
        containerTick();
    }
    private void button(int action, String key, int x, int y, int width) {
        buttons.put(action, addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.translatable("mount.darkspawn." + key),
            ignored -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action))
            .bounds(leftPos+x, topPos+y, width, 18).tooltip(Tooltip.create(Component.translatable("mount.darkspawn."+key+"_hint"))).build()));
    }
    @Override protected void containerTick() {
        super.containerTick();
        buttons.forEach((id, button) -> { button.visible = id == MountMenu.ACCEPT ? !menu.owner() : menu.owner(); button.active = menu.getCarried().isEmpty(); });
        buttons.get(MountMenu.FOLLOW).active &= !menu.following(); buttons.get(MountMenu.STAY).active &= menu.following() || menu.breeding();
        buttons.get(MountMenu.BREED).active &= !menu.baby() && !menu.following() && !menu.breeding() && !menu.pending();
        buttons.get(MountMenu.OFFER).active &= menu.selectedId() >= 0 && !menu.pending() && !menu.breeding();
        buttons.get(MountMenu.CANCEL).active &= menu.pending();
        buttons.get(MountMenu.ACCEPT).active &= menu.pending();
        var selected = minecraft.level == null ? null : minecraft.level.getEntity(menu.selectedId());
        buttons.get(MountMenu.NEXT_PLAYER).setMessage(selected == null ? Component.translatable("mount.darkspawn.next_player")
            : Component.literal(font.plainSubstrByWidth(selected.getName().getString(), 162)));
        buttons.get(MountMenu.BREED).setMessage(Component.translatable(menu.baby() ? "mount.darkspawn.growing" : menu.breeding() ? "mount.darkspawn.breeding" : "mount.darkspawn.breed"));
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        darkspawn.black.client.UiTheme.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            int x = leftPos+slot.x, y = topPos+slot.y;
            graphics.fill(x-1,y-1,x+17,y+17,0xFF566F73); graphics.fill(x,y,x+16,y+16,0xFFB0C1B9);
        }
    }
}
