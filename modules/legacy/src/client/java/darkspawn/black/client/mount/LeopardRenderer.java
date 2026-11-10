package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.Leopard;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class LeopardRenderer extends MobRenderer<Leopard, GriffinRenderState, LeopardModel> {
    public LeopardRenderer(EntityRendererProvider.Context context) {
        super(context, new LeopardModel(LeopardModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/leopard.png"); }
    @Override public void extractRenderState(Leopard entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick); state.swimming = entity.isInWater();
    }
}
