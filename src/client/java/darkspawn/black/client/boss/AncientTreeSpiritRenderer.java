package darkspawn.black.client.boss;

import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.CustomBoneTextureGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import darkspawn.black.Darkspawn;
import darkspawn.black.boss.AncientTreeSpirit;
import darkspawn.black.boss.BossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class AncientTreeSpiritRenderer<R extends LivingEntityRenderState & GeoRenderState>
		extends GeoEntityRenderer<AncientTreeSpirit, R> {
	public AncientTreeSpiritRenderer(EntityRendererProvider.Context context) {
		super(context, BossEntities.TREE_SPIRIT);
		withScale(AncientTreeSpirit.MODEL_SCALE);
		shadowRadius = 4 * AncientTreeSpirit.MODEL_SCALE;
		withRenderLayer(new CustomBoneTextureGeoLayer<AncientTreeSpirit, Void, R>(this, "heartwood",
				Darkspawn.id("textures/entity/ancient_tree_spirit.png")) {
			@Override
			public boolean shouldRenderBone(R state) {
				return state.getOrDefaultGeckolibData(AncientTreeSpirit.ANIMATION_PHASE, 1) >= 2
						&& state.getOrDefaultGeckolibData(AncientTreeSpirit.ANIMATION_RECOVERY, 0) > 0;
			}
			@Override
			public void renderCube(GeoCube cube, PoseStack pose, VertexConsumer buffer, int light, int overlay,
					int color, float widthScale, float heightScale) {
				// Exposed Heartwood: Keep the four-sided weak point readable at full brightness in dark forests.
				super.renderCube(cube, pose, buffer, 15728880, overlay, 0xFFFFFFFF, widthScale, heightScale);
			}
		});
	}

	@Override
	public void addRenderData(AncientTreeSpirit entity, Void context, R state, float partialTicks) {
		state.addGeckolibData(AncientTreeSpirit.ANIMATION_PHASE, entity.phase());
		state.addGeckolibData(AncientTreeSpirit.ANIMATION_WINDUP, entity.windup());
		state.addGeckolibData(AncientTreeSpirit.ANIMATION_RECOVERY, entity.recovery());
		state.addGeckolibData(AncientTreeSpirit.ANIMATION_ATTACK, entity.attackKind());
		state.addGeckolibData(AncientTreeSpirit.ANIMATION_AWAKENING, entity.awakening());
		float lifecycleTicks = entity.isDeadOrDying() ? entity.defeatTime() : AncientTreeSpirit.AWAKENING_TICKS - entity.awakening();
		state.addGeckolibData(AncientTreeSpirit.ANIMATION_LIFECYCLE_TIME,
				Math.clamp((lifecycleTicks + partialTicks) / 20F, 0, 3));
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState state) {
		// Collapse Animation: Avoid applying vanilla's sideways death roll on top of the Blockbench pose.
		return 0;
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
		int phase = renderPassInfo.getOrDefaultGeckolibData(AncientTreeSpirit.ANIMATION_PHASE, 1);
		// Broken Canopy: Saved phase controls missing foliage for both current viewers and arriving clients.
		snapshots.ifPresent("canopy_left", bone -> bone.skipRender(phase >= 2));
		snapshots.ifPresent("canopy_right", bone -> bone.skipRender(phase >= 3));
		// Weak Point Visibility: Only the gated full-bright layer draws heartwood, never the base bark pass.
		snapshots.ifPresent("heartwood", bone -> bone.skipRender(true));
	}

	@Override
	public int getRenderColor(AncientTreeSpirit entity, Void context, float partialTicks) {
		return entity.recovery() > 0 ? 0xFFC5EB90 : entity.phase() == 3 ? 0xFFFFAA77 : entity.phase() == 2 ? 0xFFE4C68C : 0xFFFFFFFF;
	}
}
