package darkspawn.black.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

public final class TreeSpiritHeartLayer extends RenderLayer<TreeSpiritRenderState, TreeSpiritModel> {
	private static final Identifier HEART = Identifier.withDefaultNamespace("textures/block/emerald_block.png");
	private final EntityModel<TreeSpiritRenderState> heart;

	public TreeSpiritHeartLayer(TreeSpiritRenderer renderer) {
		super(renderer);
		MeshDefinition mesh = new MeshDefinition();
		// Weak Point: Four visible faces mark the vulnerable trunk band, 8-14 blocks above the roots.
		mesh.getRoot().addOrReplaceChild("heartwood", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-12, -224, -30, 24, 96, 4).addBox(-12, -224, 26, 24, 96, 4)
				.addBox(-34, -224, -12, 4, 96, 24).addBox(30, -224, -12, 4, 96, 24),
				PartPose.offset(0, 24, 0));
		heart = new EntityModel<>(LayerDefinition.create(mesh, 256, 256).bakeRoot()) { };
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, TreeSpiritRenderState state, float yaw, float pitch) {
		if (!state.sapling && state.phase >= 2 && state.recovery > 0) {
			coloredCutoutModelCopyLayerRender(heart, HEART, pose, collector, 15728880, state, 0xFFFFFFFF, 1);
		}
	}
}
