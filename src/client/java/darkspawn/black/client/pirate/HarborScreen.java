package darkspawn.black.client.pirate;

import darkspawn.black.client.UiTheme;
import darkspawn.black.pirate.HarborMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class HarborScreen extends AbstractContainerScreen<HarborMenu> {
	private net.minecraft.client.gui.components.Button compassButton,claimButton,abandonButton;
    private final java.util.List<net.minecraft.client.gui.components.Button> cycleButtons=new java.util.ArrayList<>();
	public HarborScreen(HarborMenu menu,Inventory inventory,Component title) {super(menu,inventory,title,280,240);inventoryLabelY=-1000;}
	@Override protected void init() {
		super.init();cycleButtons.clear();
		String[] labels={"Where am I?","Who is in charge?","How do I sail?","Compass / replace","Accept contract","Claim reward","Abandon contract","Previous contract","Next contract","Join faction","Leave (-25 rep)","Amends (8 gold)"};
		for(int i=0;i<labels.length;i++){final int action=i;var button=addRenderableWidget(UiTheme.button(Component.literal(labels[i]),clicked->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action)).bounds(leftPos+8+(i%2)*134,topPos+128+(i/2)*18,130,17).build());if(i==3)compassButton=button;if(i==5)claimButton=button;if(i==6)abandonButton=button;if(i==7||i==8)cycleButtons.add(button);}
	}
	@Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partial) {
		super.extractBackground(graphics,mouseX,mouseY,partial);if(compassButton!=null)compassButton.active=menu.status(3)!=0;UiTheme.panel(graphics,leftPos,topPos,imageWidth,imageHeight);for(var button:cycleButtons)button.active=true;
        if(claimButton!=null)claimButton.setMessage(Component.literal(menu.status(4)==10?"Recruit Garrick":"Claim reward"));
        if(abandonButton!=null)abandonButton.setMessage(Component.literal(menu.status(4)==10&&menu.status(12)==3?"Free Garrick":"Abandon contract"));
		String[] text={
			menu.status(10)==1?"Outpost contact: cycle between this faction's first contract and repeat voyage, join at Trusted, or make amends. Ships and hired crew stay yours. Other factions are represented through Briggs and the harbor board.":"Welcome to Breaker's Rest. The return gate takes you home. Take a compass and choose your own voyage. The sea has enough rules without me inventing more.",
			"Standing: Corsairs "+menu.status(5)+", Iron Fleet "+menu.status(6)+", Drowned "+menu.status(7)+". Contracts earn independent relationships and crew offers. Membership: "+(menu.status(9)==0?"Independent":darkspawn.black.pirate.PirateProgression.Faction.values()[Math.clamp(menu.status(9)-1,0,2)].title)+". Choose a faction contract before joining or making amends.",
			"Use the southern shipyard: 105 oak planks and a helm build a starter ship. Sail seated, dock to edit. Ordinary boats work too. The cove lies east.",
			"Compass issued and calibrated. Follow it to Castaway Cove. Spend nine minutes there to chart another island. Your route survives a lost compass.",
			darkspawn.black.pirate.HarborGuide.CONTRACT_DETAILS[menu.status(4)]+(menu.status(4)<7?" Optional. One-time reward.":""),
			"","",
			"Already carrying a Logbound Compass? Keep it. Otherwise leave an empty inventory slot. Your first is free; a replacement costs one ordinary compass.",
			"Check your contract status below. Complete the selected objective first. Emerald rewards need an empty inventory slot; commendations do not. Repeat voyages have a ten-minute cooldown. Garrick needs a nearby docked ship and a free gunner berth.",
            "Diplomacy updated. One membership at a time. Joining is free at Trusted (25+); leaving costs 25 standing. Hired crew and ships remain yours. Amends cost 8 gold for +5 standing, up to 25.",
            "Select a faction contract first. Join requires Trusted standing and no current membership. Leave costs 25 standing with your current faction. Amends need 8 gold and standing below 25."};
		var lines=font.split(Component.literal(text[Math.clamp(menu.status(0),0,text.length-1)]),264);
		for(int i=0;i<Math.min(7,lines.size());i++)graphics.text(font,lines.get(i),leftPos+8,topPos+24+i*10,UiTheme.INK,false);
		String status=menu.status(1)==2?"Reward claimed":menu.status(1)==0?"Not accepted":menu.status(2)==1?"Ready to claim":"Objective pending";
        if(menu.status(11)>0)status="Available in "+menu.status(11)/60+":"+String.format(java.util.Locale.ROOT,"%02d",menu.status(11)%60);
        if(menu.status(4)==10)status=switch(menu.status(12)){case 1->"Find fort papers";case 2->"Speak to captive";case 3->"Recruit or free";case 4->"Freed";case 5->"Recruited";default->"Not accepted";};
		graphics.text(font,darkspawn.black.pirate.HarborGuide.CONTRACT_NAMES[menu.status(4)]+": "+status,leftPos+8,topPos+108,UiTheme.INK,false);
	}
}
