package darkspawn.black.creaturekit;

import darkspawn.black.core.DarkspawnCore;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Library entrypoint. The only registered content is the shared {@code darkspawn:boss_bolt} projectile. */
public final class CreatureKit implements ModInitializer {
	public static final String MOD_ID = "darkspawn_creature_kit";
	public static final EntityType<BossBolt> BOSS_BOLT = registerBolt();
	private static EntityType<BossBolt> registerBolt() {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, DarkspawnCore.id("boss_bolt"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<BossBolt>of(BossBolt::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).clientTrackingRange(12).updateInterval(2).noLootTable().build(key));
	}
	@Override public void onInitialize() { initialize(); }
	/** Idempotent; touching this class registers the bolt type. */
	public static void initialize() { }
}
