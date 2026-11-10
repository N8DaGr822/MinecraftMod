package darkspawn.black.mixin;

import darkspawn.black.pirate.PiratePlayer;
import darkspawn.black.pirate.PiratePlayerState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PiratePlayerMixin implements PiratePlayer {
	@Unique private final PiratePlayerState darkspawn$pirate = new PiratePlayerState();
	@Override public PiratePlayerState darkspawn$pirate() { return darkspawn$pirate; }
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$readPirate(ValueInput input, CallbackInfo ci) { darkspawn$pirate.read(input); }
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$savePirate(ValueOutput output, CallbackInfo ci) { darkspawn$pirate.write(output); }
}
