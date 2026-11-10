package darkspawn.black.fishing;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class FishingGameTest {
    @GameTest public void fishBucketsPreserveSpeciesAndFishingKeepsSpecialLoot(GameTestHelper h){
        for(int i=0;i<6;i++){
            var fish=RegionalFishing.TYPES.get(i).create(h.getLevel(),EntitySpawnReason.COMMAND);fish.setFromBucket(true);fish.setCustomName(Component.literal("Fish "+i));
            h.assertTrue(fish.getBucketItemStack().is(RegionalFishing.BUCKETS.get(i)),"Each swimming species has its own bucket");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());fish.saveWithoutId(out);var copy=RegionalFishing.TYPES.get(i).create(h.getLevel(),EntitySpawnReason.LOAD);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));h.assertTrue(copy.fromBucket()&&copy.getName().getString().equals("Fish "+i),"Bucket persistence and custom name survive");
        }
        var book=new ItemStack(Items.ENCHANTED_BOOK);var named=new ItemStack(Items.COD);named.set(DataComponents.CUSTOM_NAME,Component.literal("Other mod's fish"));var result=RegionalFishing.catches(h.getLevel(),h.absolutePos(BlockPos.ZERO),List.of(book,named));h.assertTrue(ItemStack.matches(result.get(0),book)&&ItemStack.matches(result.get(1),named),"Treasure and customized catches remain untouched");h.succeed();
    }
    @GameTest public void everyRegionalFishSmeltsAndSmokesIntoItsCookedForm(GameTestHelper h){
        var level=h.getLevel();
        for(int i=0;i<RegionalFishing.IDS.length;i++){
            var input=new SingleRecipeInput(new ItemStack(RegionalFishing.RAW.get(i)));
            for(var type:List.of(RecipeType.SMELTING,RecipeType.SMOKING)){
                var recipe=level.recipeAccess().getRecipeFor(type,input,level);
                h.assertTrue(recipe.isPresent()&&recipe.get().value().assemble(input).is(RegionalFishing.COOKED.get(i)),RegionalFishing.IDS[i]+" cooks in "+type);
            }
        }
        h.succeed();
    }
}
