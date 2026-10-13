package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.RidingGolem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class RidingGolemRenderer extends MobRenderer<RidingGolem, GriffinRenderState, RidingGolemModel> {
    public RidingGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new RidingGolemModel(RidingGolemModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/riding_golem.png"); }
    @Override public void extractRenderState(RidingGolem entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick);
    }
}
