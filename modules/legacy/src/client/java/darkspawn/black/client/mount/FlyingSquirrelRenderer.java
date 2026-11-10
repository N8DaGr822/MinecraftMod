package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.FlyingSquirrel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class FlyingSquirrelRenderer extends MobRenderer<FlyingSquirrel, GriffinRenderState, FlyingSquirrelModel> {
    public FlyingSquirrelRenderer(EntityRendererProvider.Context context) {
        super(context, new FlyingSquirrelModel(FlyingSquirrelModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/flying_squirrel.png"); }
    @Override public void extractRenderState(FlyingSquirrel entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick);
    }
}
