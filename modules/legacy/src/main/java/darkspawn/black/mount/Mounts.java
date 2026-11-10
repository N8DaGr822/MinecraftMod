package darkspawn.black.mount;

import darkspawn.black.mountkit.PrototypeMount;

import darkspawn.black.Darkspawn;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class Mounts {
	private Mounts() { }
    public static final EntityType<Thalassar> THALASSAR = register("thalassar", Thalassar::new, 1.3F, 1.8F, 1.45F);
    private static <T extends PrototypeMount> EntityType<T> register(String id, EntityType.EntityFactory<T> factory, float width, float height, float seat) {
        var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id(id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
            EntityType.Builder.of(factory, MobCategory.CREATURE).sized(width, height)
                .passengerAttachments(seat).clientTrackingRange(12).updateInterval(1).noLootTable().build(key));
    }
	public static void initialize() {
		darkspawn.black.mountkit.MountKit.initialize();
		darkspawn.black.creaturemounts.CreatureMounts.initialize();
		darkspawn.black.magiccarpet.MagicCarpetMod.initialize();
		darkspawn.black.aircraft.AircraftMod.initialize();
		darkspawn.black.groundwalker.GroundWalkerMod.initialize();
        FabricDefaultAttributeRegistry.register(THALASSAR, Thalassar.attributes());
	}
}
