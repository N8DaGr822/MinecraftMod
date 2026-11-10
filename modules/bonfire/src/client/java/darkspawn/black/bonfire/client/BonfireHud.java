package darkspawn.black.bonfire.client;

import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.bonfire.BonfireBlock;
import darkspawn.black.bonfire.Bonfires;
import darkspawn.black.bonfire.WardVolume;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;

public final class BonfireHud {
	private BonfireHud() { }
	public static void initialize() {
		// Bonfire Help: Read synchronized block state so inspecting never toggles or pays for an upgrade.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, DarkspawnCore.id("bonfire_help"), (graphics, delta) -> {
			var client = Minecraft.getInstance();
			if (client.level == null || client.player == null || client.gui.screen() != null
				|| !(client.hitResult instanceof BlockHitResult hit)) return;
			var state = client.level.getBlockState(hit.getBlockPos());
			if (!state.is(Bonfires.CORE)) return;
			int tier = state.getValue(BonfireBlock.RANGE);
			var volume = WardVolume.forTier(hit.getBlockPos(), tier);
			var status = Component.translatable("hud.darkspawn.bonfire.phase." + state.getValue(BonfireBlock.PHASE));
			var title = Component.translatable("hud.darkspawn.bonfire.level", tier, status,
				volume.radius() * 2, volume.height());
			var upgrade = tier == 4 ? Component.translatable("hud.darkspawn.bonfire.max")
				: Component.translatable("hud.darkspawn.bonfire.upgrade", tier == 1 ? 8 : tier == 2 ? 4 : 2,
					(tier == 1 ? net.minecraft.world.item.Items.IRON_INGOT : tier == 2
						? net.minecraft.world.item.Items.GOLD_INGOT : net.minecraft.world.item.Items.DIAMOND).getDefaultInstance().getHoverName());
			var lines = java.util.List.of(title, Component.translatable("hud.darkspawn.bonfire.toggle"), upgrade);
			int y = 34;
			for (var message : lines) for (var line : client.font.split(message, graphics.guiWidth() - 16)) {
				int x = (graphics.guiWidth() - client.font.width(line)) / 2;
				graphics.fill(x - 3, y - 2, x + client.font.width(line) + 3, y + 10, 0xB014282E);
				graphics.text(client.font, line, x, y, 0xFFFFFFFF, false);
				y += 13;
			}
		});
	}
}
