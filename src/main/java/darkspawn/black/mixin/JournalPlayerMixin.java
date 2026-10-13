package darkspawn.black.mixin;

import com.mojang.serialization.Codec;
import darkspawn.black.journal.JournalPlayer;
import darkspawn.black.journal.JournalProgress;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class JournalPlayerMixin implements JournalPlayer {
    @Unique private final JournalProgress darkspawn$journal = new JournalProgress();
    @Override public JournalProgress darkspawn$journal() { return darkspawn$journal; }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void read(ValueInput input, CallbackInfo ci) {
        darkspawn$journal.restore(input.read("darkspawn_journal", Codec.STRING.listOf()).orElse(java.util.List.of()));
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void write(ValueOutput output, CallbackInfo ci) { output.store("darkspawn_journal", Codec.STRING.listOf(), darkspawn$journal.saved()); }
}
