package darkspawn.black.worldgen;

import darkspawn.black.Darkspawn;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.*;

/** Copy a previously located landmark into a native, shareable map without another structure search. */
public final class DiscoveryCharts extends Item {
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,Darkspawn.id("blank_discovery_chart"),new DiscoveryCharts());
    private DiscoveryCharts(){super(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Darkspawn.id("blank_discovery_chart"))).stacksTo(16)
        .component(DataComponents.LORE,new ItemLore(java.util.List.of(Component.literal("Use with a located Landmark Compass in your other hand."),Component.literal("Creates a shareable map. Terrain fills in as you explore.")))));}
    public static ItemStack chart(ServerPlayer player,ItemStack compass) {
        if(!compass.is(LandmarkCompass.ITEM))return ItemStack.EMPTY;
        var tracker=compass.get(DataComponents.LODESTONE_TRACKER);if(tracker==null||tracker.target().isEmpty())return ItemStack.EMPTY;
        var target=tracker.target().get();if(!target.dimension().equals(player.level().dimension()))return ItemStack.EMPTY;
        var map=MapItem.create(player.level(),target.pos().getX(),target.pos().getZ(),(byte)2,true,true);
        MapItemSavedData.addTargetDecoration(map,target.pos(),"darkspawn_landmark",MapDecorationTypes.RED_X);
        map.set(DataComponents.CUSTOM_NAME,Component.literal(LandmarkCompass.selected(compass).title()+" Discovery Chart"));
        map.set(DataComponents.LORE,new ItemLore(java.util.List.of(Component.literal("Ritual site: "+target.pos().getX()+", "+target.pos().getY()+", "+target.pos().getZ()),Component.literal("Normal summon rules apply. Sharing a chart grants no progression."))));return map;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer server&&player.isAlive()&&!player.isSpectator()) {
            if(player.getInventory().getFreeSlot()<0){player.sendSystemMessage(Component.literal("Leave one inventory slot for your discovery map."));return InteractionResult.FAIL;}
            var map=chart(server,player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND));
            if(map.isEmpty()){player.sendSystemMessage(Component.literal("Locate a site with the Landmark Compass first. Hold it in your other hand and use the chart in that site's dimension."));return InteractionResult.FAIL;}
            player.getItemInHand(hand).consume(1,player);player.getInventory().add(map);
        }
        return InteractionResult.SUCCESS;
    }
    public static void initialize(){net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries->entries.accept(ITEM));}
}
