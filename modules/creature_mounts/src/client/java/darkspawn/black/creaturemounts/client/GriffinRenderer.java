package darkspawn.black.creaturemounts.client;

import darkspawn.black.mountkit.client.GriffinRenderState;

import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.creaturemounts.Griffin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class GriffinRenderer extends MobRenderer<Griffin, GriffinRenderState, GriffinModel> {
	public GriffinRenderer(EntityRendererProvider.Context context) {
		super(context, new GriffinModel(GriffinModel.createBodyLayer().bakeRoot()), .8F);
	}
	@Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
	@Override public Identifier getTextureLocation(GriffinRenderState state) { return DarkspawnCore.id("textures/entity/griffin.png"); }
	@Override public void extractRenderState(Griffin entity, GriffinRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.flying = entity.flying(); state.flightPose = entity.flightPose(partialTick);
	}
}
