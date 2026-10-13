package darkspawn.black.forsaken;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ForsakenPressure extends SavedData {
	public record Zone(int pressure, long updated, long noise, long suppressed, long surgeEnd, long nextSurge) {
		static final Codec<Zone> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(0, 100).fieldOf("pressure").forGetter(Zone::pressure), Codec.LONG.fieldOf("updated").forGetter(Zone::updated),
			Codec.LONG.fieldOf("noise").forGetter(Zone::noise), Codec.LONG.fieldOf("suppressed").forGetter(Zone::suppressed),
			Codec.LONG.fieldOf("surge_end").forGetter(Zone::surgeEnd), Codec.LONG.fieldOf("next_surge").forGetter(Zone::nextSurge)
		).apply(i, Zone::new));
		public Zone decay(long now) {
			long steps = Math.max(0, now - updated) / 100;
			return new Zone((int)Math.max(0, pressure - steps), updated + steps * 100, noise, suppressed, surgeEnd, nextSurge);
		}
	}
	public static final Codec<ForsakenPressure> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Zone.CODEC).optionalFieldOf("zones", Map.of()).forGetter(data -> data.zones)
	).apply(i, ForsakenPressure::new));
	public static final SavedDataType<ForsakenPressure> TYPE = new SavedDataType<>(Darkspawn.id("forsaken_pressure"), () -> new ForsakenPressure(Map.of()), CODEC, null);
	private final Map<String, Zone> zones;
	private ForsakenPressure(Map<String, Zone> zones) { this.zones = new HashMap<>(zones); }
	public static ForsakenPressure get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
	public static BlockPos origin(BlockPos pos) { return new BlockPos(Math.floorDiv(pos.getX() + 32, 512) * 512, 80, Math.floorDiv(pos.getZ() - 16, 512) * 512); }
	private static String key(BlockPos pos) { var origin = origin(pos); return origin.getX() + "," + origin.getZ(); }
	public Zone at(BlockPos pos, long now) { return zones.getOrDefault(key(pos), new Zone(0, now, Long.MIN_VALUE, 0, 0, 0)).decay(now); }
	public void disturb(BlockPos pos, long now, int amount) {
		var old = at(pos, now);
		// Shared Noise Budget: A crowd or explosion cannot add pressure more than once per city per second.
		if (old.noise != Long.MIN_VALUE && now - old.noise < 20) return;
		int pressure = Math.min(100, old.pressure + amount);
		boolean surge = pressure >= 75 && now >= old.nextSurge;
		put(pos, new Zone(pressure, now, now, old.suppressed, surge ? now + 600 : old.surgeEnd, surge ? now + 2400 : old.nextSurge), now);
	}
	public void suppress(BlockPos pos, long now) {
		var old = at(pos, now);
		put(pos, new Zone(Math.max(0, old.pressure - 25), now, old.noise, now + 6000, 0, old.nextSurge), now);
	}
	private void put(BlockPos pos, Zone zone, long now) {
		// Bounded Save State: Expired quiet cities are discarded; offline time does not advance world ticks.
		zones.entrySet().removeIf(entry -> { var value = entry.getValue().decay(now); return value.pressure == 0 && value.suppressed <= now && value.nextSurge <= now; });
		if (zones.size() >= 4096 && !zones.containsKey(key(pos))) {
			zones.entrySet().stream().min(java.util.Comparator.comparingLong(entry -> entry.getValue().updated)).ifPresent(entry -> zones.remove(entry.getKey()));
		}
		zones.put(key(pos), zone); setDirty();
	}
}
