package darkspawn.black.core;

import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * One shared {@code config/darkspawn-server.properties}. Each mod reads its own keys with bounded fallbacks; missing
 * keys are written back with their defaults so the file documents every installed mod. Restart to apply.
 */
public final class ServerTuning {
	private static final String HEADER = "Darkspawn server budgets. Restart to apply. Defaults suit a small cooperative server.";
	private ServerTuning() { }

	/** Loads the shared file, adding any of {@code defaults} it lacks. Never throws; I/O failures fall back to defaults. */
	public static synchronized Properties load(Map<String, String> defaults) {
		var path = FabricLoader.getInstance().getConfigDir().resolve("darkspawn-server.properties");
		var properties = new Properties();
		try {
			if (Files.exists(path)) { try (var input = Files.newInputStream(path)) { properties.load(input); } }
			boolean missing = false;
			for (var entry : defaults.entrySet()) if (!properties.containsKey(entry.getKey())) { properties.setProperty(entry.getKey(), entry.getValue()); missing = true; }
			if (missing) {
				Files.createDirectories(path.getParent());
				try (var output = Files.newOutputStream(path)) { properties.store(output, HEADER); }
			}
		} catch (java.io.IOException failure) {
			DarkspawnCore.LOGGER.warn("Could not read/write server tuning; using defaults", failure);
			defaults.forEach(properties::setProperty);
		}
		return properties;
	}

	public static int integer(Properties properties, String key, int fallback, int min, int max) {
		try { return Math.clamp(Integer.parseInt(properties.getProperty(key, Integer.toString(fallback))), min, max); }
		catch (NumberFormatException invalid) { DarkspawnCore.LOGGER.warn("Invalid {}: using {}", key, fallback); return fallback; }
	}

	public static boolean bool(Properties properties, String key, boolean fallback) {
		return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
	}
}
