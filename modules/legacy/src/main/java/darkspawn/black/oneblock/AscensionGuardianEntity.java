package darkspawn.black.oneblock;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Guardian Presentation: Synchronized chapter and warning state; native golem combat stays intact. */
public final class AscensionGuardianEntity extends IronGolem {
    private static final EntityDataAccessor<Integer> CHAPTER=SynchedEntityData.defineId(AscensionGuardianEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WARNING=SynchedEntityData.defineId(AscensionGuardianEntity.class,EntityDataSerializers.INT);
    public AscensionGuardianEntity(EntityType<? extends AscensionGuardianEntity> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);builder.define(CHAPTER,1);builder.define(WARNING,0);
    }
    public int chapter(){return entityData.get(CHAPTER);}
    public void chapter(int chapter){entityData.set(CHAPTER,chapter==1||chapter>=4&&chapter<=11?chapter:1);}
    public int warning(){return entityData.get(WARNING);}
    public void warning(int ticks){entityData.set(WARNING,Math.clamp(ticks,0,40));}
    @Override public void baseTick(){
        super.baseTick();
        if(!level().isClientSide()&&warning()>0)warning(warning()-1);
    }
    @Override protected void addAdditionalSaveData(ValueOutput output){super.addAdditionalSaveData(output);output.putInt("AscensionChapter",chapter());}
    @Override protected void readAdditionalSaveData(ValueInput input){super.readAdditionalSaveData(input);chapter(input.getIntOr("AscensionChapter",1));warning(0);}
    // Guardian Score: Each chapter selects its arrangement of the shared original Ascension motif.
    public static String musicTheme(int chapter){return switch(chapter){
        case 4->"ascension_storm";case 5->"ascension_ember";case 6->"ascension_frost";case 7->"ascension_tidal";
        case 8->"ascension_crystal";case 9->"ascension_end";case 10->"ascension_astral";case 11->"ascension_summit";default->"ascension_cave";
    };}
    public int intensity(){return getHealth()<=getMaxHealth()/3?3:getHealth()<=getMaxHealth()*2/3?2:1;}
}
