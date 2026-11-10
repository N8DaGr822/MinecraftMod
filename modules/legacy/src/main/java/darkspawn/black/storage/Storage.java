package darkspawn.black.storage;

import darkspawn.black.Darkspawn;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class Storage {
	public static final DataComponentType<BankContents> BANK_CONTENTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		Darkspawn.id("bank_contents"), DataComponentType.<BankContents>builder().persistent(BankContents.CODEC)
			.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(BankContents.CODEC)).build());
	public static final StorageBlock DUMP = chest("dump_chest");
	public static final StorageBlock SORTING = chest("sorting_chest");
	public static final StorageBlock BANK = chest("bank_chest");
	public static final SortingTubeBlock TUBE = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("sorting_tube"),
		new SortingTubeBlock(properties("sorting_tube").strength(1.5F).sound(SoundType.METAL).noOcclusion()));
	public static final BlockEntityType<StorageBlockEntity> ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Darkspawn.id("storage"), new BlockEntityType<>(StorageBlockEntity::new, Set.of(DUMP, SORTING, BANK)));
	public static final MenuType<StorageMenu> MENU = Registry.register(BuiltInRegistries.MENU, Darkspawn.id("storage"),
		new MenuType<>(StorageMenu::new, FeatureFlags.VANILLA_SET));

	private Storage() { }
	private static BlockBehaviour.Properties properties(String name) {
		return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id(name)));
	}
	private static StorageBlock chest(String name) {
		return Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id(name),
			new StorageBlock(properties(name).strength(2.5F).sound(SoundType.WOOD)));
	}

	public static void initialize() {
		for (Block block : new Block[] { DUMP, SORTING, BANK, TUBE }) {
			var key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
			Item item = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block,
				new Item.Properties().setId(key).useBlockDescriptionPrefix().stacksTo(block == BANK ? 1 : 64)) {
				@Override public boolean canFitInsideContainerItems() { return block != BANK; }
			});
			CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(item));
		}
		PayloadTypeRegistry.serverboundPlay().register(StorageActionPayload.TYPE, StorageActionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(StorageActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageMenu menu && menu.containerId == payload.menuId()
					&& menu.stillValid(context.player())) {
				if (menu.getStateId() != payload.stateId()) {
					menu.broadcastFullState();
					context.player().sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.darkspawn.inventory_changed"));
					return;
				}
				menu.action(context.player(), payload.action(), payload.query());
			}
		});
	}
}
