package darkspawn.black.oneblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Bridge Ownership: Only successful player placements become cleanup candidates. */
public final class AscensionBridges extends SavedData {
    public static final int MIN_Y=72,MAX_Y=96,HALF_WIDTH=3,CLEANUP_BUDGET=64;
    public record Entry(BlockPos pos,UUID owner,String corridor,String block) {
        static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner),
            Codec.STRING.fieldOf("corridor").forGetter(Entry::corridor),Codec.STRING.fieldOf("block").forGetter(Entry::block)
        ).apply(i,Entry::new));
    }
    public static final Codec<AscensionBridges> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.INT.fieldOf("schema").forGetter(s->s.schema),Entry.CODEC.listOf().fieldOf("blocks").forGetter(s->s.records())
    ).apply(i,AscensionBridges::new));
    public static final SavedDataType<AscensionBridges> TYPE=new SavedDataType<>(Darkspawn.id("ascension_bridges"),AscensionBridges::new,CODEC,null);
    private final int schema;
    private final LinkedHashMap<BlockPos,Entry> blocks=new LinkedHashMap<>();
    private final List<Entry> original;
    private boolean compatible;
    public AscensionBridges(){this(1,List.of());}
    private AscensionBridges(int schema,List<Entry> entries){
        this.schema=schema;original=List.copyOf(entries);compatible=schema==1;
        for(var entry:entries)if(blocks.putIfAbsent(entry.pos(),entry)!=null)compatible=false;
    }
    private List<Entry> records(){return compatible?List.copyOf(blocks.values()):original;}
    public boolean compatible(){return compatible;}
    public static AscensionBridges get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public Entry at(BlockPos pos){return compatible?blocks.get(pos):null;}
    public int size(){return blocks.size();}
    public boolean record(BlockPos pos,UUID owner,String corridor,String block){
        if(!compatible)return false;
        var previous=blocks.get(pos);if(previous!=null&&!previous.owner().equals(owner))return false;
        blocks.put(pos.immutable(),new Entry(pos.immutable(),owner,corridor,block));setDirty();return true;
    }
    public void remove(BlockPos pos){if(compatible&&blocks.remove(pos)!=null)setDirty();}
    public static boolean between(BlockPos a,BlockPos b,BlockPos pos){
        if(pos.getY()<MIN_Y||pos.getY()>MAX_Y)return false;
        long dx=(long)b.getX()-a.getX(),dz=(long)b.getZ()-a.getZ();
        if(dz==0&&Math.abs(dx)==AscensionLedger.SPACING)
            return Math.abs((long)pos.getZ()-a.getZ())<=HALF_WIDTH&&pos.getX()>Math.min(a.getX(),b.getX())+AscensionLedger.CLAIM&&pos.getX()<Math.max(a.getX(),b.getX())-AscensionLedger.CLAIM;
        if(dx==0&&Math.abs(dz)==AscensionLedger.SPACING)
            return Math.abs((long)pos.getX()-a.getX())<=HALF_WIDTH&&pos.getZ()>Math.min(a.getZ(),b.getZ())+AscensionLedger.CLAIM&&pos.getZ()<Math.max(a.getZ(),b.getZ())-AscensionLedger.CLAIM;
        return false;
    }
    public static String corridor(AscensionLedger data,AscensionLedger.Team team,BlockPos pos){
        if(team==null||team.archived()||data.team(team.id())==null)return null;
        for(var neighbor:data.teams())if(!neighbor.archived()&&between(team.center(),neighbor.center(),pos))
            return Math.min(team.cell(),neighbor.cell())+":"+Math.max(team.cell(),neighbor.cell());
        return null;
    }
    public int cleanup(ServerLevel level){
        var teams=AscensionLedger.get(level);if(!compatible||!teams.compatible())return 0;
        int removed=0,limit=Math.min(CLEANUP_BUDGET,blocks.size());
        // Bounded Rotation: Unloaded entries cannot starve later loaded chunks; never request a chunk ticket.
        for(int i=0;i<limit;i++){
            var first=blocks.pollFirstEntry();var entry=first.getValue();boolean completed=false;
            if(teams.archived(entry.owner())&&level.hasChunkAt(entry.pos())){
                var state=level.getBlockState(entry.pos());
                // Administrative Changes: Never delete a different block that replaced the recorded bridge material.
                completed=!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(entry.block())
                    // Suppress neighbor-shape propagation too: a boundary removal must not load adjacent chunks.
                    ||level.setBlock(entry.pos(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            }
            if(completed){removed++;setDirty();}else blocks.putLast(first.getKey(),entry);
        }
        return removed;
    }
}
