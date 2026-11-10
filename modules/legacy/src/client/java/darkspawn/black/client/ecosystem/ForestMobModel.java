package darkspawn.black.client.ecosystem;

import darkspawn.black.ecosystem.ForestMob;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public final class ForestMobModel extends EntityModel<ForestMobRenderState> {
	private final ModelPart body;
	private final ModelPart left;
	private final ModelPart right;
	private final ForestMob.Kind kind;

	public ForestMobModel(ModelPart root, ForestMob.Kind kind) {
		super(root);
		this.kind = kind;
		body = root.getChild("body");
		left = root.getChild("left");
		right = root.getChild("right");
	}

	public static LayerDefinition createBodyLayer(ForestMob.Kind kind) {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		var body = CubeListBuilder.create().texOffs(0, 0);
		var left = CubeListBuilder.create().texOffs(0, 0);
		var right = CubeListBuilder.create().texOffs(0, 0);
		// Forest Silhouettes: Sapling, hollow trunk, low roots, and broad crowned herald remain distinguishable.
		switch (kind) {
			case BARKLING -> {
				body.addBox(-3, -14, -3, 6, 12, 6).addBox(-5, -18, -4, 10, 5, 8);
				left.addBox(3, -12, -1, 5, 2, 2).addBox(6, -17, -1, 2, 5, 2);
				right.addBox(-8, -10, -1, 5, 2, 2).addBox(-8, -15, -1, 2, 5, 2);
			}
			case HOLLOWED -> {
				body.addBox(-6, -32, -4, 3, 24, 8).addBox(3, -32, -4, 3, 24, 8)
						.addBox(-3, -32, 2, 6, 24, 2).addBox(-6, -37, -4, 12, 5, 8);
				left.addBox(6, -28, -2, 4, 20, 4).addBox(1, -8, -3, 4, 8, 6);
				right.addBox(-10, -28, -2, 4, 20, 4).addBox(-5, -8, -3, 4, 8, 6);
			}
			case ROOTCRAWLER -> {
				body.addBox(-5, -8, -5, 10, 6, 10);
				left.addBox(3, -4, -9, 6, 3, 3).addBox(3, -3, 1, 7, 3, 3);
				right.addBox(-9, -4, -3, 6, 3, 3).addBox(-10, -3, 7, 7, 3, 3);
			}
			case ANCIENT_ENT -> {
				body.addBox(-10, -44, -8, 20, 40, 16).addBox(-14, -61, -9, 28, 17, 18)
						.addBox(-18, -7, -6, 36, 7, 12);
				left.addBox(10, -43, -4, 12, 7, 8).addBox(18, -55, -3, 4, 16, 6);
				right.addBox(-22, -43, -4, 12, 7, 8).addBox(-22, -55, -3, 4, 16, 6);
			}
		}
		root.addOrReplaceChild("body", body, PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("left", left, PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("right", right, PartPose.offset(0, 24, 0));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(ForestMobRenderState state) {
		super.setupAnim(state);
		float walk = state.hiding ? 0 : (float) Math.sin(state.walkAnimationPos * 0.7F) * state.walkAnimationSpeed * 0.12F;
		left.zRot = walk;
		right.zRot = -walk;
		if (state.windup > 0) {
			left.zRot = -0.15F;
			right.zRot = 0.15F;
			body.xRot = (float) Math.sin(state.ageInTicks * 0.6F) * 0.03F;
		}
		// Burrowing is a visual crouch only; collision and terrain stay unchanged.
		if (kind == ForestMob.Kind.ROOTCRAWLER && state.windup == 0) { body.y += 2; }
	}
}
