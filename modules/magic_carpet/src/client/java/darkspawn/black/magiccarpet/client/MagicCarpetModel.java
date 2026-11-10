package darkspawn.black.magiccarpet.client;

import darkspawn.black.mountkit.client.GriffinRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Thin woven deck with rolled ends and individual fringe; native wool supplies the textile. */
public final class MagicCarpetModel extends EntityModel<GriffinRenderState> {
    private final ModelPart front, back;
    public MagicCarpetModel(ModelPart root) { super(root); front=root.getChild("front"); back=root.getChild("back"); }
    public static LayerDefinition createBodyLayer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("deck",CubeListBuilder.create().texOffs(0,0).addBox(-12,0,-12,24,1,24),PartPose.offset(0,22,0));
        root.addOrReplaceChild("front",CubeListBuilder.create().texOffs(0,0).addBox(-12,-1,-4,24,1,4),PartPose.offset(0,22,-12));
        root.addOrReplaceChild("back",CubeListBuilder.create().texOffs(0,0).addBox(-12,-1,0,24,1,4),PartPose.offset(0,22,12));
        for(int i=0;i<8;i++) for(int end : new int[]{-1,1})
            root.addOrReplaceChild("fringe_"+end+"_"+i,CubeListBuilder.create().texOffs(0,0).addBox(-.5F,0,-1.5F,1,1,3),
                PartPose.offset(-10.5F+i*3,21,end*17));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void setupAnim(GriffinRenderState state) {
        super.setupAnim(state); front.xRot=-.15F*state.flightPose; back.xRot=.15F*state.flightPose;
    }
}
