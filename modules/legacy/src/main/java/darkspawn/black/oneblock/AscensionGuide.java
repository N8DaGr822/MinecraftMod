package darkspawn.black.oneblock;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Ascension Guide: Bounded server snapshots and single-use action tokens reuse the authoritative game rules. */
public final class AscensionGuide {
    private record Reply(long tick,String context,AscensionGuideSnapshot snapshot) { }
    private static final Map<ServerPlayer,Reply> replies=new WeakHashMap<>();
    private static final Map<ServerPlayer,String> feedback=new WeakHashMap<>();
    private AscensionGuide(){ }
    static void feedback(ServerPlayer p,String text){if(feedback.containsKey(p))feedback.put(p,text);}
    public static void initialize(){
        PayloadTypeRegistry.serverboundPlay().register(AscensionGuideRequest.TYPE,AscensionGuideRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AscensionGuideSnapshot.TYPE,AscensionGuideSnapshot.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AscensionGuideRequest.TYPE,(request,context)->{
            var p=context.player();if(p.level().getServer().getLevel(Ascension.LEVEL)==null)return;
            ServerPlayNetworking.send(p,handle(p,request));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{replies.clear();feedback.clear();});
    }
    private static String context(ServerPlayer p){
        var team=AscensionLedger.get(p.level()).member(p.getUUID());
        return p.level().dimension().toString()+"|"+(team==null?"none":team.id()+"|"+team.owner()+"|"+team.members()+"|"+(team.phases()==null?"legacy":team.phases().phase()));
    }
    static AscensionGuideSnapshot handle(ServerPlayer p,AscensionGuideRequest request){
        long now=p.level().getServer().overworld().getGameTime();var previous=replies.get(p);String context=context(p);
        // Replay/Stale Protection: A click must match an offered action in the same team, phase and dimension.
        if(previous!=null&&now>=previous.tick()&&now-previous.tick()<5)return previous.snapshot();
        String notice=request.action().isEmpty()?"":"That action expired or your team changed. Review the refreshed details.";
        if(!request.action().isEmpty()&&previous!=null&&now-previous.tick()<=200&&now>=previous.tick()
            &&previous.context().equals(context)&&previous.snapshot().token().equals(request.token())
            &&previous.snapshot().entries().stream().anyMatch(e->e.action().equals(request.action())
                &&(e.argument().equals(request.argument())||e.action().equals("create")||e.action().equals("invite")))){
            replies.remove(p);feedback.put(p,"");
            try{act(p,request.action(),request.argument());notice=feedback.get(p);}
            finally{feedback.remove(p);}
        }
        var fresh=snapshot(p);var snapshot=new AscensionGuideSnapshot(fresh.token(),notice,fresh.entries());
        replies.put(p,new Reply(now,context(p),snapshot));return snapshot;
    }
    private static void act(ServerPlayer p,String action,String argument){
        if(!p.isAlive()||p.isSpectator()||p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty()){
            feedback(p,"Close other containers and clear the cursor. A living, non-spectator player is required.");return;
        }
        switch(action){
            case "create"->Ascension.create(p,argument);
            case "invite"->{var target=p.level().getServer().getPlayerList().getPlayerByName(argument);if(target!=null)Ascension.invite(p,target);else feedback(p,"That player must be online.");}
            case "accept"->Ascension.accept(p);
            case "home"->Ascension.home(p);
            case "return"->Ascension.request(p,null);
            case "next"->Ascension.advance(p);
            case "submit"->AscensionObjectives.submit(p,argument);
            case "claim"->AscensionObjectives.claim(p,argument);
            case "guardian"->AscensionGuardian.start(p);
            case "upgrade"->AscensionAutomation.buy(p,argument);
            case "toggle"->AscensionAutomation.toggle(p,argument.equals("on"));
            case "leave"->Ascension.leave(p);
            case "archive"->Ascension.archive(p);
            case "remove","transfer"->{try{Ascension.manage(p,UUID.fromString(argument),action.equals("transfer"));}catch(IllegalArgumentException ignored){ }}
            default->{ }
        }
    }
    static AscensionGuideSnapshot snapshot(ServerPlayer p){
        var entries=new ArrayList<AscensionGuideSnapshot.Entry>();var team=AscensionLedger.get(p.level()).member(p.getUUID());
        entries.add(entry("Overview","How Ascension works","Build a team island with its own inventory. Mine the core, submit held items, defeat required Guardians and claim team rewards. Rewards belong to the team, once each. Legacy OneBlock is separate.\nKeep crafting and cursor slots clear before travel.","","",false));
        if(team==null){
            entries.add(entry("Team","Create team","Enter a team name below (1-32 characters). Create while outside both island modes.","create","",false));
            entries.add(entry("Team","Accept invitation","The owner must invite you first. Invitations last 60 seconds. Accept outside both island modes.","accept","",false));
        }else{
            entries.add(entry("Overview","Home: "+team.name(),"Travel to your island. Remain still for three seconds. Your outside possessions are saved separately.","home","",false));
            if(Ascension.island(p.level()))entries.add(entry("Overview","Return outside","Restore your outside location and possessions. Island items remain saved in Ascension.","return","",false));
            if(team.phases()!=null){
                var state=team.phases();var phase=AscensionPhases.get(p.level()).phase(state.phase());
                boolean terminal=phase.id().equals(AscensionPhases.IDS.getLast());
                String next=state.completed()?(terminal?"Ascension complete. Keep building and mining.":"Previous chapter complete: Continue to the next phase without repeating rewards."):
                    team.progress().breaks<phase.breaks()?"Mine the core and complete the submissions below.":
                    !state.prepared(phase,team.progress())?"Hold each requested item and submit it from Objectives.":
                    !state.ready(phase,team.progress())?"Prepare a safe arena near the core, then start the Guardian.":"All requirements met. Complete the phase and claim rewards.";
                entries.add(entry("Overview",phase.name()+" — "+Math.min(team.progress().breaks,phase.breaks())+"/"+phase.breaks(),next+"\nTotal confirmed breaks: "+team.progress().total+"\nGenerator: "+team.generator().state()+"\nHome: "+team.center().toShortString(),"","",false));
                for(var goal:phase.objectives())entries.add(entry("Objectives",goal.id()+": "+state.submitted(goal.id())+"/"+goal.count(),
                    "Hold "+goal.item()+" in your main hand, then submit. Only the remaining amount is consumed. Credit is shared by the team. Craft these supplies; this objective does not inspect placed buildings.",
                    !state.completed()&&state.submitted(goal.id())<goal.count()?"submit":"",goal.id(),false));
                if(!phase.encounters().isEmpty())entries.add(entry("Objectives","Guardian: "+(state.encountered(phase.encounters().getFirst())?"defeated":"required"),
                    "Finish breaks and submissions first. Start within 16 blocks of the core. Build a roomy arena; stay within 32 blocks during combat. You have five minutes. Warning particles mark the attack before damage; no terrain is destroyed.\n"+AscensionGuardian.hint(team),
                    !state.encountered(phase.encounters().getFirst())?"guardian":"","",true));
                entries.add(entry("Objectives",state.completed()&&!terminal?"Continue chapter":"Complete phase",
                    "Requires all mining, submission and encounter goals. Earned rewards remain available under Rewards. Mining always remains available.",
                    !state.completed()||!terminal?"next":"","",true));
                for(var grant:state.rewards())entries.add(entry("Rewards",grant.reward().id()+(grant.claimed()?" — claimed":""),
                    grant.reward().count()+" x "+grant.reward().item()+"\nOne team reward, not one per member. Leave one empty inventory slot. Claimed rewards cannot be repeated.",
                    grant.claimed()?"":"claim",grant.reward().id(),false));
                var level=p.level().getServer().getLevel(Ascension.LEVEL);var f=state.features();
                entries.add(entry("Automation","Live status",AscensionAutomation.status(level,team),"","",false));
                if(team.owner().equals(p.getUUID())){
                    for(String upgrade:List.of("collector","miner","relay","drive")){
                        boolean installed=switch(upgrade){case "collector"->f.collector;case "miner"->f.miner;case "relay"->f.relay;default->f.drive;};
                        String cost=switch(upgrade){case "collector"->"16 iron ingots. Complete Genesis.";case "miner"->"32 iron ingots. Collector and Cave Guardian victory required.";case "relay"->"32 copper ingots. Complete Forgeworks; requires collector. Attendance expands to the entire claim, with chunks still required to be loaded.";default->"32 redstone dust. Complete Stormbound; requires miner. Mining interval becomes three seconds.";};
                        entries.add(entry("Automation",upgrade+(installed?" — installed":""),"Hold the payment in your main hand.\n"+cost+"\nOwner only, while safely on your island.",installed?"":"upgrade",upgrade,true));
                    }
                    entries.add(entry("Automation",f.enabled?"Pause automation":"Enable automation","Owner only. No offline mining, catch-up harvests or chunk loading.","toggle",f.enabled?"off":"on",false));
                }
            }else entries.add(entry("Overview","Legacy team","This team retains its original six phases. It is not automatically converted to the chapter system.","","",false));
            entries.add(entry("Team","Members: "+team.members().size()+"/6","Owner: "+memberName(p,team.owner())+"\nManagement requires returning outside both island modes. Removing members never deletes their saved inventories.","","",false));
            if(team.owner().equals(p.getUUID())){
                entries.add(entry("Team","Invite player","Enter the online player's exact name. They must accept the invitation.","invite","",false));
                for(var member:team.members())if(!member.equals(p.getUUID())){
                    String name=memberName(p,member);
                    entries.add(entry("Team","Transfer to "+name,"You lose owner controls; this member becomes owner. Online target must be outside both island modes.","transfer",member.toString(),true));
                    entries.add(entry("Team","Remove "+name,"Revokes this member's access. Online target must return outside first. Offline target retains possessions and recovers on reconnect.","remove",member.toString(),true));
                }
                entries.add(entry("Team","Archive island","Freezes the island and cleans up only its neutral bridge blocks as chunks load. It does not delete the build. Online members must return outside first; offline members recover on reconnect.","archive","",true));
            }else entries.add(entry("Team","Leave team","Return outside both island modes first. The team's builds and supplies remain with the team.","leave","",true));
        }
        return new AscensionGuideSnapshot(UUID.randomUUID().toString(),"",List.copyOf(entries));
    }
    private static String memberName(ServerPlayer p,UUID id){var member=p.level().getServer().getPlayerList().getPlayer(id);return member==null?id.toString():member.getName().getString();}
    private static AscensionGuideSnapshot.Entry entry(String section,String title,String body,String action,String argument,boolean confirm){return new AscensionGuideSnapshot.Entry(section,title,body,action,argument,confirm);}
}
