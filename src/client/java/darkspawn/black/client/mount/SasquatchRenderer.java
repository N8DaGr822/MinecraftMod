package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.Sasquatch;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class SasquatchRenderer extends MobRenderer<Sasquatch, GriffinRenderState, SasquatchModel> {
    public SasquatchRenderer(EntityRendererProvider.Context context) {
        super(context, new SasquatchModel(SasquatchModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/sasquatch.png"); }
    @Override public void extractRenderState(Sasquatch entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying();
    }
}
