package darkspawn.black.client.mount;

import darkspawn.black.mountkit.client.GriffinRenderState;
import darkspawn.black.Darkspawn;
import darkspawn.black.mount.MagicCarpet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class MagicCarpetRenderer extends MobRenderer<MagicCarpet, GriffinRenderState, MagicCarpetModel> {
    public MagicCarpetRenderer(EntityRendererProvider.Context context) {
        super(context, new MagicCarpetModel(MagicCarpetModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Identifier.withDefaultNamespace("textures/block/purple_wool.png"); }
    @Override public void extractRenderState(MagicCarpet entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick);
    }
}
