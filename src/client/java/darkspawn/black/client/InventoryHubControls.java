package darkspawn.black.client;
import darkspawn.black.inventory.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class InventoryHubControls {
    public static String query="";
    private InventoryHubControls() { }
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(AccessorySyncPayload.TYPE,(payload,context)->{var s=Accessories.state(context.player());s.connected=true;s.activeMask=payload.mask();s.glasses=payload.glasses();s.favorites=payload.favorites();});
        ClientPlayNetworking.registerGlobalReceiver(HubSnapshotPayload.TYPE,(payload,context)->{if(context.client().gui.screen() instanceof InventoryHubScreen hub)hub.receive(payload);});
        var key=net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.darkspawn.glasses",com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD,com.mojang.blaze3d.platform.InputConstants.KEY_N,net.minecraft.client.KeyMapping.Category.register(darkspawn.black.Darkspawn.id("accessories"))));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{while(key.consumeClick())if(client.player!=null&&client.gui.screen()==null&&ClientPlayNetworking.canSend(AccessoryActionPayload.TYPE))ClientPlayNetworking.send(new AccessoryActionPayload(-1,client.player.inventoryMenu.getStateId()));});
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,darkspawn.black.Darkspawn.id("accessories"),(g,delta)->{
            var client=net.minecraft.client.Minecraft.getInstance();var p=client.player;if(p==null||p.isSpectator())return;
            if(Accessories.equipped(p,0))g.text(client.font,"NV "+(Accessories.vision(p)?"ON":"OFF"),6,6,0xFFE8DBB5,false);
            if(!Accessories.equipped(p,4))return;
            String text=p.getDirection().getName()+" | "+p.blockPosition().toShortString();
            for(int i=0;i<p.getInventory().getContainerSize();i++){var tracker=p.getInventory().getItem(i).get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);if(tracker==null||tracker.target().isEmpty()||!tracker.target().get().dimension().equals(p.level().dimension()))continue;var target=tracker.target().get().pos();double dx=target.getX()-p.getX(),dz=target.getZ()-p.getZ();String direction=(dz<0?"N":"S")+(dx<0?"W":"E");text+=" | Target "+direction+" "+Math.round(Math.sqrt(dx*dx+dz*dz))+"m";break;}
            g.text(client.font,text,6,18,0xFFE8DBB5,false);
        });
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof InventoryScreen)||!ClientPlayNetworking.canSend(HubRequestPayload.TYPE))return;
            var search=new net.minecraft.client.gui.components.EditBox(client.font,6,104,62,18,Component.literal("Search inventory"));search.setMaxLength(64);search.setValue(query);search.setHint(Component.literal("Search"));search.setResponder(value->query=value);Screens.getWidgets(screen).add(search);
            var button=UiTheme.button(Component.literal("Hub"),b->client.gui.setScreen(new InventoryHubScreen(screen))).bounds(6,78,62,20).build();
            Runnable update=()->button.active=client.player!=null&&client.player.inventoryMenu.getCarried().isEmpty();update.run();Screens.getWidgets(screen).add(button);ScreenEvents.afterTick(screen).register(s->update.run());
        });
    }
}
