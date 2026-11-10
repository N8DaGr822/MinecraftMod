package darkspawn.black.client.boss;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public final class TreeSpiritModel extends EntityModel<TreeSpiritRenderState> {
	private final ModelPart left;
	private final ModelPart right;
	private final ModelPart crown;

	public TreeSpiritModel(ModelPart root) {
		super(root);
		left = root.getChild("left_branch");
		right = root.getChild("right_branch");
		crown = root.getChild("crown");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		var root = mesh.getRoot();
		// Giant Silhouette: Geometry is 18 blocks tall; the small healing sapling uses the same shape.
		root.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-32, -192, -28, 64, 192, 56), PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("roots", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-72, -16, -16, 144, 16, 32).addBox(-16, -16, -72, 32, 16, 144),
				PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-40, -96, -24, 80, 96, 48)
				.addBox(-64, -88, -12, 24, 48, 24).addBox(40, -80, -12, 24, 40, 24),
				PartPose.offset(0, -168, 0));
		root.addOrReplaceChild("left_branch", CubeListBuilder.create().texOffs(0, 0)
				.addBox(0, -12, -14, 48, 24, 28).addBox(36, -60, -10, 16, 56, 20),
				PartPose.offset(28, -132, 0));
		root.addOrReplaceChild("right_branch", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-48, -12, -14, 48, 24, 28).addBox(-52, -64, -10, 16, 56, 20),
				PartPose.offset(-28, -132, 0));
		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(TreeSpiritRenderState state) {
		super.setupAnim(state);
		float sway = (float) Math.sin(state.ageInTicks * 0.035) * 0.06F;
		float charge = state.windup > 0 ? (40 - state.windup) / 40.0F : 0;
		left.zRot = -sway - charge * 0.5F;
		right.zRot = sway + charge * 0.5F;
		crown.xRot = state.recovery > 0 ? 0.12F : sway * 0.3F;
	}
}
