package darkspawn.black.mixin;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.storage.loot.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(FishingHook.class)
public abstract class RegionalFishingMixin {
    @Redirect(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> catchFish(LootTable table,LootParams params){var hook=(FishingHook)(Object)this;return new it.unimi.dsi.fastutil.objects.ObjectArrayList<>(darkspawn.black.cooking.RegionalFishing.catches(hook.level(),hook.blockPosition(),table.getRandomItems(params)));}
}
