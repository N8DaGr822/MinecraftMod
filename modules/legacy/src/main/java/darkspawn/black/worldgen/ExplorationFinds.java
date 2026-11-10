package darkspawn.black.worldgen;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.TrophyBlock;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Archaeology finds use native recipes and item persistence; neither chain awards boss progress. */
public final class ExplorationFinds {
    public static final Item FOSSIL_FRAGMENT = find("tyrant_fossil_fragment");
    public static final Item OCEAN_NOTE = find("saltworn_ocean_note");
    public static final Item TIDE_CHART = find("decoded_tide_chart");
    public static final Block DISPLAY = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("tyrant_fossil_display"),
        new TrophyBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id("tyrant_fossil_display")))
            .strength(1.5F).sound(SoundType.BONE_BLOCK).noOcclusion()));
    public static final Item DISPLAY_ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("tyrant_fossil_display"),
        new BlockItem(DISPLAY, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("tyrant_fossil_display")))
            .useBlockDescriptionPrefix().component(DataComponents.LORE, lore("tyrant_fossil_display"))));
    private ExplorationFinds() { }
    private static ItemLore lore(String id) { return new ItemLore(List.of(Component.translatable("hint.darkspawn." + id))); }
    private static Item find(String id) {
        return Registry.register(BuiltInRegistries.ITEM, Darkspawn.id(id), new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Darkspawn.id(id))).component(DataComponents.LORE, lore(id))) {
            @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
                if (player instanceof ServerPlayer && player.isAlive() && !player.isSpectator())
                    player.sendSystemMessage(Component.translatable("guide.darkspawn." + id));
                return InteractionResult.SUCCESS;
            }
        });
    }
    public static List<ItemStack> catches(Level level, BlockPos pos, boolean openWater, List<ItemStack> original) {
        // Optional Clue: Append at most one note; preserve the native catch, components and treasure rolls.
        if (level.isClientSide() || level.dimension() != Level.OVERWORLD || !openWater || !level.getBiome(pos).is(BiomeTags.IS_OCEAN)
            || original.stream().noneMatch(s -> (s.is(Items.COD) || s.is(Items.SALMON) || s.is(Items.TROPICAL_FISH) || s.is(Items.PUFFERFISH))
                && ItemStack.isSameItemSameComponents(s, new ItemStack(s.getItem()))) || level.getRandom().nextInt(16) != 0) return original;
        var result = new ArrayList<>(original); result.add(new ItemStack(OCEAN_NOTE)); return result;
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> { e.accept(FOSSIL_FRAGMENT); e.accept(OCEAN_NOTE); e.accept(TIDE_CHART); });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> e.accept(DISPLAY_ITEM));
    }
}
