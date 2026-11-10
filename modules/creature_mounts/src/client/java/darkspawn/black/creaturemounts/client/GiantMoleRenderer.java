package darkspawn.black.creaturemounts.client;

import darkspawn.black.mountkit.client.GriffinRenderState;

import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.creaturemounts.GiantMole;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class GiantMoleRenderer extends MobRenderer<GiantMole, GriffinRenderState, GiantMoleModel> {
    public GiantMoleRenderer(EntityRendererProvider.Context context) {
        super(context, new GiantMoleModel(GiantMoleModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return DarkspawnCore.id("textures/entity/giant_mole.png"); }
    @Override public void extractRenderState(GiantMole entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick); state.swimming = entity.isInWater();
    }
}
