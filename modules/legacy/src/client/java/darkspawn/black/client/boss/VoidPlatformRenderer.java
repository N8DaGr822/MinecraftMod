package darkspawn.black.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import darkspawn.black.boss.VoidPlatform;
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

public final class VoidPlatformRenderer extends EntityRenderer<VoidPlatform, EntityRenderState> {
	private static final Identifier STONE = Identifier.withDefaultNamespace("textures/block/obsidian.png");
	private final EntityModel<EntityRenderState> platform;
	public VoidPlatformRenderer(EntityRendererProvider.Context context) {
		super(context);
		var mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("platform", CubeListBuilder.create().texOffs(0, 0).addBox(-64, 0, -64, 128, 16, 128), PartPose.ZERO);
		platform = new EntityModel<>(LayerDefinition.create(mesh, 256, 256).bakeRoot()) { };
	}
	@Override
	public EntityRenderState createRenderState() { return new EntityRenderState(); }
	@Override
	public void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		collector.submitModel(platform, state, pose, platform.renderType(STONE), state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
		super.submit(state, pose, collector, camera);
	}
}
