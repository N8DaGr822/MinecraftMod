package darkspawn.black.cooking;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;

public final class AnimalUtility {
    public static final Item TRUFFLE=food("woodland_truffle",2),RICH_EGG=Cuisine.register("rich_egg",Item::new),HERBED_EGG=Cuisine.register("herbed_egg",Item::new),GOAT_CHEESE=food("goat_cheese",4),FUNGAL_CREAM=Cuisine.register("fungal_cream",p->new Item(p.craftRemainder(Items.BOWL)));
    public static final Item GOAT_MILK=Cuisine.register("goat_milk",p->new Item(p.stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    private static Item food(String id,int hunger){return Cuisine.register(id,p->new Item(p.food(new FoodProperties.Builder().nutrition(hunger).saturationModifier(.4F).build())));}
    private AnimalUtility() { }
    public static AnimalUtilityState state(Animal animal){return ((AnimalUtilityState.Holder)animal).darkspawn$animalUtility();}
    public static void initialize(){
        String[] meals={"pike_chowder","tuna_rice_bowl","anglerfish_stew","arctic_cod_soup","piranha_pepper_bowl","cave_fish_broth","truffle_risotto","farm_omelette","herbed_omelette","goat_cheese_plate","fungal_cream_soup"};
        for(int i=0;i<meals.length;i++){
            String name=meals[i];var effect=i==3?MealEffects.WARMING:i==5?MealEffects.MINER:i==6?MealEffects.HEARTY:i==8?MealEffects.IRON_STOMACH:MealEffects.DEEP_BREATH;
            var item=Cuisine.register(name,p->new MealItem(p.stacksTo(16).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(8).saturationModifier(.7F).alwaysEdible().build()),effect,2400));
            Cuisine.DISHES.put(name,new Cuisine.Dish(item,"Field kitchen",""));
        }
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(e->{for(var item:java.util.List.of(TRUFFLE,RICH_EGG,HERBED_EGG,GOAT_MILK,GOAT_CHEESE,FUNGAL_CREAM))e.accept(item);});
        UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(!player.isShiftKeyDown()||!(entity instanceof Animal animal)||animal.isBaby()||!animal.isAlive()||player.isSpectator())return InteractionResult.PASS;
            var held=player.getItemInHand(hand);boolean candidate=animal instanceof Pig&&held.is(Items.CARROT)||animal instanceof Chicken&&(held.is(Items.PUMPKIN_SEEDS)||held.is(Cooking.WILD_HERBS))||animal instanceof Goat&&held.is(Items.GLASS_BOTTLE)||animal instanceof MushroomCow&&held.is(Items.BOWL);
            if(!candidate)return InteractionResult.PASS;
            if(player instanceof ServerPlayer server)interact(server,animal,hand);
            return InteractionResult.SUCCESS;
        });
    }
    public static boolean interact(ServerPlayer p,Animal animal,InteractionHand hand){
        if(!p.isAlive()||p.isSpectator()||animal.isBaby()||!animal.isAlive()||p.level()!=animal.level()||p.distanceToSqr(animal)>36||!p.hasLineOfSight(animal)||!p.level().mayInteract(p,animal.blockPosition()))return false;
        var s=state(animal);var held=p.getItemInHand(hand);long now=p.level().getGameTime();
        if(now<s.readyAt||s.forage!=null||animal instanceof Chicken&&s.egg!=0){p.sendSystemMessage(Component.literal("This animal is not ready yet. No supplies were consumed."));return false;}
        Item reward=null;
        if(animal instanceof Pig&&held.is(Items.CARROT)){
            if(!p.level().getBiome(animal.blockPosition()).is(net.minecraft.tags.BiomeTags.IS_FOREST)){p.sendSystemMessage(Component.literal("Pigs hunt truffles on grass or podzol in forests."));return false;}
            for(var pos:BlockPos.betweenClosed(animal.blockPosition().offset(-3,-1,-3),animal.blockPosition().offset(3,0,3))){if(!p.level().hasChunkAt(pos))continue;var ground=p.level().getBlockState(pos);if((ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.PODZOL))&&p.level().getBlockState(pos.above()).isAir()){s.forage=pos.above().immutable();break;}}
            if(s.forage==null)return false;s.forageUntil=now+600;animal.getNavigation().moveTo(s.forage.getX()+.5,s.forage.getY(),s.forage.getZ()+.5,.8);
        }else if(animal instanceof Chicken chicken&&(held.is(Items.PUMPKIN_SEEDS)||held.is(Cooking.WILD_HERBS))&&!chicken.isChickenJockey()){s.egg=held.is(Items.PUMPKIN_SEEDS)?1:2;}
        else if(animal instanceof Goat&&held.is(Items.GLASS_BOTTLE))reward=GOAT_MILK;
        else if(animal instanceof MushroomCow&&held.is(Items.BOWL))reward=FUNGAL_CREAM;
        else return false;
        held.consume(1,p);s.readyAt=now+6000;if(reward!=null){var stack=new ItemStack(reward);if(!p.getInventory().add(stack))animal.spawnAtLocation(p.level(),stack);}
        p.sendSystemMessage(Component.literal(s.forage!=null?"Truffle hunt started. Keep the pig and forest patch loaded.":animal instanceof Chicken?"Feed accepted. The next naturally laid egg will be special.":"Collected. This animal needs five minutes before another specialty harvest."));return true;
    }
    public static void tick(Animal animal){
        if(!(animal.level() instanceof ServerLevel level)||animal.tickCount%20!=0)return;var s=state(animal);if(s.forage==null)return;
        long now=level.getGameTime();if(now>=s.forageUntil||!animal.isAlive()){s.forage=null;return;}
        if(!level.hasChunkAt(s.forage))return;
        var ground=level.getBlockState(s.forage.below());if(!(ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.PODZOL))){s.forage=null;return;}
        if(now<s.forageUntil-500){animal.getNavigation().moveTo(s.forage.getX()+.5,s.forage.getY(),s.forage.getZ()+.5,.8);return;}
        if(animal.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(s.forage))<4){s.forage=null;animal.spawnAtLocation(level,new ItemStack(TRUFFLE));level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,animal.getX(),animal.getY()+.5,animal.getZ(),6,.4,.2,.4,0);}
    }
}
