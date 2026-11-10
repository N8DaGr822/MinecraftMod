package darkspawn.black.culinarybrewing;

import darkspawn.black.core.DarkspawnCore;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

public final class CulinaryBrewingGameTest {
    private static final Map<String,String> REAGENTS=Map.of("herbal_restoration","wild_herbs","pepper_haste","jungle_pepper","dairy_guard","goat_milk",
        "truffle_vigor","woodland_truffle","marsh_breath","marsh_rice","frostfire_tonic","frost_garlic");

    private static BrewingStandBlockEntity stand(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlock(pos,Blocks.BREWING_STAND.defaultBlockState(),3);
        return (BrewingStandBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void brew(GameTestHelper h,BrewingStandBlockEntity stand){
        var level=h.getLevel();var pos=stand.getBlockPos();stand.setItem(4,new ItemStack(Items.BLAZE_POWDER));
        for(int i=0;i<401;i++)BrewingStandBlockEntity.serverTick(level,pos,level.getBlockState(pos),stand);
    }

    @GameTest public void familiesBrewOnlyWhenTheirReagentIsInstalled(GameTestHelper h){
        h.assertTrue(CulinaryBrewing.POTIONS.size()==16,"Six families register sixteen potions");
        var stand=stand(h);
        REAGENTS.forEach((family,reagent)->{
            var item=BuiltInRegistries.ITEM.getOptional(DarkspawnCore.id(reagent));
            stand.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.AWKWARD));stand.setItem(3,item.map(ItemStack::new).orElse(ItemStack.EMPTY));
            brew(h,stand);
            boolean brewed=stand.getItem(0).get(DataComponents.POTION_CONTENTS).is(CulinaryBrewing.POTIONS.get(family));
            h.assertTrue(brewed==item.isPresent(),family+" is brewable exactly when "+reagent+" is installed");
        });
        h.succeed();
    }

    @GameTest public void vanillaReagentsUpgradeCulinaryPotionsAndVanillaBrewingStaysIntact(GameTestHelper h){
        var stand=stand(h);
        stand.setItem(0,PotionContents.createItemStack(Items.POTION,CulinaryBrewing.POTIONS.get("herbal_restoration")));stand.setItem(3,new ItemStack(Items.REDSTONE));
        brew(h,stand);
        h.assertTrue(stand.getItem(0).get(DataComponents.POTION_CONTENTS).is(CulinaryBrewing.POTIONS.get("long_herbal_restoration")),"Redstone extends a culinary potion");
        stand.setItem(0,PotionContents.createItemStack(Items.POTION,CulinaryBrewing.POTIONS.get("pepper_haste")));stand.setItem(3,new ItemStack(Items.GUNPOWDER));
        brew(h,stand);
        h.assertTrue(stand.getItem(0).is(Items.SPLASH_POTION)&&stand.getItem(0).get(DataComponents.POTION_CONTENTS).is(CulinaryBrewing.POTIONS.get("pepper_haste")),"Gunpowder makes a splash culinary potion");
        stand.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.WATER));stand.setItem(3,new ItemStack(Items.NETHER_WART));
        brew(h,stand);
        h.assertTrue(stand.getItem(0).get(DataComponents.POTION_CONTENTS).is(Potions.AWKWARD),"Vanilla brewing remains intact");
        h.succeed();
    }
}
