package darkspawn.black.mixin;

import darkspawn.black.oneblock.OneBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class OneBlockTravelMixin {
	@Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"), cancellable = true)
	private void darkspawn$guardProfiles(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> ci) {
		var player = (ServerPlayer) (Object) this;
		if (OneBlock.island(player.level()) != OneBlock.island(transition.newLevel()) && !OneBlock.state(player).travelling) {
			OneBlock.message(player, "use_menu"); ci.setReturnValue(null);
		}
	}
	@Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
	private void darkspawn$islandRespawn(boolean consume, TeleportTransition.PostTeleportTransition after, CallbackInfoReturnable<TeleportTransition> ci) {
		var player = (ServerPlayer) (Object) this;
		if (OneBlock.island(player.level())) ci.setReturnValue(new TeleportTransition(player.level(), OneBlock.arrival(player.level()), Vec3.ZERO, 0, 0, after));
	}
	@Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
	private void darkspawn$preserveOutsideBed(ServerPlayer.RespawnConfig config, boolean showMessage, CallbackInfo ci) {
		// Shared Respawn: Sleeping on the island must not replace the player's outside-world bed.
		if (showMessage && OneBlock.island(((ServerPlayer) (Object) this).level())) ci.cancel();
	}
}
