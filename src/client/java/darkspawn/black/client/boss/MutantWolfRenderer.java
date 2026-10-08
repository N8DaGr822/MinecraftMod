package darkspawn.black.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import darkspawn.black.boss.FrostWolf;
import darkspawn.black.boss.MutantWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public final class MutantWolfRenderer extends MobRenderer<Mob, MutantWolfRenderState, MutantWolfModel> {
	private static final Identifier FUR = Identifier.withDefaultNamespace("textures/entity/wolf/wolf_angry.png");

	public MutantWolfRenderer(EntityRendererProvider.Context context) {
		super(context, new MutantWolfModel(MutantWolfModel.createBodyLayer().bakeRoot()), 3);
	}

	@Override
	public MutantWolfRenderState createRenderState() { return new MutantWolfRenderState(); }

	@Override
	public Identifier getTextureLocation(MutantWolfRenderState state) { return FUR; }

	@Override
	public void extractRenderState(Mob entity, MutantWolfRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.packMember = entity instanceof FrostWolf;
		if (entity instanceof MutantWolf boss) {
			state.phase = boss.phase();
			state.windup = boss.windup();
			state.recovery = boss.recovery();
			state.leaping = boss.leaping();
			state.variant = boss.variant();
		}
	}

	@Override
	protected void scale(MutantWolfRenderState state, PoseStack poseStack) {
		float size = state.packMember ? 0.9F : 6;
		poseStack.scale(size, size, size);
	}

	@Override
	protected int getModelTint(MutantWolfRenderState state) {
		return state.packMember ? 0xFFAADDFF : state.recovery > 0 ? 0xFFFFFFAA : state.phase == 3 ? 0xFFFF9999
				: state.variant == 1 ? 0xFF99DDFF : state.variant == 2 ? 0xFFC4AD94 : 0xFFFFFFFF;
	}

	@Override
	protected float getShadowRadius(MutantWolfRenderState state) {
		return state.packMember ? 0.45F : super.getShadowRadius(state);
	}
}
