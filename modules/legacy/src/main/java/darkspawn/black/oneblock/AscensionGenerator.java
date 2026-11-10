package darkspawn.black.oneblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

/** Generator Transaction: Persist the next resource with progress; recovery never replays harvesting or rewards. */
public final class AscensionGenerator {
    public enum State { READY, PENDING, REGENERATING, LOCKED, ERROR }
    public record Attempt(UUID id,UUID player,long generation,long tick,boolean eligible) { }
    public static final Codec<AscensionGenerator> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.fieldOf("state").forGetter(g->g.status),
        Codec.LONG.fieldOf("generation").forGetter(g->g.generation),
        Codec.LONG.fieldOf("lastBreakTick").forGetter(g->g.lastBreakTick)
    ).apply(i,AscensionGenerator::new));
    private String status;
    private long generation,lastBreakTick;
    private Attempt pending;
    private boolean removed;
    public AscensionGenerator(long generation){this("READY",generation,Long.MIN_VALUE);}
    private AscensionGenerator(String status,long generation,long lastBreakTick){this.status=status;this.generation=generation;this.lastBreakTick=lastBreakTick;}
    public boolean compatible(){return generation>=0&&java.util.Arrays.stream(State.values()).anyMatch(s->s.name().equals(status));}
    public State state(){return State.valueOf(status);}
    public long generation(){return generation;}
    Attempt begin(UUID player,long tick,boolean eligible){
        if(state()!=State.READY||pending!=null||lastBreakTick==tick)return null;
        if(generation==Long.MAX_VALUE){status="LOCKED";return null;}
        pending=new Attempt(UUID.randomUUID(),player,generation,tick,eligible);removed=false;status="PENDING";return pending;
    }
    private boolean matches(Attempt attempt){return attempt!=null&&attempt.equals(pending)&&attempt.generation()==generation&&state()==State.PENDING;}
    boolean owns(UUID player){return pending!=null&&pending.player().equals(player)&&matches(pending);}
    void removed(UUID player){if(owns(player))removed=true;}
    boolean finish(Attempt attempt,OneBlockProgress progress,IntSupplier nextResource){
        if(!matches(attempt))return false;
        pending=null;
        if(!removed){status="READY";return false;}
        removed=false;generation++;lastBreakTick=attempt.tick();status="REGENERATING";
        if(!attempt.eligible())return false;
        progress.mined();progress.resource=nextResource.getAsInt();return true;
    }
    void lock(){status="LOCKED";pending=null;removed=false;}
    // Restart Recovery: PENDING without a live reservation is unconfirmed; restore its saved resource without credit.
    boolean recovering(){if(state()==State.LOCKED||pending!=null)return false;status="REGENERATING";return true;}
    void repaired(boolean success){status=success?"READY":"ERROR";}

    public static boolean destroyBlock(ServerPlayer player,BlockPos pos,BooleanSupplier vanilla){
        var level=player.level();if(!Ascension.island(level))return vanilla.getAsBoolean();
        var block=level.getBlockState(pos);
        if(!Ascension.corePosition(pos)&&!block.is(OneBlock.CORE))return vanilla.getAsBoolean();
        var data=AscensionLedger.get(level);var team=data.at(pos);
        if(team==null||team.archived()||!team.center().equals(pos)||!team.members().contains(player.getUUID())
                ||!team.progress().initialized||!block.equals(expected(team)))return false;
        var generator=team.generator();var attempt=generator.begin(player.getUUID(),level.getGameTime(),!player.preventsBlockDrops());
        data.setDirty();if(attempt==null)return false;
        try{return vanilla.getAsBoolean();}
        finally{
            // Confirmed Removal: Fabric AFTER marks actual removal; false returns/cancellation cannot grant progress.
            complete(level,team,attempt);
        }
    }
    private static void complete(ServerLevel level,AscensionLedger.Team team,Attempt attempt){
        var generator=team.generator();
        boolean rewarded=generator.finish(attempt,team.progress(),()->team.phases()==null?OneBlock.nextResource(level,team.progress()):AscensionPhases.get(level).phase(team.phases().phase()).resource(generator.generation(),level.getRandom()));
        AscensionLedger.get(level).setDirty();
        if(generator.state()==State.REGENERATING){
            reconcile(level,team);
            if(rewarded){if(team.phases()==null)OneBlock.milestoneRewards(level,team.progress(),team.center());else if(team.progress().total%50==0)OneBlock.creature(level,Math.min(2,team.progress().phase),team.center());}
        }
    }
    static boolean automine(ServerLevel level,AscensionLedger.Team team,net.minecraft.world.level.block.entity.ChestBlockEntity chest){
        if(!Ascension.island(level)||team.archived()||!team.progress().initialized||team.phases()==null||!team.phases().features().miner||!team.phases().features().enabled
                ||!AscensionAutomation.present(level,team)||!AscensionAutomation.loadedArea(level,team)||AscensionAutomation.chest(level,team)!=chest)return false;
        var state=level.getBlockState(team.center());var tool=chest.getItem(0);
        if(!state.equals(expected(team))||!AscensionAutomation.tool(tool)||OneBlockCore.resource(state).requiresCorrectToolForDrops()&&!tool.isCorrectToolForDrops(OneBlockCore.resource(state)))return false;
        // Backpressure: Reserve a full empty output slot before sampling native loot; a blocked machine cannot reroll drops.
        boolean space=false;for(int i=1;i<chest.getContainerSize();i++)if(chest.getItem(i).isEmpty()){space=true;break;}if(!space)return false;
        var generator=team.generator();var attempt=generator.begin(team.id(),level.getGameTime(),true);if(attempt==null)return false;
        AscensionLedger.get(level).setDirty();
        try{
            var drops=Block.getDrops(state,level,team.center(),null,null,tool.copy());var output=AscensionAutomation.plan(chest,drops);if(output==null)return false;
            if(!level.setBlock(team.center(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE))return false;
            generator.removed(team.id());AscensionAutomation.apply(chest,output);tool.hurtAndBreak(1,level,null,broken->{});chest.setChanged();return true;
        }finally{complete(level,team,attempt);}
    }
    static void removed(ServerPlayer player,BlockPos pos){
        var team=AscensionLedger.get(player.level()).at(pos);
        if(team!=null&&!team.archived()&&team.center().equals(pos)&&player.level().isEmptyBlock(pos))team.generator().removed(player.getUUID());
    }
    private static net.minecraft.world.level.block.state.BlockState expected(AscensionLedger.Team team){
        return OneBlock.CORE.defaultBlockState().setValue(OneBlockCore.RESOURCE,team.progress().resource);
    }
    public static boolean reconcile(ServerLevel level,AscensionLedger.Team team){
        if(!Ascension.island(level)||team.archived()||!team.progress().initialized||!level.hasChunkAt(team.center()))return false;
        var generator=team.generator();var target=expected(team);
        if(generator.state()==State.READY&&level.getBlockState(team.center()).equals(target))return true;
        if(!generator.recovering())return false;
        // Loaded Repair: No drops, RNG, rewards or neighbor propagation; a boundary core must not load adjacent chunks.
        boolean success=level.getBlockState(team.center()).equals(target)
            ||level.setBlock(team.center(),target,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        generator.repaired(success);AscensionLedger.get(level).setDirty();return success;
    }
}
