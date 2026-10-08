package darkspawn.black.client.ecosystem;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import darkspawn.black.Darkspawn;
import darkspawn.black.boss.AnimatedCreature;
import darkspawn.black.boss.BossMinion;
import darkspawn.black.boss.CreatureAnimations;
import darkspawn.black.ecosystem.ForestMob;
import darkspawn.black.ecosystem.RegionalKind;
import darkspawn.black.ecosystem.RegionalMob;
import darkspawn.black.ecosystem.TaigaWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class CreatureGeoRenderer<T extends Mob & AnimatedCreature, R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<T, R> {
	public CreatureGeoRenderer(EntityRendererProvider.Context context, EntityType<T> type, float shadow, String texture) {
		super(context, new DefaultedEntityGeoModel<T>(type).withAltTexture(Darkspawn.id(texture)));
		shadowRadius = shadow;
	}
	@Override
	public void addRenderData(T entity, Void context, R state, float partialTicks) {
		int warning = 0; boolean active = false, hiding = false; int role = -1; float shield = 0;
		if (entity instanceof RegionalMob mob) {
			warning = mob.warning(); active = mob.active() > 0; hiding = mob.hiding();
			if (mob.kind() == RegionalKind.VOID_SENTINEL) { shield = (float)Math.toRadians(mob.shieldAngle()); }
			else if (mob.kind() == RegionalKind.SHARDLING) { shield = (mob.tickCount + partialTicks) * .04F; }
		} else if (entity instanceof ForestMob mob) { warning = mob.windup(); hiding = mob.hiding(); }
		else if (entity instanceof TaigaWolf wolf) { warning = wolf.windup(); active = wolf.leaping() || wolf.frenzy() > 0; }
		else if (entity instanceof BossMinion minion) { warning = minion.windup(); role = minion.role().ordinal(); }
		state.addGeckolibData(CreatureAnimations.WARNING, warning);
		state.addGeckolibData(CreatureAnimations.ACTIVE, active);
		state.addGeckolibData(CreatureAnimations.HIDING, hiding);
		state.addGeckolibData(CreatureAnimations.ROLE, role);
		state.addGeckolibData(CreatureAnimations.SHIELD_ANGLE, shield);
		state.addGeckolibData(CreatureAnimations.DEATH_TIME, (entity.deathTime + partialTicks) / 20F);
	}
	@Override protected float getDeathMaxRotation(GeoRenderState state) { return 0; }
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
		int role = info.getOrDefaultGeckolibData(CreatureAnimations.ROLE, -1);
		boolean egg = role == BossMinion.Role.EGG.ordinal(), cage = role == BossMinion.Role.SOUL_CAGE.ordinal();
		// Role Geometry: Hatching switches back to the same creature rig on the synchronized role update.
		snapshots.ifPresent("body", bone -> { bone.skipRender(egg || cage); bone.skipChildrenRender(egg || cage); });
		snapshots.ifPresent("egg", bone -> { bone.skipRender(!egg); bone.skipChildrenRender(!egg); });
		snapshots.ifPresent("cage", bone -> { bone.skipRender(!cage); bone.skipChildrenRender(!cage); });
		snapshots.ifPresent("crest", bone -> bone.setRotY(info.getOrDefaultGeckolibData(CreatureAnimations.SHIELD_ANGLE, 0F)));
		if (!info.getOrDefaultGeckolibData(DataTickets.IS_DEAD_OR_DYING, false) && !egg && !cage
				&& info.getOrDefaultGeckolibData(CreatureAnimations.WARNING, 0) == 0
				&& !info.getOrDefaultGeckolibData(CreatureAnimations.ACTIVE, false) && !info.getOrDefaultGeckolibData(CreatureAnimations.HIDING, false)) {
			DefaultAnimations.hardcodedHeadRotation(info, snapshots, "head_look");
		}
	}
	@Override
	public int getRenderColor(T entity, Void context, float partialTicks) {
		if (entity instanceof RegionalMob mob) { return mob.warning() > 0 ? 0xFFFFD080 : mob.active() > 0 ? 0xFF90DDFF : mob.hiding() ? 0xFF707070 : 0xFFFFFFFF; }
		if (entity instanceof ForestMob mob) { return mob.windup() > 0 ? 0xFFFFD080 : 0xFFFFFFFF; }
		if (entity instanceof TaigaWolf wolf) {
			return wolf.frenzy() > 0 ? 0xFFFF9999 : wolf.windup() > 0 ? 0xFFFFDD88
					: wolf.kind() == TaigaWolf.Kind.FROSTFANG ? 0xFFAADDFF : wolf.kind() == TaigaWolf.Kind.RAVAGED ? 0xFFAB8E7A
					: wolf.kind() == TaigaWolf.Kind.ALPHA ? 0xFFC9B79B : wolf.packed() ? 0xFFD8D3C6 : 0xFFFFFFFF;
		}
		if (entity instanceof BossMinion minion) { return minion.role() == BossMinion.Role.HEALER ? 0xFF77FF88 : minion.role() == BossMinion.Role.SOUL_CAGE ? 0xFF77FFFF : 0xFFFFFFFF; }
		return 0xFFFFFFFF;
	}
}
