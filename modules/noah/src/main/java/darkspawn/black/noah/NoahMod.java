package darkspawn.black.noah;

import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.mountkit.MountKit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class NoahMod implements ModInitializer {
	public static final String MOD_ID = "darkspawn_noah";
	public static final EntityType<Thalassar> THALASSAR = register();
	private static boolean initialized;
	private static EntityType<Thalassar> register() {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, DarkspawnCore.id("thalassar"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.of(Thalassar::new, MobCategory.CREATURE).sized(1.3F, 1.8F).passengerAttachments(1.45F)
				.clientTrackingRange(12).updateInterval(1).noLootTable().build(key));
	}
	@Override public void onInitialize() { initialize(); }
	public static synchronized void initialize() {
		if (initialized) return;
		initialized = true;
		MountKit.initialize();
		FabricDefaultAttributeRegistry.register(THALASSAR, Thalassar.attributes());
		NoahLegacy.initialize();
	}
}
