package darkspawn.black.mixin;

import darkspawn.black.equipment.ArmorStyleBonuses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ArmorStyleAttackMixin {
	// Mining Prediction: Apply the same worn-set rule on both client and server, after native mining modifiers.
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void darkspawn$pickaxeSpeed(CallbackInfoReturnable<Float> callback) {
		Player player = (Player) (Object) this;
		if (player.getMainHandItem().is(net.minecraft.tags.ItemTags.PICKAXES) && ArmorStyleBonuses.fullStyle(player).equals("conquistador")) {
			callback.setReturnValue(callback.getReturnValue() * 1.10F);
		}
	}

	@Inject(method = "attack", at = @At("HEAD"))
	private void darkspawn$refreshAttack(CallbackInfo callback) {
		if ((Object) this instanceof ServerPlayer player) ArmorStyleBonuses.update(player);
	}
	@Inject(method = "stabAttack", at = @At("HEAD"))
	private void darkspawn$refreshSpear(CallbackInfoReturnable<Boolean> callback) {
		if ((Object) this instanceof ServerPlayer player) ArmorStyleBonuses.update(player);
	}
}
