package darkspawn.black.equipment;

import darkspawn.black.Darkspawn;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

public final class ThematicEnchantmentsGameTest {
    @GameTest public void hunterUsesNativeArrowDamageAndExcludesBosses(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);var bow=new ItemStack(Items.BOW);
        bow.enchant(level.registryAccess().getOrThrow(ThematicEnchantments.HUNTER),2);
        var zombie=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,level);var damage=player.damageSources().arrow(arrow,player);
        h.assertTrue(EnchantmentHelper.modifyDamage(level,bow,zombie,damage,5)==7,"Hunter II adds two native damage points to eligible arrow hits");
        h.assertTrue(EnchantmentHelper.modifyDamage(level,bow,cow,damage,5)==5&&EnchantmentHelper.modifyDamage(level,bow,player,damage,5)==5,"No livestock or player bonus");
        var boss=new darkspawn.black.boss.BiomeBoss(darkspawn.black.boss.BossEntities.BIOME_BOSSES.get(darkspawn.black.boss.BossProfile.KRAKEN),level,darkspawn.black.boss.BossProfile.KRAKEN);
        h.assertTrue(EnchantmentHelper.modifyDamage(level,bow,boss,damage,5)==5,"Existing custom boss is excluded");
        h.assertTrue(EnchantmentHelper.modifyDamage(level,bow,zombie,player.damageSources().playerAttack(player),5)==5,"Hitting with the bow in melee does not activate Hunter");
        h.succeed();
    }
    @GameTest public void soulwardUsesWornChestAndStacksWithinNativeProtection(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);var chest=new ItemStack(Items.IRON_CHESTPLATE);
        chest.enchant(level.registryAccess().getOrThrow(ThematicEnchantments.SOULWARD),2);
        var zombie=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);var spider=EntityTypes.SPIDER.create(level,EntitySpawnReason.COMMAND);
        var damage=player.damageSources().mobAttack(zombie);
        player.setItemSlot(EquipmentSlot.MAINHAND,chest);
        h.assertTrue(EnchantmentHelper.getDamageProtection(level,player,damage)==0,"Carried chestplate grants nothing");
        player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.CHEST,chest);
        h.assertTrue(EnchantmentHelper.getDamageProtection(level,player,damage)==2,"Soulward II contributes two protection points");
        h.assertTrue(EnchantmentHelper.getDamageProtection(level,player,player.damageSources().mobAttack(spider))==0,"Non-undead damage is unaffected");
        chest.enchant(level.registryAccess().getOrThrow(Enchantments.PROTECTION),4);
        h.assertTrue(EnchantmentHelper.getDamageProtection(level,player,damage)==6,"Protection IV and Soulward II add within the native protection calculation");
        player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        h.assertTrue(EnchantmentHelper.getDamageProtection(level,player,damage)==0,"Unequipping removes the effect immediately");h.succeed();
    }
    @GameTest public void butcherAddsAtMostOneMeatAndRejectsBabiesAndProjectiles(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        var sword=new ItemStack(Items.IRON_SWORD);var enchanted=sword.copy();enchanted.enchant(level.registryAccess().getOrThrow(ThematicEnchantments.BUTCHER),2);
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withParameter(LootContextParams.THIS_ENTITY,cow)
            .withParameter(LootContextParams.DAMAGE_SOURCE,player.damageSources().playerAttack(player)).withParameter(LootContextParams.LAST_DAMAGE_PLAYER,player).create(LootContextParamSets.ENTITY);
        var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.Identifier.withDefaultNamespace("entities/cow")));boolean bonus=false;
        for(long seed=1;seed<=128;seed++){
            long mixed=seed*0x9E3779B97F4A7C15L;
            player.setItemSlot(EquipmentSlot.MAINHAND,sword);int base=table.getRandomItems(params,mixed).stream().filter(s->s.is(Items.BEEF)).mapToInt(ItemStack::getCount).sum();
            player.setItemSlot(EquipmentSlot.MAINHAND,enchanted);int total=table.getRandomItems(params,mixed).stream().filter(s->s.is(Items.BEEF)).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(total>=base&&total<=base+1,"Butcher preserves native loot and adds at most one meat");bonus|=total>base;
        }
        h.assertTrue(bonus,"Real cow loot reaches Butcher bonus");cow.setBaby(true);
        var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(java.util.Optional.empty());
        for(int i=0;i<128;i++)h.assertTrue(!new ThematicEnchantments.FoodBonus(false).test(context),"Baby livestock never qualify");
        cow.setBaby(false);var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,level);
        var ranged=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withParameter(LootContextParams.THIS_ENTITY,cow)
            .withParameter(LootContextParams.DAMAGE_SOURCE,player.damageSources().arrow(arrow,player)).create(LootContextParamSets.ENTITY);
        var rangedContext=new net.minecraft.world.level.storage.loot.LootContext.Builder(ranged).create(java.util.Optional.empty());
        for(int i=0;i<128;i++)h.assertTrue(!new ThematicEnchantments.FoodBonus(false).test(rangedContext),"Holding a Butcher weapon does not boost projectile kills");h.succeed();
    }
    @GameTest public void foragerRequiresMatureCropAndRejectsSilkTouch(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);var hoe=new ItemStack(Items.IRON_HOE);
        hoe.enchant(level.registryAccess().getOrThrow(ThematicEnchantments.FORAGER),2);var crop=(CropBlock)Blocks.WHEAT;
        var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.Identifier.withDefaultNamespace("blocks/wheat")));boolean bonus=false;
        for(int age:new int[]{0,7}){
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.BLOCK_STATE,crop.getStateForAge(age)).withParameter(LootContextParams.TOOL,hoe).withParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.BLOCK);
            for(long seed=1;seed<=128;seed++){
                int wheat=table.getRandomItems(params,seed*0x9E3779B97F4A7C15L).stream().filter(s->s.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum();
                h.assertTrue(age==0?wheat==0:wheat>=1&&wheat<=2,"Only mature crops gain at most one extra produce");bonus|=wheat==2;
            }
            hoe.enchant(level.registryAccess().getOrThrow(Enchantments.SILK_TOUCH),1);
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(java.util.Optional.empty());
            for(int i=0;i<128;i++)h.assertTrue(!new ThematicEnchantments.FoodBonus(true).test(context),"Silk Touch disables Forager even if combined by commands");
            hoe=new ItemStack(Items.IRON_HOE);hoe.enchant(level.registryAccess().getOrThrow(ThematicEnchantments.FORAGER),2);
        }
        h.assertTrue(bonus,"Actual wheat loot reaches Forager bonus");h.succeed();
    }
}
