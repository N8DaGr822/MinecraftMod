package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.Seahorse;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class SeahorseRenderer extends MobRenderer<Seahorse, GriffinRenderState, SeahorseModel> {
    public SeahorseRenderer(EntityRendererProvider.Context context) {
        super(context, new SeahorseModel(SeahorseModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/seahorse.png"); }
    @Override public void extractRenderState(Seahorse entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick); state.swimming = entity.isInWater();
    }
}
