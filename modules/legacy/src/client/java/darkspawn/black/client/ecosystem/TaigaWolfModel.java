package darkspawn.black.client.ecosystem;

import darkspawn.black.ecosystem.TaigaWolf;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public final class TaigaWolfModel extends EntityModel<TaigaWolfRenderState> {
	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart[] legs;
	private final TaigaWolf.Kind kind;

	public TaigaWolfModel(ModelPart root, TaigaWolf.Kind kind) {
		super(root);
		this.kind = kind;
		head = root.getChild("head");
		tail = root.getChild("tail");
		legs = new ModelPart[] { root.getChild("right_front_leg"), root.getChild("left_front_leg"),
				root.getChild("right_hind_leg"), root.getChild("left_hind_leg") };
	}

	public static LayerDefinition createBodyLayer(TaigaWolf.Kind kind) {
		var mesh = AdultWolfModel.createBodyLayer(CubeDeformation.NONE);
		var root = mesh.getRoot();
		if (kind == TaigaWolf.Kind.RAVAGED || kind == TaigaWolf.Kind.ALPHA) {
			root.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(21, 0)
					.addBox(-4, 7, -5, 8, 7, 7), PartPose.ZERO);
		}
		if (kind == TaigaWolf.Kind.FROSTFANG || kind == TaigaWolf.Kind.ALPHA) {
			root.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(16, 14)
					.addBox(-1, 5, -4, 2, 5, 2).addBox(-1, 7, 0, 2, 4, 2), PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(TaigaWolfRenderState state) {
		super.setupAnim(state);
		head.xRot = state.xRot * (float) (Math.PI / 180) + (state.windup > 0 ? -0.35F : 0);
		head.yRot = state.yRot * (float) (Math.PI / 180);
		for (int i = 0; i < legs.length; i++) {
			float offset = i == 0 || i == 3 ? 0 : (float) Math.PI;
			legs[i].xRot = state.leaping ? (i < 2 ? -0.8F : 0.8F)
					: (float) Math.cos(state.walkAnimationPos * 0.6662F + offset) * state.walkAnimationSpeed * 1.4F;
		}
		// Mutation Silhouette: Unequal enlarged forelimbs distinguish Ravaged Wolves from normal packs.
		if (kind == TaigaWolf.Kind.RAVAGED) {
			legs[0].xScale = 1.8F; legs[0].zScale = 1.8F;
			legs[1].xScale = 1.4F; legs[1].zScale = 1.4F;
		}
		tail.yRot = (float) Math.sin(state.ageInTicks * 0.08) * (state.frenzy > 0 ? 0.3F : 0.12F);
	}
}
