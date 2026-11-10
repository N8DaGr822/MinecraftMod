package darkspawn.black.client;

import darkspawn.black.Darkspawn;
import darkspawn.black.journal.JournalRequestPayload;
import darkspawn.black.journal.JournalSnapshotPayload;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Read-only server snapshot. Paging keeps long entries usable at minimum GUI sizes. */
public final class JournalScreen extends Screen {
    private static final String[] SECTIONS = {"overview","creatures","mounts","bosses","cooking","forsaken","landmarks"};
    private final Screen parent;
    private List<JournalSnapshotPayload.Entry> entries = List.of(), filtered = List.of();
    private String section = "overview", query = "";
    private int selected, listPage, detailPage, rows, listWidth, refreshTicks;
    private boolean requested, received;
    private Button previous, next, detailPrevious, detailNext, refresh;
    private final java.util.ArrayList<Button> entryButtons = new java.util.ArrayList<>();
    public JournalScreen(Screen parent) { super(Component.translatable("screen.darkspawn.journal")); this.parent = parent; }
    public void receive(JournalSnapshotPayload payload) { entries = payload.entries(); received = true; filter(); }
    @Override protected void init() {
        listWidth = Math.max(104, Math.min(170, width/3)); rows = Math.max(3,(height-132)/22); entryButtons.clear();
        int tabWidth=(width-16)/SECTIONS.length;
        for (int i=0;i<SECTIONS.length;i++) {
            String tab=SECTIONS[i];
            addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.translatable("journal.darkspawn."+tab), pressed -> { section=tab; selected=0; listPage=0; detailPage=0; filter(); })
                .bounds(8+i*tabWidth,32,tabWidth-2,20).tooltip(Tooltip.create(Component.translatable("journal.darkspawn."+tab))).build());
        }
        var search = addRenderableWidget(new EditBox(font,8,59,width-16,18,Component.translatable("journal.darkspawn.search")));
        search.setMaxLength(64); search.setValue(query); search.setHint(Component.translatable("journal.darkspawn.search"));
        search.setResponder(value -> { query=value; selected=0; listPage=0; detailPage=0; filter(); });
        for(int i=0;i<rows;i++) {
            final int row=i;
            entryButtons.add(addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.empty(), pressed -> { selected=listPage*rows+row; detailPage=0; update(); })
                .bounds(8,88+i*22,listWidth,20).build()));
        }
        previous=addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.literal("<"), b -> { listPage--; update(); }).bounds(8,height-48,28,18).build());
        next=addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.literal(">"), b -> { listPage++; update(); }).bounds(40,height-48,28,18).build());
        detailPrevious=addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.translatable("journal.darkspawn.previous"), b -> { detailPage--; update(); }).bounds(listWidth+24,height-28,70,20).build());
        detailNext=addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.translatable("journal.darkspawn.next"), b -> { detailPage++; update(); }).bounds(width-78,height-28,70,20).build());
        addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.translatable("gui.back"), b -> onClose()).bounds(8,height-26,listWidth,20).build());
        refresh=addRenderableWidget(darkspawn.black.client.UiTheme.button(Component.translatable("journal.darkspawn.refresh"), b -> request()).bounds(width-78,6,70,20).build());
        filter(); if (!requested) request();
    }
    private void request() { requested=true; refreshTicks=20; ClientPlayNetworking.send(new JournalRequestPayload()); }
    @Override public void tick() { if(refreshTicks>0)refreshTicks--; refresh.active=refreshTicks==0; }
    private void filter() {
        String needle=query.toLowerCase(Locale.ROOT);
        filtered=entries.stream().filter(entry -> entry.section().equals(section)
            && (entry.title().toLowerCase(Locale.ROOT).contains(needle) || entry.body().toLowerCase(Locale.ROOT).contains(needle))).toList();
        selected=Math.clamp(selected,0,Math.max(0,filtered.size()-1)); detailPage=0; update();
    }
    private JournalSnapshotPayload.Entry current() { return filtered.isEmpty() ? null : filtered.get(selected); }
    private List<net.minecraft.util.FormattedCharSequence> lines() {
        var entry=current(); return entry==null ? List.of() : font.split(Component.literal(entry.body()),Math.max(60,width-listWidth-40));
    }
    private int detailRows() { return Math.max(2,(height-190)/10); }
    private void update() {
        if(previous==null)return;
        listPage=Math.clamp(listPage,0,Math.max(0,(filtered.size()-1)/rows));
        for(int i=0;i<entryButtons.size();i++) {
            int index=listPage*rows+i; var button=entryButtons.get(i); button.visible=index<filtered.size();
            if(button.visible) {
                var entry=filtered.get(index); String name=(entry.discovered()?"":"? ")+entry.title();
                button.setMessage(Component.literal(font.plainSubstrByWidth(name,listWidth-10)));
                button.setTooltip(Tooltip.create(Component.literal(name+"\n"+entry.body().split("\n",2)[0]))); button.active=index!=selected;
            }
        }
        previous.active=listPage>0; next.active=(listPage+1)*rows<filtered.size();
        int pages=Math.max(1,(lines().size()+detailRows()-1)/detailRows()); detailPage=Math.clamp(detailPage,0,pages-1);
        detailPrevious.active=detailPage>0; detailNext.active=detailPage+1<pages;
        scheduleNarration();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        graphics.fill(0,0,width,height,0xFF14282E);
        UiTheme.panel(graphics,listWidth+18,82,width-listWidth-26,height-116);
        graphics.text(font,title,8,12,0xFFF3E8CA,false);
        var entry=current();
        if(entry!=null) {
            int x=listWidth+26;
            if(entry.id().startsWith("entity/")) {
                var texture=Darkspawn.id("textures/gui/journal/"+entry.id().substring(7)+(entry.discovered()?"":"_unknown")+".png");
                graphics.blit(RenderPipelines.GUI_TEXTURED,texture,x,88,0,0,56,56,56,56);
            } else {
                graphics.fill(x+6,94,x+43,132,entry.discovered()?0xFF756543:0xFF313A38);
                graphics.text(font,entry.discovered()?"J":"?",x+21,109,UiTheme.PAPER,false);
            }
            var heading=font.split(Component.literal(entry.title()),Math.max(40,width-x-76));
            for(int i=0;i<Math.min(4,heading.size());i++)graphics.text(font,heading.get(i),x+64,90+i*10,UiTheme.INK,false);
            graphics.text(font,entry.discovered()?"Discovered":"Habitat hint",x+64,134,0xFF42665D,false);
            var lines=lines();int from=detailPage*detailRows();
            for(int i=from;i<Math.min(lines.size(),from+detailRows());i++)graphics.text(font,lines.get(i),x,154+(i-from)*10,UiTheme.INK,false);
            graphics.text(font,(detailPage+1)+"/"+Math.max(1,(lines.size()+detailRows()-1)/detailRows()),width/2,height-43,UiTheme.PAPER,false);
        } else graphics.text(font,received?"No matching entries":"Loading journal...",listWidth+26,94,UiTheme.INK,false);
        long known=entries.stream().filter(e -> e.section().equals(section)&&e.discovered()).count();
        long total=entries.stream().filter(e -> e.section().equals(section)).count();
        graphics.text(font,known+"/"+total,76,height-43,UiTheme.PAPER,false);
        super.extractRenderState(graphics,mouseX,mouseY,delta);
    }
    @Override public Component getNarrationMessage() {
        var entry=current();
        return entry==null ? title : Component.literal(title.getString()+". "+entry.title()+". "+entry.body());
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
