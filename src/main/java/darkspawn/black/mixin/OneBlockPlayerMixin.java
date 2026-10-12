package darkspawn.black.mixin;

import darkspawn.black.oneblock.OneBlockPlayer;
import darkspawn.black.oneblock.OneBlockPlayerState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class OneBlockPlayerMixin implements OneBlockPlayer {
	@Unique private final OneBlockPlayerState darkspawn$oneBlock = new OneBlockPlayerState();
	@Override public OneBlockPlayerState darkspawn$oneBlock() { return darkspawn$oneBlock; }
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$readOneBlock(ValueInput input, CallbackInfo ci) { darkspawn$oneBlock.read(input); }
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$saveOneBlock(ValueOutput output, CallbackInfo ci) { darkspawn$oneBlock.write(output); }
}
