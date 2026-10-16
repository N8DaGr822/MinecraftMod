package darkspawn.black.client.legacy;

import darkspawn.black.Darkspawn;
import darkspawn.black.legacy.NoahPowerPayload;
import darkspawn.black.mount.Mounts;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import com.mojang.blaze3d.platform.InputConstants;

public final class NoahClient {
    private NoahClient() { }
    public static void initialize() {
        EntityRenderers.register(Mounts.THALASSAR,ThalassarRenderer::new);
        var category=KeyMapping.Category.register(Darkspawn.id("noah"));
        var cast=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.noah_cast",InputConstants.Type.KEYBOARD,InputConstants.KEY_K,category));
        var ultimate=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.noah_ultimate",InputConstants.Type.KEYBOARD,InputConstants.KEY_B,category));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(cast.consumeClick())if(client.player!=null&&client.gui.screen()==null&&ClientPlayNetworking.canSend(NoahPowerPayload.TYPE))ClientPlayNetworking.send(new NoahPowerPayload(false));
            while(ultimate.consumeClick())if(client.player!=null&&client.gui.screen()==null&&ClientPlayNetworking.canSend(NoahPowerPayload.TYPE))ClientPlayNetworking.send(new NoahPowerPayload(true));
        });
    }
}
