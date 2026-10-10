package darkspawn.black.client.mixin;

import darkspawn.black.client.audio.EncounterMusic;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class EncounterMusicMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void darkspawn$encounterScore(CallbackInfo ci) {
		if (EncounterMusic.active()) ci.cancel();
	}
}
