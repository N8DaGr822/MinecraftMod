package darkspawn.black.inventory;

import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Equipment ownership stays separate from normal storage and from inactive OneBlock possessions. */
public final class AccessoryState {
    public final SimpleContainer slots = new SimpleContainer(3);
    public boolean glasses = true;
    public long favorites;
    public int activeMask;
    public boolean connected;
    public void restore(List<ItemStack> items) {
        for(int i=0;i<3;i++) slots.setItem(i,i<items.size()?items.get(i).copy():ItemStack.EMPTY);
    }
    public List<ItemStack> items(){return java.util.stream.IntStream.range(0,3).mapToObj(i->slots.getItem(i).copy()).toList();}
    public void read(ValueInput in){restore(in.read("darkspawn_accessories",ItemStack.OPTIONAL_CODEC.listOf(0,3)).orElse(List.of()));glasses=in.getBooleanOr("darkspawn_glasses",true);favorites=in.getLongOr("darkspawn_favorites",0);}
    public void write(ValueOutput out){out.store("darkspawn_accessories",ItemStack.OPTIONAL_CODEC.listOf(0,3),items());out.putBoolean("darkspawn_glasses",glasses);out.putLong("darkspawn_favorites",favorites);}
    public static int favoriteIndex(int slot){return slot>=9&&slot<36?slot-9:slot>=43&&slot<70?slot-16:-1;}
    public boolean favorite(int slot){int i=favoriteIndex(slot);return i>=0&&(favorites&(1L<<i))!=0;}
}
