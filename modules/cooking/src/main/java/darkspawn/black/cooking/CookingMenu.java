package darkspawn.black.cooking;

import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

public final class CookingMenu extends AbstractContainerMenu {
	public static final int INPUT_COUNT = 4;
	public static final int RESULT_SLOT = 4;
	private static final int INVENTORY_START = 5;
	private static final int HOTBAR_START = 32;
	private static final int INVENTORY_END = 41;
	private final ContainerLevelAccess access;
	private final Level level;
	private final Player owner;
	private final net.minecraft.world.inventory.ContainerData discoveryState = new net.minecraft.world.inventory.SimpleContainerData(1);
	private final ResultContainer result = new ResultContainer();
	private RecipeHolder<CookingRecipe> recipe;
	private boolean takingResult;
	private final Container ingredients = new SimpleContainer(INPUT_COUNT) {
		@Override public void setChanged() {
			super.setChanged();
			CookingMenu.this.slotsChanged(this);
		}
	};

	public CookingMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public CookingMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(Cooking.MENU, id);
		this.access = access;
		this.level = inventory.player.level();
		this.owner = inventory.player;
		addDataSlots(discoveryState);
		for (int i = 0; i < INPUT_COUNT; i++) {
			addSlot(new Slot(ingredients, i, 20 + i * 26, 36));
		}
		addSlot(new Slot(result, 0, 190, 36) {
			@Override public boolean mayPlace(ItemStack stack) { return false; }
			@Override public boolean isFake() { return true; }
			@Override public boolean mayPickup(Player player) {
				return hasItem() && (level.isClientSide() || recipe != null && canCook() && recipe.value().matches(input(), level));
			}
			@Override public void onTake(Player player, ItemStack stack) { takeResult(player, stack); }
		});
		addStandardInventorySlots(inventory, 34, 124);
	}

	private CraftingInput input() {
		return positionedInput().input();
	}

	private CraftingInput.Positioned positionedInput() {
		var stacks = new ArrayList<ItemStack>(INPUT_COUNT);
		for (int i = 0; i < INPUT_COUNT; i++) {
			stacks.add(ingredients.getItem(i));
		}
		return CraftingInput.ofPositioned(INPUT_COUNT, 1, stacks);
	}

	@Override
	public void slotsChanged(Container container) {
		if (!takingResult && level instanceof ServerLevel serverLevel) {
			CraftingInput input = input();
			recipe = serverLevel.recipeAccess().getRecipeFor(Cooking.RECIPE_TYPE, input, serverLevel).orElse(null);
			discoveryState.set(0, recipe != null && !canCook() ? 1 : 0);
			result.setItem(0, recipe == null || !canCook() ? ItemStack.EMPTY : recipe.value().assemble(input));
			broadcastChanges();
		}
	}

	private void takeResult(Player player, ItemStack stack) {
		if (!(level instanceof ServerLevel) || recipe == null || !canCook() || !recipe.value().matches(input(), level)) {
			return;
		}
		// Crafting Transaction: Consume once after the output is taken; defer previews until all remainders are returned.
		var positioned = positionedInput();
		var remaining = CraftingRecipe.defaultCraftingReminder(positioned.input());
		takingResult = true;
		try {
			stack.onCraftedBy(player, stack.getCount());
			for (int i = 0; i < remaining.size(); i++) {
				int slot = i + positioned.left();
				ingredients.removeItem(slot, 1);
				ItemStack remainder = remaining.get(i);
				if (remainder.isEmpty()) {
					continue;
				}
				if (ingredients.getItem(slot).isEmpty()) {
					ingredients.setItem(slot, remainder);
				} else if (!player.getInventory().add(remainder)) {
					player.drop(remainder, false, Prediction.PREDICTED);
				}
			}
		} finally {
			takingResult = false;
		}
		slotsChanged(ingredients);
	}
	private boolean canCook() { return recipe != null && Cuisine.canCook(owner, recipe.id().identifier().getPath()); }
	public boolean isRecipeLocked() { return discoveryState.get(0) != 0; }

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, Cooking.STATION);
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != result && super.canTakeItemForPickAll(stack, slot);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(index);
		if (!slot.hasItem() || !slot.mayPickup(player)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index <= RESULT_SLOT) {
			if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, index == RESULT_SLOT)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, INPUT_COUNT, false)) {
			if (index < HOTBAR_START ? !moveItemStackTo(stack, HOTBAR_START, INVENTORY_END, false)
					: !moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
				return ItemStack.EMPTY;
			}
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
		if (index == RESULT_SLOT) {
			player.drop(stack, false, Prediction.PREDICTED);
		}
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		// Station Lifecycle: Inputs belong to this open menu, so closing, disconnecting, or breaking the block returns them.
		result.clearContent();
		if (!level.isClientSide()) {
			takingResult = true;
			clearContainer(player, ingredients);
			takingResult = false;
			recipe = null;
		}
	}
}
