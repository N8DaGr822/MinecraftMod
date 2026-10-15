package darkspawn.black.pirate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/** Small, atomic harbor transactions and the shared shore-service action vocabulary. */
public final class SeaServices {
    public static final String[] NAMES={"Tavern provisions","Repair timber","Cannon supplies","Repair vessel","Tavern rumors","Ghost voyage","Clear your name","Rescue Mara","Rescue Bram","Rescue Nell","Rescue Garrick","Capture this fort","Fortify this fort","Collect fort supplies","Attune the Fifth Seal","Pirate King","Liberator","Eternal Guardian"};
    public static final String[] DETAILS={
        "Buy eight cooked cod for one emerald. The Rusty Anchor keeps sailors fed; leave an empty inventory slot.",
        "Buy 32 oak planks for two emeralds, or one emerald at 50 renown. Timber is used for construction and repairs.",
        "Buy sixteen solid shot for two emeralds, or one emerald at 50 renown. Leave an empty inventory slot.",
        "Repair your nearby docked ship: ten oak planks and two emeralds restore 50 hull and extinguish its fire. Available at harbor or your own fort.",
        "Hear the current island and Seven Seals leads. Use the Journal to keep these clues. Rumors never force travel or skip compass study.",
        "Accept an optional ghost voyage. Sail your own ship outside harbor; the next eligible patrol attempt seeks a spectral vessel. Clear loaded water and encounter caps still apply.",
        "Pay eight gold ingots to reduce infamy by ten. Renown and faction standing are unchanged. Infamy of 30 or more can draw bounty hunters at sea.",
        "Pay eight emeralds to rescue Mara after her ship sank. Her commission remains completed; hire her again free from a docked harbor ship's Crew tab.",
        "Pay eight emeralds to rescue Bram after his ship sank. His commission remains completed; hire him again free at harbor.",
        "Pay eight emeralds to rescue Nell after her ship sank. Her commission remains completed; hire her again free at harbor.",
        "Pay eight emeralds to rescue Garrick after his ship sank. Then recruit him through his harbor contract with a free cannoneer berth. Ordinary crew must be rehired.",
        "At Saltwatch Fort, defeat the responding garrison and hold the fort for sixty uncontested seconds. Stay within 24 blocks. Rival captains contest the claim. Offline owners can lose control. No blocks or cargo are replaced.",
        "An owned fort accepts eight iron ingots and 24 oak planks per reinforcement, up to two. Each level adds two defenders against the next claimant. Repairs and fort supplies are owner-only.",
        "An owned fort provides four emeralds once every twenty minutes. Supplies do not accumulate while away; one empty inventory slot is required. Capturing resets the timer.",
        "At Tideglass Shrine, offer eight prismarine shards after learning the First Binding. This records the Fifth Seal, Mercy, once. Other shrine contracts have separate offerings.",
        "With all seven seals, return to Tideglass Shrine and claim the captain's crown. Gain a named golden crown, 24 emeralds and 40 infamy. This final personal choice does not change anyone else's story.",
        "With all seven seals, confront the Bound Admiral at Tideglass Shrine. Defeat your summoned adversary, then return here to confirm liberation and claim a named trident. Failed encounters can be retried after ten minutes. Helpers may assist, but only your encounter advances your story. Summoning does not lock your ending; the final claim does.",
        "With all seven seals, restore the prison at Tideglass Shrine. Gain the Warden's Vow, sixteen emeralds, 30 renown and lose 20 infamy. This final personal choice leaves the sea open to everyone."};
    public enum Reply {
        NONE("Choose a service and read its cost before ordering."),DONE("Completed. Your progress has been saved in this world."),HARBOR("Return to Briggs or the harbor board for this service."),SPACE("Leave an empty inventory slot. Nothing was charged."),COST("You do not have the listed payment. Nothing was charged."),SHIP("Bring your own docked ship within 48 blocks."),HEALTHY("Your vessel is already fully repaired and not burning."),NO_RESCUE("No sinking rescue is owed for this officer. Active, unearned or already rescued officers cannot be duplicated."),GHOST_ACTIVE("Your ghost voyage is already active. Sail outside harbor with clear loaded ocean nearby."),NO_INFAMY("You have no infamy to clear."),FORT("Stand inside a naturally generated Saltwatch Fort. Sneak-use your Logbound Compass there to open services."),OWNER("Only this fort's current owner may use this service."),CONTESTED("A capture is already in progress. Rival captains inside the fort pause its timer."),CAPTURE("Garrison challenge begun. Defeat the defenders and hold the fort for sixty uncontested seconds. Leaving abandons the attempt."),MAX_DEFENSE("This fort already has both reinforcement levels."),SUPPLY_WAIT("Fort supplies are not ready. They refresh twenty minutes after collection or capture; no offline stockpile accumulates."),SHRINE("Stand inside Tideglass Shrine. Sneak-use your Logbound Compass there to open the seal ritual."),FIRST("Complete and claim The First Binding before seeking later seals."),RECORDED("That seal is already recorded. No further offering is needed."),SEALS("Discover all seven seals first. Your Journal lists the missing clues."),ENDED("You already chose an ending. Your Journal keeps its outcome; exploration and all ordinary services remain open."),CONFIRM("This is a final personal ending choice. Press Confirm choice to proceed, or select another service to cancel."),BATTLE("The Bound Admiral has answered. Defeat your adversary and return to claim liberation. Its attacks are limited to your encounter."),BATTLE_ACTIVE("Your adversary is still active. Return to it, or retry after the ten-minute encounter expires."),BLOCKED("No safe loaded space is available for this encounter. Clear the marked area and retry; no blocks are removed."),PEACEFUL("This combat challenge requires a non-Peaceful difficulty and mob spawning enabled."),ENDING_SPACE("Leave two empty inventory slots for the trophy and emeralds. Your ending remains unclaimed."),ALREADY_OWNER("You already own this fort. Choose fortification or supplies instead."),BUSY("The shared encounter limit is reached. Finish an existing challenge and try again.");
        public final String text;Reply(String text){this.text=text;}
    }
    private SeaServices() { }
    private static Reply trade(ServerPlayer p,Item item,int count,int price){
        if(p.getInventory().getFreeSlot()<0)return Reply.SPACE;
        if(!Ships.pay(p,Items.EMERALD,price,false))return Reply.COST;
        Ships.pay(p,Items.EMERALD,price,true);p.getInventory().add(new ItemStack(item,count));return Reply.DONE;
    }
    public static Reply execute(ServerPlayer p,int action){
        if(!p.isAlive()||p.isSpectator()||!p.level().dimension().equals(Pirate.LEVEL))return Reply.HARBOR;
        var state=Pirate.state(p);var ledger=SeaLedger.get(p.level());
        if(action>=11&&action<=13)return SeaTerritory.order(p,action);
        if(action>=14&&action<=17)return SevenSeals.order(p,action);
        boolean harbor=HarborGuide.inHarbor(p);
        if(action==3){
            if(!harbor&&!SeaTerritory.ownsHere(p))return Reply.OWNER;
            var ship=Ships.nearby(p,p.blockPosition());if(ship==null||!ship.docked()||p.distanceToSqr(ship)>48*48)return Reply.SHIP;
            if(ship.hull()>=ship.maxHull()&&!ship.burning())return Reply.HEALTHY;
            return ship.harborRepair(p)?Reply.DONE:Reply.COST;
        }
        if(!harbor)return Reply.HARBOR;
        if(action==0)return trade(p,Items.COOKED_COD,8,1);
        if(action==1)return trade(p,Items.OAK_PLANKS,32,state.renown>=50?1:2);
        if(action==2)return trade(p,Ships.SHOT,16,state.renown>=50?1:2);
        if(action==4){p.sendSystemMessage(net.minecraft.network.chat.Component.literal(SevenSeals.rumor(state)));return Reply.DONE;}
        if(action==5){if(state.ghostVoyage)return Reply.GHOST_ACTIVE;state.ghostVoyage=true;return Reply.DONE;}
        if(action==6){if(state.infamy==0)return Reply.NO_INFAMY;if(!Ships.pay(p,Items.GOLD_INGOT,8,false))return Reply.COST;Ships.pay(p,Items.GOLD_INGOT,8,true);state.infamy=Math.max(0,state.infamy-10);return Reply.DONE;}
        if(action>=7&&action<=10){
            int index=action-7,bit=1<<index;
            if((ledger.lost(p.getUUID())&bit)==0)return Reply.NO_RESCUE;
            if(!Ships.pay(p,Items.EMERALD,8,false))return Reply.COST;
            Ships.pay(p,Items.EMERALD,8,true);ledger.recover(p.getUUID(),bit);
            if(index<3)state.officerContracts[index]=3;else state.garrickRescue=3;
            return Reply.DONE;
        }
        return Reply.BLOCKED;
    }
}
