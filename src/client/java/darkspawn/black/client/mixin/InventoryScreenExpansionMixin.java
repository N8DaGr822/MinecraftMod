package darkspawn.black.client.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import darkspawn.black.client.ExpandedInventoryScreen;
import darkspawn.black.inventory.InventoryExpansion;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenExpansionMixin extends AbstractContainerScreen<InventoryMenu> {
	protected InventoryScreenExpansionMixin(InventoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void darkspawn$positionHotbar(CallbackInfo callback) {
		for (int slot = 36; slot < 45; slot++) {
			((SlotPositionAccessor) menu.getSlot(slot)).darkspawn$setY(imageHeight > 166 ? 196 : 142);
		}
	}

	@Inject(method = "getRecipeBookButtonPosition", at = @At("RETURN"), cancellable = true)
	private void darkspawn$positionRecipeButton(CallbackInfoReturnable<ScreenPosition> callback) {
		if (imageHeight > 166) {
			callback.setReturnValue(new ScreenPosition(leftPos + 104, topPos + 61));
		}
	}

	@Redirect(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private void darkspawn$drawExtraRows(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
			int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		if (imageHeight <= 166) {
			graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
			return;
		}
		darkspawn.black.client.UiTheme.panel(graphics, x, y, width, imageHeight);
		graphics.fill(x + 26, y + 7, x + 77, y + 78, 0xFF243C42);
		for (var slot : menu.slots) {
			darkspawn.black.client.UiTheme.slot(graphics, x + slot.x, y + slot.y, slot.index == InventoryExpansion.TRASH_MENU_SLOT);
			if (!slot.isActive()) graphics.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xBB566F73);
		}
		graphics.text(font, ">", x + 137, y + 32, darkspawn.black.client.UiTheme.INK, false);
		if (!menu.getSlot(InventoryExpansion.TRASH_MENU_SLOT).hasItem()) {
			int bx=x+156, by=y+65;
			graphics.fill(bx,by+3,bx+8,by+11,0xFF8C4848);
			graphics.fill(bx-1,by,bx+9,by+2,0xFF8C4848);
		}

	}

	@Redirect(method = { "init", "containerTick" }, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
	private boolean darkspawn$allowCreativeStorageScreen(LocalPlayer player) {
		return !((Object) this instanceof ExpandedInventoryScreen) && player.hasInfiniteMaterials();
	}
}
