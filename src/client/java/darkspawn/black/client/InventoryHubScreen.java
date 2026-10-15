package darkspawn.black.client;

import darkspawn.black.inventory.*;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

/** Compact hub retains native item movement; supporting views are paged and never conceal interactive slots. */
public final class InventoryHubScreen extends Screen {
    private final Screen parent;
    private String tab="Inventory",query="";
    private int page,selected,detailPage,rows;
    private List<HubSnapshotPayload.Entry> serverEntries=List.of(),entries=List.of();
    private final List<Button> buttons=new ArrayList<>();
    private Button favorite,open,deposit,prev,next,details,glasses;
    public InventoryHubScreen(Screen parent){super(Component.literal("Expedition Hub"));this.parent=parent;}
    public void receive(HubSnapshotPayload payload){serverEntries=payload.entries();refresh();}
    private void request(int action,long target){ClientPlayNetworking.send(new HubRequestPayload(action,target,minecraft.player.inventoryMenu.getStateId()));}
    @Override protected void init(){
        rows=Math.max(2,(height-145)/22);buttons.clear();int tw=(width-16)/5;
        String[] tabs={"Inventory","Character","Companions","Storage","Journal"};
        for(int i=0;i<tabs.length;i++){String value=tabs[i];addRenderableWidget(UiTheme.button(Component.literal(value),b->{if(value.equals("Journal")){minecraft.gui.setScreen(new JournalScreen(this));return;}tab=value;page=selected=detailPage=0;refresh();}).bounds(8+i*tw,28,tw-2,20).build());}
        var search=addRenderableWidget(new EditBox(font,8,53,width-16,18,Component.literal("Search inventory or roster")));search.setMaxLength(64);search.setValue(query);search.setHint(Component.literal("Search items / companions / storage"));search.setResponder(value->{query=value;page=selected=detailPage=0;refresh();});
        int lw=Math.max(100,width/3);
        for(int i=0;i<rows;i++){int row=i;buttons.add(addRenderableWidget(UiTheme.button(Component.empty(),b->{selected=page*rows+row;detailPage=0;update();}).bounds(8,80+i*22,lw,20).build()));}
        prev=addRenderableWidget(UiTheme.button(Component.literal("<"),b->{page--;update();}).bounds(8,height-58,24,18).build());next=addRenderableWidget(UiTheme.button(Component.literal(">"),b->{page++;update();}).bounds(36,height-58,24,18).build());
        favorite=addRenderableWidget(UiTheme.button(Component.literal("Favorite"),b->{var e=current();if(e!=null){ClientPlayNetworking.send(new AccessoryActionPayload(Integer.parseInt(e.id()),minecraft.player.inventoryMenu.getStateId()));}}).bounds(8,height-36,72,20).build());
        open=addRenderableWidget(UiTheme.button(Component.literal("Open / Care"),b->{var e=current();if(e!=null&&e.discovered())request(tab.equals("Storage")?1:3,Long.parseLong(e.id()));}).bounds(84,height-36,84,20).build());
        deposit=addRenderableWidget(UiTheme.button(Component.literal("Deposit matching"),b->{var e=current();if(e!=null)request(2,Long.parseLong(e.id()));}).bounds(172,height-36,106,20).build());
        details=addRenderableWidget(UiTheme.button(Component.literal("More details"),b->{detailPage++;update();}).bounds(width-100,height-60,92,18).build());
        addRenderableWidget(UiTheme.button(Component.literal("Move items / Back"),b->onClose()).bounds(width-126,5,118,18).build());
        addRenderableWidget(UiTheme.button(Component.literal("Refresh"),b->{refresh();request(0,0);}).bounds(8,5,58,18).build());
        glasses=addRenderableWidget(UiTheme.button(Component.literal("Glasses on/off"),b->ClientPlayNetworking.send(new AccessoryActionPayload(-1,minecraft.player.inventoryMenu.getStateId()))).bounds(width-106,height-36,98,20).build());
        refresh();request(0,0);
    }
    private String itemDetails(ItemStack stack){
        var result=new StringBuilder(stack.getHoverName().getString()+"\nCount: "+stack.getCount()+" / "+stack.getMaxStackSize());
        if(stack.isDamageableItem())result.append("\nDurability: ").append(stack.getMaxDamage()-stack.getDamageValue()).append(" / ").append(stack.getMaxDamage());
        var equipment=stack.get(DataComponents.EQUIPPABLE);
        if(equipment!=null){var current=minecraft.player.getItemBySlot(equipment.slot());result.append("\nEquipped: ").append(current.getHoverName().getString());appendAttributes(result,"Selected",stack);appendAttributes(result,"Equipped",current);}
        else {appendAttributes(result,"Selected",stack);appendAttributes(result,"Main hand",minecraft.player.getMainHandItem());}
        var lore=stack.get(DataComponents.LORE);if(lore!=null)for(var line:lore.lines())result.append("\n").append(line.getString());
        var enchants=stack.get(DataComponents.ENCHANTMENTS);if(enchants!=null)for(var entry:enchants.entrySet())result.append("\n").append(entry.getKey().value().description().getString()).append(' ').append(entry.getIntValue());
        int accessory=Accessories.ITEMS.indexOf(stack.getItem());if(accessory>=0)result.append("\n").append(new String[]{"Toggleable night vision. No charge. N toggles by default.","Air drains half as often; does not stack with Deep Breath.","25% less fall damage; does not prevent lethal falls.","Collects eligible nearby drops within 3 blocks and clear sight.","Shows coordinates and bearing to a carried located compass target.","25 points of environmental heat protection; no fire immunity."}[accessory]);
        result.append("\nConditional armor/meal powers are separate; attribute amounts alone do not predict combat damage.");return result.toString();
    }
    private void appendAttributes(StringBuilder result,String label,ItemStack stack){var mods=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);if(mods==null)return;for(var e:mods.modifiers())result.append("\n").append(label).append(": ").append(Component.translatable(e.attribute().value().getDescriptionId()).getString()).append(' ').append(e.modifier().operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE?e.modifier().amount():e.modifier().amount()*100).append(e.modifier().operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE?" flat":e.modifier().operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE?"% of base":"% multiplier");}
    private void refresh(){
        var p=minecraft.player;if(p==null)return;var all=new ArrayList<HubSnapshotPayload.Entry>();
        if(tab.equals("Inventory")){for(int i=0;i<p.getInventory().getContainerSize();i++){if(i>=36&&i<43)continue;var stack=p.getInventory().getItem(i);if(stack.isEmpty())continue;all.add(new HubSnapshotPayload.Entry(Integer.toString(i),"inventory",(Accessories.state(p).favorite(i)?"* ":"")+stack.getHoverName().getString(),itemDetails(stack),true));}}
        else if(tab.equals("Character")){
            String text="Health: "+p.getHealth()+" / "+p.getMaxHealth()+"\nArmor: "+p.getArmorValue()+"\nAbsorption: "+p.getAbsorptionAmount()+"\nSpeed: "+p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)+"\nGlasses: "+(Accessories.vision(p)?"ON":"OFF / unequipped")+"\nFavorites protect sorting and hub deposits, not deliberate item movement.";
            for(var effect:p.getActiveEffects())text+="\n"+effect.getEffect().value().getDisplayName().getString()+" "+(effect.getAmplifier()+1)+": "+(effect.isInfiniteDuration()?"persistent":effect.getDuration()/20+"s");
            all.add(new HubSnapshotPayload.Entry("stats","character","Character and effects",text,true));
            for(int i=0;i<3;i++){var stack=Accessories.state(p).slots.getItem(i);all.add(new HubSnapshotPayload.Entry("accessory"+i,"character","Accessory "+(i+1),stack.isEmpty()?"Empty. Equip using the three slots left of the native inventory.":itemDetails(stack),true));}
        }else all.addAll(serverEntries.stream().filter(e->e.section().equals(tab.toLowerCase(Locale.ROOT))).toList());
        String q=query.toLowerCase(Locale.ROOT);entries=all.stream().filter(e->(e.title()+" "+e.body()).toLowerCase(Locale.ROOT).contains(q)).toList();selected=Math.clamp(selected,0,Math.max(0,entries.size()-1));update();
    }
    private HubSnapshotPayload.Entry current(){return entries.isEmpty()?null:entries.get(selected);}
    private List<net.minecraft.util.FormattedCharSequence> lines(){var e=current();return e==null?List.of():font.split(Component.literal(e.body()),Math.max(70,width-width/3-35));}
    private void update(){if(prev==null)return;page=Math.clamp(page,0,Math.max(0,(entries.size()-1)/rows));for(int i=0;i<buttons.size();i++){int n=page*rows+i;var b=buttons.get(i);b.visible=n<entries.size();if(b.visible){b.setMessage(Component.literal(font.plainSubstrByWidth(entries.get(n).title(),width/3-10)));b.active=n!=selected;}}
        prev.active=page>0;next.active=(page+1)*rows<entries.size();var e=current();favorite.visible=tab.equals("Inventory");favorite.active=favorite.visible&&e!=null&&AccessoryState.favoriteIndex(Integer.parseInt(e.id()))>=0;
        glasses.visible=tab.equals("Character");open.visible=tab.equals("Storage")||tab.equals("Companions");open.active=e!=null&&e.discovered();deposit.visible=tab.equals("Storage");deposit.active=e!=null;int count=Math.max(1,(lines().size()+detailRows()-1)/detailRows());detailPage=Math.floorMod(detailPage,count);details.active=count>1;
    }
    private int detailRows(){return Math.max(3,(height-150)/10);}
    @Override public void tick(){if(minecraft.player!=null&&minecraft.player.tickCount%20==0)refresh();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        UiTheme.panel(g,0,0,width,height);g.text(font,"Expedition Hub",75,10,UiTheme.INK,false);var e=current();var lines=lines();int from=detailPage*detailRows();for(int i=from;i<Math.min(lines.size(),from+detailRows());i++)g.text(font,lines.get(i),width/3+22,82+(i-from)*10,UiTheme.INK,false);
        if(e==null)g.text(font,tab.equals("Storage")?"No accessible storage nearby":"No matching entries",width/3+22,82,UiTheme.INK,false);
        int used=0;var p=minecraft.player;if(p!=null){for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if((slot<36||slot>=43)&&!p.getInventory().getItem(slot).isEmpty())used++;g.text(font,used+" / "+(36+InventoryExpansion.unlockedSlots(p.getInventory()))+" slots | Tier "+InventoryExpansion.tier(p.getInventory()),8,height-12,UiTheme.INK,false);}
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
