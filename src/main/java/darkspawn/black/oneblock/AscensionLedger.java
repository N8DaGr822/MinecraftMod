package darkspawn.black.oneblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Ascension Teams: Separate versioned storage never reinterprets the legacy shared island. */
public final class AscensionLedger extends SavedData {
    public static final int SPACING=250, CLAIM=100, TEAM_LIMIT=6, MAX_CELLS=1000000;
    public record Team(UUID id,UUID owner,String name,int cell,List<UUID> members,OneBlockProgress progress,boolean archived,AscensionGenerator generator,AscensionPhaseProgress phases) {
        public Team { members=List.copyOf(members); }
        static final Codec<Team> CODEC=RecordCodecBuilder.create(i->i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Team::id),UUIDUtil.CODEC.fieldOf("owner").forGetter(Team::owner),
            Codec.STRING.fieldOf("name").forGetter(Team::name),Codec.INT.fieldOf("cell").forGetter(Team::cell),
            UUIDUtil.CODEC.listOf().fieldOf("members").forGetter(Team::members),OneBlockProgress.ASCENSION_CODEC.fieldOf("progress").forGetter(Team::progress),
            Codec.BOOL.optionalFieldOf("archived",false).forGetter(Team::archived),
            AscensionGenerator.CODEC.optionalFieldOf("generator").forGetter(t->Optional.ofNullable(t.generator())),
            AscensionPhaseProgress.CODEC.optionalFieldOf("phases").forGetter(t->Optional.ofNullable(t.phases()))
        ).apply(i,(id,owner,name,cell,members,progress,archived,generator,phases)->new Team(id,owner,name,cell,members,progress,archived,generator.orElse(null),phases.orElse(null))));
        public BlockPos center(){return cellCenter(cell);}
        public boolean contains(BlockPos pos){var c=center();return Math.abs((long)pos.getX()-c.getX())<=CLAIM&&Math.abs((long)pos.getZ()-c.getZ())<=CLAIM;}
    }
    public static final Codec<AscensionLedger> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.INT.fieldOf("schema").forGetter(s->s.schema),Codec.INT.fieldOf("spacing").forGetter(s->s.spacing),
        Codec.INT.fieldOf("claim").forGetter(s->s.claim),Codec.INT.fieldOf("nextCell").forGetter(s->s.nextCell),
        Team.CODEC.listOf().fieldOf("teams").forGetter(s->s.savedTeams)
    ).apply(i,AscensionLedger::new));
    public static final SavedDataType<AscensionLedger> TYPE=new SavedDataType<>(Darkspawn.id("ascension"),AscensionLedger::new,CODEC,null);
    private final int schema,spacing,claim;
    private int nextCell;
    private final List<Team> savedTeams;
    private final Map<UUID,Team> teams=new LinkedHashMap<>();
    private final Map<UUID,UUID> membership=new HashMap<>();
    private boolean compatible;
    private int maintenanceCursor;
    public static final int MAINTENANCE_BUDGET=32;
    public AscensionLedger(){this(6,SPACING,CLAIM,0,List.of());}
    private AscensionLedger(int schema,int spacing,int claim,int nextCell,List<Team> savedTeams){
        this.spacing=spacing;this.claim=claim;this.nextCell=nextCell;this.savedTeams=new ArrayList<>(savedTeams);
        compatible=(schema>=1&&schema<=6)&&spacing==SPACING&&claim==CLAIM&&nextCell>=0&&nextCell<=MAX_CELLS;
        var cells=new HashSet<Integer>();
        for(var team:savedTeams){
            compatible&=team.cell()>=0&&team.cell()<nextCell&&cells.add(team.cell())&&teams.putIfAbsent(team.id(),team)==null
                &&(team.generator()!=null?team.generator().compatible():schema<3)
                &&(team.phases()==null?team.progress().phase<=5&&team.progress().resource<=19
                    :schema>=4&&team.phases().compatible()&&team.progress().phase==AscensionPhases.IDS.indexOf(team.phases().phase()))
                &&team.members().size()<=TEAM_LIMIT&&team.members().contains(team.owner())&&new HashSet<>(team.members()).size()==team.members().size();
            if(!team.archived())for(var member:team.members())if(membership.putIfAbsent(member,team.id())!=null)compatible=false;
        }
        this.schema=compatible?6:schema;
        if(compatible&&schema<6){
            // Versioned Upgrade: Keep all cells/progress; seed old generators once from their existing total.
            for(var team:savedTeams)if(team.generator()==null){
                var generator=new AscensionGenerator(team.progress().total);if(team.archived())generator.lock();
                teams.put(team.id(),new Team(team.id(),team.owner(),team.name(),team.cell(),team.members(),team.progress(),team.archived(),generator,null));
            }
            saveTeams();
        }
    }
    public static AscensionLedger get(ServerLevel level){return level.getServer().getLevel(Ascension.LEVEL).getDataStorage().computeIfAbsent(TYPE);}
    public boolean compatible(){return compatible;}
    public Team team(UUID id){var team=compatible?teams.get(id):null;return team!=null&&!team.archived()?team:null;}
    public boolean archived(UUID id){var team=teams.get(id);return compatible&&team!=null&&team.archived();}
    public Team member(UUID player){return team(membership.get(player));}
    public Collection<Team> teams(){return compatible?List.copyOf(teams.values()):List.of();}
    public Team at(BlockPos pos){return teams().stream().filter(t->t.contains(pos)).findFirst().orElse(null);}
    public List<Team> maintenanceBatch(){
        if(!compatible||savedTeams.isEmpty())return List.of();
        var batch=new ArrayList<Team>();
        for(int i=0;i<Math.min(MAINTENANCE_BUDGET,savedTeams.size());i++){
            if(maintenanceCursor>=savedTeams.size())maintenanceCursor=0;
            batch.add(savedTeams.get(maintenanceCursor++));
        }
        return batch;
    }
    public BlockPos nextCenter(){return cellCenter(nextCell);}
    public Team create(UUID owner,String name){
        if(!compatible||member(owner)!=null||nextCell>=MAX_CELLS||name.isBlank()||name.length()>32||name.chars().anyMatch(Character::isISOControl))return null;
        var team=new Team(UUID.randomUUID(),owner,name,nextCell++,List.of(owner),new OneBlockProgress(),false,new AscensionGenerator(0),new AscensionPhaseProgress());
        teams.put(team.id(),team);membership.put(owner,team.id());saveTeams();return team;
    }
    public boolean join(UUID teamId,UUID player){
        var team=team(teamId);if(team==null||member(player)!=null||team.members().size()>=TEAM_LIMIT)return false;
        var members=new ArrayList<>(team.members());members.add(player);
        teams.put(team.id(),new Team(team.id(),team.owner(),team.name(),team.cell(),members,team.progress(),false,team.generator(),team.phases()));membership.put(player,team.id());saveTeams();return true;
    }
    public boolean leave(UUID player){
        var team=member(player);if(team==null||team.owner().equals(player))return false;
        var members=new ArrayList<>(team.members());members.remove(player);
        teams.put(team.id(),new Team(team.id(),team.owner(),team.name(),team.cell(),members,team.progress(),false,team.generator(),team.phases()));membership.remove(player);saveTeams();return true;
    }
    public boolean archive(UUID owner){
        var team=member(owner);if(team==null||!team.owner().equals(owner))return false;
        teams.put(team.id(),new Team(team.id(),team.owner(),team.name(),team.cell(),team.members(),team.progress(),true,team.generator(),team.phases()));
        team.generator().lock();team.members().forEach(membership::remove);saveTeams();return true;
    }
    public boolean transfer(UUID owner,UUID successor){
        var team=member(owner);if(team==null||!team.owner().equals(owner)||owner.equals(successor)||!team.members().contains(successor))return false;
        teams.put(team.id(),new Team(team.id(),successor,team.name(),team.cell(),team.members(),team.progress(),false,team.generator(),team.phases()));saveTeams();return true;
    }
    public boolean remove(UUID owner,UUID member){
        var team=member(owner);if(team==null||!team.owner().equals(owner)||owner.equals(member)||!team.members().contains(member))return false;
        return leave(member);
    }
    private void saveTeams(){savedTeams.clear();savedTeams.addAll(teams.values());setDirty();}
    // Durable Allocation: Spiral cells are never reused, including after a member leaves.
    public static BlockPos cellCenter(int index){
        if(index==0)return new BlockPos(0,80,0);
        int r=(int)Math.ceil((Math.sqrt((double)index+1)-1)/2),side=2*r,d=(2*r+1)*(2*r+1)-1-index,x,z;
        if(d<side){x=r-d;z=-r;}else if(d<2*side){x=-r;z=-r+d-side;}
        else if(d<3*side){x=-r+d-2*side;z=r;}else{x=r;z=r-d+3*side;}
        return new BlockPos(x*SPACING,80,z*SPACING);
    }
}
