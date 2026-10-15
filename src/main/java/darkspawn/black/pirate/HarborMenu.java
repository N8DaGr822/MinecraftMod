package darkspawn.black.pirate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class HarborMenu extends AbstractContainerMenu {
	private final Entity guide;
	private final BlockPos board;
	private final Player viewer;
	private final SimpleContainerData data=new SimpleContainerData(15);
	public HarborMenu(int id,Inventory inventory) {this(id,inventory,null,null);}
	public HarborMenu(int id,Inventory inventory,Entity guide,BlockPos board) {
		super(HarborGuide.MENU,id);this.guide=guide;this.board=board;viewer=inventory.player;addDataSlots(data);if(guide!=null&&PirateDiplomacy.contact(guide)){data.set(4,4+PirateDiplomacy.faction(guide).ordinal());data.set(0,4);}refresh();
	}
	public int status(int index) {return data.get(index);}
	private void refresh() {if(viewer instanceof ServerPlayer player) {
        var state=Pirate.state(player);
        for(int i=0;i<3;i++)data.set(5+i,state.factionStanding[i]);
        data.set(8,PirateProgression.captainTier(state));data.set(3,guide!=null&&guide.entityTags().contains(HarborGuide.BRIGGS_TAG)?1:0);
        data.set(10,guide!=null&&PirateDiplomacy.contact(guide)?1:0);
        var member=PirateDiplomacy.faction(state.factionMembership);data.set(9,member==null?0:member.ordinal()+1);
        data.set(14,member==null?0:state.factionRanks[member.ordinal()]);
        int selected=data.get(4);data.set(11,0);data.set(12,state.garrickRescue);
        if(selected>=7&&selected<10){int index=selected-7;data.set(1,state.repeatContracts[index]==0?0:1);data.set(2,state.repeatContracts[index]==2?1:0);data.set(11,(int)Math.min(600,(PirateActivities.cooldown(player,index)+19)/20));}
        else if(selected==10){data.set(1,state.garrickRescue>=4?2:state.garrickRescue==0?0:1);data.set(2,state.garrickRescue==3?1:0);}
        else if(selected>=11){int progress=selected==11?state.sealContract:state.officerContracts[selected-12];data.set(1,progress>=3?2:progress==0?0:1);data.set(2,progress==2?1:0);}
        else {data.set(1,selected==0?state.harborContract:selected<4?state.islandContracts[selected-1]:state.factionContracts[selected-4]);data.set(2,selected==0?(state.harborSurveyed?1:0):(((selected<4?state.islandObjectives:state.factionObjectives)&(1<<(selected<4?selected-1:selected-4)))!=0?1:0));}
    }}
	@Override public void broadcastChanges() {refresh();super.broadcastChanges();}
	@Override public boolean stillValid(Player player) {
		if(player.level().isClientSide())return true;
		if(guide!=null&&PirateDiplomacy.contact(guide))return player.isAlive()&&!player.isSpectator()&&player.level().dimension().equals(Pirate.LEVEL)&&guide.isAlive()&&guide.level()==player.level()&&player.distanceToSqr(guide)<=36;
        if(!HarborGuide.inHarbor(player))return false;
		return guide!=null ? guide.isAlive()&&guide.level()==player.level()&&guide.entityTags().contains(HarborGuide.BRIGGS_TAG)&&player.distanceToSqr(guide)<=36
			: board!=null&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(board))<=36&&player.level().getBlockState(board).is(HarborGuide.BOARD);
	}
	@Override public boolean clickMenuButton(Player player,int action) {
		if(!(player instanceof ServerPlayer server)||!stillValid(player))return false;
        data.set(13,0);if(!getCarried().isEmpty()){data.set(0,8);data.set(13,HarborFeedback.CURSOR.ordinal());broadcastChanges();return false;}
		if(action>=0&&action<=2)data.set(0,action);
		else if(action==3) {if(data.get(3)==0)return false;boolean issued=HarborGuide.compass(server);data.set(0,issued?3:8);if(!issued){var reason=HarborFeedback.COMPASS_COST;for(int i=0;i<server.getInventory().getContainerSize();i++)if(server.getInventory().getItem(i).is(LogboundCompass.ITEM))reason=HarborFeedback.COMPASS_OWNED;if(reason!=HarborFeedback.COMPASS_OWNED&&server.getInventory().getFreeSlot()<0)reason=HarborFeedback.SPACE;data.set(13,reason.ordinal());}}
		else if(action>=4&&action<=6) {var reason=HarborFeedback.contract(server,data.get(4),action);if(reason!=HarborFeedback.NONE){data.set(0,8);data.set(13,reason.ordinal());broadcastChanges();return false;}data.set(0,(data.get(4)==0?HarborGuide.contract(server,action):data.get(4)<4?HarborGuide.islandContract(server,data.get(4)-1,action):data.get(4)<7?PirateProgression.contract(server,data.get(4)-4,action):data.get(4)<10?PirateActivities.contract(server,data.get(4)-7,action):data.get(4)==10?PirateActivities.rescueOrder(server,action):PirateOfficers.contract(server,data.get(4),action))?4:8);}
		else if(action==7||action==8){if(data.get(10)!=0)data.set(4,(data.get(4)<7?7:4)+PirateDiplomacy.faction(guide).ordinal());else data.set(4,Math.floorMod(data.get(4)+(action==7?-1:1),HarborGuide.CONTRACT_NAMES.length));data.set(0,4);}
        else if(action>=9&&action<=11) {
            var selected=data.get(4)>=4&&data.get(4)<10?PirateProgression.Faction.values()[(data.get(4)-4)%3]:action==10?PirateDiplomacy.faction(Pirate.state(server).factionMembership):null;
            var state=Pirate.state(server);HarborFeedback reason=HarborFeedback.NONE;
            if(action==10&&state.factionMembership.isEmpty())reason=HarborFeedback.NO_MEMBERSHIP;
            else if(selected==null)reason=HarborFeedback.FACTION;
            else if(action==9&&!state.factionMembership.isEmpty())reason=HarborFeedback.MEMBERSHIP;
            else if(action==9&&PirateProgression.standing(state,selected)<25)reason=HarborFeedback.TRUST;
            else if(action==11&&PirateProgression.standing(state,selected)>=25)reason=HarborFeedback.TRUSTED;
            else if(action==11&&!Ships.pay(server,net.minecraft.world.item.Items.GOLD_INGOT,8,false))reason=HarborFeedback.GOLD;
            if(reason==HarborFeedback.NONE&&!PirateDiplomacy.membership(server,selected,action))reason=HarborFeedback.LOCATION;
            data.set(0,reason==HarborFeedback.NONE?9:8);data.set(13,reason.ordinal());
        }
		else if(action==12){SeaMenu.open(server);return true;}
        else if(action==13){FactionPromotions.promote(server);data.set(0,1);}
        else return false;
        if(data.get(0)==8&&data.get(13)==0)data.set(13,HarborFeedback.RETRY.ordinal());
		refresh();broadcastChanges();return true;
	}
	@Override public ItemStack quickMoveStack(Player player,int index) {return ItemStack.EMPTY;}
}
