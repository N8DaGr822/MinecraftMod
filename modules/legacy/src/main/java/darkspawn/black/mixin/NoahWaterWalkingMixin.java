package darkspawn.black.mixin;

import darkspawn.black.legacy.NoahLegacy;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class NoahWaterWalkingMixin {
    @Inject(method="canStandOnFluid",at=@At("RETURN"),cancellable=true)
    private void darkspawn$wavewalking(FluidState fluid,CallbackInfoReturnable<Boolean> callback) {
        if((Object)this instanceof Player player&&!player.isSpectator()&&!player.isShiftKeyDown()&&!player.isUnderWater()
            &&fluid.is(FluidTags.WATER)&&NoahLegacy.wearing(player,ArmorType.BOOTS))callback.setReturnValue(true);
    }
}
