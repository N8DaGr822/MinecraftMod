package darkspawn.black.oneblock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Objective Commands: Consume only the explicitly held item and deliver team rewards without spilling into the world. */
public final class AscensionObjectives {
    private AscensionObjectives(){ }
    private static int message(ServerPlayer p,String text){AscensionGuide.feedback(p,text);p.sendSystemMessage(Component.literal(text));return 1;}
    private static AscensionLedger.Team editable(ServerPlayer p){
        if(!Ascension.island(p.level())||!p.isAlive()||p.isSpectator()||p.preventsBlockDrops()||p.containerMenu!=p.inventoryMenu
                ||!p.inventoryMenu.getCarried().isEmpty()||OneBlock.state(p).pendingTicks>0)return null;
        var team=AscensionLedger.get(p.level()).member(p.getUUID());return team!=null&&team.phases()!=null&&team.contains(p.blockPosition())?team:null;
    }
    public static int submit(ServerPlayer p,String id){
        var team=editable(p);if(team==null)return message(p,"Stand on your MVP team's island in survival, close other containers and finish travel first.");
        var held=p.getMainHandItem();var definition=AscensionPhases.get(p.level()).phase(team.phases().phase());
        int amount=team.phases().submit(definition,id,BuiltInRegistries.ITEM.getKey(held.getItem()).toString(),held.getCount());
        if(amount==0)return message(p,"No items submitted: check the objective ID, held item and remaining amount with /ascension objectives.");
        held.shrink(amount);AscensionLedger.get(p.level()).setDirty();p.inventoryMenu.broadcastChanges();
        return message(p,"Submitted "+amount+" toward "+id+" for the team. Use /ascension next when all gates are complete.");
    }
    public static int advance(ServerPlayer p,AscensionLedger.Team team){
        if(editable(p)!=team)return message(p,"Phase completion requires survival on your own island with no pending travel or open container.");
        var phase=AscensionPhases.get(p.level()).phase(team.phases().phase());
        if(!team.phases().advance(phase,team.progress())){info(p);return message(p,"Complete the break threshold, submissions and encounters before advancing.");}
        AscensionLedger.get(p.level()).setDirty();
        return message(p,(team.phases().completed()?"Chapter progression complete; mining remains available.":"Team phase advanced to "+AscensionPhases.get(p.level()).phase(team.phases().phase()).name()+".")+" Earned rewards: /ascension objectives, then /ascension claim <reward>.");
    }
    public static int claim(ServerPlayer p,String id){
        var team=editable(p);if(team==null)return message(p,"Claim rewards in survival on your own MVP island after closing other containers and finishing travel.");
        var grant=team.phases().unclaimed(id);if(grant==null)return message(p,"That team reward is unavailable or already claimed.");
        var reward=grant.reward().stack();if(reward.isEmpty()||reward.getCount()>reward.getMaxStackSize())return message(p,"Saved reward item is unavailable; the entitlement has been preserved.");
        int slot=p.getInventory().getFreeSlot();if(slot<0)return message(p,"Leave one empty inventory slot; your team reward remains unclaimed.");
        // Claim Authority: Record once before delivery; repeated commands by any teammate cannot duplicate this reward.
        if(!team.phases().claim(id))return 0;
        AscensionLedger.get(p.level()).setDirty();p.getInventory().setItem(slot,reward);p.inventoryMenu.broadcastChanges();
        return message(p,"Claimed team reward "+id+". It cannot be claimed again by another member.");
    }
    public static int info(ServerPlayer p){
        var team=AscensionLedger.get(p.level()).member(p.getUUID());if(team==null)return message(p,"Create or join a team first.");
        if(team.phases()==null)return message(p,"This existing team retains its six resource phases; MVP objectives apply to newly created teams.");
        var catalog=AscensionPhases.get(p.level());var phase=catalog.phase(team.phases().phase());
        message(p,phase.name()+" | "+team.progress().breaks+"/"+phase.breaks()+" breaks"+(team.phases().completed()?" | Completed":""));
        for(var objective:phase.objectives())message(p,objective.id()+": "+team.phases().submitted(objective.id())+"/"+objective.count()+" "+objective.item()+". Hold the item and use /ascension submit "+objective.id());
        for(var encounter:phase.encounters())message(p,"Encounter "+encounter+": "+(team.phases().encountered(encounter)?"complete":"required; finish submissions/breaks, then /ascension guardian start"));
        for(var grant:team.phases().rewards())if(!grant.claimed())message(p,"Reward "+grant.reward().id()+": "+grant.reward().count()+" "+grant.reward().item()+". /ascension claim "+grant.reward().id());
        if(!catalog.lastError().isEmpty())message(p,"Latest phase reload was rejected. The last accepted phase rules remain active; see the server log.");return 1;
    }
}
