package darkspawn.black.client.ecosystem;

import darkspawn.black.ecosystem.ForestMob;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class ForestMobRenderer extends MobRenderer<ForestMob, ForestMobRenderState, ForestMobModel> {
	private final Identifier texture;

	public ForestMobRenderer(EntityRendererProvider.Context context, ForestMob.Kind kind) {
		super(context, new ForestMobModel(ForestMobModel.createBodyLayer(kind).bakeRoot(), kind), kind.width * 0.5F);
		texture = Identifier.withDefaultNamespace("textures/block/" + switch (kind) {
			case BARKLING -> "oak_log.png";
			case HOLLOWED -> "stripped_dark_oak_log.png";
			case ROOTCRAWLER -> "mangrove_roots_side.png";
			case ANCIENT_ENT -> "dark_oak_log.png";
		});
	}

	@Override public ForestMobRenderState createRenderState() { return new ForestMobRenderState(); }
	@Override public Identifier getTextureLocation(ForestMobRenderState state) { return texture; }

	@Override
	public void extractRenderState(ForestMob entity, ForestMobRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.hiding = entity.hiding();
		state.windup = entity.windup();
	}

	@Override
	protected int getModelTint(ForestMobRenderState state) { return state.windup > 0 ? 0xFFFFD080 : 0xFFFFFFFF; }
}
