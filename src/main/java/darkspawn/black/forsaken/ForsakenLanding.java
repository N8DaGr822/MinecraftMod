package darkspawn.black.forsaken;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ForsakenLanding extends SavedData {
	public static final Codec<ForsakenLanding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.intRange(1, 1).optionalFieldOf("version", 1).forGetter(value -> 1),
		Codec.BOOL.optionalFieldOf("initialized", false).forGetter(value -> value.initialized)
	).apply(instance, (version, initialized) -> new ForsakenLanding(initialized)));
	public static final SavedDataType<ForsakenLanding> TYPE = new SavedDataType<>(Darkspawn.id("forsaken_landing"), () -> new ForsakenLanding(false), CODEC, null);
	private boolean initialized;
	private ForsakenLanding(boolean initialized) { this.initialized = initialized; }
	public static void prepare(ServerLevel level) {
		var data = level.getDataStorage().computeIfAbsent(TYPE);
		if (data.initialized) return;
		// One-Time Landing: Subsequent visits and restarts preserve all player changes.
		for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
			var pos = Forsaken.LANDING.offset(x, -1, z);
			level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
			for (int y = 1; y <= 4; y++) level.setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
			if (Math.abs(x) == 6 || Math.abs(z) == 6) {
				if (z != 6 || Math.abs(x) > 1) for (int y = 1; y <= 3; y++) level.setBlockAndUpdate(pos.above(y), Blocks.IRON_BARS.defaultBlockState());
			}
			if (Math.abs(x) == 5 && Math.abs(z) == 5) level.setBlockAndUpdate(pos.above(), Blocks.SEA_LANTERN.defaultBlockState());
		}
		level.setBlockAndUpdate(Forsaken.EXIT, Forsaken.RETURN.defaultBlockState());
		data.initialized = true; data.setDirty();
	}
}
