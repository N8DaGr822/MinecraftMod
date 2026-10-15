package darkspawn.black.client.pirate;

import darkspawn.black.client.UiTheme;
import darkspawn.black.pirate.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SeaScreen extends AbstractContainerScreen<SeaMenu> {
    private Button previousText,nextText,order;
    private int page,lastService=-1,lastReply=-1;
    public SeaScreen(SeaMenu menu,Inventory inventory,Component title){super(menu,inventory,title,312,232);inventoryLabelY=-1000;}
    private void send(int action){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    @Override protected void init(){
        super.init();
        previousText=addRenderableWidget(UiTheme.button(Component.literal("< Read"),b->page--).bounds(leftPos+8,topPos+118,66,18).build());
        nextText=addRenderableWidget(UiTheme.button(Component.literal("Read >"),b->page++).bounds(leftPos+238,topPos+118,66,18).build());
        addRenderableWidget(UiTheme.button(Component.literal("< Service"),b->send(0)).bounds(leftPos+8,topPos+184,144,18).build());
        addRenderableWidget(UiTheme.button(Component.literal("Service >"),b->send(1)).bounds(leftPos+160,topPos+184,144,18).build());
        order=addRenderableWidget(UiTheme.button(Component.literal("Order service"),b->send(2)).bounds(leftPos+8,topPos+207,296,18).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
        super.extractBackground(g,mouseX,mouseY,partial);UiTheme.panel(g,leftPos,topPos,imageWidth,imageHeight);
        int selected=Math.clamp(menu.status(8),0,SeaServices.NAMES.length-1),reply=Math.clamp(menu.status(9),0,SeaServices.Reply.values().length-1);
        if(selected!=lastService||reply!=lastReply){page=0;lastService=selected;lastReply=reply;}
        g.text(font,"Renown "+menu.status(0)+" | Infamy "+menu.status(1)+" | Seals "+Integer.bitCount(menu.status(2))+"/7",leftPos+8,topPos+24,UiTheme.INK,false);
        g.text(font,SeaServices.NAMES[selected],leftPos+8,topPos+39,UiTheme.INK,false);
        var lines=font.split(Component.literal(SeaServices.Reply.values()[reply].text+"\n\n"+SeaServices.DETAILS[selected]),296);
        int pages=Math.max(1,(lines.size()+5)/6);page=Math.clamp(page,0,pages-1);previousText.active=page>0;nextText.active=page+1<pages;
        for(int i=0;i<6&&page*6+i<lines.size();i++)g.text(font,lines.get(page*6+i),leftPos+8,topPos+54+i*10,UiTheme.INK,false);
        g.text(font,"Text "+(page+1)+"/"+pages,leftPos+130,topPos+123,UiTheme.INK,false);
        g.text(font,"Rescues: "+Integer.bitCount(menu.status(4))+" | Home port: "+(menu.status(11)<0?"none":"level "+menu.status(11)),leftPos+8,topPos+145,UiTheme.INK,false);
        g.text(font,menu.status(5)==1?"Your fort | Defenses "+menu.status(6):"Fort services require ownership",leftPos+8,topPos+157,UiTheme.INK,false);
        g.text(font,"Capture hold: "+menu.status(7)+"/60 seconds",leftPos+8,topPos+169,UiTheme.INK,false);
        order.setMessage(Component.literal(menu.status(10)!=0?"Confirm choice":selected>=15&&selected<=17?"Review ending choice":"Order service"));
    }
}
