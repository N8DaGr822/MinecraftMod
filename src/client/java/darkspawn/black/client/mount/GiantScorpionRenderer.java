package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.GiantScorpion;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class GiantScorpionRenderer extends MobRenderer<GiantScorpion, GriffinRenderState, GiantScorpionModel> {
    public GiantScorpionRenderer(EntityRendererProvider.Context context) {
        super(context, new GiantScorpionModel(GiantScorpionModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/giant_scorpion.png"); }
    @Override public void extractRenderState(GiantScorpion entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick); state.attackPose = entity.attackPose(partialTick); state.swimming = entity.isInWater();
    }
}
