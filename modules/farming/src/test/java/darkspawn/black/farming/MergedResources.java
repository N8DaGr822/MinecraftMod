package darkspawn.black.farming;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Several module jars ship the files Minecraft merges (lang, sounds.json, tags). A plain classpath lookup returns only
 * the first copy, so tests read those through here to see the merged view the game uses.
 */
final class MergedResources {
	private MergedResources() { }

	static InputStream open(Class<?> anchor, String path) throws IOException {
		boolean tag = path.contains("/tags/");
		if (!tag && !path.endsWith("/sounds.json") && !path.contains("/lang/")) return anchor.getResourceAsStream(path);
		var copies = Collections.list(anchor.getClassLoader().getResources(path.startsWith("/") ? path.substring(1) : path));
		if (copies.isEmpty()) return null;
		var merged = new JsonObject(); var values = new JsonArray();
		for (var url : copies) try (var reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
			var json = JsonParser.parseReader(reader).getAsJsonObject();
			if (tag) json.getAsJsonArray("values").forEach(values::add); else json.entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
		}
		if (tag) { merged.addProperty("replace", false); merged.add("values", values); }
		return new ByteArrayInputStream(merged.toString().getBytes(StandardCharsets.UTF_8));
	}
}
