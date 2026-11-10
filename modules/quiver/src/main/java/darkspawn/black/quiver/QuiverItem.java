package darkspawn.black.quiver;

import darkspawn.black.core.DarkspawnCore;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Quiver Ammunition: Native bundle storage, with authoritative bow consumption after loose arrows. */
public final class QuiverItem extends BundleItem {
    public static Item ITEM;
    private record Ammo(ItemStack quiver,BundleContents contents,int index,ItemStack arrow) { }
    private QuiverItem(Properties properties){super(properties);}
    public static void register(){
        var key=ResourceKey.create(Registries.ITEM,DarkspawnCore.id("quiver"));
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new QuiverItem(new Properties().setId(key).stacksTo(1)
            .component(DataComponents.BUNDLE_CONTENTS,BundleContents.EMPTY)
            .component(DataComponents.LORE,new ItemLore(List.of(Component.translatable("tooltip.darkspawn.quiver.capacity"),
                Component.translatable("tooltip.darkspawn.quiver.feeding"),Component.translatable("tooltip.darkspawn.ingredient_pouch.controls"))))));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries->entries.accept(ITEM));
    }
    public static boolean accepts(ItemStack stack){
        return !stack.isEmpty()&&stack.getItem() instanceof ArrowItem&&stack.is(ItemTags.ARROWS)&&stack.getItem().canFitInsideContainerItems()
            &&!stack.has(DataComponents.BUNDLE_CONTENTS)&&!stack.has(DataComponents.CONTAINER);
    }
    @Override public boolean canFitInsideContainerItems(){return false;}
    private boolean reject(ItemStack other,ClickAction action,Player player){
        if(action!=ClickAction.PRIMARY||other.isEmpty()||accepts(other))return false;
        player.sendOverlayMessage(Component.translatable("message.darkspawn.quiver.rejected"));return true;
    }
    @Override public boolean overrideStackedOnOther(ItemStack self,Slot slot,ClickAction action,Player player){
        return reject(slot.getItem(),action,player)||super.overrideStackedOnOther(self,slot,action,player);
    }
    @Override public boolean overrideOtherStackedOnMe(ItemStack self,ItemStack other,Slot slot,ClickAction action,Player player,SlotAccess carried){
        return reject(other,action,player)||super.overrideOtherStackedOnMe(self,other,slot,action,player,carried);
    }
    private static Ammo find(Player player,ItemStack weapon){
        if(!(weapon.getItem() instanceof BowItem bow))return null;
        // Native Priority: Held and loose ammunition always wins over quiver contents.
        if(!ProjectileWeaponItem.getHeldProjectile(player,bow.getSupportedHeldProjectiles()).isEmpty())return null;
        var supported=bow.getAllSupportedProjectiles();var inventory=player.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++)if(supported.test(inventory.getItem(slot)))return null;
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            var quiver=inventory.getItem(slot);if(!quiver.is(ITEM))continue;
            var contents=quiver.getOrDefault(DataComponents.BUNDLE_CONTENTS,BundleContents.EMPTY);
            var arrows=contents.itemCopies().toList();int selected=contents.getSelectedItemIndex();
            if(selected>=0&&selected<arrows.size()&&accepts(arrows.get(selected))&&supported.test(arrows.get(selected)))
                return new Ammo(quiver,contents,selected,arrows.get(selected));
            for(int i=0;i<arrows.size();i++)if(accepts(arrows.get(i))&&supported.test(arrows.get(i)))return new Ammo(quiver,contents,i,arrows.get(i));
        }
        return null;
    }
    public static ItemStack projectile(Player player,ItemStack weapon){
        var ammo=find(player,weapon);return ammo==null?ItemStack.EMPTY:ammo.arrow();
    }
    /** Null delegates to native consumption; an empty stack deliberately refuses an unaffordable shot. */
    public static ItemStack consume(Player player,ItemStack weapon,ItemStack projectile){
        if(!(player.level() instanceof ServerLevel level))return null;
        var ammo=find(player,weapon);if(ammo==null||!ItemStack.isSameItemSameComponents(projectile,ammo.arrow()))return null;
        int count=player.hasInfiniteMaterials()?0:EnchantmentHelper.processAmmoUse(level,weapon,ammo.arrow(),1);
        if(count>ammo.arrow().getCount())return ItemStack.EMPTY;
        if(count==0){
            var free=ammo.arrow().copyWithCount(1);free.set(DataComponents.INTANGIBLE_PROJECTILE,Unit.INSTANCE);return free;
        }
        // Bundle Transaction: Replace immutable contents only after the native ammo cost is known.
        var arrows=new ArrayList<>(ammo.contents().itemCopies().toList());var selected=arrows.remove(ammo.index());var used=selected.split(count);
        // Selection Persistence: Bundle selection itself is transient; keep the fired stack first after synchronization.
        if(!selected.isEmpty())arrows.addFirst(selected);
        ammo.quiver().set(DataComponents.BUNDLE_CONTENTS,new BundleContents(arrows.stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();return used;
    }
}
