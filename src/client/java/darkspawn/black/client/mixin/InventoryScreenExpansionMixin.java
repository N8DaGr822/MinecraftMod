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
		// Inventory Layout: Repeat vanilla slot-row artwork; keep the crafting area and hotbar intact.
		graphics.blit(pipeline, texture, x, y, 0, 0, width, 137, textureWidth, textureHeight);
		for (int row = 0; row < 3; row++) {
			graphics.blit(pipeline, texture, x, y + 137 + row * 18, 0, 83, width, 18, textureWidth, textureHeight);
			if (row >= InventoryExpansion.tier(minecraft.player.getInventory())) {
				graphics.fill(x + 7, y + 137 + row * 18, x + 169, y + 155 + row * 18, 0xAA555555);
				graphics.text(font, Component.translatable("container.darkspawn.locked_row." + (row + 1)),
						x + 12, y + 142 + row * 18, 0xFFFFFFFF, true);
			}
		}
		graphics.blit(pipeline, texture, x, y + 191, 0, 137, width, 29, textureWidth, textureHeight);
	}

	@Redirect(method = { "init", "containerTick" }, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
	private boolean darkspawn$allowCreativeStorageScreen(LocalPlayer player) {
		return !((Object) this instanceof ExpandedInventoryScreen) && player.hasInfiniteMaterials();
	}
}
