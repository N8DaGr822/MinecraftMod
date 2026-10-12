package darkspawn.black.forsaken;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class QuarantineMenu extends AbstractContainerMenu {
	public record Cost(Item item, int count) { }
	public static final List<Cost> GATE_COST = List.of(new Cost(Items.IRON_BLOCK, 2), new Cost(Items.COPPER_BLOCK.weathering().unaffected(), 2), new Cost(Items.QUARTZ, 4), new Cost(Items.REDSTONE_BLOCK, 1));
	public static final List<Cost> KEY_COST = List.of(new Cost(Items.ENDER_PEARL, 1), new Cost(Items.BLAZE_POWDER, 2), new Cost(Items.QUARTZ, 4), new Cost(Items.IRON_INGOT, 2));
	private final BlockPos terminal;
	private final net.minecraft.world.level.Level level;
	public QuarantineMenu(int id, Inventory inventory) { this(id, inventory, null); }
	public QuarantineMenu(int id, Inventory inventory, BlockPos terminal) {
		super(Forsaken.MENU, id); this.terminal = terminal; this.level = inventory.player.level();
		addStandardInventorySlots(inventory, 24, 112);
	}
	@Override public boolean stillValid(Player player) {
		if (!player.isAlive() || player.isSpectator() || player.level() != level) return false;
		if (terminal == null) return level.isClientSide();
		var block = level.getBlockState(terminal);
		return player instanceof ServerPlayer serverPlayer && Forsaken.state(serverPlayer).researched
			&& player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(terminal)) <= 64 && (block.is(Forsaken.ARCHIVE) || block.is(Forsaken.GATE));
	}
	@Override public boolean clickMenuButton(Player player, int action) {
		if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || !getCarried().isEmpty() || action < 0 || action > 1) return false;
		var costs = action == 0 ? GATE_COST : KEY_COST;
		var inventory = player.getInventory();
		// Fabrication Transaction: Validate every ingredient before consuming anything; each accepted click pays once.
		for (var cost : costs) {
			int available = 0;
			for (int i = 0; i < 36; i++) if (inventory.getItem(i).is(cost.item())) available += inventory.getItem(i).getCount();
			if (available < cost.count()) { Forsaken.message(serverPlayer, "materials"); return false; }
		}
		for (var cost : costs) {
			int remaining = cost.count();
			for (int i = 0; i < 36 && remaining > 0; i++) {
				var stack = inventory.getItem(i);
				if (stack.is(cost.item())) { int taken = Math.min(remaining, stack.getCount()); stack.shrink(taken); remaining -= taken; }
			}
		}
		setCarried(new ItemStack(action == 0 ? Forsaken.GATE_ITEM : Forsaken.KEY));
		inventory.setChanged(); broadcastChanges(); return true;
	}
	@Override public ItemStack quickMoveStack(Player player, int index) {
		if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
		var slot = slots.get(index); var stack = slot.getItem(); var original = stack.copy();
		if (stack.isEmpty() || !(index < 27 ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false))) return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		return original;
	}
}
