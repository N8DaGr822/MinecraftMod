package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.Crocodile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class CrocodileRenderer extends MobRenderer<Crocodile, GriffinRenderState, CrocodileModel> {
    public CrocodileRenderer(EntityRendererProvider.Context context) {
        super(context, new CrocodileModel(CrocodileModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/crocodile.png"); }
    @Override public void extractRenderState(Crocodile entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick); state.attackPose = entity.attackPose(partialTick); state.swimming = entity.isInWater();
    }
}
