package darkspawn.black.storage.client;

import darkspawn.black.storage.BankContents;
import darkspawn.black.storage.SortingTubeBlock;
import darkspawn.black.storage.Storage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;

public final class StorageClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(Storage.MENU, StorageScreen::new);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SortingTubeBlock) {
				lines.add(Component.translatable(blockItem.getBlock() == Storage.TUBE
					? "storage.darkspawn.tube_hint" : "storage.darkspawn.block_hint").withStyle(ChatFormatting.GRAY));
				var contents = stack.get(Storage.BANK_CONTENTS);
				if (contents != null) {
					long count = java.util.stream.Stream.concat(contents.stored().stream(), contents.trash().stream())
						.mapToLong(BankContents.Entry::count).sum();
					lines.add(Component.translatable("storage.darkspawn.bank_item_hint", count, contents.tier() + 1).withStyle(ChatFormatting.GRAY));
				}
			}
		});
	}
}
