package darkspawn.black.groundwalker.client;

import darkspawn.black.mountkit.client.GriffinRenderState;
import darkspawn.black.groundwalker.GroundWalker;
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
