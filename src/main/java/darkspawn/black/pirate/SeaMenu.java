package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class SeaMenu extends AbstractContainerMenu {
    public static final MenuType<SeaMenu> TYPE=Registry.register(BuiltInRegistries.MENU,Darkspawn.id("sea_services"),new MenuType<>(SeaMenu::new,FeatureFlags.VANILLA_SET));
    private final Player viewer;private final BlockPos origin;
    private final SimpleContainerData data=new SimpleContainerData(12);
    public SeaMenu(int id,Inventory inventory){this(id,inventory,null);}
    public SeaMenu(int id,Inventory inventory,BlockPos origin){super(TYPE,id);this.viewer=inventory.player;this.origin=origin;addDataSlots(data);refresh();}
    public static boolean available(ServerPlayer p){if(!p.isAlive()||p.isSpectator()||!p.level().dimension().equals(Pirate.LEVEL))return false;if(HarborGuide.inHarbor(p))return true;var island=IslandDiscoveries.at(p.level(),p.blockPosition());return island!=null&&(island.kind()==1||island.kind()==4);}
    public static void open(ServerPlayer p){if(available(p))p.openMenu(new SimpleMenuProvider((id,inv,who)->new SeaMenu(id,inv,p.blockPosition()),Component.literal("Harbor & Sea Services")));}
    public static void initialize() { }
    public int status(int i){return data.get(i);}
    private void refresh(){if(viewer instanceof ServerPlayer p){var s=Pirate.state(p);data.set(0,s.renown);data.set(1,s.infamy);data.set(2,s.seals);data.set(3,s.ending);data.set(4,SeaLedger.get(p.level()).lost(p.getUUID()));var island=IslandDiscoveries.at(p.level(),p.blockPosition());var fort=island==null?null:SeaLedger.get(p.level()).fort(island.center());data.set(5,fort!=null&&fort.owner().equals(p.getUUID())?1:0);data.set(6,fort==null?0:fort.defenses());data.set(7,SeaTerritory.progress(p));var port=SeaLedger.get(p.level()).homePort(p.getUUID());data.set(11,port==null?-1:port.level());}}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p.level().isClientSide()||p instanceof ServerPlayer server&&origin!=null&&available(server)&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(origin))<=64;}
    @Override public boolean clickMenuButton(Player p,int action){
        if(!(p instanceof ServerPlayer server)||!stillValid(p)||!getCarried().isEmpty())return false;
        if(action==0||action==1){data.set(8,Math.floorMod(data.get(8)+(action==0?-1:1),SeaServices.NAMES.length));data.set(9,0);data.set(10,0);}
        else if(action==2){int selected=data.get(8);if((selected>=15&&selected<=17||selected==20)&&data.get(10)==0){data.set(10,1);data.set(9,(selected==20?SeaServices.Reply.PORT_CONFIRM:SeaServices.Reply.CONFIRM).ordinal());}
            else {data.set(10,0);data.set(9,SeaServices.execute(server,selected).ordinal());}}
        else return false;
        broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
}
