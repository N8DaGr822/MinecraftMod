package darkspawn.black.core.hook;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.player.Player;

/**
 * Extra cold/heat protection contributed by other mods (gear, meals); the climate mod sums every provider.
 * {@code wet} is the climate mod's own wetness state (water, rain, or drying off). Negative heat makes hot climates worse.
 */
public final class ClimateProtection {
	public record Protection(int cold, int heat) {
		public static final Protection NONE = new Protection(0, 0);
	}
	@FunctionalInterface public interface Provider { Protection protection(Player player, boolean wet); }
	private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
	private ClimateProtection() { }
	public static void register(Provider provider) { PROVIDERS.add(provider); }
	public static Protection total(Player player, boolean wet) {
		int cold = 0, heat = 0;
		for (var provider : PROVIDERS) { var protection = provider.protection(player, wet); cold += protection.cold(); heat += protection.heat(); }
		return new Protection(cold, heat);
	}
}
