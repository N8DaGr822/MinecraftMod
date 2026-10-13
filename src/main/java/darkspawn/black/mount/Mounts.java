package darkspawn.black.mount;

import darkspawn.black.Darkspawn;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class Mounts {
	private Mounts() { }
	public static final EntityType<Griffin> GRIFFIN = registerGriffin();
	private static EntityType<Griffin> registerGriffin() {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id("griffin"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.of(Griffin::new, MobCategory.CREATURE).sized(1.5F, 1.7F)
				.passengerAttachments(1.35F).clientTrackingRange(12).updateInterval(1).noLootTable().build(key));
	}
	public static void initialize() {
		FabricDefaultAttributeRegistry.register(GRIFFIN, Griffin.attributes());
		PayloadTypeRegistry.serverboundPlay().register(GriffinControlsPayload.TYPE, GriffinControlsPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GriffinControlsPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof Griffin griffin && griffin.getId() == payload.entityId()) {
				griffin.acceptControls(context.player(), payload.buttons());
			}
		});
	}
}
