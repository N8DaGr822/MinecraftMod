package darkspawn.black.mixin;

import darkspawn.black.equipment.TemperaturePlayer;
import darkspawn.black.equipment.TemperatureState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class TemperaturePlayerMixin implements TemperaturePlayer {
	@Unique private final TemperatureState darkspawn$temperature = new TemperatureState();
	@Override public TemperatureState darkspawn$temperature() { return darkspawn$temperature; }
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$readTemperature(ValueInput input, CallbackInfo ci) {
		darkspawn$temperature.restore(input.getFloatOr("darkspawn_temperature", 0), input.getIntOr("darkspawn_wet_seconds", 0), input.getIntOr("darkspawn_temperature_danger", 0));
	}
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$saveTemperature(ValueOutput output, CallbackInfo ci) {
		// Exposure Persistence: Logging out pauses exposure instead of providing instant recovery.
		output.putFloat("darkspawn_temperature", darkspawn$temperature.exposure);
		output.putInt("darkspawn_wet_seconds", darkspawn$temperature.wetSeconds);
		output.putInt("darkspawn_temperature_danger", darkspawn$temperature.dangerSeconds);
	}
}
