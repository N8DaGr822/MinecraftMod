package darkspawn.black.client.boss;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public final class MutantWolfModel extends EntityModel<MutantWolfRenderState> {
	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart[] legs;

	public MutantWolfModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		tail = root.getChild("tail");
		legs = new ModelPart[] { root.getChild("right_front_leg"), root.getChild("left_front_leg"),
				root.getChild("right_hind_leg"), root.getChild("left_hind_leg") };
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = AdultWolfModel.createBodyLayer(CubeDeformation.NONE);
		var root = mesh.getRoot();
		// Mutant Silhouette: Keep the vanilla wolf's UVs, then add a raised mane and a jagged dorsal crest.
		root.addOrReplaceChild("mutated_mane", CubeListBuilder.create().texOffs(21, 0)
				.addBox(-5, 6, -6, 10, 8, 8), PartPose.ZERO);
		root.addOrReplaceChild("frost_crest", CubeListBuilder.create().texOffs(16, 14)
				.addBox(-1, 0, -4, 2, 7, 2).addBox(-1, 2, 0, 2, 6, 2).addBox(-1, 4, 4, 2, 7, 2), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(MutantWolfRenderState state) {
		super.setupAnim(state);
		head.xRot = state.xRot * (float) (Math.PI / 180) + (state.windup > 0 ? -0.35F : state.recovery > 0 ? 0.3F : 0);
		head.yRot = state.yRot * (float) (Math.PI / 180);
		for (int i = 0; i < legs.length; i++) {
			float offset = i == 0 || i == 3 ? 0 : (float) Math.PI;
			legs[i].xRot = state.leaping ? (i < 2 ? -0.8F : 0.8F)
					: (float) Math.cos(state.walkAnimationPos * 0.6662F + offset) * state.walkAnimationSpeed * 1.4F;
		}
		tail.yRot = (float) Math.sin(state.ageInTicks * 0.08) * 0.12F;
	}
}
