package darkspawn.black.boss;

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

import darkspawn.black.enchantments.ThematicEnchantments;

/** Hunter's target tag holds no Darkspawn bosses: they keep native arrow damage. */
public final class BossHunterEnchantmentGameTest {
    @GameTest public void hunterExcludesBiomeBosses(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);var bow=new ItemStack(Items.BOW);
        bow.enchant(level.registryAccess().getOrThrow(ThematicEnchantments.HUNTER),2);
        var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,level);var damage=player.damageSources().arrow(arrow,player);
        var boss=new darkspawn.black.boss.BiomeBoss(darkspawn.black.boss.BossEntities.BIOME_BOSSES.get(darkspawn.black.boss.BossProfile.KRAKEN),level,darkspawn.black.boss.BossProfile.KRAKEN);
        h.assertTrue(EnchantmentHelper.modifyDamage(level,bow,boss,damage,5)==5,"Existing custom boss is excluded");
        h.succeed();
    }
}
