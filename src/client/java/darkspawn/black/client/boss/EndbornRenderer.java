package darkspawn.black.client.boss;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.Endborn;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class EndbornRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<Endborn, R> {
	public EndbornRenderer(EntityRendererProvider.Context context) {
		super(context, BossEntities.ENDBORN);
		shadowRadius = 0.7F;
	}
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
		// Endborn Look: Preserve vanilla head tracking alongside the Blockbench movement animations.
		DefaultAnimations.hardcodedHeadRotation(renderPassInfo, snapshots, "head");
	}
}
