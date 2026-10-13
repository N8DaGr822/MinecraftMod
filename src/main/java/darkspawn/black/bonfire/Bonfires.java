package darkspawn.black.bonfire;

import darkspawn.black.Darkspawn;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class Bonfires {
	private Bonfires() { }
	public static final Block CORE = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("barracks_bonfire"),
		new BonfireBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id("barracks_bonfire")))
			.strength(3.5F).sound(SoundType.METAL).noOcclusion()
			.lightLevel(state -> state.getValue(BonfireBlock.PHASE) == 0 ? 0 : 15)));
	public static final Block LIGHT = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("ward_light"),
		new WardLightBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id("ward_light")))
			.noCollision().noOcclusion().replaceable().noLootTable().lightLevel(state -> 15)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("barracks_bonfire"),
		new BlockItem(CORE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("barracks_bonfire"))).useBlockDescriptionPrefix()));
	public static final BlockEntityType<BonfireBlockEntity> ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Darkspawn.id("barracks_bonfire"), new BlockEntityType<>(BonfireBlockEntity::new, Set.of(CORE)));
	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(ITEM));
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> chunk.getBlockEntities().values().forEach(entity -> {
			if (entity instanceof BonfireBlockEntity bonfire) BonfireWards.register(level, bonfire.getBlockPos());
		}));
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> BonfireWards.unload(level, chunk.getPos()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BonfireWards.clear());
	}
}
