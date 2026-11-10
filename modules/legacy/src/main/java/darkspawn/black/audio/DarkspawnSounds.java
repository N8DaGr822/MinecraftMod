package darkspawn.black.audio;

import com.google.gson.JsonParser;
import darkspawn.black.Darkspawn;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

/** The sound manifest is also the registry inventory, so resource-pack replacements keep stable IDs. */
public final class DarkspawnSounds {
	private static final Map<String, SoundEvent> EVENTS = new HashMap<>();
	private DarkspawnSounds() {}
	public static void initialize() {
		if (!EVENTS.isEmpty()) return;
		EVENTS.putAll(darkspawn.black.core.Sounds.register(Darkspawn.MOD_ID));
	}
	public static SoundEvent get(String key) {
		var event = EVENTS.get(key);
		if (event == null) throw new IllegalArgumentException("Unregistered Darkspawn sound: " + key);
		return event;
	}
	public static SoundEvent creature(Entity entity, String cue) {
		return get("entity." + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath() + "." + cue);
	}
}
