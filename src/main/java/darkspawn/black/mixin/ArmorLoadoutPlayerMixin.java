package darkspawn.black.mixin;

import darkspawn.black.inventory.ArmorLoadoutPlayer;
import darkspawn.black.inventory.ArmorLoadoutState;
import darkspawn.black.inventory.ArmorLoadouts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class ArmorLoadoutPlayerMixin implements ArmorLoadoutPlayer {
	@Unique private final ArmorLoadoutState darkspawn$loadouts = new ArmorLoadoutState();
	@Override public ArmorLoadoutState darkspawn$armorLoadouts() { return darkspawn$loadouts; }
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$readArmor(ValueInput input, CallbackInfo callback) { darkspawn$loadouts.read(input); }
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$saveArmor(ValueOutput output, CallbackInfo callback) { darkspawn$loadouts.write(output); }
	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void darkspawn$dropStoredArmor(ServerLevel level, CallbackInfo callback) {
		ArmorLoadouts.dropStored((Player) (Object) this, level);
	}
}
