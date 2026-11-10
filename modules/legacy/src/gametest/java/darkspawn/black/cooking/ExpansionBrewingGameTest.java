package darkspawn.black.cooking;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

public final class ExpansionBrewingGameTest {
    @GameTest public void nativeBrewingConsumesIngredientAndPreservesVanilla(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,1,1));level.setBlock(pos,Blocks.BREWING_STAND.defaultBlockState(),3);
        var stand=(BrewingStandBlockEntity)level.getBlockEntity(pos);
        stand.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.AWKWARD));stand.setItem(1,PotionContents.createItemStack(Items.POTION,Potions.AWKWARD));stand.setItem(3,new ItemStack(Cooking.WILD_HERBS,2));stand.setItem(4,new ItemStack(Items.BLAZE_POWDER));
        for(int i=0;i<401;i++)BrewingStandBlockEntity.serverTick(level,pos,level.getBlockState(pos),stand);
        h.assertTrue(stand.getItem(0).get(DataComponents.POTION_CONTENTS).is(CulinaryBrewing.POTIONS.get("herbal_restoration"))&&stand.getItem(1).get(DataComponents.POTION_CONTENTS).is(CulinaryBrewing.POTIONS.get("herbal_restoration")),"Actual stand brews both bottles");
        h.assertTrue(stand.getItem(3).getCount()==1,"One reagent per batch");
        stand.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.WATER));stand.setItem(1,ItemStack.EMPTY);stand.setItem(3,new ItemStack(Items.NETHER_WART));
        for(int i=0;i<401;i++)BrewingStandBlockEntity.serverTick(level,pos,level.getBlockState(pos),stand);
        h.assertTrue(stand.getItem(0).get(DataComponents.POTION_CONTENTS).is(Potions.AWKWARD),"Vanilla brewing remains intact");h.succeed();
    }
}
