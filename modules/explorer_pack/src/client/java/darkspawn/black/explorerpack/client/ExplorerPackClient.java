package darkspawn.black.explorerpack.client;

import darkspawn.black.explorerpack.ExplorerPackItem;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ExplorerPackClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ExplorerPackItem.MENU, ExplorerPackScreen::new);
	}
}
