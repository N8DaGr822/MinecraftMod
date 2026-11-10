package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.WingedUnicorn;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class WingedUnicornRenderer extends MobRenderer<WingedUnicorn, GriffinRenderState, WingedUnicornModel> {
    public WingedUnicornRenderer(EntityRendererProvider.Context context) {
        super(context, new WingedUnicornModel(WingedUnicornModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/winged_unicorn.png"); }
    @Override public void extractRenderState(WingedUnicorn entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick);
    }
}
