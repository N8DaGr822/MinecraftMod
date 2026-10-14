package darkspawn.black.client;

import darkspawn.black.journal.JournalRequestPayload;
import darkspawn.black.journal.JournalSnapshotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class JournalControls {
    private JournalControls() { }
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(JournalSnapshotPayload.TYPE, (payload, context) -> {
            // Response Ownership: A late reply must never reopen a closed journal or replace another screen.
            if (context.client().gui.screen() instanceof JournalScreen journal) journal.receive(payload);
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof InventoryScreen) || !ClientPlayNetworking.canSend(JournalRequestPayload.TYPE)) return;
            var button = darkspawn.black.client.UiTheme.button(Component.translatable("button.darkspawn.journal"), pressed -> client.gui.setScreen(new JournalScreen(screen)))
                .bounds(6,54,62,20).build();
            Runnable refresh = () -> button.active = client.player != null && client.player.inventoryMenu.getCarried().isEmpty();
            refresh.run(); Screens.getWidgets(screen).add(button);
            ScreenEvents.afterTick(screen).register(current -> refresh.run());
        });
    }
}
