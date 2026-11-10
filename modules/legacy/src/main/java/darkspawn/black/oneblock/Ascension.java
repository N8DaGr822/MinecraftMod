package darkspawn.black.oneblock;

import com.mojang.brigadier.arguments.StringArgumentType;
import darkspawn.black.Darkspawn;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Team Islands: Opt-in possessions and claims with preserved legacy teams or MVP objective progression. */
public final class Ascension {
    public static final ResourceKey<Level> LEVEL=ResourceKey.create(Registries.DIMENSION,Darkspawn.id("ascension"));
    private record Invitation(UUID team,long expires) { }
    private static final Map<UUID,Invitation> invitations=new HashMap<>();
    private Ascension() { }
    public static boolean island(Level level){return level.dimension().equals(LEVEL);}
    public static boolean corePosition(BlockPos pos){return pos.getY()==80&&pos.getX()%AscensionLedger.SPACING==0&&pos.getZ()%AscensionLedger.SPACING==0;}
    private static int message(ServerPlayer p,String text){AscensionGuide.feedback(p,text);p.sendSystemMessage(Component.literal(text));return 1;}
    public static void initialize(){
        AscensionPhases.initialize();
        AscensionGuardian.initialize();
        AscensionGuide.initialize();
        ServerLifecycleEvents.SERVER_STOPPED.register(server->invitations.clear());
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var p=handler.player;
            if(OneBlock.state(p).ascensionInside!=island(p.level()))switchItems(p);
        });
        PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity)->{
            if(!island(level)||!(level instanceof ServerLevel server))return true;
            if(!AscensionProtection.canEdit(player,pos))return false;
            var team=AscensionLedger.get(server).at(pos);
            return !corePosition(pos)||team!=null&&team.center().equals(pos)&&team.generator().owns(player.getUUID());
        });
        PlayerBlockBreakEvents.AFTER.register((level,player,pos,state,entity)->{
            if(island(level)&&player instanceof ServerPlayer p){
                if(state.is(OneBlock.CORE))mined(p,pos);
                AscensionBridges.get(p.level()).remove(pos);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%20!=0)return;
            invitations.values().removeIf(inv->inv.expires()<server.overworld().getGameTime());
            var level=server.getLevel(LEVEL);if(level==null)return;
            // Membership Recovery: Offline removals never discard the inactive inventory or strand a returning player.
            if(AscensionLedger.get(level).compatible())for(var p:List.copyOf(level.players()))
                if(AscensionLedger.get(level).member(p.getUUID())==null)recover(p);
            maintain(level);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(Commands.literal("ascension")
            .requires(source->source.getEntity() instanceof ServerPlayer&&source.getServer().getLevel(LEVEL)!=null)
            .executes(c->info(c.getSource().getPlayer()))
            .then(Commands.literal("create").then(Commands.argument("name",StringArgumentType.greedyString()).executes(c->create(c.getSource().getPlayer(),StringArgumentType.getString(c,"name")))))
            .then(Commands.literal("invite").then(Commands.argument("player",EntityArgument.player()).executes(c->invite(c.getSource().getPlayer(),EntityArgument.getPlayer(c,"player")))))
            .then(Commands.literal("accept").executes(c->accept(c.getSource().getPlayer())))
            .then(Commands.literal("archive").executes(c->message(c.getSource().getPlayer(),"Archive freezes this island and removes only this team's neutral bridge blocks as chunks load. Online members must return outside OneBlock; offline members recover on reconnect. Use /ascension archive confirm."))
                .then(Commands.literal("confirm").executes(c->archive(c.getSource().getPlayer()))))
            .then(Commands.literal("transfer").then(Commands.argument("member",net.minecraft.commands.arguments.UuidArgument.uuid())
                .then(Commands.literal("confirm").executes(c->manage(c.getSource().getPlayer(),net.minecraft.commands.arguments.UuidArgument.getUuid(c,"member"),true)))))
            .then(Commands.literal("remove").then(Commands.argument("member",net.minecraft.commands.arguments.UuidArgument.uuid())
                .then(Commands.literal("confirm").executes(c->manage(c.getSource().getPlayer(),net.minecraft.commands.arguments.UuidArgument.getUuid(c,"member"),false)))))
            .then(Commands.literal("recover").executes(c->recover(c.getSource().getPlayer())?1:message(c.getSource().getPlayer(),"Recovery is for removed members inside Ascension. Close containers, clear crafting/trash slots and dismount first.")))
            .then(Commands.literal("leave").executes(c->leave(c.getSource().getPlayer())))
            .then(Commands.literal("home").executes(c->home(c.getSource().getPlayer())))
            .then(Commands.literal("visit").then(Commands.argument("player",EntityArgument.player()).executes(c->visit(c.getSource().getPlayer(),EntityArgument.getPlayer(c,"player")))))
            .then(Commands.literal("return").executes(c->{var p=c.getSource().getPlayer();if(!island(p.level()))return message(p,"You are not in Ascension.");request(p,null);return 1;}))
            .then(Commands.literal("guardian").executes(c->message(c.getSource().getPlayer(),"Complete this phase's submissions and breaks, then start its Guardian near your core. Caverns, Stormbound and Emberlands have encounters."))
                .then(Commands.literal("start").executes(c->AscensionGuardian.start(c.getSource().getPlayer()))))
            .then(Commands.literal("upgrade")
                .then(Commands.literal("collector").executes(c->AscensionAutomation.buy(c.getSource().getPlayer(),false)))
                .then(Commands.literal("miner").executes(c->AscensionAutomation.buy(c.getSource().getPlayer(),true)))
                .then(Commands.literal("relay").executes(c->AscensionAutomation.buy(c.getSource().getPlayer(),"relay")))
                .then(Commands.literal("drive").executes(c->AscensionAutomation.buy(c.getSource().getPlayer(),"drive"))))
            .then(Commands.literal("automation").executes(c->AscensionAutomation.info(c.getSource().getPlayer()))
                .then(Commands.literal("on").executes(c->AscensionAutomation.toggle(c.getSource().getPlayer(),true)))
                .then(Commands.literal("off").executes(c->AscensionAutomation.toggle(c.getSource().getPlayer(),false))))
            .then(Commands.literal("objectives").executes(c->AscensionObjectives.info(c.getSource().getPlayer())))
            .then(Commands.literal("submit").then(Commands.argument("objective",StringArgumentType.word()).executes(c->AscensionObjectives.submit(c.getSource().getPlayer(),StringArgumentType.getString(c,"objective")))))
            .then(Commands.literal("claim").then(Commands.argument("reward",StringArgumentType.word()).executes(c->AscensionObjectives.claim(c.getSource().getPlayer(),StringArgumentType.getString(c,"reward")))))
            .then(Commands.literal("next").executes(c->advance(c.getSource().getPlayer())))
            .then(Commands.literal("info").executes(c->info(c.getSource().getPlayer())))));
    }
    static void maintain(ServerLevel level){
        // Loaded Maintenance: Bound repairs and upgrades without loading idle islands.
        var data=AscensionLedger.get(level);
        for(var team:data.maintenanceBatch())if(!team.archived()&&team.progress().initialized&&level.hasChunkAt(team.center())
                &&level.players().stream().anyMatch(p->team.members().contains(p.getUUID())&&team.contains(p.blockPosition()))){ensureIsland(level,team);AscensionAutomation.tick(level,team);}
        AscensionBridges.get(level).cleanup(level);
    }
    public static int create(ServerPlayer p,String name){
        if(island(p.level())||OneBlock.island(p.level()))return message(p,"Return outside OneBlock before creating or changing teams.");
        var data=AscensionLedger.get(p.level());var level=p.level().getServer().getLevel(LEVEL);
        if(!data.compatible())return message(p,"Ascension save geometry/schema requires migration; existing data has been preserved.");
        var center=data.nextCenter();
        if(!level.getWorldBorder().isWithinBounds(center.offset(-100,0,-100))||!level.getWorldBorder().isWithinBounds(center.offset(100,0,100)))return message(p,"The next island does not fit the world border.");
        var team=data.create(p.getUUID(),name.strip());
        return message(p,team==null?"Create failed: already in a team, invalid name (1-32 characters), or island limit reached.":"Created "+team.name()+" at "+team.center().toShortString()+". Use /ascension home; invite friends with /ascension invite <player>.");
    }
    public static int invite(ServerPlayer owner,ServerPlayer guest){
        var data=AscensionLedger.get(owner.level());var team=data.member(owner.getUUID());
        if(team==null||!team.owner().equals(owner.getUUID())||data.member(guest.getUUID())!=null||team.members().size()>=AscensionLedger.TEAM_LIMIT)return message(owner,"Only the owner can invite an unassigned player to a team with space.");
        invitations.put(guest.getUUID(),new Invitation(team.id(),owner.level().getServer().overworld().getGameTime()+1200));
        message(guest,owner.getName().getString()+" invited you to "+team.name()+". Use /ascension accept within 60 seconds while outside OneBlock.");return message(owner,"Invitation sent.");
    }
    public static int accept(ServerPlayer p){
        if(island(p.level())||OneBlock.island(p.level()))return message(p,"Return outside OneBlock before changing teams.");
        var invitation=invitations.remove(p.getUUID());
        if(invitation==null||invitation.expires()<p.level().getServer().overworld().getGameTime()||!AscensionLedger.get(p.level()).join(invitation.team(),p.getUUID()))return message(p,"No valid invitation, team is full, or you already belong to a team.");
        return message(p,"Joined the team. Use /ascension home.");
    }
    public static int archive(ServerPlayer p){
        var data=AscensionLedger.get(p.level());var team=data.member(p.getUUID());
        if(team==null||!team.owner().equals(p.getUUID()))return message(p,"Only the team owner can archive this island.");
        // Archival Safety: Do not strand offline or visiting members after releasing their active membership.
        for(var id:team.members()){
            var member=p.level().getServer().getPlayerList().getPlayer(id);
            if(member!=null&&(island(member.level())||OneBlock.island(member.level())||OneBlock.state(member).pendingTicks>0))return message(p,"Online members must finish travel and return outside both OneBlock modes before archival.");
        }
        data.archive(p.getUUID());return message(p,"Team archived. The island remains reserved and protected; its neutral bridge cleanup resumes as chunks load. Members may create or join another team.");
    }
    public static int leave(ServerPlayer p){
        if(island(p.level())||OneBlock.island(p.level()))return message(p,"Return outside OneBlock before leaving a team.");
        return message(p,AscensionLedger.get(p.level()).leave(p.getUUID())?"Left the team; its island and resources remain intact.":"Owners must transfer ownership or archive first. Open Ascension > Team.");
    }
    public static int manage(ServerPlayer p,UUID member,boolean transfer){
        if(island(p.level())||OneBlock.island(p.level())||OneBlock.state(p).pendingTicks>0)return message(p,"Return outside both island modes and finish travel before changing membership.");
        var target=p.level().getServer().getPlayerList().getPlayer(member);
        if(target!=null&&(island(target.level())||OneBlock.island(target.level())||OneBlock.state(target).pendingTicks>0))return message(p,"That member must return outside both island modes and finish travel first.");
        var data=AscensionLedger.get(p.level());
        boolean changed=transfer?data.transfer(p.getUUID(),member):data.remove(p.getUUID(),member);
        if(changed&&target!=null)message(target,transfer?"You now own the Ascension team.":"You were removed from the Ascension team; saved possessions are preserved.");
        return message(p,changed?(transfer?"Ownership transferred.":"Member removed. Offline members recover on reconnect."):"Only the owner may change another current member.");
    }
    public static int home(ServerPlayer p){var team=AscensionLedger.get(p.level()).member(p.getUUID());if(team==null)return message(p,"Create a team or accept an invitation first.");request(p,team.id());return 1;}
    private static int visit(ServerPlayer p,ServerPlayer other){var team=AscensionLedger.get(p.level()).member(other.getUUID());if(team==null)return message(p,"That player has no Ascension island.");request(p,team.id());return 1;}
    private static int info(ServerPlayer p){
        var data=AscensionLedger.get(p.level());if(!data.compatible())return message(p,"Ascension save geometry/schema requires migration; access is disabled.");
        var team=data.member(p.getUUID());
        if(team!=null)message(p,team.name()+" | "+team.members().size()+"/6 members | "+(team.owner().equals(p.getUUID())?"Owner":"Member")+" | Home "+team.center().toShortString()+" | "+(team.phases()==null?OneBlockProgress.PHASES[team.progress().phase]:AscensionPhases.get(p.level()).phase(team.phases().phase()).name())+" "+team.progress().breaks+" breaks | Generator "+team.generator().state()+" #"+team.generator().generation());
        return message(p,"/ascension create <name>, invite <player>, accept, home, visit <player>, return, next, leave. Neutral bridges: allocated cardinal neighbors, width 7, Y72-96. /ascension archive manages archival. New teams: /ascension objectives, submit <objective>, claim <reward>. Existing teams retain their six phases. MVP: /ascension guardian, upgrade, automation.");
    }
    public static void mined(ServerPlayer p,BlockPos pos){
        var data=AscensionLedger.get(p.level());var team=data.at(pos);
        if(team==null||team.archived()||!team.center().equals(pos)||!team.members().contains(p.getUUID()))return;
        AscensionGenerator.removed(p,pos);
    }
    public static int advance(ServerPlayer p){
        if(!island(p.level())||!OneBlock.safe(p))return message(p,"Stand safely on your Ascension island first.");
        var data=AscensionLedger.get(p.level());var team=data.member(p.getUUID());
        if(team!=null&&team.phases()!=null)return AscensionObjectives.advance(p,team);
        if(team==null||!team.contains(p.blockPosition())||!team.progress().advance())return message(p,"Your team's next phase is not ready here.");
        data.setDirty();return message(p,"Team phase advanced to "+OneBlockProgress.PHASES[team.progress().phase]+".");
    }
    public static boolean ensureIsland(ServerLevel level,AscensionLedger.Team team){
        if(team.archived())return false;
        var core=team.center();var progress=team.progress();
        if(!progress.initialized){
            // Starter Safety: Validate the whole 5x5 volume before writing any terrain; never overwrite an existing build.
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=2;y++)if(!level.isEmptyBlock(core.offset(x,y,z)))return false;
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(core.offset(x,0,z),Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(core.below(),Blocks.BEDROCK.defaultBlockState());progress.initialized=true;AscensionLedger.get(level).setDirty();
        }
        return AscensionGenerator.reconcile(level,team);
    }
    private static Vec3 arrival(ServerLevel level,AscensionLedger.Team team,boolean ownerArrival){
        if(!level.getWorldBorder().isWithinBounds(team.center().offset(-4,0,-4))||!level.getWorldBorder().isWithinBounds(team.center().offset(4,0,4))||!ensureIsland(level,team))return null;
        var pos=OneBlock.space(level,team.center());if(pos!=null)return Vec3.atBottomCenterOf(pos);
        // Visitor Safety: A visit must never clear another team's arrival area.
        if(!ownerArrival)return null;
        level.destroyBlock(team.center().above(),true);level.destroyBlock(team.center().above(2),true);
        return Vec3.atBottomCenterOf(team.center().above());
    }
    public static Vec3 respawn(ServerPlayer p){
        var team=AscensionLedger.get(p.level()).member(p.getUUID());
        if(team==null)return p.position(); // Preserve the profile during ledger recovery; /ascension return remains available.
        var pos=arrival(p.level(),team,true);
        return pos!=null?pos:Vec3.atBottomCenterOf(team.center().above());
    }
    public static void request(ServerPlayer p,UUID destination){
        if(OneBlock.state(p).pendingTicks>0)return;
        if(!OneBlock.safe(p)||OneBlock.island(p.level())){OneBlock.message(p,"unsafe");return;}
        var data=AscensionLedger.get(p.level());
        if(destination!=null&&(data.member(p.getUUID())==null||data.team(destination)==null)||destination==null&&!island(p.level())){message(p,"Create or join a team before travelling to Ascension.");return;}
        var state=OneBlock.state(p);state.pendingAscension=true;state.pendingTeam=destination;state.pendingTicks=OneBlock.TRAVEL_TICKS;state.departure=p.position();OneBlock.message(p,"travelling");
    }
    public static boolean travel(ServerPlayer p,UUID destinationTeam){
        return travel(p,destinationTeam,false);
    }
    public static boolean recover(ServerPlayer p){
        if(!island(p.level())||!AscensionLedger.get(p.level()).compatible()||AscensionLedger.get(p.level()).member(p.getUUID())!=null)return false;
        return travel(p,null,true);
    }
    private static boolean travel(ServerPlayer p,UUID destinationTeam,boolean recovery){
        if((recovery?!recoveryReady(p):!OneBlock.safe(p))||OneBlock.island(p.level()))return false;
        var server=p.level().getServer();var state=OneBlock.state(p);boolean wasInside=island(p.level()),entering=destinationTeam!=null;
        var level=server.getLevel(entering?LEVEL:state.returnLevel);if(level==null||!entering&&!wasInside||!entering&&(island(level)||OneBlock.island(level)))return false;
        var data=AscensionLedger.get(p.level());var team=data.team(destinationTeam);if(entering&&(data.member(p.getUUID())==null||team==null))return false;
        Vec3 pos=entering?arrival(level,team,team.members().contains(p.getUUID())):state.returnPos;
        if(pos==null){message(p,"No safe arrival space is available; travel was cancelled.");return false;}
        if(!entering&&(!level.noCollision(p,p.getBoundingBox().move(pos.subtract(p.position())))||!level.getBlockState(BlockPos.containing(pos).below()).isSolidRender()||!level.getFluidState(BlockPos.containing(pos)).isEmpty())){
            var fallback=TeleportTransition.createDefault(p,TeleportTransition.DO_NOTHING);if(island(fallback.newLevel())||OneBlock.island(fallback.newLevel()))return false;level=fallback.newLevel();pos=fallback.position();
        }
        var origin=p.level();var oldPos=p.position();float yaw=p.getYRot(),pitch=p.getXRot();state.travelling=true;
        try{
            if(p.teleport(new TeleportTransition(level,pos,Vec3.ZERO,entering?0:state.returnYaw,entering?0:state.returnPitch,TeleportTransition.PLACE_PORTAL_TICKET))==null)return false;
            // Profile Boundary: Visits within Ascension keep the active inventory; only entry/return swaps it.
            if(wasInside!=entering)switchItems(p);
            if(!wasInside&&entering){state.returnLevel=origin.dimension();state.returnPos=oldPos;state.returnYaw=yaw;state.returnPitch=pitch;}
            p.fallDistance=0;((darkspawn.black.mixin.OneBlockSaveAccess)server.getPlayerList()).darkspawn$saveProfile(p);OneBlock.sync(p);return true;
        }finally{state.travelling=false;}
    }
    private static boolean recoveryReady(ServerPlayer p){
        // Emergency Return: Relax ground/combat restrictions only after membership removal, preserving all item boundaries.
        if(!p.isAlive()||p.isPassenger()||p.isVehicle()||p.isChangingDimension()||!p.connection.hasClientLoaded()
            ||p.isSleeping()||p.isUsingItem()||p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty()
            ||!p.getShoulderEntityLeft().isEmpty()||!p.getShoulderEntityRight().isEmpty())return false;
        for(int slot=1;slot<=4;slot++)if(p.inventoryMenu.getSlot(slot).hasItem())return false;
        for(var entity:p.level().getEntities(p,p.getBoundingBox().inflate(16)))
            if(entity instanceof net.minecraft.world.entity.Leashable leash&&leash.getLeashHolder()==p)return false;
        OneBlock.state(p).pendingTicks=0;return true;
    }
    private static void switchItems(ServerPlayer p){
        var state=OneBlock.state(p);var outgoing=OneBlockItems.capture(p);state.ascensionOther.apply(p);state.ascensionOther=outgoing;state.ascensionInside=island(p.level());
        darkspawn.black.equipment.ArmorStyleBonuses.update(p);darkspawn.black.equipment.LegendaryArmor.update(p);p.inventoryMenu.broadcastFullState();
    }
}
