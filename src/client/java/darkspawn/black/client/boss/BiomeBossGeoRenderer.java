package darkspawn.black.client.boss;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import darkspawn.black.boss.BiomeBoss;
import darkspawn.black.boss.BiomeBossAnimations;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.BossProfile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class BiomeBossGeoRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<BiomeBoss, R> {
	public BiomeBossGeoRenderer(EntityRendererProvider.Context context, BossProfile profile) {
		super(context, BossEntities.BIOME_BOSSES.get(profile));
		shadowRadius = profile.width / 3;
	}
	@Override
	public void addRenderData(BiomeBoss entity, Void context, R state, float partialTicks) {
		state.addGeckolibData(BiomeBoss.ANIMATION_PHASE, entity.phase());
		state.addGeckolibData(BiomeBoss.ANIMATION_WINDUP, entity.windup());
		state.addGeckolibData(BiomeBoss.ANIMATION_RECOVERY, entity.recovery());
		state.addGeckolibData(BiomeBoss.ANIMATION_ATTACK, entity.attackKind());
		state.addGeckolibData(BiomeBoss.ANIMATION_CHARGING, entity.charging());
		state.addGeckolibData(BiomeBossAnimations.AWAKENING, entity.awakening());
		state.addGeckolibData(BiomeBossAnimations.DEATH_TIME, (entity.deathTime + partialTicks) / 20F);
	}
	@Override
	protected float getDeathMaxRotation(GeoRenderState state) { return 0; }
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
		if (!info.getOrDefaultGeckolibData(DataTickets.IS_DEAD_OR_DYING, false)
				&& info.getOrDefaultGeckolibData(BiomeBoss.ANIMATION_WINDUP, 0) == 0
				&& info.getOrDefaultGeckolibData(BiomeBoss.ANIMATION_RECOVERY, 0) == 0
				&& !info.getOrDefaultGeckolibData(BiomeBoss.ANIMATION_CHARGING, false)) {
			DefaultAnimations.hardcodedHeadRotation(info, snapshots, "head_look");
		}
	}
	@Override
	public int getRenderColor(BiomeBoss entity, Void context, float partialTicks) {
		return entity.recovery() > 0 ? 0xFFFFFFAA : entity.phase() == 3 ? 0xFFFFA6A6
				: entity.profile() == BossProfile.NETHERBORN && entity.variant() == 1 ? 0xFF65DDCC : 0xFFFFFFFF;
	}
}
