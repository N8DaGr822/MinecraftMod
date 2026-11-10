package darkspawn.black.magiccarpet;

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

public final class MagicCarpetMod implements ModInitializer {
	public static final String MOD_ID = "darkspawn_magic_carpet";
	public static final EntityType<MagicCarpet> MAGIC_CARPET = register();
	private static boolean initialized;
	private static EntityType<MagicCarpet> register() {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, DarkspawnCore.id("magic_carpet"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.of(MagicCarpet::new, MobCategory.CREATURE).sized(1.7F, .25F).passengerAttachments(.3F).clientTrackingRange(12).updateInterval(1).noLootTable().build(key));
	}
	@Override public void onInitialize() { initialize(); }
	public static synchronized void initialize() {
		if (initialized) return;
		initialized = true;
		MountKit.initialize();
		FabricDefaultAttributeRegistry.register(MAGIC_CARPET, MagicCarpet.attributes());
		MagicCarpetItem.initialize();
	}
}
