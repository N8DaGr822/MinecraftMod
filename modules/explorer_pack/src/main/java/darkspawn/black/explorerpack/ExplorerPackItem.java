package darkspawn.black.explorerpack;

import darkspawn.black.core.DarkspawnCore;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

public final class ExplorerPackItem extends Item {
	public static final TagKey<Item> CONTENTS = TagKey.create(Registries.ITEM, DarkspawnCore.id("explorer_pack_contents"));
	public static final MenuType<ExplorerPackMenu> MENU = Registry.register(BuiltInRegistries.MENU,
		DarkspawnCore.id("explorer_pack"), new MenuType<>(ExplorerPackMenu::new, FeatureFlags.VANILLA_SET));
	public static Item ITEM;
	private ExplorerPackItem(Properties properties) { super(properties); }
	public static void register() {
		var key = ResourceKey.create(Registries.ITEM, DarkspawnCore.id("explorer_pack"));
		ITEM = Registry.register(BuiltInRegistries.ITEM, key, new ExplorerPackItem(new Properties().setId(key)
			.stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
			.component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("tooltip.darkspawn.explorer_pack"))))));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ITEM));
	}
	public static boolean accepts(ItemStack stack) {
		// Pack Eligibility: Data-pack extensions cannot introduce nested containers.
		return !stack.isEmpty() && stack.is(CONTENTS) && stack.getItem().canFitInsideContainerItems()
			&& !stack.has(DataComponents.CONTAINER) && !stack.has(DataComponents.BUNDLE_CONTENTS);
	}
	@Override public boolean canFitInsideContainerItems() { return false; }
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server && player.isAlive() && !player.isSpectator()
				&& player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty()) {
			ItemStack pack = player.getItemInHand(hand);
			// Corrupt/command-authored overflow must not be silently truncated by the six-slot UI.
			if (pack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).size() > 6) {
				player.sendOverlayMessage(Component.translatable("message.darkspawn.explorer_pack.overflow"));
				return InteractionResult.FAIL;
			}
			int source = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().getSelectedSlot();
			server.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new ExplorerPackMenu(id, inventory, source), pack.getHoverName()));
		}
		return InteractionResult.SUCCESS;
	}
}
