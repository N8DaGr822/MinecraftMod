package darkspawn.black.client.pirate;

import darkspawn.black.client.UiTheme;
import darkspawn.black.pirate.ShipMenu;
import darkspawn.black.pirate.ShipIdentityPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Fits Minecraft's minimum 320x240 scaled viewport without shrinking text or slots. */
public final class ShipScreen extends AbstractContainerScreen<ShipMenu> {
    private record Order(Button button,int page,int action) { }
    private final List<Order> orders=new ArrayList<>();
    private final List<Button> tabs=new ArrayList<>();
    private int page;
    private EditBox name;
    private Button rename,cargoPage;
    public ShipScreen(ShipMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,ShipMenu.WIDTH,ShipMenu.HEIGHT);
        inventoryLabelX=8;inventoryLabelY=130;
    }
    private void order(int page,int row,int action,String label,String hint) {
        var button=addRenderableWidget(UiTheme.button(Component.literal(label),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action))
            .bounds(leftPos+180,topPos+44+row*21,124,19).tooltip(Tooltip.create(Component.literal(hint))).build());
        orders.add(new Order(button,page,action));
    }
    @Override protected void init() {
        super.init();orders.clear();tabs.clear();
        String[] labels={"Sail","Crew","Fit","Care"};
        for(int i=0;i<4;i++){final int selected=i;tabs.add(addRenderableWidget(UiTheme.button(Component.literal(labels[i]),b->{page=selected;update();}).bounds(leftPos+180+i*31,topPos+21,30,18).build()));}
        order(0,0,ShipMenu.BUILD,"Build ship","105 oak planks and one helm; clear loaded water 20 blocks south of the shipyard.");
        order(0,1,ShipMenu.LAUNCH,"Launch","Captures the permitted build and seats up to four players. Clear crew seat headroom.");
        order(0,2,ShipMenu.DOCK,"Dock","Stop and extinguish fires in open water. Restores blocks along the nearest cardinal direction.");
        order(0,3,ShipMenu.UPGRADE,"Expand platform","148 then 212 oak planks. Captain/Veteran milestones unlock larger platforms; see Journal.");
        order(0,4,ShipMenu.FIRE_ORDER,"Auto fire","First mate fires at your selected target. Default is hold fire.");
        order(0,5,ShipMenu.TARGET,"Next target","Cycle nearby vessels. Harbor protection and world PvP rules apply.");
        String[] crew={"First mate","Shipworker","Cannoneer","Lookout","Mara: navigator","Bram: supplies","Nell: singer"};
        int[] actions={ShipMenu.MATE,ShipMenu.WORKER,ShipMenu.GUNNER,ShipMenu.LOOKOUT,ShipMenu.NAVIGATOR,ShipMenu.QUARTERMASTER,ShipMenu.SINGER};
        for(int i=0;i<actions.length;i++)order(1,i,actions[i],crew[i],i<4?"Hire docked at harbor or your home drydock. Earned discounts apply; role limits remain.":"Complete this officer's harbor commission, then hire here. Rescue sunk officers through harbor services.");
        order(1,7,ShipMenu.SHANTY,"Play shanty","Requires Nell aboard. Records and Master volume control the four-song playlist.");
        name=addRenderableWidget(new EditBox(font,leftPos+180,topPos+44,124,18,Component.literal("Ship name")));
        name.setMaxLength(32);name.setHint(Component.literal("Ship name"));
        rename=addRenderableWidget(UiTheme.button(Component.literal("Name ship"),b->ClientPlayNetworking.send(new ShipIdentityPayload(menu.containerId,menu.getStateId(),name.getValue())))
            .bounds(leftPos+180,topPos+65,124,19).tooltip(Tooltip.create(Component.literal("Dock to rename. Blank clears the name; maximum 32 characters."))).build());
        order(2,2,ShipMenu.FLAG,"Change flag","Toggle independent black and your faction's flag. Cosmetic only.");
        order(2,3,ShipMenu.CARGO_UPGRADE,"Upgrade hold","Harbor/home drydock: Captain + 16 iron/4 emeralds for 36 slots; Veteran + 32 iron/8 emeralds for 54.");
        order(2,4,ShipMenu.RESUPPLY,"Bram: resupply","While docked, transfer your oak planks and ammunition into available hold space.");
        order(2,5,ShipMenu.STANDARD,"Standard class","16 iron. Restores standard speed and hull; retains damage fraction.");
        order(2,6,ShipMenu.CLIPPER,"Clipper class","Captain, expanded platform, harbor/home drydock; 16 iron + 8 emeralds. +25% speed, -15% hull. Preserves build and cargo.");
        order(2,7,ShipMenu.IRONCLAD,"Ironclad class","Captain, expanded platform, harbor/home drydock; 48 iron + 8 emeralds. +25% hull, -15% speed. Preserves build and cargo.");
        order(3,0,ShipMenu.WAGES,"Pay crew wages","Docked: 1 emerald per crew, 20 minutes coverage, +20 morale. No debt or desertion.");
        order(3,1,ShipMenu.PROVISIONS,"Crew provisions","Docked: 1 bread per two crew, 10 minutes coverage, +10 morale. Low morale slows reload and repairs; never removes crew.");
        order(3,3,ShipMenu.RAIDER,"Corsair Raider","Rank 3 Corsairs, 60 standing, 64 iron + 16 emeralds. +15% speed; 20% faster reload.");
        order(3,4,ShipMenu.BATTLESHIP,"Fleet Battleship","Rank 3 Fleet, 60 standing, 64 iron + 16 emeralds. +40% hull, +15% shot damage, -25% speed.");
        order(3,5,ShipMenu.GHOSTSHIP,"Drowned Ghostship","Rank 3 Drowned, 60 standing, 64 iron + 16 emeralds. +10% speed; fires last one-third as long.");
        order(3,6,ShipMenu.AMMUNITION,"Ammunition","Cycle Auto (fire then solid), Incendiary, Chain (6 damage, 10 seconds slow), Piercing (28 damage). Selected ammunition must be in the hold.");
        order(3,7,ShipMenu.EXPEDITION,"Shore expedition","Lead crew within 40 blocks of your docked ship. Nearby mate grants Resistance, worker Haste, lookout night vision. Recall before sailing.");
        order(3,2,ShipMenu.ROAM,"Crew shore leave","Crew walk on clear docked deck space; recall before sailing. Names, cargo and crew identities are retained. Keep their chunks loaded for recall.");
        cargoPage=addRenderableWidget(UiTheme.button(Component.literal("Hold page"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,ShipMenu.CARGO_PAGE)).bounds(leftPos+8,topPos+108,162,18).build());
        update();
    }
    private void update() {
        boolean exists=menu.hasShip(),docked=menu.status(1)!=0;
        for(int i=0;i<tabs.size();i++)tabs.get(i).active=page!=i;
        for(var order:orders){
            var b=order.button();int a=order.action();b.visible=page==order.page();
            b.active=switch(a){case ShipMenu.BUILD->!exists;case ShipMenu.DOCK->exists&&!docked;case ShipMenu.TARGET,ShipMenu.AMMUNITION->exists;case ShipMenu.FIRE_ORDER->exists&&menu.status(4)>0;case ShipMenu.SHANTY->exists&&menu.status(20)>0;default->exists&&docked;};
            if(a==ShipMenu.FIRE_ORDER)b.setMessage(Component.literal(menu.status(8)!=0?"Auto fire":"Hold fire"));
            if(a==ShipMenu.AMMUNITION)b.setMessage(Component.literal(new String[]{"Ammo: Auto","Ammo: Fire","Ammo: Chain","Ammo: Piercing"}[Math.clamp(menu.status(25),0,3)]));
            if(a==ShipMenu.EXPEDITION)b.setMessage(Component.literal(menu.status(26)!=0?"End expedition":"Shore expedition"));
            if(a==ShipMenu.ROAM)b.setMessage(Component.literal(menu.status(24)!=0?"Recall crew":"Crew shore leave"));
            if(a==ShipMenu.SHANTY)b.setMessage(Component.literal(menu.status(21)!=0?"Stop shanty":"Play shanty"));
            if(a>=ShipMenu.MATE&&a<=ShipMenu.LOOKOUT)b.setTooltip(Tooltip.create(Component.literal(menu.status(11+a-ShipMenu.MATE)+" emeralds; hire docked at harbor or your home drydock. Current crew in this role: "+menu.status(4+a-ShipMenu.MATE))));
        }
        name.visible=rename.visible=page==2;name.active=rename.active=exists&&docked;if(page!=2)name.setFocused(false);
        cargoPage.active=menu.status(15)>27;cargoPage.setMessage(Component.literal("Hold "+menu.status(15)+" | Page "+(menu.status(16)+1)+"/"+(menu.status(15)>27?2:1)));
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial) {
        super.extractBackground(g,mouseX,mouseY,partial);update();UiTheme.panel(g,leftPos,topPos,imageWidth,imageHeight);
        g.text(font,"Hull "+menu.status(3)+" | Morale "+menu.status(23),leftPos+8,topPos+24,UiTheme.INK,false);
        g.text(font,menu.hasShip()?(darkspawn.black.pirate.PirateShip.CLASS_NAMES[Math.clamp(menu.status(22),0,darkspawn.black.pirate.PirateShip.CLASS_NAMES.length-1)]+(menu.status(1)!=0?" | Docked":" | Sailing")):"Build at a shipyard",leftPos+8,topPos+36,UiTheme.INK,false);
        for(var slot:menu.slots)if(slot.isActive())UiTheme.slot(g,leftPos+slot.x,topPos+slot.y,false);
    }
}
