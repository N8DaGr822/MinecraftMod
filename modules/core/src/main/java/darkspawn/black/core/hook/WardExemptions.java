package darkspawn.black.core.hook;

import darkspawn.black.core.DarkspawnCore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Hostiles that a bonfire ward must leave alone: tamed or recruited monsters, bosses. Owners register from their side. */
public final class WardExemptions {
	public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE, DarkspawnCore.id("ward_exempt"));
	private static final List<Predicate<Entity>> PREDICATES = new CopyOnWriteArrayList<>();
	private WardExemptions() { }
	public static void register(Predicate<Entity> exempt) { PREDICATES.add(exempt); }
	public static boolean exempt(Entity entity) {
		if (entity.typeHolder().is(TAG)) return true;
		for (var predicate : PREDICATES) if (predicate.test(entity)) return true;
		return false;
	}
}
