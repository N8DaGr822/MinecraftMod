package darkspawn.black.mixin;

import com.mojang.serialization.Codec;
import darkspawn.black.cooking.CulinaryPlayer;
import darkspawn.black.cooking.CulinaryProgress;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class CulinaryPlayerMixin implements CulinaryPlayer {
	@Unique private final CulinaryProgress darkspawn$culinary = new CulinaryProgress();
	@Override public CulinaryProgress darkspawn$culinary() { return darkspawn$culinary; }
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void read(ValueInput input, CallbackInfo ci) {
		darkspawn$culinary.restore(input.read("darkspawn_recent_foods", Codec.STRING.listOf()).orElse(List.of()),
			input.read("darkspawn_eaten_meals", Codec.STRING.listOf()).orElse(List.of()),
			input.read("darkspawn_known_recipes", Codec.STRING.listOf()).orElse(List.of()));
	}
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void write(ValueOutput output, CallbackInfo ci) {
		output.store("darkspawn_recent_foods", Codec.STRING.listOf(), darkspawn$culinary.recent());
		output.store("darkspawn_eaten_meals", Codec.STRING.listOf(), darkspawn$culinary.meals());
		output.store("darkspawn_known_recipes", Codec.STRING.listOf(), darkspawn$culinary.recipes());
	}
}
