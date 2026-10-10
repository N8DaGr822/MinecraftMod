package darkspawn.black.audio;

import com.google.gson.JsonParser;
import darkspawn.black.TestBootstrap;
import darkspawn.black.boss.BossKind;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioTest {
	@BeforeAll static void bootstrap() { TestBootstrap.initialize(); }
	@Test void everySoundIsRegisteredPackagedAndCaptioned() throws Exception {
		var root = "/assets/darkspawn/";
		var manifest = JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream(root+"sounds.json"), StandardCharsets.UTF_8)).getAsJsonObject();
		var language = JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream(root+"lang/en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
		var files = new HashSet<String>();
		var creatures = new HashSet<String>();
		for (var entry : manifest.entrySet()) {
			assertSame(DarkspawnSounds.get(entry.getKey()), BuiltInRegistries.SOUND_EVENT.getValue(darkspawn.black.Darkspawn.id(entry.getKey())));
			var definition = entry.getValue().getAsJsonObject();
			if (!entry.getKey().startsWith("music.")) assertTrue(language.has(definition.get("subtitle").getAsString()), entry.getKey());
			if (entry.getKey().startsWith("entity.")) creatures.add(entry.getKey().split("\\.")[1]);
			for (var value : definition.getAsJsonArray("sounds")) {
				var sound = value.getAsJsonObject();
				String name = sound.get("name").getAsString();
				assertTrue(name.startsWith("darkspawn:"));
				if (entry.getKey().startsWith("music.")) assertTrue(sound.get("stream").getAsBoolean());
				if (files.add(name)) try (var stream = getClass().getResourceAsStream(root+"sounds/"+name.substring(10)+".ogg")) {
					assertNotNull(stream, name);
					assertEquals("OggS", new String(stream.readNBytes(4), StandardCharsets.US_ASCII), name);
				}
			}
		}
		assertEquals(115, creatures.size());
		for (String creature : creatures) for (String cue : new String[]{"idle","hurt","death","warning"})
			assertTrue(manifest.has("entity."+creature+"."+cue));
		for (var boss : BossKind.values()) for (int phase=1; phase<=3; phase++) assertTrue(manifest.has("music."+boss.id()+"."+phase));
	}
	@Test void warningCountdownEmitsOneCueAndCanRearm() {
		int last=0, cues=0;
		for (int current : new int[]{0,40,39,38,20,1,0,0,35,34,1,0}) {
			if (CreatureAudio.startsWarning(last,current)) cues++;
			last=current;
		}
		assertEquals(2,cues);
	}
}
