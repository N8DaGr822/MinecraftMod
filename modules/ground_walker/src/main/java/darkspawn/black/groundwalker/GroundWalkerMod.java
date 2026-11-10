package darkspawn.black.groundwalker;

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

public final class GroundWalkerMod implements ModInitializer {
	public static final String MOD_ID = "darkspawn_ground_walker";
	public static final EntityType<GroundWalker> GROUND_WALKER = register();
	private static boolean initialized;
	private static EntityType<GroundWalker> register() {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, DarkspawnCore.id("ground_walker"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.of(GroundWalker::new, MobCategory.CREATURE).sized(1.8F, 2.5F).passengerAttachments(2.4F).clientTrackingRange(12).updateInterval(1).noLootTable().build(key));
	}
	@Override public void onInitialize() { initialize(); }
	public static synchronized void initialize() {
		if (initialized) return;
		initialized = true;
		MountKit.initialize();
		FabricDefaultAttributeRegistry.register(GROUND_WALKER, GroundWalker.attributes());
		GroundWalkerItem.initialize();
	}
}
