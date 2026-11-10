package darkspawn.black;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.registries.BuiltInRegistries;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceCoverageTest {
    @Test void registeredItemsAndBlocksHaveVisualDefinitionsAndNames() throws Exception {
        TestBootstrap.initialize();
        try (var stream = getClass().getResourceAsStream("/assets/darkspawn/lang/en_us.json")) {
            assertNotNull(stream);
            var names = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            // Registry Coverage: Check actual registered content, including items omitted from art catalogs.
            for (var item : BuiltInRegistries.ITEM) {
                var id = BuiltInRegistries.ITEM.getKey(item);
                if (!id.getNamespace().equals("darkspawn")) continue;
                assertNotNull(getClass().getResource("/assets/darkspawn/items/" + id.getPath() + ".json"), id.toString());
                assertTrue(names.has(item.getDescriptionId()), item.getDescriptionId());
            }
            for (var block : BuiltInRegistries.BLOCK) {
                var id = BuiltInRegistries.BLOCK.getKey(block);
                if (!id.getNamespace().equals("darkspawn")) continue;
                assertNotNull(getClass().getResource("/assets/darkspawn/blockstates/" + id.getPath() + ".json"), id.toString());
                assertTrue(names.has(block.getDescriptionId()), block.getDescriptionId());
            }
        }
    }
}
