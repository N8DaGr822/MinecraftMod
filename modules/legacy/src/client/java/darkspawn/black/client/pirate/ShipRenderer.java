package darkspawn.black.client.pirate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import darkspawn.black.pirate.PirateShip;
import darkspawn.black.pirate.ShipBlock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;

public final class ShipRenderer extends EntityRenderer<PirateShip,ShipRenderer.State> {
	private static final BlockDisplayContext CONTEXT=BlockDisplayContext.create();
	private final BlockModelResolver resolver;
	public ShipRenderer(EntityRendererProvider.Context context){super(context);resolver=context.getBlockModelResolver();}
	public static final class State extends EntityRenderState {
		List<ShipBlock> blocks=List.of(); final Map<BlockState,BlockModelRenderState> models=new HashMap<>();float yaw;boolean docked;int length,flag=-1;final BlockModelRenderState flagModel=new BlockModelRenderState(),postModel=new BlockModelRenderState();
	}
	@Override public State createRenderState(){return new State();}
	@Override public void extractRenderState(PirateShip ship,State state,float partial){
		super.extractRenderState(ship,state,partial);state.yaw=ship.getYRot(partial);state.docked=ship.docked();state.length=ship.length();
        if(state.flag!=ship.flag()){state.flag=ship.flag();var id=new String[]{"black_wool","red_wool","blue_wool","cyan_wool"}[state.flag];resolver.update(state.flagModel,net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id)).defaultBlockState(),CONTEXT);resolver.update(state.postModel,net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState(),CONTEXT);}
		if(state.blocks!=ship.blocks()){
			state.blocks=ship.blocks();state.models.clear();
			for(var block:state.blocks)state.models.computeIfAbsent(block.state(),key->{var model=new BlockModelRenderState();resolver.update(model,key,CONTEXT);return model;});
		}
	}
	@Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){

		pose.pushPose();pose.rotate(Axis.YP.rotationDegrees(-state.yaw));
		if(!state.docked)for(var block:state.blocks){pose.pushPose();pose.translate(block.pos().getX()-.5,block.pos().getY(),block.pos().getZ()-.5);state.models.get(block.state()).submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();}
        // Cosmetic Ensign: Render independently of the captured build; docking never places extra blocks.
        for(int y=1;y<=2;y++){pose.pushPose();pose.translate(-.5,y,-state.length/2.0);state.postModel.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();}
        pose.pushPose();pose.translate(.1,2.2,-state.length/2.0+.4);pose.scale(1.2F,.7F,.12F);state.flagModel.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
		pose.popPose();super.submit(state,pose,collector,camera);
	}
}
