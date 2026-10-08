package darkspawn.black.client.boss;

import darkspawn.black.boss.Endborn;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class EndbornRenderer extends MobRenderer<Endborn, LivingEntityRenderState, EndbornRenderer.Model> {
	private static final Identifier SKIN = Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");
	public EndbornRenderer(EntityRendererProvider.Context context) { super(context, new Model(createBodyLayer().bakeRoot()), 0.7F); }
	@Override
	public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) { return SKIN; }
	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, -36, 0));
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 16).addBox(-4, 0, -2, 8, 20, 4), PartPose.offset(0, -36, 0));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 0).addBox(-1, 0, -1, 2, 38, 2), PartPose.offset(5, -34, 0));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 0).addBox(-1, 0, -1, 2, 38, 2), PartPose.offset(-5, -34, 0));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-1, 0, -1, 2, 40, 2), PartPose.offset(2, -16, 0));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-1, 0, -1, 2, 40, 2), PartPose.offset(-2, -16, 0));
		root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 16).addBox(-6, -5.6F, -2, 12, 5.6F, 4), PartPose.offset(0, -44, 0));
		return LayerDefinition.create(mesh, 64, 32);
	}
	public static final class Model extends EntityModel<LivingEntityRenderState> {
		private final ModelPart head;
		private final ModelPart leftLeg;
		private final ModelPart rightLeg;
		private final ModelPart leftArm;
		private final ModelPart rightArm;
		public Model(ModelPart root) {
			super(root);
			head = root.getChild("head"); leftLeg = root.getChild("left_leg"); rightLeg = root.getChild("right_leg");
			leftArm = root.getChild("left_arm"); rightArm = root.getChild("right_arm");
		}
		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			head.yRot = state.yRot * (float) Math.PI / 180;
			head.xRot = state.xRot * (float) Math.PI / 180;
			float step = (float) Math.cos(state.walkAnimationPos * 0.65) * state.walkAnimationSpeed * 0.3F;
			leftLeg.xRot = step; rightLeg.xRot = -step; leftArm.xRot = -step; rightArm.xRot = step;
		}
	}
}
