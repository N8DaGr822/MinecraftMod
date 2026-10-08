package darkspawn.black.client.boss;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.MutantWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class MutantWolfGeoRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<MutantWolf, R> {
	public MutantWolfGeoRenderer(EntityRendererProvider.Context context) {
		super(context, BossEntities.MUTANT_WOLF);
		shadowRadius = 3;
	}

	@Override
	public void addRenderData(MutantWolf entity, Void context, R state, float partialTicks) {
		state.addGeckolibData(MutantWolf.ANIMATION_PHASE, entity.phase());
		state.addGeckolibData(MutantWolf.ANIMATION_WINDUP, entity.windup());
		state.addGeckolibData(MutantWolf.ANIMATION_RECOVERY, entity.recovery());
		state.addGeckolibData(MutantWolf.ANIMATION_ATTACK, entity.attackKind());
		state.addGeckolibData(MutantWolf.ANIMATION_LEAPING, entity.leaping());
		state.addGeckolibData(MutantWolf.ANIMATION_AWAKENING, entity.awakening());
		float lifecycleTicks = entity.isDeadOrDying() ? entity.defeatTime() : MutantWolf.AWAKENING_TICKS - entity.awakening();
		state.addGeckolibData(MutantWolf.ANIMATION_LIFECYCLE_TIME,
				Math.clamp((lifecycleTicks + partialTicks) / 20F, 0, (entity.isDeadOrDying() ? MutantWolf.DEFEAT_TICKS : MutantWolf.AWAKENING_TICKS) / 20F));
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState state) {
		// Wolf Collapse: The authored pose replaces vanilla's sideways death roll.
		return 0;
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
		// Wolf Aim: A separate neck pivot preserves head/jaw animation and the committed attack direction.
		if (!renderPassInfo.getOrDefaultGeckolibData(DataTickets.IS_DEAD_OR_DYING, false)
				&& renderPassInfo.getOrDefaultGeckolibData(MutantWolf.ANIMATION_AWAKENING, 0) == 0
				&& renderPassInfo.getOrDefaultGeckolibData(MutantWolf.ANIMATION_WINDUP, 0) == 0
				&& renderPassInfo.getOrDefaultGeckolibData(MutantWolf.ANIMATION_RECOVERY, 0) == 0
				&& !renderPassInfo.getOrDefaultGeckolibData(MutantWolf.ANIMATION_LEAPING, false)) {
			DefaultAnimations.hardcodedHeadRotation(renderPassInfo, snapshots, "head_look");
		}
	}

	@Override
	public int getRenderColor(MutantWolf entity, Void context, float partialTicks) {
		return entity.recovery() > 0 ? 0xFFFFFFAA : entity.phase() == 3 ? 0xFFFF9999
				: entity.variant() == 1 ? 0xFF99DDFF : entity.variant() == 2 ? 0xFFC4AD94 : 0xFFFFFFFF;
	}
}
