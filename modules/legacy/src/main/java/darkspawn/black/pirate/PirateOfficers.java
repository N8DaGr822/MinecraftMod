package darkspawn.black.pirate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Personal story knowledge and bounded recruitment commissions; exploration remains optional. */
public final class PirateOfficers {
    public static final String[] NAMES={"Mara Tidewise","Bram Ledger","Nell Saltvoice"};
    private PirateOfficers() { }
    public static boolean contract(ServerPlayer player,int selected,int action) {
        if(!HarborGuide.inHarbor(player)||selected<11||selected>14)return false;
        var state=Pirate.state(player);
        if(selected==11) {
            if(action==4&&state.firstSealClue&&state.sealContract==0){state.sealContract=1;return true;}
            if(action==6&&(state.sealContract==1||state.sealContract==2)){state.sealContract=0;return true;}
            if(action!=5||state.sealContract!=2||player.getInventory().getFreeSlot()<0)return false;
            state.sealContract=3;SevenSeals.discover(player,0);player.getInventory().add(new ItemStack(Items.EMERALD,8));return true;
        }
        int index=selected-12;
        if(action==4&&state.officerContracts[index]==0){state.officerContracts[index]=1;return true;}
        if(action==6&&(state.officerContracts[index]==1||state.officerContracts[index]==2)){state.officerContracts[index]=0;return true;}
        if(action!=5||state.officerContracts[index]!=2)return false;
        if(index==1&&(!Ships.pay(player,Items.IRON_INGOT,8,false)||!Ships.pay(player,Items.OAK_PLANKS,16,false)))return false;
        if(index==1){Ships.pay(player,Items.IRON_INGOT,8,true);Ships.pay(player,Items.OAK_PLANKS,16,true);}
        state.officerContracts[index]=3;SeaLegends.renown(player,5);Ships.message(player,NAMES[index]+" is ready to join. Use the Crew tab aboard your docked harbor ship.");return true;
    }
    static boolean inspect(ServerPlayer player,IslandDiscoveries.Island island,BlockPos chest) {
        var state=Pirate.state(player);boolean changed=false;
        if(island.kind()==4&&chest.equals(island.center().offset(0,0,-6))&&!state.firstSealClue) {
            state.firstSealClue=true;changed=true;
            Ships.message(player,"Seven Seals clue discovered: the first binding answers the beacon. Read your Journal; Briggs offers a follow-up.");
        }
        int officer=island.kind()==2?2:island.kind()==5?1:-1;
        if(officer>=0&&state.officerContracts[officer]==1&&chest.equals(island.center().offset(officer==2?3:1,0,-4))) {
            state.officerContracts[officer]=2;changed=true;
            Ships.message(player,officer==2?"Nell's songbook recovered. Return to Briggs or the board.":"Bram's supply manifest recovered. Bring eight iron ingots and sixteen oak planks to the harbor.");
        }
        return changed;
    }
    static void observe(ServerPlayer player,IslandDiscoveries.Island island) {
        if(island.kind()!=3||player.getY()<76||Math.abs(player.getX()-island.center().getX())>5||Math.abs(player.getZ()-island.center().getZ())>6)return;
        var state=Pirate.state(player);
        if(state.sealContract==1){state.sealContract=2;Ships.message(player,"The beacon answers: seven lights, one prison. Return to Briggs for your research reward.");}
        if(state.officerContracts[0]==1){state.officerContracts[0]=2;Ships.message(player,"Mara's beacon bearings recorded. Return to Briggs or the board.");}
    }
    public static String sealJournal(PiratePlayerState state) {
        if(state.sealContract==3)return SevenSeals.journal(state);
        if(!state.firstSealClue)return "An undiscovered symbol waits in the island ruins. Explore Pirate Seas; no faction oath is required.";
        return "Tideglass rubbing: seven rings surround a closed eye. One ring bears a lantern. These are bindings, not keys to a treasure vault.\n\nOptional: accept The First Binding at Briggs or the harbor board, then climb Lastlight Beacon. The signal is recorded only after accepting. Return for eight emeralds, once.\n\n"+(state.sealContract>=2?"The beacon answered in seven pulses. Beneath the drawn eye, an older hand wrote: Keep the remaining lights asleep. Return to Briggs to record the Lantern seal and begin tracing the remaining six bindings.":"The lantern mark points toward a beacon. The other six bindings remain unexplained.");
    }
}
