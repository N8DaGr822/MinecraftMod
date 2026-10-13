package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.WoollyRhino;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class WoollyRhinoRenderer extends MobRenderer<WoollyRhino, GriffinRenderState, WoollyRhinoModel> {
    public WoollyRhinoRenderer(EntityRendererProvider.Context context) {
        super(context, new WoollyRhinoModel(WoollyRhinoModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/woolly_rhino.png"); }
    @Override public void extractRenderState(WoollyRhino entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick); state.attackPose = entity.attackPose(partialTick);
    }
}
