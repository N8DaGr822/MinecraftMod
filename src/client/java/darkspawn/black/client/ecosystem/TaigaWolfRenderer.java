package darkspawn.black.client.ecosystem;

import com.mojang.blaze3d.vertex.PoseStack;
import darkspawn.black.ecosystem.TaigaWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class TaigaWolfRenderer extends MobRenderer<TaigaWolf, TaigaWolfRenderState, TaigaWolfModel> {
	private static final Identifier FUR = Identifier.withDefaultNamespace("textures/entity/wolf/wolf_angry.png");
	private final TaigaWolf.Kind kind;
	private final float scale;

	public TaigaWolfRenderer(EntityRendererProvider.Context context, TaigaWolf.Kind kind) {
		super(context, new TaigaWolfModel(TaigaWolfModel.createBodyLayer(kind).bakeRoot(), kind), kind.width * 0.5F);
		this.kind = kind;
		scale = kind == TaigaWolf.Kind.ALPHA ? 1.85F : kind == TaigaWolf.Kind.RAVAGED ? 1.5F : 1.3F;
	}

	@Override public TaigaWolfRenderState createRenderState() { return new TaigaWolfRenderState(); }
	@Override public Identifier getTextureLocation(TaigaWolfRenderState state) { return FUR; }
	@Override protected void scale(TaigaWolfRenderState state, PoseStack poseStack) { poseStack.scale(scale, scale, scale); }

	@Override
	public void extractRenderState(TaigaWolf entity, TaigaWolfRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.packed = entity.packed(); state.frenzy = entity.frenzy();
		state.windup = entity.windup(); state.leaping = entity.leaping();
	}

	@Override
	protected int getModelTint(TaigaWolfRenderState state) {
		return state.frenzy > 0 ? 0xFFFF9999 : state.windup > 0 ? 0xFFFFDD88
				: kind == TaigaWolf.Kind.FROSTFANG ? 0xFFAADDFF : kind == TaigaWolf.Kind.RAVAGED ? 0xFFAB8E7A
				: kind == TaigaWolf.Kind.ALPHA ? 0xFFC9B79B : state.packed ? 0xFFD8D3C6 : 0xFFFFFFFF;
	}
}
