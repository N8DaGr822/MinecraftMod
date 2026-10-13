package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.Griffin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class GriffinRenderer extends MobRenderer<Griffin, GriffinRenderState, GriffinModel> {
	public GriffinRenderer(EntityRendererProvider.Context context) {
		super(context, new GriffinModel(GriffinModel.createBodyLayer().bakeRoot()), .8F);
	}
	@Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
	@Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/griffin.png"); }
	@Override public void extractRenderState(Griffin entity, GriffinRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.flying = entity.flying();
	}
}
