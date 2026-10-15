package darkspawn.black.ecosystem;

import com.mojang.serialization.Codec;
import darkspawn.black.Darkspawn;
import darkspawn.black.boss.*;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.*;

/** Shared victories and harvest cooldowns; player buildings are never rewritten. */
public final class WorldAwakening extends SavedData {
    public static final SavedDataType<WorldAwakening> TYPE=new SavedDataType<>(Darkspawn.id("world_awakening"),()->new WorldAwakening(Map.of()),Codec.unboundedMap(Codec.STRING,Codec.LONG).xmap(WorldAwakening::new,s->s.events),null);
    private final Map<String,Long> events=new HashMap<>();
    private WorldAwakening(Map<String,Long> values){events.putAll(values);}
    public static WorldAwakening get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    public boolean defeated(BossKind kind){return events.containsKey("boss/"+kind.id());}
    public boolean unlock(BossKind kind,long now){if(defeated(kind))return false;events.put("boss/"+kind.id(),now);setDirty();return true;}
    public boolean allDefeated(){return Arrays.stream(BossKind.values()).allMatch(this::defeated);}
    public static void victory(ServerLevel level,BossKind kind){
        if(get(level).unlock(kind,level.getServer().overworld().getGameTime()))level.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("World awakening: "+kind.id().replace('_',' ')+" defeated. This victory is shared with all explorers."),false);
    }
    public static boolean diminished(ServerLevel level,RegionalKind kind){
        return (kind.region==BossProfile.FOSSIL_TYRANT&&get(level).defeated(BossKind.FOSSIL_TYRANT)
            ||kind.region==BossProfile.SHADOW_CREEPER_QUEEN&&get(level).defeated(BossKind.SHADOW_CREEPER_QUEEN))&&level.getRandom().nextBoolean();
    }
    public static boolean harvest(ServerPlayer player,BlockPos pos){
        if(!player.isAlive()||player.isSpectator()||!player.isShiftKeyDown()||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>36||!player.level().mayInteract(player,pos))return false;
        var level=player.level();var data=get(level);var block=level.getBlockState(pos);Item reward=null;
        if(block.is(BlockTags.LOGS)&&data.defeated(BossKind.ANCIENT_TREE_SPIRIT))reward=level.getRandom().nextInt(4)==0?HeartwoodGrove.SEEDLING_ITEM:BossItems.LIVING_HEARTWOOD;
        if((block.is(Blocks.RED_MUSHROOM)||block.is(Blocks.BROWN_MUSHROOM))&&data.defeated(BossKind.MYCELIAL_SOVEREIGN))reward=darkspawn.black.cooking.AnimalUtility.FUNGAL_CREAM;
        if(block.is(Blocks.LIGHTNING_ROD.weathering().unaffected())&&level.isThundering()&&level.canSeeSky(pos.above())&&data.defeated(BossKind.THUNDER_BIRD))reward=RegionalEcosystems.STORMFORGED_FEATHER;
        if(reward==null||player.getInventory().getFreeSlot()<0)return false;
        long now=level.getServer().overworld().getGameTime();String key="harvest/"+player.getUUID();
        if(now<data.events.getOrDefault(key,0L)){player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The awakened habitat needs time: one harvest per explorer every five minutes."));return false;}
        // Container Conservation: Fungal cream returns a bowl when cooked, so harvesting must supply one.
        if(reward==darkspawn.black.cooking.AnimalUtility.FUNGAL_CREAM&&!darkspawn.black.pirate.Ships.pay(player,Items.BOWL,1,true)){
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Carry an empty bowl to collect fungal cream."));return false;
        }
        data.events.put(key,now+6000);data.setDirty();player.getInventory().add(new ItemStack(reward));return true;
    }
    public static void initialize(){
        // Victory Ownership: World progression follows real deaths even when mob loot is disabled.
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(!(entity.level() instanceof ServerLevel level)||entity.entityTags().contains(darkspawn.black.endgame.Nexus.TAG))return;
            BossKind kind=entity instanceof BiomeBoss boss?boss.profile().kind():entity instanceof AncientTreeSpirit?BossKind.ANCIENT_TREE_SPIRIT:entity instanceof MutantWolf?BossKind.MUTANT_WOLF:null;
            if(kind!=null)victory(level,kind);
        });
        // Save Migration: Existing earned boss advancements unlock the shared world on first join.
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            for(var kind:BossKind.values()){
                var advancement=server.getAdvancements().get(Darkspawn.id("progress/defeat_"+kind.id()));
                if(advancement!=null&&handler.player.getAdvancements().getOrStartProgress(advancement).isDone())get(server.overworld()).unlock(kind,server.overworld().getGameTime());
            }
        });
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(!player.getItemInHand(hand).is(Items.BRUSH)||!player.isShiftKeyDown())return InteractionResult.PASS;
            return player instanceof ServerPlayer server&&harvest(server,hit.getBlockPos())?InteractionResult.SUCCESS:InteractionResult.PASS;
        });
    }
}
