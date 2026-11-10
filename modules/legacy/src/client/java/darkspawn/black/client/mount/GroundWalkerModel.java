package darkspawn.black.client.mount;

import darkspawn.black.mountkit.client.GriffinRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Compact mechanical walker: the moving limbs remain inside its 1.8-block footprint. */
public final class GroundWalkerModel extends EntityModel<GriffinRenderState> {
    private final ModelPart left, right;
    public GroundWalkerModel(ModelPart root) { super(root); left=root.getChild("left_leg"); right=root.getChild("right_leg"); }
    public static LayerDefinition createBodyLayer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("chassis", CubeListBuilder.create().texOffs(0,0).addBox(-9,-12,-6,18,16,12), PartPose.offset(0,0,0));
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(0,0).addBox(-7,-3,-5,14,3,10), PartPose.offset(0,-12,0));
        root.addOrReplaceChild("backrest", CubeListBuilder.create().texOffs(0,0).addBox(-7,-5,3,14,5,3), PartPose.offset(0,-11,0));
        for (int side : new int[]{-1,1}) {
            root.addOrReplaceChild(side<0?"left_leg":"right_leg", CubeListBuilder.create().texOffs(0,0)
                .addBox(-3,0,-3,6,13,6).addBox(-4,13,-6,8,3,10), PartPose.offset(side*5,8,0));
            root.addOrReplaceChild("hip_"+side, CubeListBuilder.create().texOffs(0,0).addBox(-3,0,-4,6,5,8), PartPose.offset(side*5,3,0));
            root.addOrReplaceChild("arm_"+side, CubeListBuilder.create().texOffs(0,0).addBox(-2,0,-3,4,15,6), PartPose.offset(side*12,-9,0));
        }
        return LayerDefinition.create(mesh,16,16);
    }
    @Override public void setupAnim(GriffinRenderState state) {
        super.setupAnim(state);
        float stride=(float)Math.cos(state.walkAnimationPos*.6F)*Math.min(state.walkAnimationSpeed,1)*.25F;
        left.xRot=stride; right.xRot=-stride;
    }
}
