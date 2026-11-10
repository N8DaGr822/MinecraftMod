package darkspawn.black.core;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * Registers the sound events listed in one mod's own {@code assets/darkspawn/sounds.json}. Reading through the mod
 * container (not the classpath) keeps several Darkspawn jars from shadowing each other's manifests.
 */
public final class Sounds {
	private Sounds() { }
	public static Map<String, SoundEvent> register(String modId) {
		var container = FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> new IllegalStateException("Unknown mod " + modId));
		var manifest = container.findPath("assets/" + DarkspawnCore.NAMESPACE + "/sounds.json").orElseThrow(() -> new IllegalStateException("Missing sound manifest in " + modId));
		var events = new LinkedHashMap<String, SoundEvent>();
		try (var reader = Files.newBufferedReader(manifest, StandardCharsets.UTF_8)) {
			for (String key : JsonParser.parseReader(reader).getAsJsonObject().keySet()) {
				var id = DarkspawnCore.id(key);
				events.put(key, Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
			}
		} catch (java.io.IOException error) { throw new IllegalStateException("Cannot load sounds of " + modId, error); }
		return events;
	}
}
