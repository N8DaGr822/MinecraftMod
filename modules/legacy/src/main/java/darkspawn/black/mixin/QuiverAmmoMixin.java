package darkspawn.black.mixin;

import darkspawn.black.inventory.QuiverItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileWeaponItem.class)
public abstract class QuiverAmmoMixin {
    @Inject(method="useAmmo",at=@At("HEAD"),cancellable=true)
    private static void darkspawn$consumeQuiver(ItemStack weapon,ItemStack projectile,LivingEntity holder,boolean forceInfinite,CallbackInfoReturnable<ItemStack> callback){
        // Extra multishot projectiles remain native, free copies of the first arrow.
        if(forceInfinite||!(holder instanceof Player player))return;
        var used=QuiverItem.consume(player,weapon,projectile);if(used!=null)callback.setReturnValue(used);
    }
}
