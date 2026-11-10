package darkspawn.black.creaturekit.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.creaturekit.AnimatedCreature;
import darkspawn.black.creaturekit.CreatureAnimations;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Renders any {@link AnimatedCreature} from {@code geckolib/models/entity/<id>.geo.json} with a shared texture sheet. */
public final class CreatureGeoRenderer<T extends Mob & AnimatedCreature, R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<T, R> {
	public CreatureGeoRenderer(EntityRendererProvider.Context context, EntityType<T> type, float shadow, String texture) {
		super(context, new DefaultedEntityGeoModel<T>(type).withAltTexture(DarkspawnCore.id(texture)));
		shadowRadius = shadow;
	}
	@Override
	public void addRenderData(T entity, Void context, R state, float partialTicks) {
		state.addGeckolibData(CreatureAnimations.WARNING, entity.renderWarning());
		state.addGeckolibData(CreatureAnimations.ACTIVE, entity.renderActive());
		state.addGeckolibData(CreatureAnimations.HIDING, entity.renderHiding());
		state.addGeckolibData(CreatureAnimations.SHELL, entity.renderShell());
		state.addGeckolibData(CreatureAnimations.SHIELD_ANGLE, entity.renderCrest(partialTicks));
		state.addGeckolibData(CreatureAnimations.DEATH_TIME, (entity.deathTime + partialTicks) / 20F);
	}
	@Override protected float getDeathMaxRotation(GeoRenderState state) { return 0; }
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
		int shell = info.getOrDefaultGeckolibData(CreatureAnimations.SHELL, AnimatedCreature.SHELL_NONE);
		boolean egg = shell == AnimatedCreature.SHELL_EGG, cage = shell == AnimatedCreature.SHELL_CAGE;
		// Role Geometry: Hatching switches back to the same creature rig on the synchronized role update.
		snapshots.ifPresent("body", bone -> { bone.skipRender(egg || cage); bone.skipChildrenRender(egg || cage); });
		snapshots.ifPresent("egg", bone -> { bone.skipRender(!egg); bone.skipChildrenRender(!egg); });
		snapshots.ifPresent("cage", bone -> { bone.skipRender(!cage); bone.skipChildrenRender(!cage); });
		snapshots.ifPresent("crest", bone -> {
			bone.skipRender(egg || cage); bone.skipChildrenRender(egg || cage);
			bone.setRotY(info.getOrDefaultGeckolibData(CreatureAnimations.SHIELD_ANGLE, 0F));
		});
		if (!info.getOrDefaultGeckolibData(DataTickets.IS_DEAD_OR_DYING, false) && !egg && !cage
				&& info.getOrDefaultGeckolibData(CreatureAnimations.WARNING, 0) == 0
				&& !info.getOrDefaultGeckolibData(CreatureAnimations.ACTIVE, false) && !info.getOrDefaultGeckolibData(CreatureAnimations.HIDING, false)) {
			DefaultAnimations.hardcodedHeadRotation(info, snapshots, "head_look");
		}
	}
	@Override
	public int getRenderColor(T entity, Void context, float partialTicks) { return entity.renderTint(); }
}
