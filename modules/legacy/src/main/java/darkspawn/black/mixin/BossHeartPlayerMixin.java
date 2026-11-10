package darkspawn.black.mixin;

import com.mojang.serialization.Codec;
import darkspawn.black.health.BossHeartPlayer;
import darkspawn.black.health.BossHeartProgress;
import darkspawn.black.health.BossHearts;
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
public abstract class BossHeartPlayerMixin implements BossHeartPlayer {
	@Unique private final BossHeartProgress darkspawn$hearts = new BossHeartProgress();

	@Override
	public BossHeartProgress darkspawn$bossHearts() {
		return darkspawn$hearts;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$readHearts(ValueInput input, CallbackInfo callback) {
		darkspawn$hearts.restore(input.read("darkspawn_boss_hearts", Codec.STRING.listOf()).orElse(List.of()));
		BossHearts.apply((Player) (Object) this);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$saveHearts(ValueOutput output, CallbackInfo callback) {
		// Permanent Progress: Boss identity, not the item count, determines the player's earned capacity.
		output.store("darkspawn_boss_hearts", Codec.STRING.listOf(), darkspawn$hearts.savedBosses());
	}
}
