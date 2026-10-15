package darkspawn.black.cooking;

import darkspawn.black.Darkspawn;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fish.*;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;

public final class RegionalFishing {
    public static final String[] IDS={"pike","tuna","anglerfish","arctic_cod","piranha","cave_fish"};
    public static final List<Item> RAW=new ArrayList<>(),COOKED=new ArrayList<>(),BUCKETS=new ArrayList<>();
    public static final List<EntityType<RegionalFish>> TYPES=new ArrayList<>();
    private RegionalFishing() { }
    public static int region(Level level,BlockPos pos){
        var biome=level.getBiome(pos);
        if(pos.getY()<0&&!level.canSeeSky(pos))return 5;
        if(biome.is(BiomeTags.IS_DEEP_OCEAN)&&pos.getY()<40)return 2;
        if(biome.value().getBaseTemperature()<=.15F)return 3;
        if(biome.is(BiomeTags.IS_JUNGLE)||biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP))return 4;
        if(biome.is(BiomeTags.IS_OCEAN))return 1;
        return 0;
    }
    public static List<ItemStack> catches(Level level,BlockPos pos,List<ItemStack> original){
        // Keep native treasure, junk, count and luck rolls; only plain cod/salmon are regionalized.
        return original.stream().map(stack->(stack.is(Items.COD)||stack.is(Items.SALMON))&&ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem()))&&level.getRandom().nextBoolean()?new ItemStack(RAW.get(region(level,pos)),stack.getCount()):stack).toList();
    }
    public static boolean canSpawn(int kind,ServerLevelAccessor level,BlockPos pos){
        return level.getLevel().dimension()==Level.OVERWORLD&&level.getFluidState(pos).is(FluidTags.WATER)&&level.getFluidState(pos.above()).is(FluidTags.WATER)&&region(level.getLevel(),pos)==kind;
    }
    public static void initialize(){
        for(int i=0;i<IDS.length;i++){
            final int kind=i;String id=IDS[i];
            RAW.add(Cuisine.register(id,p->new Item(p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(.1F).build()))));
            COOKED.add(Cuisine.register("cooked_"+id,p->new Item(p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(.6F).build()))));
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Darkspawn.id(id));
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<RegionalFish>of((t,l)->new RegionalFish(t,l,kind),MobCategory.WATER_AMBIENT).sized(i==1?.8F:.5F,.35F).clientTrackingRange(6).noLootTable().build(key));TYPES.add(type);
            FabricDefaultAttributeRegistry.register(type,AbstractFish.createAttributes());
            BUCKETS.add(Cuisine.register(id+"_bucket",p->new MobBucketItem(type,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH,p.stacksTo(1))));
            SpawnPlacements.register(type,SpawnPlacementTypes.IN_WATER,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,pos,random)->canSpawn(kind,l,pos));
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),MobCategory.WATER_AMBIENT,type,2,1,3);
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(e->{RAW.forEach(e::accept);COOKED.forEach(e::accept);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->BUCKETS.forEach(e::accept));
    }
    public static final class RegionalFish extends Cod {
        public final int kind;
        public RegionalFish(EntityType<? extends Cod> type,Level level,int kind){super(type,level);this.kind=kind;}
        @Override public ItemStack getBucketItemStack(){return new ItemStack(BUCKETS.get(kind));}
        @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean player){super.dropCustomDeathLoot(level,source,player);spawnAtLocation(level,new ItemStack(isOnFire()?COOKED.get(kind):RAW.get(kind)));}
    }
}
