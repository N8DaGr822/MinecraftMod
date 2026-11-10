package darkspawn.black.aircraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import darkspawn.black.aircraft.Aircraft;
import darkspawn.black.aircraft.AirframeBlock;
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

public final class AircraftRenderer extends EntityRenderer<Aircraft,AircraftRenderer.State> {
	private static final BlockDisplayContext CONTEXT=BlockDisplayContext.create();
	private final BlockModelResolver resolver;
	public AircraftRenderer(EntityRendererProvider.Context context){super(context);resolver=context.getBlockModelResolver();}
	public static final class State extends EntityRenderState {
		List<AirframeBlock> blocks=List.of(); final Map<BlockState,BlockModelRenderState> models=new HashMap<>();float yaw;
	}
	@Override public State createRenderState(){return new State();}
	@Override public void extractRenderState(Aircraft ship,State state,float partial){
		super.extractRenderState(ship,state,partial);state.yaw=ship.getYRot(partial);
		if(state.blocks!=ship.blocks()){
			state.blocks=ship.blocks();state.models.clear();
			for(var block:state.blocks)state.models.computeIfAbsent(block.state(),key->{var model=new BlockModelRenderState();resolver.update(model,key,CONTEXT);return model;});
		}
	}
	@Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){

		pose.pushPose();pose.rotate(Axis.YP.rotationDegrees(-state.yaw));
		for(var block:state.blocks){pose.pushPose();pose.translate(block.pos().getX()-.5,block.pos().getY(),block.pos().getZ()-.5);state.models.get(block.state()).submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();}
		pose.popPose();super.submit(state,pose,collector,camera);
	}
}
