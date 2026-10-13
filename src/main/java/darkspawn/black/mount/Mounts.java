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
	public static final EntityType<WoollyRhino> WOOLLY_RHINO = register("woolly_rhino", WoollyRhino::new, 1.7F, 1.8F, 1.5F);
    public static final EntityType<FlyingSquirrel> FLYING_SQUIRREL = register("flying_squirrel", FlyingSquirrel::new, 1.2F, 1F, .8F);
    public static final EntityType<PoptartCat> POPTART_CAT = register("poptart_cat", PoptartCat::new, 1.3F, 1.3F, 1.1F);
    public static final EntityType<DragonMount> DRAGON_MOUNT = register("dragon_mount", DragonMount::new, 1.7F, 1.8F, 1.45F);
    public static final EntityType<WingedUnicorn> WINGED_UNICORN = register("winged_unicorn", WingedUnicorn::new, 1.3F, 1.9F, 1.45F);
    public static final EntityType<RidingGolem> RIDING_GOLEM = register("riding_golem", RidingGolem::new, 1.8F, 2.85F, 2.85F);
    public static final EntityType<Sasquatch> SASQUATCH = register("sasquatch", Sasquatch::new, 1.6F, 2.85F, 2.85F);
    public static final EntityType<Cerberus> CERBERUS = register("cerberus", Cerberus::new, 1.7F, 1.7F, 1.45F);
    private static <T extends PrototypeMount> EntityType<T> register(String id, EntityType.EntityFactory<T> factory, float width, float height, float seat) {
        var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id(id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
            EntityType.Builder.of(factory, MobCategory.CREATURE).sized(width, height)
                .passengerAttachments(seat).clientTrackingRange(12).updateInterval(1).noLootTable().build(key));
    }
    private static EntityType<Griffin> registerGriffin() {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id("griffin"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.of(Griffin::new, MobCategory.CREATURE).sized(1.5F, 1.7F)
				.passengerAttachments(1.35F).clientTrackingRange(12).updateInterval(1).noLootTable().build(key));
	}
	public static void initialize() {
		FabricDefaultAttributeRegistry.register(GRIFFIN, Griffin.attributes());
        FabricDefaultAttributeRegistry.register(WOOLLY_RHINO, WoollyRhino.attributes());
        FabricDefaultAttributeRegistry.register(FLYING_SQUIRREL, FlyingSquirrel.attributes());
        FabricDefaultAttributeRegistry.register(POPTART_CAT, PoptartCat.attributes());
        FabricDefaultAttributeRegistry.register(DRAGON_MOUNT, DragonMount.attributes());
        FabricDefaultAttributeRegistry.register(WINGED_UNICORN, WingedUnicorn.attributes());
        FabricDefaultAttributeRegistry.register(RIDING_GOLEM, RidingGolem.attributes());
        FabricDefaultAttributeRegistry.register(SASQUATCH, Sasquatch.attributes());
        FabricDefaultAttributeRegistry.register(CERBERUS, Cerberus.attributes());
		PayloadTypeRegistry.serverboundPlay().register(GriffinControlsPayload.TYPE, GriffinControlsPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GriffinControlsPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof PrototypeMount mount && mount.getId() == payload.entityId()) {
				mount.acceptControls(context.player(), payload.buttons());
			}
		});
	}
}
