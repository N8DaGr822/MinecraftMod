package darkspawn.black.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Shared expedition UI: consistent panels, slot depth, focus and button contrast across mod screens. */
public final class UiTheme {
	public static final int INK = 0xFF243C42, PAPER = 0xFFE1E7DE, BORDER = 0xFF28464C, ACCENT = 0xFF65C8B5;
	private UiTheme() { }
	public static void panel(GuiGraphicsExtractor g, int x, int y, int width, int height) {
		g.fill(x + 3, y + 3, x + width + 3, y + height + 3, 0x70000000);
		g.fill(x, y, x + width, y + height, BORDER);
		g.fill(x + 2, y + 2, x + width - 2, y + height - 2, PAPER);
		g.fill(x + 2, y + 2, x + width - 2, y + 4, ACCENT);
		g.fill(x + 6, y + 18, x + width - 6, y + 19, 0xFFB3C7C0);
	}
	public static void slot(GuiGraphicsExtractor g, int x, int y, boolean special) {
		g.fill(x - 1, y - 1, x + 17, y + 17, special ? 0xFF438C82 : 0xFF566F73);
		g.fill(x, y, x + 16, y + 16, special ? 0xFF9FC9B9 : 0xFFB0C1B9);
		g.fill(x, y + 15, x + 16, y + 16, 0xFFF2F5EB);
	}
	public static Builder button(Component text, Button.OnPress action) { return new Builder(text, action); }
	public static final class Builder {
		private final Component text;
		private final Button.OnPress action;
		private int x, y, width = 150, height = 20;
		private Tooltip tooltip;
		private Builder(Component text, Button.OnPress action) { this.text = text; this.action = action; }
		public Builder bounds(int x, int y, int width, int height) { this.x=x; this.y=y; this.width=width; this.height=height; return this; }
		public Builder tooltip(Tooltip tooltip) { this.tooltip=tooltip; return this; }
		public Button build() {
			Button button = new Button(x,y,width,height,text,action, supplier -> supplier.get()) {
				@Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
					boolean focus = isHoveredOrFocused();
					g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(), active && focus ? ACCENT : BORDER);
					g.fill(getX()+1,getY()+1,getX()+getWidth()-1,getY()+getHeight()-1,active ? focus ? 0xFF355D60 : 0xFF243C42 : 0xFF657672);
					var font = Minecraft.getInstance().font;
					String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0,getWidth()-8));
					g.text(font,label,getX()+(getWidth()-font.width(label))/2,getY()+(getHeight()-8)/2,active ? 0xFFF1F5E7 : 0xFFCFD8CE,false);
				}
			};
			button.setTooltip(tooltip); return button;
		}
	}
}
