package darkspawn.black.mixin;

import darkspawn.black.inventory.ExpandedInventory;
import darkspawn.black.inventory.InventoryExpansion;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryExpansionMixin implements ExpandedInventory {
	@Shadow @Final private NonNullList<ItemStack> items;
	@Shadow @Final private EntityEquipment equipment;
	@Unique private int darkspawn$upgradeTier;

	@Override
	public int darkspawn$getUpgradeTier() {
		return darkspawn$upgradeTier;
	}

	@Override
	public void darkspawn$setUpgradeTier(int tier) {
		darkspawn$upgradeTier = Math.clamp(tier, 0, InventoryExpansion.MAX_TIER);
	}

	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/core/NonNullList;withSize(ILjava/lang/Object;)Lnet/minecraft/core/NonNullList;"), index = 0)
	private int darkspawn$allocateStorage(int originalSize) {
		return InventoryExpansion.STORAGE_SIZE;
	}

	@Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
	private void darkspawn$containerSize(CallbackInfoReturnable<Integer> callback) {
		callback.setReturnValue(InventoryExpansion.FIRST_EXTRA_SLOT + darkspawn$upgradeTier * 9);
	}

	@Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
	private void darkspawn$findUnlockedSlot(CallbackInfoReturnable<Integer> callback) {
		for (int slot = 0; slot < InventoryExpansion.FIRST_EXTRA_SLOT + darkspawn$upgradeTier * 9; slot++) {
			if (slot >= Inventory.INVENTORY_SIZE && slot < InventoryExpansion.FIRST_EXTRA_SLOT) {
				continue;
			}
			if (items.get(slot).isEmpty()) {
				callback.setReturnValue(slot);
				return;
			}
		}
		callback.setReturnValue(-1);
	}

	// Equipment Safety: The backing-list positions 36-42 stay empty; equipment retains vanilla ownership.
	@Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
	private void darkspawn$readEquipment(int slot, CallbackInfoReturnable<ItemStack> callback) {
		EquipmentSlot equipmentSlot = Inventory.EQUIPMENT_SLOT_MAPPING.get(slot);
		if (equipmentSlot != null) {
			callback.setReturnValue(equipment.get(equipmentSlot));
		}
	}

	@Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
	private void darkspawn$writeEquipment(int slot, ItemStack stack, CallbackInfo callback) {
		EquipmentSlot equipmentSlot = Inventory.EQUIPMENT_SLOT_MAPPING.get(slot);
		if (equipmentSlot != null) {
			equipment.set(equipmentSlot, stack);
			callback.cancel();
		}
	}

	@Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void darkspawn$removeEquipment(int slot, int count, CallbackInfoReturnable<ItemStack> callback) {
		EquipmentSlot equipmentSlot = Inventory.EQUIPMENT_SLOT_MAPPING.get(slot);
		if (equipmentSlot != null) {
			callback.setReturnValue(equipment.get(equipmentSlot).split(count));
		}
	}

	@Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
	private void darkspawn$takeEquipment(int slot, CallbackInfoReturnable<ItemStack> callback) {
		EquipmentSlot equipmentSlot = Inventory.EQUIPMENT_SLOT_MAPPING.get(slot);
		if (equipmentSlot != null) {
			callback.setReturnValue(equipment.set(equipmentSlot, ItemStack.EMPTY));
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack darkspawn$tickStorageOnly(Inventory inventory, int slot) {
		// Equipment already ticks separately; never tick reserved equipment indices a second time.
		return items.get(slot);
	}

	@Inject(method = "replaceWith", at = @At("HEAD"))
	private void darkspawn$copyCapacityBeforeItems(Inventory other, CallbackInfo callback) {
		darkspawn$setUpgradeTier(InventoryExpansion.tier(other));
	}
}
