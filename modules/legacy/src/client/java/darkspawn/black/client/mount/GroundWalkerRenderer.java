package darkspawn.black.client.mount;

import darkspawn.black.mount.GroundWalker;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class GroundWalkerRenderer extends MobRenderer<GroundWalker, GriffinRenderState, GroundWalkerModel> {
    public GroundWalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new GroundWalkerModel(GroundWalkerModel.createBodyLayer().bakeRoot()), .8F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Identifier.withDefaultNamespace("textures/block/iron_block.png"); }
}
