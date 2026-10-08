package darkspawn.black.client.boss;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import darkspawn.black.boss.BiomeBoss;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.BossProfile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class MutantZombieRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<BiomeBoss, R> {
	public MutantZombieRenderer(EntityRendererProvider.Context context) {
		super(context, BossEntities.BIOME_BOSSES.get(BossProfile.MUTANT_ZOMBIE));
		shadowRadius = BossProfile.MUTANT_ZOMBIE.width / 3;
	}

	@Override
	public void addRenderData(BiomeBoss entity, Void context, R state, float partialTicks) {
		state.addGeckolibData(BiomeBoss.ANIMATION_PHASE, entity.phase());
		state.addGeckolibData(BiomeBoss.ANIMATION_WINDUP, entity.windup());
		state.addGeckolibData(BiomeBoss.ANIMATION_RECOVERY, entity.recovery());
		state.addGeckolibData(BiomeBoss.ANIMATION_ATTACK, entity.attackKind());
		state.addGeckolibData(BiomeBoss.ANIMATION_CHARGING, entity.charging());
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
		// Zombie Aim: Keep ordinary look tracking separate from animated head and jaw gestures.
		if (!renderPassInfo.getOrDefaultGeckolibData(DataTickets.IS_DEAD_OR_DYING, false)
				&& renderPassInfo.getOrDefaultGeckolibData(BiomeBoss.ANIMATION_WINDUP, 0) == 0
				&& renderPassInfo.getOrDefaultGeckolibData(BiomeBoss.ANIMATION_RECOVERY, 0) == 0
				&& !renderPassInfo.getOrDefaultGeckolibData(BiomeBoss.ANIMATION_CHARGING, false)) {
			DefaultAnimations.hardcodedHeadRotation(renderPassInfo, snapshots, "head_look");
		}
	}

	@Override
	public int getRenderColor(BiomeBoss entity, Void context, float partialTicks) {
		return entity.recovery() > 0 ? 0xFFFFFFAA : entity.phase() == 3 ? 0xFFFFA6A6 : 0xFFFFFFFF;
	}
}
