package darkspawn.black.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import darkspawn.black.boss.AncientTreeSpirit;
import darkspawn.black.boss.HeartwoodSapling;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public final class TreeSpiritRenderer extends MobRenderer<Mob, TreeSpiritRenderState, TreeSpiritModel> {
	private static final Identifier BARK = Identifier.withDefaultNamespace("textures/block/oak_log.png");

	public TreeSpiritRenderer(EntityRendererProvider.Context context) {
		super(context, new TreeSpiritModel(TreeSpiritModel.createBodyLayer().bakeRoot()), 4);
		addLayer(new TreeSpiritHeartLayer(this));
	}

	@Override
	public TreeSpiritRenderState createRenderState() {
		return new TreeSpiritRenderState();
	}

	@Override
	public Identifier getTextureLocation(TreeSpiritRenderState state) {
		return BARK;
	}

	@Override
	public void extractRenderState(Mob entity, TreeSpiritRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.sapling = entity instanceof HeartwoodSapling;
		if (entity instanceof AncientTreeSpirit boss) {
			state.phase = boss.phase();
			state.windup = boss.windup();
			state.recovery = boss.recovery();
		}
	}

	@Override
	protected void scale(TreeSpiritRenderState state, PoseStack poseStack) {
		if (state.sapling) {
			poseStack.scale(0.16F, 0.16F, 0.16F);
		}
	}

	@Override
	protected int getModelTint(TreeSpiritRenderState state) {
		return state.sapling ? 0xFF88D070 : state.recovery > 0 ? 0xFFC5EB90 : state.phase == 3 ? 0xFFFFAA77 : 0xFFFFFFFF;
	}

	@Override
	protected float getShadowRadius(TreeSpiritRenderState state) {
		return state.sapling ? 0.6F : super.getShadowRadius(state);
	}
}
