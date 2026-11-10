package darkspawn.black.worldgen;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossItems;
import darkspawn.black.boss.BossProfile;
import darkspawn.black.ecosystem.RegionalEcosystems;
import darkspawn.black.ecosystem.RegionalKind;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

public final class ExplorationFindsGameTest {
    private static ItemStack craft(GameTestHelper h,int width,int height,net.minecraft.world.item.Item... items) {
        var input=CraftingInput.of(width,height,java.util.Arrays.stream(items).map(ItemStack::new).toList());
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input);
    }
    @GameTest public void reconstructionAndClueRecipesUseExistingBossIdentity(GameTestHelper h) {
        var fragment=ExplorationFinds.FOSSIL_FRAGMENT;var fossil=RegionalEcosystems.REWARDS.get(RegionalKind.TYRANT_SKULL);
        h.assertTrue(craft(h,3,2,fragment,fragment,fragment,fragment,Items.BONE_BLOCK,Items.AIR).is(fossil),"Four fragments reconstruct the existing fossil");
        h.assertTrue(craft(h,3,2,Items.BONE_BLOCK,fossil,Items.BONE_BLOCK,Items.SMOOTH_STONE_SLAB,Items.SMOOTH_STONE_SLAB,Items.SMOOTH_STONE_SLAB).is(ExplorationFinds.DISPLAY_ITEM),"Existing fossil assembles into the decorative display");
        h.assertTrue(craft(h,2,2,ExplorationFinds.OCEAN_NOTE,Items.COMPASS,Items.INK_SAC,Items.PRISMARINE_SHARD).is(ExplorationFinds.TIDE_CHART),"Ocean note decodes through native crafting");
        h.assertTrue(craft(h,3,1,ExplorationFinds.TIDE_CHART,Items.NAUTILUS_SHELL,Items.DRAGON_BREATH).is(BossItems.SUMMONS.get(BossProfile.KRAKEN)),"Clue route produces the existing Kraken ritual item");
        h.assertTrue(craft(h,3,3,Items.INK_SAC,Items.NAUTILUS_SHELL,Items.INK_SAC,Items.NAUTILUS_SHELL,Items.DRAGON_BREATH,Items.NAUTILUS_SHELL,Items.INK_SAC,Items.NAUTILUS_SHELL,Items.INK_SAC).is(BossItems.SUMMONS.get(BossProfile.KRAKEN)),"Original Kraken recipe remains available");
        h.assertTrue(craft(h,2,1,fossil,Items.DRAGON_BREATH).is(BossItems.SUMMONS.get(BossProfile.FOSSIL_TYRANT)),"Original fossil summon alternative remains available");
        h.succeed();
    }
    @GameTest public void archaeologyFindsAreObtainableWithoutVictoryRewards(GameTestHelper h) {
        var level=h.getLevel();var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
            .withParameter(LootContextParams.THIS_ENTITY,h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL))
            .withParameter(LootContextParams.TOOL,new ItemStack(Items.BRUSH)).create(LootContextParamSets.ARCHAEOLOGY);
        for(var site:List.of("fossil_altar","expedition_wreck")) {
            var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Darkspawn.id("archaeology/"+site)));
            boolean found=false;
            for(long seed=1;seed<=128;seed++) {
                // Loot Sampling: Spread deterministic seeds across the long range; adjacent seeds share first-roll bias.
                var loot=table.getRandomItems(params,seed*0x9E3779B97F4A7C15L);
                h.assertTrue(loot.size()==1&&loot.getFirst().getCount()==1,"Deposits retain one finite find");
                found|=loot.getFirst().is(site.equals("fossil_altar")?ExplorationFinds.FOSSIL_FRAGMENT:ExplorationFinds.OCEAN_NOTE);
                h.assertTrue(!BossItems.HEARTS.containsValue(loot.getFirst().getItem())&&!darkspawn.black.cooking.Cuisine.TROPHIES.containsValue(loot.getFirst().getItem()),"Finds never grant boss victory rewards");
            }
            h.assertTrue(found,"New find is reachable from real archaeology loot: "+site);
        }
        h.succeed();
    }
    @GameTest public void oceanFishingAddsRenewableNotesWithoutReplacingCatches(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,1,1));var biome=level.getBiome(pos).unwrapKey().orElseThrow();
        try {
            h.setBiome(Biomes.OCEAN);
            var fish=new ItemStack(Items.COD,2);var book=new ItemStack(Items.ENCHANTED_BOOK);var original=List.of(fish,book);boolean found=false;
            for(int i=0;i<2048;i++) {
                var result=ExplorationFinds.catches(level,pos,true,original);
                h.assertTrue(result.size()==2||result.size()==3,"At most one note per catch");
                h.assertTrue(ItemStack.matches(fish,result.get(0))&&ItemStack.matches(book,result.get(1)),"Original fish count and treasure survive unchanged");
                if(result.size()==3){found=true;h.assertTrue(result.get(2).is(ExplorationFinds.OCEAN_NOTE)&&result.get(2).getCount()==1,"Bonus is exactly one note");}
            }
            h.assertTrue(found&&original.size()==2&&fish.getCount()==2,"Renewable source produces notes without mutating input");
            var named=new ItemStack(Items.COD);named.set(DataComponents.CUSTOM_NAME,Component.literal("Custom fish"));
            for(int i=0;i<64;i++) {
                h.assertTrue(ExplorationFinds.catches(level,pos,false,original)==original,"Closed-water catches are unchanged");
                h.assertTrue(ExplorationFinds.catches(level,pos,true,List.of(book,named)).size()==2,"Treasure and custom fish cannot trigger the bonus");
                h.assertTrue(ExplorationFinds.catches(level.getServer().getLevel(darkspawn.black.pirate.Pirate.LEVEL),pos,true,original)==original,"Clues do not broaden fishing into custom dimensions");
            }
            h.setBiome(Biomes.PLAINS);
            for(int i=0;i<64;i++)h.assertTrue(ExplorationFinds.catches(level,pos,true,original)==original,"Inland fishing cannot produce ocean notes");
        } finally {h.setBiome(biome);}h.succeed();
    }
    @GameTest public void fossilDisplayDropsItselfAndIsNotBossTrophy(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));
        try {
            level.setBlock(pos,ExplorationFinds.DISPLAY.defaultBlockState(),2);
            var drops=Block.getDrops(level.getBlockState(pos),level,pos,null);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(ExplorationFinds.DISPLAY_ITEM)&&drops.getFirst().getCount()==1,"Breaking relocates exactly one reconstructed display");
            h.assertTrue(!darkspawn.black.cooking.Cuisine.TROPHIES.containsValue(ExplorationFinds.DISPLAY_ITEM),"Reconstruction is separate from earned boss trophies");
            h.assertTrue(new ItemStack(ExplorationFinds.TIDE_CHART).get(DataComponents.LORE)!=null,"New finds carry visible next-step guidance");
        } finally {level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}h.succeed();
    }
}
