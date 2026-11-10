package darkspawn.black.aircraft;

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

public final class AircraftMod implements ModInitializer {
	public static final String MOD_ID = "darkspawn_aircraft";
	public static final EntityType<Aircraft> AIRCRAFT = register();
	private static boolean initialized;
	private static EntityType<Aircraft> register() {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, DarkspawnCore.id("aircraft"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.of(Aircraft::new, MobCategory.CREATURE).sized(4.25F, 1F).passengerAttachments(1.1F).clientTrackingRange(12).updateInterval(1).noLootTable().build(key));
	}
	@Override public void onInitialize() { initialize(); }
	public static synchronized void initialize() {
		if (initialized) return;
		initialized = true;
		MountKit.initialize();
		FabricDefaultAttributeRegistry.register(AIRCRAFT, Aircraft.attributes());
		AircraftCoreItem.initialize();
	}
}
