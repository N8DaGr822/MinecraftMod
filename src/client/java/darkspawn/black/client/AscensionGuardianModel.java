package darkspawn.black.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Chapter Silhouettes: A jointed stone construct with one chapter-specific crest assembly. */
public final class AscensionGuardianModel extends EntityModel<AscensionGuardianRenderState> {
    private final ModelPart head,leftArm,rightArm,leftLeg,rightLeg;
    private final ModelPart[] crests=new ModelPart[12];
    public AscensionGuardianModel(ModelPart root){
        super(root);head=root.getChild("head");leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");
        leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");
        for(int chapter:new int[]{1,4,5,6,7,8,9,10,11})crests[chapter]=root.getChild("crest_"+chapter);
    }
    public static LayerDefinition createBodyLayer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-7,-17,-4,14,18,8),PartPose.offset(0,10,0));
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,32).addBox(-4,-8,-4,8,8,8)
            .addBox(-3,-5,-5,6,2,1),PartPose.offset(0,-7,0));
        for(int side:new int[]{-1,1}){
            root.addOrReplaceChild(side<0?"right_arm":"left_arm",CubeListBuilder.create().texOffs(32,0).addBox(-3,-2,-3,6,22,6),PartPose.offset(side*10,-4,0));
            root.addOrReplaceChild(side<0?"right_leg":"left_leg",CubeListBuilder.create().texOffs(32,32).addBox(-3,0,-3,6,14,6),PartPose.offset(side*4,10,0));
        }
        // Profiles stay near the original golem envelope; decoration never changes collision or reach.
        root.addOrReplaceChild("crest_1",CubeListBuilder.create().texOffs(0,0).addBox(-14,-10,-4,7,8,8).addBox(7,-10,-4,7,8,8),PartPose.ZERO);
        root.addOrReplaceChild("crest_4",CubeListBuilder.create().texOffs(0,0).addBox(-11,-19,-1,2,14,2).addBox(9,-19,-1,2,14,2).addBox(-11,-19,-1,22,2,2),PartPose.ZERO);
        root.addOrReplaceChild("crest_5",CubeListBuilder.create().texOffs(0,0).addBox(-10,-16,2,5,13,5).addBox(5,-16,2,5,13,5),PartPose.ZERO);
        var frost=CubeListBuilder.create().texOffs(0,0);
        for(int i=-1;i<=1;i++)frost.addBox(i*7-1,-18+Math.abs(i)*3,0,2,14,2);
        root.addOrReplaceChild("crest_6",frost,PartPose.ZERO);
        root.addOrReplaceChild("crest_7",CubeListBuilder.create().texOffs(0,0).addBox(-15,-11,-1,6,16,2).addBox(9,-11,-1,6,16,2).addBox(-1,-17,3,2,18,6),PartPose.ZERO);
        var crystal=CubeListBuilder.create().texOffs(0,0);
        for(int i=-2;i<=2;i++)crystal.addBox(i*5-1,-19+Math.abs(i)*2,3,3,11,3);
        root.addOrReplaceChild("crest_8",crystal,PartPose.ZERO);
        root.addOrReplaceChild("crest_9",CubeListBuilder.create().texOffs(0,0).addBox(-10,-19,4,2,20,2).addBox(8,-19,4,2,20,2).addBox(-10,-19,4,20,2,2).addBox(-10,-1,4,20,2,2),PartPose.ZERO);
        root.addOrReplaceChild("crest_10",CubeListBuilder.create().texOffs(0,0).addBox(-14,-10,-5,8,7,10).addBox(6,-10,-5,8,7,10).addBox(-5,-13,4,10,6,5),PartPose.ZERO);
        var crown=CubeListBuilder.create().texOffs(0,0).addBox(-6,-16,-5,12,2,10);
        for(int i=-1;i<=1;i++)crown.addBox(i*5-1,-20,-5,2,5,2);
        root.addOrReplaceChild("crest_11",crown,PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(AscensionGuardianRenderState state){
        super.setupAnim(state);
        head.yRot=state.yRot*(float)(Math.PI/180);head.xRot=state.xRot*(float)(Math.PI/180);
        float walk=(float)Math.cos(state.walkAnimationPos*.6662F)*state.walkAnimationSpeed;
        leftLeg.xRot=walk;rightLeg.xRot=-walk;
        leftArm.xRot=state.warning>0?-1.8F:-walk*.6F;rightArm.xRot=state.warning>0?-1.8F:walk*.6F;
        for(int i=0;i<crests.length;i++)if(crests[i]!=null)crests[i].visible=i==state.chapter;
    }
}
