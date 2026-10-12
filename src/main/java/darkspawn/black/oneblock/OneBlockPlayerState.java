package darkspawn.black.oneblock;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class OneBlockPlayerState {
	public OneBlockItems other = OneBlockItems.EMPTY;
	public boolean inside;
	public ResourceKey<Level> returnLevel = Level.OVERWORLD;
	public Vec3 returnPos = new Vec3(0, 80, 0);
	public float returnYaw, returnPitch;
	public int pendingTicks, hurtTicks;
	public Vec3 departure;
	public boolean travelling;

	public void read(ValueInput input) {
		other = input.read("darkspawn_oneblock_other", OneBlockItems.CODEC).orElse(OneBlockItems.EMPTY);
		inside = input.getBooleanOr("darkspawn_oneblock_inside", false);
		returnLevel = input.read("darkspawn_oneblock_return_level", Level.RESOURCE_KEY_CODEC).orElse(Level.OVERWORLD);
		returnPos = input.read("darkspawn_oneblock_return_pos", Vec3.CODEC).orElse(new Vec3(0, 80, 0));
		returnYaw = input.getFloatOr("darkspawn_oneblock_return_yaw", 0); returnPitch = input.getFloatOr("darkspawn_oneblock_return_pitch", 0);
		pendingTicks = 0; travelling = false;
		hurtTicks = Math.clamp(input.getIntOr("darkspawn_oneblock_hurt", 0), 0, 100);
	}
	public void write(ValueOutput output) {
		output.store("darkspawn_oneblock_other", OneBlockItems.CODEC, other);
		output.putBoolean("darkspawn_oneblock_inside", inside);
		output.store("darkspawn_oneblock_return_level", Level.RESOURCE_KEY_CODEC, returnLevel);
		output.store("darkspawn_oneblock_return_pos", Vec3.CODEC, returnPos);
		output.putFloat("darkspawn_oneblock_return_yaw", returnYaw); output.putFloat("darkspawn_oneblock_return_pitch", returnPitch);
		output.putInt("darkspawn_oneblock_hurt", hurtTicks);
	}
	public void copyFrom(OneBlockPlayerState old) {
		// Death: Only the inactive profile is copied here; vanilla owns active item drops and keepInventory.
		other = old.other; inside = old.inside; returnLevel = old.returnLevel; returnPos = old.returnPos;
		returnYaw = old.returnYaw; returnPitch = old.returnPitch;
	}
}
