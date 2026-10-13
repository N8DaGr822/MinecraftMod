package darkspawn.black.journal;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JournalCatalog {
    public record Entry(String id, String section, String title, String hint, String body, String entity, String site, String boss) { }
    private static final Map<String, Entry> ENTRIES = load();
    private static final com.google.gson.JsonObject NAMES = names();
    private JournalCatalog() { }
    public static Map<String, Entry> entries() { return ENTRIES; }
    private static com.google.gson.JsonObject names() {
        try (var stream = JournalCatalog.class.getResourceAsStream("/assets/darkspawn/lang/en_us.json")) {
            return new Gson().fromJson(new InputStreamReader(java.util.Objects.requireNonNull(stream), StandardCharsets.UTF_8), com.google.gson.JsonObject.class);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read journal names", failure); }
    }
    public static String itemName(net.minecraft.world.item.ItemStack stack) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        String key = "item." + id.getNamespace() + "." + id.getPath();
        if (!NAMES.has(key)) key = "block." + id.getNamespace() + "." + id.getPath();
        return NAMES.has(key) ? NAMES.get(key).getAsString() : stack.getHoverName().getString();
    }
    private static Map<String, Entry> load() {
        try (var stream = JournalCatalog.class.getResourceAsStream("/data/darkspawn/journal/catalog.json")) {
            if (stream == null) throw new IllegalStateException("Missing journal catalog");
            var entries = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Entry[].class);
            var result = new LinkedHashMap<String, Entry>();
            for (var entry : entries) {
                if (entry.id() == null || result.putIfAbsent(entry.id(), entry) != null) throw new IllegalStateException("Duplicate journal entry");
            }
            return java.util.Collections.unmodifiableMap(result);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read journal catalog", failure); }
    }
}
