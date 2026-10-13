package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.PoptartCat;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class PoptartCatRenderer extends MobRenderer<PoptartCat, GriffinRenderState, PoptartCatModel> {
    public PoptartCatRenderer(EntityRendererProvider.Context context) {
        super(context, new PoptartCatModel(PoptartCatModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/poptart_cat.png"); }
    @Override public void extractRenderState(PoptartCat entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying();
    }
}
