package darkspawn.black.mixin;

import darkspawn.black.inventory.InventoryExpansion;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerInventoryDataMixin {
	@Shadow public abstract Inventory getInventory();

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	private void darkspawn$loadCapacity(ValueInput input, CallbackInfo callback) {
		InventoryExpansion.setTier(getInventory(), input.getIntOr("darkspawn_inventory_tier", 0));
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$saveCapacity(ValueOutput output, CallbackInfo callback) {
		// Persistence: Extra items use vanilla Inventory serialization; only the upgrade tier needs a new field.
		output.putInt("darkspawn_inventory_tier", InventoryExpansion.tier(getInventory()));
	}
}
