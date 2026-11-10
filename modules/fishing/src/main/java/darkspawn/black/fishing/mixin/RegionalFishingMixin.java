package darkspawn.black.fishing.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import darkspawn.black.fishing.RegionalFishing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
// Chains with other mods' catch modifiers (legacy's ocean notes run first at priority 900).
@Mixin(FishingHook.class)
public abstract class RegionalFishingMixin {
    @ModifyExpressionValue(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> darkspawn$regionalFish(ObjectArrayList<ItemStack> catches){
        var hook=(FishingHook)(Object)this;
        return new ObjectArrayList<>(RegionalFishing.catches(hook.level(),hook.blockPosition(),catches));
    }
}
