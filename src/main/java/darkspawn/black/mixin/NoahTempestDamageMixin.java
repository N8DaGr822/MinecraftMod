package darkspawn.black.mixin;

import darkspawn.black.legacy.NoahLegacy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class NoahTempestDamageMixin {
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true)
    private float darkspawn$tempestImpaling(float amount,ServerLevel level,DamageSource source,float original) {
        if(amount<=0||!(source.getEntity() instanceof ServerPlayer player)||((LivingEntity)(Object)this).getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_IMPALING))return amount;
        ItemStack weapon=source.is(DamageTypes.TRIDENT)&&source.getDirectEntity() instanceof ThrownTrident trident?trident.getWeaponItem()
            :source.is(DamageTypes.PLAYER_ATTACK)&&source.getDirectEntity()==player?player.getMainHandItem():ItemStack.EMPTY;
        // Mythic Impaling: Native vulnerable mobs already receive this enchantment bonus; never apply it twice.
        if(!weapon.is(NoahLegacy.TEMPEST)||!NoahLegacy.bound(weapon,player))return amount;
        return amount+2.5F*EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.IMPALING),weapon);
    }
}
