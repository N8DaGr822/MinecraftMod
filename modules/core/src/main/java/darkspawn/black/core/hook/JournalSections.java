package darkspawn.black.core.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/** Journal entries computed per player by the mod that owns them; the journal lists every provider's entries. */
public final class JournalSections {
	public record Entry(String id, String section, String title, String body, boolean discovered) { }
	@FunctionalInterface public interface Provider { void entries(ServerPlayer player, Consumer<Entry> out); }
	private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
	private JournalSections() { }
	public static void register(Provider provider) { PROVIDERS.add(provider); }
	public static List<Entry> entries(ServerPlayer player) {
		var result = new ArrayList<Entry>();
		for (var provider : PROVIDERS) provider.entries(player, result::add);
		return result;
	}
}
