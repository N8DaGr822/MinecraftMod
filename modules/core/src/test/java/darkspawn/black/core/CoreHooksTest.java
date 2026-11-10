package darkspawn.black.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import darkspawn.black.core.hook.ClimateProtection;
import darkspawn.black.core.hook.JournalSections;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class CoreHooksTest {
	@Test
	void namespaceIsSharedByEveryModule() {
		assertEquals("darkspawn:dump_chest", DarkspawnCore.id("dump_chest").toString());
	}

	@Test
	void tuningClampsAndFallsBack() {
		var properties = new Properties();
		properties.setProperty("high", "99"); properties.setProperty("bad", "x"); properties.setProperty("flag", "false");
		assertEquals(8, ServerTuning.integer(properties, "high", 4, 0, 8));
		assertEquals(4, ServerTuning.integer(properties, "bad", 4, 0, 8));
		assertEquals(3, ServerTuning.integer(properties, "missing", 3, 0, 8));
		assertFalse(ServerTuning.bool(properties, "flag", true));
		assertTrue(ServerTuning.bool(properties, "missing", true));
	}

	@Test
	void providersAreSummedAndCollected() {
		ClimateProtection.register(player -> new ClimateProtection.Protection(15, 0));
		ClimateProtection.register(player -> new ClimateProtection.Protection(5, 20));
		assertEquals(new ClimateProtection.Protection(20, 20), ClimateProtection.total(null));
		JournalSections.register((player, out) -> { out.accept(new JournalSections.Entry("a", "overview", "A", "", true)); out.accept(new JournalSections.Entry("b", "landmarks", "B", "", false)); });
		assertEquals(2, JournalSections.entries(null).size());
	}
}
