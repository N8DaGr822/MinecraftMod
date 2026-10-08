package darkspawn.black.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import darkspawn.black.boss.BossAttack;
import darkspawn.black.boss.BossHazard;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class BossHazardRenderer extends EntityRenderer<BossHazard, BossHazardRenderer.State> {
	private static final Identifier MYCELIUM = Identifier.withDefaultNamespace("textures/block/mycelium_top.png");
	private final EntityModel<State> patch;
	public static final class State extends EntityRenderState { public float radius; public boolean mycelium; }
	public BossHazardRenderer(EntityRendererProvider.Context context) {
		super(context);
		var mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("living_patch", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 0, -8, 16, 0.2F, 16), PartPose.ZERO);
		patch = new EntityModel<>(LayerDefinition.create(mesh, 32, 32).bakeRoot()) { };
	}
	@Override
	public State createRenderState() { return new State(); }
	@Override
	public void extractRenderState(BossHazard entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.radius = entity.radius();
		state.mycelium = entity.attack() == BossAttack.FUNGAL_GROWTH;
	}
	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.mycelium) {
			pose.pushPose();
			pose.translate(0, 0.025, 0);
			pose.scale(state.radius * 1.4F, 1, state.radius * 1.4F);
			collector.submitModel(patch, state, pose, patch.renderType(MYCELIUM), state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
			pose.popPose();
		}
		super.submit(state, pose, collector, camera);
	}
}
