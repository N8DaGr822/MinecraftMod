package darkspawn.black.pirate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/** Personal faction relationships and captain milestones; no membership is required to sail or hire. */
public final class PirateProgression {
    public enum Faction {
        CORSAIRS("corsairs", "Piglin Corsairs", 5, PirateShip.MATE, 0),
        FLEET("iron_fleet", "Illager Iron Fleet", 1, PirateShip.GUNNER, 0),
        DROWNED("drowned", "The Drowned", 4, PirateShip.WORKER, -25);
        public final String id, title;
        public final int island, role, initial;
        Faction(String id,String title,int island,int role,int initial) {this.id=id;this.title=title;this.island=island;this.role=role;this.initial=initial;}
    }
    private PirateProgression() { }
    public static int standing(PiratePlayerState state,Faction faction) {return state.factionStanding[faction.ordinal()];}
    public static void addStanding(PiratePlayerState state,Faction faction,int amount) {
        state.factionStanding[faction.ordinal()]=(int)Math.clamp((long)standing(state,faction)+amount,-100L,100L);
    }
    public static String relationship(int value) {return value<-49?"Hunted":value<0?"Hostile":value<25?"Neutral":value<60?"Trusted":value<90?"Honored":"Legendary";}
    public static int completedContracts(PiratePlayerState state) {
        int count=state.harborContract==2?1:0;
        for(int status:state.islandContracts)if(status==2)count++;
        for(int status:state.factionContracts)if(status==2)count++;
        if(state.sealContract==3)count++;for(int status:state.officerContracts)if(status>=3)count++;
        return count;
    }
    public static int captainTier(PiratePlayerState state) {
        // Captain Milestones: Preserve the old five-win unlock and offer a fully independent exploration path.
        long discoveries=state.logboundVisited.stream().distinct().count();int contracts=completedContracts(state);
        if(state.navalVictories>=12||(discoveries>=8&&contracts>=4))return 2;
        if(state.navalVictories>=5||(discoveries>=4&&contracts>=2))return 1;
        return 0;
    }
    public static boolean crewOffer(PiratePlayerState state,int role) {
        for(var faction:Faction.values())if(faction.role==role&&state.factionContracts[faction.ordinal()]==2&&standing(state,faction)>=25)return true;
        return false;
    }
    public static int crewCost(PiratePlayerState state,int role) {
        int normal=role==PirateShip.GUNNER?8:12;
        var member=PirateDiplomacy.faction(state.factionMembership);
        return normal-(crewOffer(state,role)?(member!=null&&member.role==role?6:4):captainTier(state)>=1?2:0);
    }
    public static String crewName(PiratePlayerState state,int role) {
        if(crewOffer(state,role))return switch(role){case PirateShip.MATE->"Corsair First Mate";case PirateShip.GUNNER->"Fleet Cannoneer";default->"Tidebound Shipworker";};
        return new String[]{"First Mate","Shipworker","Cannoneer","Lookout"}[role];
    }
    public static boolean contract(ServerPlayer player,int index,int action) {
        if(index<0||index>=Faction.values().length||!PirateDiplomacy.canParley(player,Faction.values()[index]))return false;
        var state=Pirate.state(player);int bit=1<<index;
        if(action==4&&state.factionContracts[index]==0){state.factionContracts[index]=1;state.factionObjectives&=~bit;return true;}
        if(action==6&&state.factionContracts[index]==1){state.factionContracts[index]=0;state.factionObjectives&=~bit;return true;}
        if(action!=5||state.factionContracts[index]!=1||(state.factionObjectives&bit)==0)return false;
        // One-Time Commendation: Repeated claims cannot farm standing or captain milestones.
        state.factionContracts[index]=2;addStanding(state,Faction.values()[index],index==Faction.DROWNED.ordinal()?50:25);
        Ships.message(player,"Commendation earned. Crew offers require Trusted standing; check your Journal for progress.");return true;
    }
    static boolean inspect(ServerPlayer player,IslandDiscoveries.Island island,BlockPos pos) {
        var state=Pirate.state(player);
        for(var faction:Faction.values()) {
            int index=faction.ordinal(),bit=1<<index;
            if(island.kind()!=faction.island||state.factionContracts[index]!=1||(state.factionObjectives&bit)!=0)continue;
            BlockPos cache=island.center().offset(faction==Faction.CORSAIRS?1:faction==Faction.FLEET?-1:0,0,faction==Faction.FLEET?-5:faction==Faction.CORSAIRS?-4:-6);
            if(!pos.equals(cache))return false;
            if(faction==Faction.DROWNED&&!Ships.pay(player,Items.PRISMARINE_SHARD,4,false)){Ships.message(player,"Bring four prismarine shards as an offering; inspect the shrine cache again.");return false;}
            if(faction==Faction.DROWNED)Ships.pay(player,Items.PRISMARINE_SHARD,4,true);
            state.factionObjectives|=bit;Ships.message(player,"Faction objective complete. Return to Briggs, the board or your faction contact for your commendation.");return true;
        }
        return false;
    }
    public static String journal(PiratePlayerState state) {
        var text=new StringBuilder("Independent captaincy: "+new String[]{"Sailor","Captain","Veteran Captain"}[captainTier(state)]+"\nGuided discoveries: "+state.logboundVisited.stream().distinct().count()+" | Claimed contracts: "+completedContracts(state)+" | Naval victories: "+state.navalVictories);
        for(var faction:Faction.values())text.append("\n").append(faction.title).append(": ").append(standing(state,faction)).append(" (").append(relationship(standing(state,faction))).append(")");
        text.append("\n\n11x23 / 1,024 blocks: five NPC victories OR four discoveries and two claimed contracts. Costs 148 oak planks.\n15x31 / 1,536 blocks: twelve NPC victories OR eight discoveries and four claimed contracts. Costs 212 more oak planks. Heights: 6 / 8 / 10; guns: 4 / 8 / 12. Expand docked in clear water.\n\nCaptain milestone discounts all ordinary crew by two emeralds. A claimed faction contract and Trusted standing discount that faction's role by four instead: Corsair first mate, Fleet cannoneer, Tidebound shipworker. Offers recruit a Piglin first mate, Pillager cannoneer or sun-safe Husk shipworker. Membership discounts that matching offer by two more emeralds. Crew supplies and abilities remain unchanged; hired sailors stay loyal to their original captain through leaving and reloads.\n\nAll contracts are optional and require no membership. The four independent contracts alone qualify for both ship upgrades. Tagged outpost guards respect neutral or positive standing; negative standing is hostile. Ordinary untagged mobs remain unchanged. Return travel and existing crew are never gated.");
        var member=PirateDiplomacy.faction(state.factionMembership);
        text.append("\n\nMembership: ").append(member==null?"Independent":member.title).append(". Join one faction free at Trusted (25+). Leave costs 25 standing with that faction; ships and crew stay yours. Make amends costs 8 gold for +5 standing, capped at Trusted. Use Briggs, the board, or a faction contact. Attacking a tagged guard costs 10 standing per second of aggression; harbor protection blocks such attacks there.");
        text.append("\n\nRepeat voyages: Cargo Recovery (Pale Gull cache), Outlaw Patrol Hunt (sink a white-sailed outlaw), Shrine Expedition (four shards at Tideglass). Accept first; each pays +10 matching standing and six emeralds, with ten minutes between completions. Repeat rewards do not count as new captain milestone contracts. Status (Corsairs/Fleet/Drowned; 0 available, 1 active, 2 ready): ").append(java.util.Arrays.toString(state.repeatContracts));
        text.append("\n\nGarrick personal rescue: ").append(new String[]{"Not accepted","Find release papers at Saltwatch","Speak to your captive at the same fort","Rescued: recruit or free at harbor","Freed without hiring","Recruited as cannoneer"}[state.garrickRescue]);
        if(state.garrickPrison!=null)text.append(". Fort: ").append(state.garrickPrison.getX()).append(", ").append(state.garrickPrison.getZ());
        text.append("\n\nNaval patrols: red Corsairs, blue Fleet, cyan Drowned; white sails mark outlaw raiders. Faction patrols respect their target captain's standing. Attacking one loses standing and provokes that patrol for one minute. Harbor protection remains in force.");
        text.append("\n\nShip refit: owner-only names up to 32 characters; independent or membership flags are cosmetic. Captain unlocks 36 cargo slots for 16 iron and four emeralds; Veteran unlocks 54 for 32 iron and eight emeralds. Upgrade docked at harbor; Cargo Page selects accessible hold slots.");
        text.append("\n\nOfficers: Mara Tidewise reports your plotted compass route while sailing. Bram Ledger transfers your planks/ammunition into the hold on a docked Resupply order. Nell Saltvoice sings four recorded shanties in rotation when Shanty is enabled (Records volume). Each requires a separate optional harbor commission, then free recruitment in Refit / Crew. Commission states: 0 unaccepted, 1 active, 2 objective ready, 3 hire available, 4 hired: ").append(java.util.Arrays.toString(state.officerContracts));
        return text.toString();
    }
}
