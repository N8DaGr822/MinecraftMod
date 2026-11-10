package darkspawn.black.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
// Priority 900: ocean notes inspect the native catch before darkspawn_fishing regionalizes cod and salmon.
@Mixin(value=FishingHook.class,priority=900)
public abstract class ExplorationFishingMixin {
    @ModifyExpressionValue(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> darkspawn$oceanNotes(ObjectArrayList<ItemStack> catches){
        var hook=(FishingHook)(Object)this;
        return new ObjectArrayList<>(darkspawn.black.worldgen.ExplorationFinds.catches(hook.level(),hook.blockPosition(),hook.isOpenWaterFishing(),catches));
    }
}
