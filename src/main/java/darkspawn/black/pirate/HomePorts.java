package darkspawn.black.pirate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Owner-only port services reuse captured forts without replacing blocks or sharing ship cargo. */
public final class HomePorts {
    private HomePorts() { }
    public static boolean near(PirateShip ship,int required) {
        if(!(ship.level() instanceof net.minecraft.server.level.ServerLevel level)||ship.owner()==null||!level.dimension().equals(Pirate.LEVEL))return false;
        var port=SeaLedger.get(level).homePort(ship.owner());
        return port!=null&&port.level()>=required&&ship.distanceToSqr(Vec3.atCenterOf(port.center()))<=48*48;
    }
    public static boolean services(ServerPlayer player) {
        var port=SeaLedger.get(player.level()).homePort(player.getUUID());
        return port!=null&&port.level()>=2&&SeaTerritory.ownsHere(player)&&player.distanceToSqr(Vec3.atCenterOf(port.center()))<=24*24;
    }
    public static SeaServices.Reply order(ServerPlayer player,int action) {
        if(!player.isAlive()||player.isSpectator()||!player.level().dimension().equals(Pirate.LEVEL)||!SeaTerritory.ownsHere(player))return SeaServices.Reply.OWNER;
        if(SeaTerritory.contested(player))return SeaServices.Reply.CONTESTED;
        var island=IslandDiscoveries.at(player.level(),player.blockPosition());var ledger=SeaLedger.get(player.level());var port=ledger.homePort(player.getUUID());
        if(action==20) {
            if(port!=null&&port.center().equals(island.center()))return SeaServices.Reply.RECORDED;
            ledger.homePort(new SeaLedger.HomePort(player.getUUID(),island.center(),0));return SeaServices.Reply.DONE;
        }
        if(port==null||!port.center().equals(island.center()))return SeaServices.Reply.OWNER;
        if(port.level()>=2)return SeaServices.Reply.PORT_MAX;
        int iron=port.level()==0?32:16,wood=port.level()==0?32:64;
        if(!Ships.pay(player,Items.IRON_INGOT,iron,false)||!Ships.pay(player,Items.OAK_PLANKS,wood,false)||!Ships.pay(player,Items.EMERALD,8,false))return SeaServices.Reply.COST;
        Ships.pay(player,Items.IRON_INGOT,iron,true);Ships.pay(player,Items.OAK_PLANKS,wood,true);Ships.pay(player,Items.EMERALD,8,true);
        ledger.homePort(new SeaLedger.HomePort(player.getUUID(),port.center(),port.level()+1));return SeaServices.Reply.DONE;
    }
}
