package darkspawn.black.oneblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** Team Objectives: Submission credit and reward entitlements belong to the island, not the submitting player. */
public final class AscensionPhaseProgress {
    public record Grant(AscensionPhases.Offering reward,boolean claimed){
        static final Codec<Grant> CODEC=RecordCodecBuilder.create(i->i.group(AscensionPhases.Offering.CODEC.fieldOf("reward").forGetter(Grant::reward),Codec.BOOL.fieldOf("claimed").forGetter(Grant::claimed)).apply(i,Grant::new));
    }
    public static final Codec<AscensionPhaseProgress> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.fieldOf("phase").forGetter(p->p.phase),Codec.unboundedMap(Codec.STRING,Codec.INT).fieldOf("submitted").forGetter(p->p.submitted),
        Codec.STRING.listOf().fieldOf("encounters").forGetter(p->List.copyOf(p.encounters)),Codec.STRING.listOf().fieldOf("completed").forGetter(p->List.copyOf(p.completed)),
        Grant.CODEC.listOf().fieldOf("rewards").forGetter(p->p.rewards),
        AscensionFeatures.CODEC.optionalFieldOf("features").forGetter(p->Optional.of(p.features))
    ).apply(i,(phase,submitted,encounters,completed,rewards,features)->new AscensionPhaseProgress(phase,submitted,encounters,completed,rewards,features.orElseGet(AscensionFeatures::new))));
    private final AscensionFeatures features;
    public AscensionFeatures features(){return features;}
    private String phase;
    private final Map<String,Integer> submitted;
    private final List<String> encounters,completed;
    private final List<Grant> rewards;
    public AscensionPhaseProgress(){this("genesis",Map.of(),List.of(),List.of(),List.of(),new AscensionFeatures());}
    private AscensionPhaseProgress(String phase,Map<String,Integer> submitted,List<String> encounters,List<String> completed,List<Grant> rewards,AscensionFeatures features){
        this.features=features;this.phase=phase;this.submitted=new LinkedHashMap<>(submitted);this.encounters=new ArrayList<>(encounters);this.completed=new ArrayList<>(completed);this.rewards=new ArrayList<>(rewards);
    }
    public String phase(){return phase;}
    public boolean completed(){return completed.contains(phase);}
    public boolean finished(String id){return completed.contains(id);}
    public int submitted(String id){return submitted.getOrDefault(phase+":"+id,0);}
    public List<Grant> rewards(){return List.copyOf(rewards);}
    public boolean encountered(String id){return encounters.contains(id);}
    public boolean compatible(){
        int index=AscensionPhases.IDS.indexOf(phase);if(!features.compatible()||index<0||submitted.size()>96||rewards.size()>96)return false;
        if(submitted.entrySet().stream().anyMatch(e->!e.getKey().matches("("+String.join("|",AscensionPhases.IDS)+"):[a-z][a-z0-9_]{0,47}")||e.getValue()<0||e.getValue()>1000000))return false;
        if(encounters.size()>9||new HashSet<>(encounters).size()!=encounters.size()||encounters.stream().anyMatch(e->e.isEmpty()||AscensionPhases.IDS.stream().noneMatch(id->e.equals(AscensionPhases.encounter(id)))))return false;
        if(!(completed.equals(AscensionPhases.IDS.subList(0,index))||completed.equals(AscensionPhases.IDS.subList(0,index+1))))return false;
        var ids=new HashSet<String>();return rewards.stream().allMatch(g->ids.add(g.reward().id())&&g.reward().id().matches("[a-z][a-z0-9_]{0,47}")&&g.reward().count()>0&&g.reward().count()<=64
            &&net.minecraft.resources.Identifier.tryParse(g.reward().item())!=null);
    }
    int submit(AscensionPhases.Phase definition,String id,String item,int offered){
        if(!phase.equals(definition.id())||completed()||offered<=0)return 0;
        var objective=definition.objectives().stream().filter(o->o.id().equals(id)&&o.item().equals(item)).findFirst().orElse(null);if(objective==null)return 0;
        int accepted=Math.min(offered,Math.max(0,objective.count()-submitted(id)));
        if(accepted>0)submitted.put(phase+":"+id,submitted(id)+accepted);return accepted;
    }
    boolean prepared(AscensionPhases.Phase definition,OneBlockProgress progress){
        return phase.equals(definition.id())&&!completed()&&progress.breaks>=definition.breaks()&&definition.objectives().stream().allMatch(o->submitted(o.id())>=o.count());
    }
    boolean ready(AscensionPhases.Phase definition,OneBlockProgress progress){return prepared(definition,progress)&&definition.encounters().stream().allMatch(this::encountered);}
    boolean advance(AscensionPhases.Phase definition,OneBlockProgress progress){
        // Chapter Continuation: Old terminal saves continue explicitly without granting their rewards twice.
        int previous=AscensionPhases.IDS.indexOf(phase);
        if(phase.equals(definition.id())&&completed()&&previous<AscensionPhases.IDS.size()-1){phase=AscensionPhases.IDS.get(previous+1);progress.phase=previous+1;progress.breaks=0;return true;}
        if(!ready(definition,progress))return false;
        // Reward Snapshot: Later balancing cannot change an already earned entitlement or refresh a claimed ID.
        completed.add(phase);for(var reward:definition.rewards())if(rewards.stream().noneMatch(g->g.reward().id().equals(reward.id())))rewards.add(new Grant(reward,false));
        int index=AscensionPhases.IDS.indexOf(phase);if(index<AscensionPhases.IDS.size()-1){phase=AscensionPhases.IDS.get(index+1);progress.phase=index+1;progress.breaks=0;}
        return true;
    }
    boolean recordEncounter(String id){if(id.isEmpty()||!id.equals(AscensionPhases.encounter(phase))||encountered(id))return false;encounters.add(id);return true;}
    Grant unclaimed(String id){return rewards.stream().filter(g->!g.claimed()&&g.reward().id().equals(id)).findFirst().orElse(null);}
    boolean claim(String id){var grant=unclaimed(id);if(grant==null)return false;rewards.set(rewards.indexOf(grant),new Grant(grant.reward(),true));return true;}
}
