package darkspawn.black.client.pirate;

import darkspawn.black.client.UiTheme;
import darkspawn.black.pirate.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class HarborScreen extends AbstractContainerScreen<HarborMenu> {
    private Button compass,claim,abandon,previousText,nextText;
    private int textPage,lastMessage=-1,lastContract=-1,lastFeedback=-1;
    public HarborScreen(HarborMenu menu,Inventory inventory,Component title){super(menu,inventory,title,312,232);inventoryLabelY=-1000;}
    @Override protected void init(){
        super.init();
        String[] labels={"Where am I?","Factions","Sailing help","Compass","Accept","Claim reward","Abandon","Previous","Next","Join faction","Leave faction","Amends","Harbor services"};
        String[] hints={"About Breaker's Rest","Standing and membership","Ship construction and navigation","First free; replacement costs an ordinary compass","Accept selected activity","Claim the completed objective","Abandon unfinished work; rescued Garrick can instead be freed","Previous contract","Next contract","Requires 25 standing; one faction at a time","Costs 25 standing with your current faction","8 gold for 5 standing, capped at 25","Trade, repairs, rumors, officer rescue and your sea legend"};
        for(int i=0;i<labels.length;i++){
            final int action=i;
            var b=addRenderableWidget(UiTheme.button(Component.literal(labels[i]),clicked->{textPage=0;minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);})
                .bounds(leftPos+8+i%3*100,topPos+132+i/3*18,96,17).tooltip(Tooltip.create(Component.literal(hints[i]))).build());
            if(i==3)compass=b;if(i==5)claim=b;if(i==6)abandon=b;
        }
        previousText=addRenderableWidget(UiTheme.button(Component.literal("< Read"),b->textPage--).bounds(leftPos+8,topPos+86,66,16).build());
        nextText=addRenderableWidget(UiTheme.button(Component.literal("Read >"),b->textPage++).bounds(leftPos+238,topPos+86,66,16).build());
    }
    private String dialogue(){
        int selected=menu.status(4);
        return switch(menu.status(0)){
            case 0 -> menu.status(10)!=0?"Outpost contact: use Previous/Next to choose this faction's introduction or repeat voyage. Join at Trusted or make amends. Ships and hired crew stay yours. Harbor services require returning to Breaker's Rest.":"Welcome to Breaker's Rest. The return gate takes you home. Take a compass and choose your own voyage. Harbor services offer supplies, repairs, rumors and officer rescue.";
            case 1 -> "Standing: Corsairs "+menu.status(5)+", Iron Fleet "+menu.status(6)+", Drowned "+menu.status(7)+". Membership: "+(menu.status(9)==0?"Independent":PirateProgression.Faction.values()[Math.clamp(menu.status(9)-1,0,2)].title)+". Select a faction contract to join or make amends.";
            case 2 -> "The southern shipyard builds a starter platform for 105 oak planks and one helm. Sail seated; dock to edit. Ordinary boats work too. Castaway Cove lies east. Ship menus have Sail, Crew and Refit tabs.";
            case 3 -> "Your compass is ready. Your saved route is preserved. Spend nine minutes on each marked island to chart the next; study pauses away from the island.";
            case 4 -> HarborGuide.CONTRACT_DETAILS[selected]+(selected<7?" Optional; one-time reward.":"");
            case 8 -> HarborFeedback.values()[Math.clamp(menu.status(13),1,HarborFeedback.values().length-1)].text;
            case 9 -> "Diplomacy updated. Hired crew and owned ships remain yours. Check your standing below and in the Journal.";
            case 10 -> "Select a faction contract. Join requires Trusted (25+) and no current membership. Leave costs 25 standing. Amends cost 8 gold and require standing below 25.";
            default -> "Select a contract or harbor service.";
        };
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
        super.extractBackground(g,mouseX,mouseY,partial);UiTheme.panel(g,leftPos,topPos,imageWidth,imageHeight);
        compass.active=menu.status(3)!=0;
        claim.setMessage(Component.literal(menu.status(4)==10?"Recruit Garrick":"Claim reward"));
        abandon.setMessage(Component.literal(menu.status(4)==10&&menu.status(12)==3?"Free Garrick":"Abandon"));
        if(lastMessage!=menu.status(0)||lastContract!=menu.status(4)||lastFeedback!=menu.status(13)){textPage=0;lastMessage=menu.status(0);lastContract=menu.status(4);lastFeedback=menu.status(13);}
        var lines=font.split(Component.literal(dialogue()),296);
        int pages=Math.max(1,(lines.size()+5)/6);textPage=Math.clamp(textPage,0,pages-1);
        previousText.active=textPage>0;nextText.active=textPage+1<pages;
        for(int i=0;i<6&&textPage*6+i<lines.size();i++)g.text(font,lines.get(textPage*6+i),leftPos+8,topPos+24+i*10,UiTheme.INK,false);
        g.text(font,"Text "+(textPage+1)+"/"+pages,leftPos+130,topPos+90,UiTheme.INK,false);
        String status=menu.status(1)==2?"Reward claimed":menu.status(1)==0?"Not accepted":menu.status(2)==1?"Ready to claim":"Objective pending";
        if(menu.status(11)>0)status="Available in "+menu.status(11)/60+":"+String.format(java.util.Locale.ROOT,"%02d",menu.status(11)%60);
        if(menu.status(4)==10)status=switch(menu.status(12)){case 1->"Find fort papers";case 2->"Speak to captive";case 3->"Recruit or free";case 4->"Freed";case 5->"Recruited";default->"Not accepted";};
        g.text(font,HarborGuide.CONTRACT_NAMES[menu.status(4)],leftPos+8,topPos+108,UiTheme.INK,false);
        g.text(font,status,leftPos+8,topPos+120,UiTheme.INK,false);
    }
}
