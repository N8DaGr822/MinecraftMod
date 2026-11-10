package darkspawn.black.noah.client;

import darkspawn.black.noah.NoahMod;

import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.noah.NoahPowerPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import com.mojang.blaze3d.platform.InputConstants;

public final class NoahClient implements net.fabricmc.api.ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRenderers.register(NoahMod.THALASSAR,ThalassarRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(context -> new darkspawn.black.core.client.ArmorGeometryRenderer(context, "noah", darkspawn.black.core.client.ArmorGeometryRenderer.IRON_TRIM, "noah_pulse"), darkspawn.black.noah.NoahLegacy.ARMOR.values().toArray(net.minecraft.world.level.ItemLike[]::new));
        EntityRenderers.register(net.minecraft.world.entity.EntityTypes.TRIDENT,TempestRenderer::new);
        var category=KeyMapping.Category.register(DarkspawnCore.id("noah"));
        var cast=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.noah_cast",InputConstants.Type.KEYBOARD,InputConstants.KEY_K,category));
        var ultimate=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.noah_ultimate",InputConstants.Type.KEYBOARD,InputConstants.KEY_B,category));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(cast.consumeClick())if(client.player!=null&&client.gui.screen()==null&&ClientPlayNetworking.canSend(NoahPowerPayload.TYPE))ClientPlayNetworking.send(new NoahPowerPayload(false));
            while(ultimate.consumeClick())if(client.player!=null&&client.gui.screen()==null&&ClientPlayNetworking.canSend(NoahPowerPayload.TYPE))ClientPlayNetworking.send(new NoahPowerPayload(true));
        });
    }
}
