package darkspawn.black.pirate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/** Shared, bounded feedback IDs; the server chooses the reason, clients only display it. */
public enum HarborFeedback {
    NONE(""), CURSOR("Put the item on your cursor back into your inventory first."),
    ACCEPT("Accept this contract before claiming its reward."), ACTIVE("This contract is already active. Follow its objective in the contract description."),
    CLAIMED("You have already completed this one-time reward. Officer hiring is on your ship's Crew tab."),
    OBJECTIVE("The objective is still unfinished. Read the selected contract for the required location or item."),
    SPACE("Leave one empty inventory slot for the reward, then claim again. Your completed objective is preserved."),
    COOLDOWN("This repeat voyage is cooling down. The remaining time appears below."),
    CLUE("First inspect the relic chest at Tideglass Shrine to discover the Seven Seals symbol."),
    SUPPLIES("Bram needs eight iron ingots AND sixteen oak planks in your inventory. Nothing was charged."),
    SHIP("Bring your own docked ship within 48 blocks of this harbor service."),
    BERTH("Garrick needs a free cannoneer berth. Expand your ship or use another owned ship."),
    NO_ACTIVE("There is no active contract to abandon."),
    COMPASS_OWNED("You already carry a Logbound Compass. Use it; its route follows your saved progress."),
    COMPASS_COST("A replacement costs one ordinary compass. Your first Logbound Compass is free."),
    FACTION("Select a faction introduction or repeat voyage before joining or making amends."),
    MEMBERSHIP("Leave your current faction before joining another. Leaving costs 25 standing."),
    TRUST("Joining requires Trusted standing: at least 25 with the selected faction."),
    NO_MEMBERSHIP("You are already independent; there is no membership to leave."),
    GOLD("Amends cost eight gold ingots for five standing, up to Trusted (25)."),
    TRUSTED("This faction already considers you Trusted. Amends cannot raise standing above 25."),
    LOCATION("Return to Briggs or the harbor board for this personal contract."),
    RETRY("That order could not complete. Check your location and available crew berths, then try again.");
    public final String text;
    HarborFeedback(String text){this.text=text;}
    public static HarborFeedback contract(ServerPlayer player,int selected,int action) {
        var s=Pirate.state(player);
        if((selected<4||selected>=10)&&!HarborGuide.inHarbor(player))return LOCATION;
        int progress=selected==0?s.harborContract:selected<4?s.islandContracts[selected-1]:selected<7?s.factionContracts[selected-4]:selected<10?s.repeatContracts[selected-7]:selected==10?s.garrickRescue:selected==11?s.sealContract:s.officerContracts[selected-12];
        boolean claimed=selected<7?progress==2:selected<10?false:selected==10?progress>=4:progress>=3;
        boolean ready=selected==0?s.harborSurveyed:selected<4?(s.islandObjectives&(1<<(selected-1)))!=0:selected<7?(s.factionObjectives&(1<<(selected-4)))!=0:selected==10?progress==3:progress==2;
        if(claimed)return CLAIMED;
        if(action==4){
            if(progress!=0)return ACTIVE;
            if(selected>=7&&selected<10&&PirateActivities.cooldown(player,selected-7)>0)return COOLDOWN;
            if(selected==11&&!s.firstSealClue)return CLUE;
        } else if(action==6){if(progress==0)return NO_ACTIVE;}
        else if(action==5){
            if(progress==0)return ACCEPT;
            if(!ready)return OBJECTIVE;
            if(selected==13&&(!Ships.pay(player,Items.IRON_INGOT,8,false)||!Ships.pay(player,Items.OAK_PLANKS,16,false)))return SUPPLIES;
            if(selected==10){var ship=Ships.nearby(player,player.blockPosition());if(ship==null||!ship.docked()||!ship.protectedHarbor()||player.distanceToSqr(ship)>48*48)return SHIP;if(ship.crew(PirateShip.GUNNER)>=ship.gunLimit())return BERTH;}
            if((selected<4||selected>=7&&selected<=9||selected==11)&&player.getInventory().getFreeSlot()<0)return SPACE;
        }
        return NONE;
    }
}
