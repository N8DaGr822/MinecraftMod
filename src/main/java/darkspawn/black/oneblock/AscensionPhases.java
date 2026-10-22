package darkspawn.black.oneblock;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Phase Catalog: Validate a complete candidate before replacing the world's last accepted content. */
public final class AscensionPhases extends SavedData {
    public static final List<String> IDS=List.of("genesis","caverns","verdant_wilds","forgeworks","stormbound","emberlands",
        "frostbound","tidal_reach","crystal_depths","endreach","astral_forge","ascension_summit");
    public record Weighted(int resource,int weight){
        static final Codec<Weighted> CODEC=RecordCodecBuilder.create(i->i.group(Codec.INT.fieldOf("resource").forGetter(Weighted::resource),Codec.INT.fieldOf("weight").forGetter(Weighted::weight)).apply(i,Weighted::new));
    }
    public record Guaranteed(int offset,int resource){
        static final Codec<Guaranteed> CODEC=RecordCodecBuilder.create(i->i.group(Codec.INT.fieldOf("offset").forGetter(Guaranteed::offset),Codec.INT.fieldOf("resource").forGetter(Guaranteed::resource)).apply(i,Guaranteed::new));
    }
    public record Offering(String id,String item,int count){
        public static final Codec<Offering> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("id").forGetter(Offering::id),Codec.STRING.fieldOf("item").forGetter(Offering::item),Codec.INT.fieldOf("count").forGetter(Offering::count)).apply(i,Offering::new));
        public ItemStack stack(){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(item)),count);}
    }
    public record Phase(int schema,String id,String name,int breaks,int cycle,List<Weighted> pool,List<Guaranteed> guaranteed,List<Offering> objectives,List<String> encounters,List<Offering> rewards){
        public Phase {pool=List.copyOf(pool);guaranteed=List.copyOf(guaranteed);objectives=List.copyOf(objectives);encounters=List.copyOf(encounters);rewards=List.copyOf(rewards);}
        public static final Codec<Phase> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.INT.fieldOf("schema").forGetter(Phase::schema),Codec.STRING.fieldOf("id").forGetter(Phase::id),Codec.STRING.fieldOf("name").forGetter(Phase::name),
            Codec.INT.fieldOf("breaks").forGetter(Phase::breaks),Codec.INT.fieldOf("cycle").forGetter(Phase::cycle),
            Weighted.CODEC.listOf().fieldOf("pool").forGetter(Phase::pool),Guaranteed.CODEC.listOf().fieldOf("guaranteed").forGetter(Phase::guaranteed),
            Offering.CODEC.listOf().fieldOf("objectives").forGetter(Phase::objectives),Codec.STRING.listOf().fieldOf("encounters").forGetter(Phase::encounters),
            Offering.CODEC.listOf().fieldOf("rewards").forGetter(Phase::rewards)
        ).apply(i,Phase::new));
        int resource(long generation,net.minecraft.util.RandomSource random){
            int offset=(int)(generation%cycle);for(var entry:guaranteed)if(entry.offset()==offset)return entry.resource();
            int roll=random.nextInt(pool.stream().mapToInt(Weighted::weight).sum());
            for(var entry:pool){roll-=entry.weight();if(roll<0)return entry.resource();}throw new IllegalStateException("Invalid resource pool");
        }
    }
    public static final Codec<AscensionPhases> CODEC=Phase.CODEC.listOf().xmap(AscensionPhases::new,p->p.phases);
    public static final SavedDataType<AscensionPhases> TYPE=new SavedDataType<>(Darkspawn.id("ascension_phases"),()->new AscensionPhases(defaults()),CODEC,null);
    private List<Phase> phases;
    private String lastError="";
    private AscensionPhases(List<Phase> phases){
        validate(phases);
        // Catalog Migration: Preserve accepted definitions when extending either earlier chapter.
        if(phases.size()<IDS.size()){var extended=new ArrayList<>(phases);extended.addAll(defaults().subList(phases.size(),IDS.size()));this.phases=List.copyOf(extended);setDirty();}
        else this.phases=List.copyOf(phases);
    }
    public static AscensionPhases get(ServerLevel level){return level.getServer().getLevel(Ascension.LEVEL).getDataStorage().computeIfAbsent(TYPE);}
    public Phase phase(String id){return phases.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();}
    public String lastError(){return lastError;}
    static List<Phase> defaults(){
        var result=new ArrayList<Phase>();
        for(String id:IDS)try(var stream=AscensionPhases.class.getResourceAsStream("/data/darkspawn/ascension/phases/"+id+".json")){
            if(stream==null)throw new IllegalStateException("Missing bundled phase "+id);
            try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){result.add(Phase.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseReader(reader)).getOrThrow());}
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot read phase "+id,e);}
        return List.copyOf(result);
    }
    public static void initialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->reload(server,server.getResourceManager()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,manager,success)->{if(success&&server.getLevel(Ascension.LEVEL)!=null)reload(server,manager);});
    }
    static void reload(MinecraftServer server,ResourceManager manager){
        var data=get(server.getLevel(Ascension.LEVEL));
        try{
            var candidate=new ArrayList<Phase>();
            for(String id:IDS){
                var resource=manager.getResource(Darkspawn.id("ascension/phases/"+id+".json")).orElseThrow();
                try(var reader=resource.openAsReader()){candidate.add(Phase.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseReader(reader)).getOrThrow());}
            }
            var teams=AscensionLedger.get(server.getLevel(Ascension.LEVEL));
            data.replace(candidate,!teams.compatible()||teams.teams().stream().anyMatch(t->t.phases()!=null));
        }catch(Exception failure){data.lastError=failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage();}
        if(!data.lastError.isEmpty())Darkspawn.LOGGER.error("Ascension phase reload rejected; retaining saved catalog: {}",data.lastError);
    }
    boolean replace(List<Phase> candidate){return replace(candidate,true);}
    private boolean replace(List<Phase> candidate,boolean populated){
        try{
            validate(candidate);require(candidate.size()==IDS.size(),"All twelve phases are required on reload");
            // Stable Identities: Balance edits may change amounts/pools, but cannot reinterpret already submitted items or gates.
            for(int i=0;populated&&i<phases.size();i++){
                var old=phases.get(i);var next=candidate.get(i);
                if(!identities(old.objectives()).equals(identities(next.objectives()))||!old.encounters().equals(next.encounters())
                        ||!old.rewards().stream().map(Offering::id).toList().equals(next.rewards().stream().map(Offering::id).toList()))
                    throw new IllegalArgumentException("Phase identities/gates require migration: "+old.id());
            }
            phases=List.copyOf(candidate);lastError="";setDirty();return true;
        }catch(RuntimeException failure){lastError=failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage();return false;}
    }
    private static Map<String,String> identities(List<Offering> offers){var map=new HashMap<String,String>();offers.forEach(o->map.put(o.id(),o.item()));return map;}
    static void validate(List<Phase> phases){
        require(phases.size()==3||phases.size()==6||phases.size()==IDS.size(),"Expected a three-, six- or twelve-phase catalog");var rewards=new HashSet<String>();
        for(int i=0;i<phases.size();i++){
            var p=phases.get(i);require(p.schema()==1&&p.id().equals(IDS.get(i)),"Unsupported phase schema/order");
            require(!p.name().isBlank()&&p.name().length()<=64&&!p.name().chars().anyMatch(Character::isISOControl),"Invalid phase name");
            require(p.breaks()>0&&p.breaks()<=1000000&&p.cycle()>=2&&p.cycle()<=1000,"Invalid threshold/cycle");
            require(!p.pool().isEmpty()&&p.pool().size()<=64,"Invalid pool size");
            var resources=new HashSet<Integer>();for(var w:p.pool())require(resource(w.resource())&&resources.add(w.resource())&&w.weight()>0&&w.weight()<=1000,"Invalid or duplicate weighted resource");
            var offsets=new HashSet<Integer>();var essentials=new HashSet<Integer>();
            for(var g:p.guaranteed()){require(resource(g.resource())&&g.offset()>=0&&g.offset()<p.cycle()&&offsets.add(g.offset()),"Invalid/conflicting guarantee");essentials.add(g.resource());}
            require(essentials.contains(0)&&essentials.contains(1),"Every phase must guarantee dirt and oak logs");
            require(!p.objectives().isEmpty()&&p.objectives().size()<=8&&p.rewards().size()<=8,"Invalid objective/reward count");
            var goals=new HashSet<String>();for(var o:p.objectives()){offering(o,false);require(goals.add(o.id()),"Duplicate objective id");}
            for(var r:p.rewards()){offering(r,true);require(rewards.add(r.id()),"Duplicate global reward id");}
            require(p.encounters().size()<=1&&p.encounters().stream().allMatch(e->!e.isEmpty()&&e.equals(encounter(p.id()))),"Unknown/duplicate encounter gate");
        }
    }
    public static String encounter(String phase){return switch(phase){case "caverns"->"cave_guardian";case "stormbound"->"storm_guardian";case "emberlands"->"ember_guardian";
        case "frostbound","tidal_reach","crystal_depths","endreach","astral_forge","ascension_summit"->phase+"_guardian";default->"";};}
    private static boolean resource(int index){return index>=0&&index<26;}
    private static void offering(Offering o,boolean reward){
        require(o.id().matches("[a-z][a-z0-9_]{0,47}"),"Invalid objective/reward id");
        var id=Identifier.tryParse(o.item());require(id!=null&&id.toString().equals(o.item())&&BuiltInRegistries.ITEM.containsKey(id)&&BuiltInRegistries.ITEM.getValue(id)!=Items.AIR,"Unknown offering item: "+o.item());
        require(o.count()>0&&o.count()<=(reward?o.stack().getMaxStackSize():1000000),"Invalid offering amount");
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
