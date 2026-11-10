package darkspawn.black.client;

import darkspawn.black.core.client.UiTheme;
import darkspawn.black.oneblock.*;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Expedition UI: Paged server-owned objectives and actions, with explicit confirmation for consequential changes. */
public final class AscensionScreen extends Screen {
    private final Screen parent;
    private AscensionGuideSnapshot snapshot;
    private List<AscensionGuideSnapshot.Entry> entries=List.of();
    private final List<Button> buttons=new ArrayList<>();
    private String section="Overview",input="",notice="";
    private int selected,page,detailPage,rows,left,cooldown,refreshTicks;
    private boolean waiting,confirming;
    private Button execute,previous,next,detailPrevious,detailNext;
    private EditBox argument;
    public AscensionScreen(Screen parent){super(Component.literal("OneBlock Ascension"));this.parent=parent;}
    public void receive(AscensionGuideSnapshot payload){snapshot=payload;if(!payload.notice().isEmpty()){notice=payload.notice();detailPage=0;}waiting=false;cooldown=6;refreshTicks=0;filter();}
    @Override protected void init(){
        left=Math.max(108,Math.min(175,width/3));rows=Math.max(2,(height-122)/22);buttons.clear();
        String[] sections={"Overview","Objectives","Rewards","Automation","Team"};int tabWidth=(width-16)/sections.length;
        for(int i=0;i<sections.length;i++){
            String tab=sections[i];addRenderableWidget(UiTheme.button(Component.literal(tab),b->{section=tab;selected=page=detailPage=0;confirming=false;notice="";filter();})
                .bounds(8+i*tabWidth,30,tabWidth-2,20).tooltip(Tooltip.create(Component.literal(tab))).build());
        }
        for(int i=0;i<rows;i++){int row=i;buttons.add(addRenderableWidget(UiTheme.button(Component.empty(),b->{selected=page*rows+row;detailPage=0;confirming=false;update();}).bounds(8,58+i*22,left,20).build()));}
        previous=addRenderableWidget(UiTheme.button(Component.literal("<"),b->{page--;update();}).bounds(8,height-58,28,20).build());
        next=addRenderableWidget(UiTheme.button(Component.literal(">"),b->{page++;update();}).bounds(40,height-58,28,20).build());
        addRenderableWidget(UiTheme.button(Component.literal("Back"),b->onClose()).bounds(8,height-28,left,20).build());
        int right=left+22;
        argument=addRenderableWidget(new EditBox(font,right,height-78,width-right-8,18,Component.literal("Team name or player")));
        argument.setMaxLength(32);argument.setValue(input);argument.setResponder(value->input=value);
        execute=addRenderableWidget(UiTheme.button(Component.literal("Select an action"),b->activate()).bounds(right,height-28,width-right-8,20).build());
        detailPrevious=addRenderableWidget(UiTheme.button(Component.literal("<"),b->{detailPage--;update();}).bounds(right,height-55,28,20).build());
        detailNext=addRenderableWidget(UiTheme.button(Component.literal(">"),b->{detailPage++;update();}).bounds(width-36,height-55,28,20).build());
        filter();if(snapshot==null)request("","");
    }
    private AscensionGuideSnapshot.Entry current(){return entries.isEmpty()?null:entries.get(selected);}
    private boolean inputAction(){var e=current();return e!=null&&(e.action().equals("create")||e.action().equals("invite"));}
    private void filter(){
        String old=current()==null?"":current().title();
        entries=snapshot==null?List.of():snapshot.entries().stream().filter(e->e.section().equals(section)).toList();
        selected=Math.clamp(selected,0,Math.max(0,entries.size()-1));
        if(current()==null||!current().title().equals(old))confirming=false;
        update();
    }
    private int detailRows(){return Math.max(2,(height-164)/10);}
    private List<net.minecraft.util.FormattedCharSequence> lines(){
        var entry=current();return entry==null?List.of():font.split(Component.literal((notice.isEmpty()?"":"Result: "+notice+"\n\n")+entry.title()+"\n\n"+entry.body()),width-left-38);
    }
    private void update(){
        if(previous==null)return;
        page=Math.clamp(page,0,Math.max(0,(entries.size()-1)/rows));
        for(int i=0;i<buttons.size();i++){
            var button=buttons.get(i);int index=page*rows+i;button.visible=index<entries.size();
            if(button.visible){var entry=entries.get(index);button.setMessage(Component.literal(entry.title()));button.setTooltip(Tooltip.create(Component.literal(entry.title())));button.active=index!=selected;}
        }
        previous.active=page>0;next.active=(page+1)*rows<entries.size();
        int pages=Math.max(1,(lines().size()+detailRows()-1)/detailRows());detailPage=Math.clamp(detailPage,0,pages-1);
        detailPrevious.active=detailPage>0;detailNext.active=detailPage+1<pages;
        var entry=current();argument.visible=inputAction();
        argument.setHint(Component.literal(entry!=null&&entry.action().equals("create")?"New team name":"Online player name"));
        execute.active=!waiting&&cooldown==0&&entry!=null&&!entry.action().isEmpty()&&(!inputAction()||!input.isBlank());
        execute.setMessage(Component.literal(waiting?"Updating...":confirming?"Confirm "+entry.action():entry==null||entry.action().isEmpty()?"Information":label(entry.action())));
    }
    private static String label(String action){return switch(action){case "next"->"Complete / Continue";case "submit"->"Submit held items";case "claim"->"Claim team reward";case "guardian"->"Start encounter";case "upgrade"->"Install upgrade";default->action.substring(0,1).toUpperCase(Locale.ROOT)+action.substring(1);};}
    private void activate(){
        var entry=current();if(entry==null||!execute.active)return;
        if(entry.confirm()&&!confirming){confirming=true;update();return;}
        confirming=false;notice="";request(entry.action(),inputAction()?input.strip():entry.argument());
    }
    private void request(String action,String value){
        waiting=true;refreshTicks=0;ClientPlayNetworking.send(new AscensionGuideRequest(snapshot==null?"":snapshot.token(),action,value));update();
    }
    @Override public void tick(){
        if(cooldown>0)cooldown--;refreshTicks++;
        // Late Replies: Controls own their current screen; refreshes never reopen a closed guide.
        if(!waiting&&!confirming&&refreshTicks>=40)request("","");
        update();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        g.fill(0,0,width,height,0xFF14282E);g.text(font,title,8,12,UiTheme.PAPER,false);
        int x=left+22;UiTheme.panel(g,x-4,56,width-x-4,height-140);
        var lines=lines();int from=detailPage*detailRows();
        if(snapshot==null)g.text(font,"Loading...",x+4,68,UiTheme.INK,false);
        else if(lines.isEmpty())g.text(font,"No entries yet.",x+4,68,UiTheme.INK,false);
        else for(int i=from;i<Math.min(lines.size(),from+detailRows());i++)g.text(font,lines.get(i),x+4,68+(i-from)*10,UiTheme.INK,false);
        g.text(font,(detailPage+1)+"/"+Math.max(1,(lines.size()+detailRows()-1)/detailRows()),x+34,height-49,UiTheme.PAPER,false);
        super.extractRenderState(g,mouseX,mouseY,delta);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
