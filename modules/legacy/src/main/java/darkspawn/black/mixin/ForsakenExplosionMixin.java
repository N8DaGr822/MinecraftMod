package darkspawn.black.mixin;

import darkspawn.black.forsaken.ForsakenThreats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerExplosion.class)
public abstract class ForsakenExplosionMixin {
	@Inject(method = "explode", at = @At("HEAD"))
	private void localDisruption(CallbackInfoReturnable<Integer> result) {
		var explosion = (ServerExplosion)(Object)this;
		ForsakenThreats.noise(explosion.level(), BlockPos.containing(explosion.center()), 15);
	}
}
