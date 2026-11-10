package darkspawn.black.quiver.mixin;

import darkspawn.black.quiver.QuiverItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class QuiverProjectileMixin {
    @Inject(method="getProjectile",at=@At("RETURN"),cancellable=true)
    private void darkspawn$quiverProjectile(ItemStack weapon,CallbackInfoReturnable<ItemStack> callback){
        var arrow=QuiverItem.projectile((Player)(Object)this,weapon);
        if(!arrow.isEmpty())callback.setReturnValue(arrow);
    }
}
