package darkspawn.black.mixin;

import darkspawn.black.equipment.LegendaryArmorPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class LegendaryArmorPlayerMixin implements LegendaryArmorPlayer {
	@Unique private long darkspawn$phoenixReady;
	@Unique private long darkspawn$reaperReady;
	public long darkspawn$phoenixReady() { return darkspawn$phoenixReady; }
	public void darkspawn$phoenixReady(long time) { darkspawn$phoenixReady = time; }
	public long darkspawn$reaperReady() { return darkspawn$reaperReady; }
	public void darkspawn$reaperReady(long time) { darkspawn$reaperReady = time; }
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$readLegendary(ValueInput input, CallbackInfo ci) {
		darkspawn$phoenixReady = input.getLongOr("darkspawn_phoenix_ready", 0);
		darkspawn$reaperReady = input.getLongOr("darkspawn_reaper_ready", 0);
	}
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$saveLegendary(ValueOutput output, CallbackInfo ci) {
		// Persistent Cooldowns: Absolute overworld time prevents equipment swaps and reconnects from resetting abilities.
		output.putLong("darkspawn_phoenix_ready", darkspawn$phoenixReady);
		output.putLong("darkspawn_reaper_ready", darkspawn$reaperReady);
	}
}
