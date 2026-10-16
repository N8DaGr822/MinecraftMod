package darkspawn.black.mount;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MountMenu extends AbstractContainerMenu {
    public static final int FOLLOW = 0, STAY = 1, BREED = 2, NEXT_PLAYER = 3, OFFER = 4, ACCEPT = 5, CANCEL = 6;
    final PrototypeMount mount;
    private final Player viewer;
    private final SimpleContainerData data = new SimpleContainerData(7);
    private UUID selected;
    public MountMenu(int id, Inventory inventory) { this(id, inventory, null); }
    public MountMenu(int id, Inventory inventory, PrototypeMount mount) {
        super(MountCompanions.MENU, id); this.mount = mount; viewer = inventory.player;
        var cargo = mount == null ? new SimpleContainer(9) : mount.care.cargo;
        for (int i = 0; i < 9; i++) addSlot(new Slot(cargo, i, 17 + i * 18, 76) {
            @Override public boolean mayPlace(ItemStack stack) { return owner() && MountCompanions.cargoAllowed(stack); }
            @Override public boolean mayPickup(Player player) { return owner() && (mount == null || mount.care.accessible(player) && mount.isOwnedBy(player)); }
        });
        addStandardInventorySlots(inventory, 17, 160);
        addDataSlots(data); refresh();
    }
    public boolean owner() { return mount == null ? data.get(0) != 0 : mount.isOwnedBy(viewer); }
    public boolean following() { return data.get(1) != 0; }
    public boolean baby() { return data.get(2) != 0; }
    public boolean breeding() { return data.get(3) != 0; }
    public boolean pending() { return data.get(4) != 0; }
    public int selectedId() { return (data.get(5) & 65535) | (data.get(6) << 16); }
    private void refresh() {
        if (mount == null) return;
        data.set(0, mount.isOwnedBy(viewer) ? 1 : 0); data.set(1, mount.care.following ? 1 : 0);
        data.set(2, mount.isBaby() ? 1 : 0); data.set(3, mount.care.breeding() ? 1 : 0);
        data.set(4, mount.care.offerPending() ? 1 : 0);
        var player = selected == null ? null : mount.level().getPlayerByUUID(selected);
        int entityId = player == null ? -1 : player.getId(); data.set(5, entityId & 65535); data.set(6, entityId >>> 16);
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return mount == null || !(mount instanceof Thalassar) && mount.care.accessible(player) && !mount.isVehicle(); }
    @Override public boolean clickMenuButton(Player player, int action) {
        if (!(player instanceof ServerPlayer server) || mount == null || !stillValid(player) || !getCarried().isEmpty()) return false;
        boolean ok = false;
        if (action == ACCEPT) ok = mount.care.accept(server);
        else if (mount.isOwnedBy(player)) {
            if (action == FOLLOW || action == STAY) { mount.care.setFollowing(action == FOLLOW); ok = true; }
            else if (action == BREED) ok = mount.care.requestBreeding(server);
            else if (action == CANCEL) { mount.care.cancelOffer(); ok = true; }
            else if (action == NEXT_PLAYER) {
                var nearby = server.level().players().stream().filter(p -> p != player && p.isAlive() && !p.isSpectator() && p.distanceToSqr(mount) <= 64)
                    .sorted(Comparator.comparing(p -> p.getUUID().toString())).toList();
                int index = -1;
                for (int i = 0; i < nearby.size(); i++) if (nearby.get(i).getUUID().equals(selected)) index = i;
                selected = nearby.isEmpty() ? null : nearby.get((index + 1) % nearby.size()).getUUID(); ok = selected != null;
            } else if (action == OFFER) ok = mount.care.offer(server, selected == null ? null : server.level().getServer().getPlayerList().getPlayer(selected));
        }
        MountCompanions.message(player, ok ? "mount_care_updated" : "mount_care_failed");
        refresh(); broadcastChanges();
        return ok;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || !owner() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var source = slot.getItem(); var copy = source.copy();
        if (index < 9) { if (!moveItemStackTo(source, 9, slots.size(), true)) return ItemStack.EMPTY; }
        else if (!MountCompanions.cargoAllowed(source) || !moveItemStackTo(source, 0, 9, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, source); return copy;
    }
}
