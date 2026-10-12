package darkspawn.black.forsaken;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ForsakenPlayerState {
	public boolean researched;
	public GlobalPos entrance;
	public long lastTravel = Long.MIN_VALUE;
	public void read(ValueInput input) {
		researched = input.getBooleanOr("darkspawn_forsaken_researched", false);
		entrance = input.read("darkspawn_forsaken_entrance", GlobalPos.CODEC).orElse(null);
		lastTravel = Long.MIN_VALUE;
	}
	public void write(ValueOutput output) {
		output.putInt("darkspawn_forsaken_version", 1);
		output.putBoolean("darkspawn_forsaken_researched", researched);
		if (entrance != null) output.store("darkspawn_forsaken_entrance", GlobalPos.CODEC, entrance);
	}
	public void copyFrom(ForsakenPlayerState old) {
		// Expedition Death: Knowledge and extraction links survive; vanilla owns active inventory and respawn.
		researched = old.researched; entrance = old.entrance;
	}
}
