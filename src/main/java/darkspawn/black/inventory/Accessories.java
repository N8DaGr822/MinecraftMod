package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.gamerules.GameRules;

public final class Accessories {
    public static final String[] IDS={"night_vision_glasses","diving_charm","fall_talisman","pickup_magnet","navigation_lens","heat_trinket"};
    public static final String[] DETAILS={"Equip: night vision. Toggle with N or the hub. No charge.","Equip: air drains half as often. Does not stack with Deep Breath.","Equip: 25% less fall damage. Does not prevent lethal falls.","Equip: collect eligible drops within 3 blocks and clear sight.","Equip: coordinates and bearing to a carried located compass.","Equip: 25 points of climate heat protection. No fire immunity."};
    public static final List<Item> ITEMS=new ArrayList<>();
    public static final int FIRST_MENU_SLOT=InventoryExpansion.TRASH_MENU_SLOT+1;
    private Accessories() { }
    public static AccessoryState state(Player p){return ((AccessoryPlayer)p).darkspawn$accessories();}
    public static boolean equipped(Player p,int kind){return ((p.level().isClientSide()?state(p).activeMask:mask(p))&(1<<kind))!=0;}
    public static int mask(Player p){int mask=0;for(var stack:state(p).items()){int i=ITEMS.indexOf(stack.getItem());if(i>=0)mask|=1<<i;}return mask;}
    public static boolean vision(Player p){return state(p).glasses&&equipped(p,0);}
    public static void sync(ServerPlayer p){var s=state(p);s.activeMask=mask(p);if(ServerPlayNetworking.canSend(p,AccessorySyncPayload.TYPE))ServerPlayNetworking.send(p,new AccessorySyncPayload(s.activeMask,s.glasses,s.favorites));}
    public static void action(ServerPlayer p,AccessoryActionPayload request){
        if(!p.isAlive()||p.isSpectator()||p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty())return;
        if(request.stateId()!=p.inventoryMenu.getStateId()){p.inventoryMenu.broadcastFullState();sync(p);return;}
        if(request.action()==-1)state(p).glasses=!state(p).glasses;
        else {int slot=request.action(),i=AccessoryState.favoriteIndex(slot);if(i<0||slot>=InventoryExpansion.FIRST_EXTRA_SLOT+InventoryExpansion.unlockedSlots(p.getInventory()))return;state(p).favorites^=1L<<i;}
        sync(p);
    }
    public static void initialize(){
        for(String id:IDS){var key=ResourceKey.create(Registries.ITEM,Darkspawn.id(id));ITEMS.add(Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key).stacksTo(1).component(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(List.of(net.minecraft.network.chat.Component.literal(DETAILS[ITEMS.size()])))))));}
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries->ITEMS.forEach(entries::accept));
        PayloadTypeRegistry.serverboundPlay().register(AccessoryActionPayload.TYPE,AccessoryActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AccessorySyncPayload.TYPE,AccessorySyncPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AccessoryActionPayload.TYPE,(request,context)->action(context.player(),request));
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->sync(handler.player));
        ServerPlayerEvents.COPY_FROM.register((old,next,alive)->{var source=state(old);var target=state(next);target.glasses=source.glasses;target.favorites=source.favorites;if(alive||next.level().getGameRules().get(GameRules.KEEP_INVENTORY)||old.isSpectator())target.restore(source.items());});
        ServerPlayerEvents.AFTER_RESPAWN.register((old,next,alive)->sync(next));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10==0)server.getPlayerList().getPlayers().forEach(Accessories::tick);});
    }
    public static void tick(ServerPlayer p){
            if(state(p).activeMask!=mask(p))sync(p);
            var fall=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER);
            if(fall!=null){var id=Darkspawn.id("fall_talisman");boolean worn=equipped(p,2);if(!worn)fall.removeModifier(id);else if(!fall.hasModifier(id))fall.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,-.25,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
            if(!p.isAlive()||p.isSpectator()||!equipped(p,3))return;
            // Magnet Ownership: Native pickup validates delay, owner and capacity; solid walls block attraction.
            for(var item:p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(3),e->e.isAlive()&&!e.hasPickUpDelay()&&p.distanceToSqr(e)<=9)){
                if(!p.hasLineOfSight(item))continue;item.playerTouch(p);
            }
    }
}
