package darkspawn.black.pirate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ShipMenu extends AbstractContainerMenu {
	public static final int BUILD=0,LAUNCH=1,DOCK=2,UPGRADE=3,MATE=4,WORKER=5,GUNNER=6,LOOKOUT=7,FIRE_ORDER=8,TARGET=9,FLAG=10,CARGO_UPGRADE=11,CARGO_PAGE=12,RESUPPLY=13,NAVIGATOR=14,QUARTERMASTER=15,SINGER=16,SHANTY=17;
	private final Player viewer;
	private final BlockPos station;
	private final PirateShip ship;
	private final SimpleContainerData data=new SimpleContainerData(22);
	public ShipMenu(int id,Inventory inventory) { this(id,inventory,null,null); }
	public ShipMenu(int id,Inventory inventory,BlockPos station,PirateShip ship) {
		super(Ships.MENU,id);this.viewer=inventory.player;this.station=station;this.ship=ship;
		var cargo=ship==null?new SimpleContainer(54):ship.cargo;
		for(int row=0;row<6;row++)for(int col=0;col<9;col++)addSlot(new Slot(cargo,row*9+col,43+18*col,120+18*(row%3)){
			@Override public boolean isActive(){return getContainerSlot()<data.get(15)&&getContainerSlot()/27==data.get(16);}
            @Override public boolean mayPlace(ItemStack stack){return isActive()&&hasShip()&&(ShipMenu.this.ship==null||ShipMenu.this.ship.ownedBy(inventory.player));}
			@Override public boolean mayPickup(Player player){return mayPlace(getItem())&&stillValid(player);}
		});
		addStandardInventorySlots(inventory,43,194);addDataSlots(data);refresh(inventory.player);
	}
	public boolean hasShip(){return data.get(0)!=0;}
	public int status(int index){return data.get(index);}
	private void refresh(Player player){
		if(player instanceof ServerPlayer server) {
            data.set(9,Pirate.state(server).navalVictories);data.set(10,PirateProgression.captainTier(Pirate.state(server)));
            for(int role=0;role<4;role++)data.set(11+role,PirateProgression.crewCost(Pirate.state(server),role));
        }
        if(ship==null)return;
		data.set(15,ship.cargoSlots());data.set(17,ship.flag());data.set(18,ship.crew(PirateShip.NAVIGATOR));data.set(19,ship.crew(PirateShip.QUARTERMASTER));data.set(20,ship.crew(PirateShip.SINGER));data.set(21,ship.shantyPlaying()?1:0);
		data.set(0,1);data.set(1,ship.docked()?1:0);data.set(2,ship.tier());data.set(3,Math.round(ship.hull()));
		for(int i=0;i<4;i++)data.set(4+i,ship.crew(i));data.set(8,ship.holdFire()?1:0);
	}
	@Override public boolean stillValid(Player player){
		if(player.level().isClientSide())return true;
		if(!player.isAlive()||player.isSpectator())return false;
		if(ship!=null && (!ship.isAlive()||ship.level()!=player.level()||!ship.ownedBy(player)))return false;
		return station!=null ? player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(station))<=36 && (player.level().getBlockState(station).is(Ships.SHIPYARD)||player.level().getBlockState(station).is(Ships.HELM)||player.level().getBlockState(station).is(Ships.CANNON))
			&& (ship==null||ship.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(station))<=48*48) : ship!=null&&(player.getVehicle()==ship||player.distanceToSqr(ship)<=64);
	}
	@Override public void broadcastChanges(){refresh(viewer);super.broadcastChanges();}
	@Override public boolean clickMenuButton(Player player,int action){
		if(!(player instanceof ServerPlayer captain)||!stillValid(player)||!getCarried().isEmpty())return false;
		String error=null;
		if(action==BUILD && ship==null && station!=null && player.level().getBlockState(station).is(Ships.SHIPYARD)){
			if(!player.level().dimension().equals(Pirate.LEVEL)){Ships.message(player,"Build ships in Pirate Seas.");return false;}
			var built=PirateShip.build(captain,new BlockPos(station.getX(),64,station.getZ()+20));
			if(built==null)error="Need 105 oak planks, one helm, and clear water 20 blocks south of the shipyard.";
			else {captain.closeContainer();Ships.open(captain,station);return true;}
		}else if(ship==null)return false;
		else if(action==LAUNCH)error=ship.launch(captain);
		else if(action==DOCK)error=ship.dock(captain);
		else if(action==UPGRADE)error=ship.upgrade(captain);
		else if(action>=MATE&&action<=LOOKOUT){if(!ship.recruit(captain,action-MATE))error="Recruit docked at harbor: this role costs "+PirateProgression.crewCost(Pirate.state(captain),action-MATE)+" emeralds. Crew limits apply.";}
		else if(action==FIRE_ORDER)ship.toggleFire();
		else if(action==TARGET)ship.selectTarget(captain);
        else if(action==FLAG){if(!ship.cycleFlag(captain))error="Dock your ship to change flags.";}
        else if(action==CARGO_UPGRADE){if(!ship.upgradeCargo(captain))error="At harbor: Captain + 16 iron/4 emeralds for 36 slots; Veteran + 32 iron/8 emeralds for 54.";}
        else if(action==CARGO_PAGE){data.set(16,ship.cargoSlots()>27?1-data.get(16):0);}
        else if(action==RESUPPLY){if(!ship.resupply(captain))error="Dock with Bram aboard and room for your planks or ammunition.";}
        else if(action>=NAVIGATOR&&action<=SINGER){if(!ship.recruitOfficer(captain,action-NAVIGATOR))error="Complete and claim this officer's harbor commission, then hire docked at harbor.";}
        else if(action==SHANTY){if(!ship.toggleShanty(captain))error="Recruit Nell before starting a shanty.";}
		else return false;
		Ships.message(player,error==null?"Ship orders updated.":error);refresh(player);broadcastChanges();return error==null;
	}
    public boolean rename(ServerPlayer player,String name) {
        if(!stillValid(player)||!getCarried().isEmpty()||ship==null||!ship.rename(player,name))return false;
        Ships.message(player,name.isBlank()?"Ship name cleared.":"Ship named "+name.strip()+".");return true;
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.isActive()&&super.canTakeItemForPickAll(stack,slot);}

	@Override public ItemStack quickMoveStack(Player player,int index){
		if(!stillValid(player)||!hasShip()||index<0||index>=slots.size())return ItemStack.EMPTY;
		var slot=slots.get(index);if(!slot.isActive()||!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
		var stack=slot.getItem();var copy=stack.copy();if(index<54){if(!moveItemStackTo(stack,54,slots.size(),true))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,data.get(16)*27,Math.min(ship.cargoSlots(),(data.get(16)+1)*27),false))return ItemStack.EMPTY;
		if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
	}
}
