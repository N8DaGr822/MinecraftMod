package darkspawn.black.client.pirate;

import darkspawn.black.client.UiTheme;
import darkspawn.black.pirate.ShipMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ShipScreen extends AbstractContainerScreen<ShipMenu> {
    private final java.util.List<net.minecraft.client.gui.components.Button> crewButtons=new java.util.ArrayList<>();
    private boolean management;
    private net.minecraft.client.gui.components.Button shantyButton,cargoPageButton;
    private net.minecraft.client.gui.components.EditBox shipName;
    private final java.util.List<net.minecraft.client.gui.components.Button> sailingButtons=new java.util.ArrayList<>(),managementButtons=new java.util.ArrayList<>();
    private final int[] shownCrewCosts={-1,-1,-1,-1};
    private static final String[] CREW_NAMES={"First mate","Repair worker","Cannoneer","Lookout"};
    private static final String[] CREW_ABILITIES={"Coordinates marked-target fire when ordered.","Uses hold planks to repair hull and reduce fires.","Operates one cannon per broadside.","Reports loaded ships and shores out to 192 blocks."};
	public ShipScreen(ShipMenu menu,Inventory inventory,Component title){super(menu,inventory,title,246,276);inventoryLabelX=43;inventoryLabelY=182;}
	@Override protected void init(){
		super.init();crewButtons.clear();sailingButtons.clear();managementButtons.clear();java.util.Arrays.fill(shownCrewCosts,-1);
		String[] labels={"Build ship","Launch","Dock","Expand","First mate","Repair worker","Cannoneer","Lookout","Fire / hold","Next target"};
		String[] hints={"105 oak planks + helm; clear water 20 blocks SOUTH of shipyard.","Capture permitted blocks and seat up to four players.","Stop in open water; restores editable blocks, aligned to the nearest compass direction.","Expand: 148 then 212 oak planks. Captain milestones unlock 11x23 then 15x31 platforms. See Journal: Factions and Captaincy.","Up to 12 emeralds at harbor; earned discounts apply. Coordinates marked-target fire when ordered.","Up to 12 emeralds at harbor; earned discounts apply. Uses oak planks from hold to repair hull and reduce fires.","Up to 8 emeralds at harbor; earned discounts apply. Each gunner operates one gun per broadside; captain can fire one alone.","Up to 12 emeralds at harbor; earned discounts apply. Reports loaded ships and shores out to 192 blocks.","First mate fires on selected target while sailing. Default is hold fire.","Cycle nearby NPC and player ships. Player ships can be damaged outside the harbor."};
		for(int i=0;i<labels.length;i++){final int action=i;var button=addRenderableWidget(UiTheme.button(Component.literal(labels[i]),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action)).bounds(leftPos+8+(i%3)*78,topPos+20+(i/3)*20,72,18).tooltip(Tooltip.create(Component.literal(hints[i]))).build());sailingButtons.add(button);if(i>=4&&i<=7)crewButtons.add(button);}
        shipName=addRenderableWidget(new net.minecraft.client.gui.components.EditBox(font,leftPos+8,topPos+20,146,18,Component.literal("Ship name")));shipName.setMaxLength(32);shipName.setHint(Component.literal("Ship name"));
        managementButtons.add(addRenderableWidget(UiTheme.button(Component.literal("Name ship"),b->net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new darkspawn.black.pirate.ShipIdentityPayload(menu.containerId,menu.getStateId(),shipName.getValue()))).bounds(leftPos+160,topPos+20,78,18).build()));
        int[] actions={ShipMenu.FLAG,ShipMenu.CARGO_UPGRADE,ShipMenu.RESUPPLY,ShipMenu.NAVIGATOR,ShipMenu.QUARTERMASTER,ShipMenu.SINGER,ShipMenu.SHANTY};
        String[] managementLabels={"Flag","Cargo +","Resupply","Hire Mara","Hire Bram","Hire Nell","Shanty on/off"};
        String[] managementHints={"Docked: toggle independent black or your membership's faction flag. Cosmetic; no disguise or diplomacy bonus.","Harbor cargo: Captain + 16 iron/4 emeralds for 36 slots; Veteran + 32 iron/8 emeralds for 54.","Bram transfers your oak planks and ammunition into available hold space while docked.","Navigator: complete Beacon Bearings at harbor; reports your compass route while sailing.","Quartermaster: complete Supply Ledger at harbor; enables safe supply transfers.","Shanty singer: recover her songbook from The Pale Gull and claim her harbor commission.","Nell sings four supplied shanties in rotation with a short rest. Records volume controls sound; toggle to stop."};
        for(int i=0;i<actions.length;i++){final int action=actions[i];var button=addRenderableWidget(UiTheme.button(Component.literal(managementLabels[i]),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action)).bounds(leftPos+8+(i%3)*78,topPos+40+(i/3)*20,72,18).tooltip(Tooltip.create(Component.literal(managementHints[i]))).build());managementButtons.add(button);if(action==ShipMenu.SHANTY)shantyButton=button;}
        addRenderableWidget(UiTheme.button(Component.literal("Refit / Crew"),b->{management=!management;showPage();}).bounds(leftPos+86,topPos+80,72,18).build());
        cargoPageButton=addRenderableWidget(UiTheme.button(Component.literal("Cargo page"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,ShipMenu.CARGO_PAGE)).bounds(leftPos+164,topPos+80,72,18).build());showPage();
    }
    private void showPage(){for(var button:sailingButtons)button.visible=!management;for(var button:managementButtons)button.visible=management;shipName.visible=management;if(!management)shipName.setFocused(false);}
	@Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partial){
		super.extractBackground(graphics,mouseX,mouseY,partial);UiTheme.panel(graphics,leftPos,topPos,imageWidth,imageHeight);
		if(shantyButton!=null){shantyButton.active=menu.status(20)>0;shantyButton.setMessage(Component.literal(menu.status(21)>0?"Stop shanty":"Play shanty"));}
        if(cargoPageButton!=null){cargoPageButton.active=menu.status(15)>27;cargoPageButton.setMessage(Component.literal("Hold "+(menu.status(16)+1)+"/"+(menu.status(15)>27?2:1)));}
		for(var slot:menu.slots)if(slot.isActive()){graphics.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xFF566F73);graphics.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xFFB0C1B9);}
        for(int role=0;role<crewButtons.size();role++)if(shownCrewCosts[role]!=menu.status(11+role)) {
            shownCrewCosts[role]=menu.status(11+role);
            crewButtons.get(role).setTooltip(Tooltip.create(Component.literal(CREW_NAMES[role]+": "+shownCrewCosts[role]+" emeralds at harbor. "+CREW_ABILITIES[role])));
        }
        graphics.text(font,management?"Hold "+menu.status(15)+" | Page "+(menu.status(16)+1)+" | "+new String[]{"Independent","Corsairs","Fleet","Drowned"}[Math.clamp(menu.status(17),0,3)]:"Hull "+menu.status(3)+" | Wins "+menu.status(9)+" | Crew "+menu.status(4)+"/"+menu.status(5)+"/"+menu.status(6)+"/"+menu.status(7),leftPos+8,topPos+105,0xFF223333,false);
	}
}
