package darkspawn.black.ecosystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WolfHuntLedger extends SavedData {
	public record Entry(UUID player, long next, long offer, boolean reward) {
		private static final Codec<Long> TIME = Codec.LONG.validate(value -> value >= 0
			? com.mojang.serialization.DataResult.success(value) : com.mojang.serialization.DataResult.error(() -> "Negative hunt time"));
		private static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
			TIME.fieldOf("next").forGetter(Entry::next),
			TIME.fieldOf("offer").forGetter(Entry::offer),
			Codec.BOOL.fieldOf("reward").forGetter(Entry::reward)).apply(i, Entry::new));
	}
	public static final Codec<WolfHuntLedger> CODEC = Entry.CODEC.listOf().xmap(WolfHuntLedger::new, l -> List.copyOf(l.entries.values()));
	public static final SavedDataType<WolfHuntLedger> TYPE = new SavedDataType<>(Darkspawn.id("wolf_hunts"),
		() -> new WolfHuntLedger(List.of()), CODEC, null);
	private final Map<UUID, Entry> entries = new HashMap<>();

	public WolfHuntLedger(List<Entry> saved) { saved.forEach(e -> entries.put(e.player(), e)); }
	public static WolfHuntLedger get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE); }
	private Entry entry(UUID id) { return entries.getOrDefault(id, new Entry(id, 0, 0, false)); }
	public boolean reward(UUID id) { return entry(id).reward(); }
	public long remaining(UUID id, long now) { return Math.max(0, entry(id).next() - now); }
	public boolean ready(UUID id, long now) { return !reward(id) && remaining(id, now) == 0; }
	public boolean offer(UUID id, long now, long delay) {
		var e = entry(id);
		if (!ready(id, now) || now < e.offer()) return false;
		entries.put(id, new Entry(id, e.next(), now + delay, e.reward())); setDirty(); return true;
	}
	public void started(UUID id, long now, long delay) {
		entries.put(id, new Entry(id, now + delay, now + delay, false)); setDirty();
	}
	public void won(UUID id) {
		var e = entry(id);
		entries.put(id, new Entry(id, e.next(), e.offer(), true)); setDirty();
	}
	public boolean claim(UUID id) {
		var e = entry(id);
		if (!e.reward()) return false;
		entries.put(id, new Entry(id, e.next(), e.offer(), false)); setDirty(); return true;
	}
}
