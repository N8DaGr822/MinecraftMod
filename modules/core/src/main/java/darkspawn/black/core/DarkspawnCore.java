package darkspawn.black.core;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The shared content namespace. Every Darkspawn mod registers under {@code darkspawn:} whatever its mod id. */
public final class DarkspawnCore {
	public static final String MOD_ID = "darkspawn_core";
	public static final String NAMESPACE = "darkspawn";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
	private DarkspawnCore() { }
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}
}
