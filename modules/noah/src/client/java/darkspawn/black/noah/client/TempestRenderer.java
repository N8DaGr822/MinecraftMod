package darkspawn.black.noah.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import darkspawn.black.noah.NoahLegacy;
import darkspawn.black.noah.TempestProjectile;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class TempestRenderer extends EntityRenderer<ThrownTrident,TempestRenderer.State> {
    private final ThrownTridentRenderer vanilla;
    private final ItemModelResolver resolver;
    public static final class State extends ThrownTridentRenderState {
        boolean tempest;
        final ItemStackRenderState item=new ItemStackRenderState();
    }
    public TempestRenderer(EntityRendererProvider.Context context) {
        super(context);vanilla=new ThrownTridentRenderer(context);resolver=context.getItemModelResolver();
    }
    @Override public State createRenderState() { return new State(); }
    @Override protected AABB getBoundingBoxForCulling(ThrownTrident entity,float partial) {
        return super.getBoundingBoxForCulling(entity,partial).inflate(1.5);
    }
    @Override public void extractRenderState(ThrownTrident entity,State state,float partial) {
        vanilla.extractRenderState(entity,state,partial);
        state.tempest=((TempestProjectile)entity).darkspawn$isTempest();
        if(state.tempest) {
            var stack=new ItemStack(NoahLegacy.TEMPEST);stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,entity.isFoil());
            resolver.updateForNonLiving(state.item,stack,ItemDisplayContext.NONE,entity);
        }
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        if(!state.tempest){vanilla.submit(state,pose,collector,camera);return;}
        // Flight Pose: The authored item's prongs point up, opposite the native trident model's axis.
        pose.pushPose();pose.rotateDegrees(Axis.YP,state.yRot-90);pose.rotateDegrees(Axis.ZP,state.xRot-90);
        state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);
        pose.popPose();super.submit(state,pose,collector,camera);
    }
}
