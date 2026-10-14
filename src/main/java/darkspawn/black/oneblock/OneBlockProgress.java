package darkspawn.black.oneblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class OneBlockProgress extends SavedData {
	public static final String[] PHASES = { "grassland", "stone", "farmland", "deep_caves", "nether", "end" };
	public static final int[] REQUIRED = { 300, 600, 800, 1000, 1200, 0 };
	public static final Codec<OneBlockProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.intRange(0, 5).fieldOf("phase").forGetter(value -> value.phase),
		Codec.intRange(0, Integer.MAX_VALUE).fieldOf("breaks").forGetter(value -> value.breaks),
		Codec.intRange(0, Integer.MAX_VALUE).fieldOf("total").forGetter(value -> value.total),
		Codec.BOOL.fieldOf("initialized").forGetter(value -> value.initialized),
		Codec.intRange(0, 19).fieldOf("resource").forGetter(value -> value.resource)
	).apply(instance, OneBlockProgress::new));
	public static final SavedDataType<OneBlockProgress> TYPE = new SavedDataType<>(Darkspawn.id("oneblock"), OneBlockProgress::new, CODEC, null);
	public int phase, breaks, total, resource;
	public boolean initialized;
	public long lastBreakTick = Long.MIN_VALUE;
	public OneBlockProgress() { }
	private OneBlockProgress(int phase, int breaks, int total, boolean initialized, int resource) {
		this.phase = phase; this.breaks = breaks; this.total = total; this.initialized = initialized; this.resource = resource;
	}
	public boolean ready() { return phase < 5 && breaks >= REQUIRED[phase]; }
	public boolean advance() { if (!ready()) return false; phase++; breaks = 0; setDirty(); return true; }
	public void mined() { if (breaks < Integer.MAX_VALUE) breaks++; if (total < Integer.MAX_VALUE) total++; setDirty(); }
}
