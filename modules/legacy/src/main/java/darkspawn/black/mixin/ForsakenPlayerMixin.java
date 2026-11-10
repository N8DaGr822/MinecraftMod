package darkspawn.black.mixin;

import darkspawn.black.forsaken.ForsakenPlayer;
import darkspawn.black.forsaken.ForsakenPlayerState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class ForsakenPlayerMixin implements ForsakenPlayer {
	@Unique private final ForsakenPlayerState darkspawn$forsaken = new ForsakenPlayerState();
	@Override public ForsakenPlayerState darkspawn$forsaken() { return darkspawn$forsaken; }
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$readForsaken(ValueInput input, CallbackInfo ci) { darkspawn$forsaken.read(input); }
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$saveForsaken(ValueOutput output, CallbackInfo ci) { darkspawn$forsaken.write(output); }
}
