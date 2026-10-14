package darkspawn.black.pirate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Local block coordinates survive docking, sailing and registry changes across saves. */
public record ShipBlock(BlockPos pos, BlockState state) {
	public static final Codec<ShipBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		BlockPos.CODEC.fieldOf("pos").forGetter(ShipBlock::pos),
		BlockState.CODEC.fieldOf("state").forGetter(ShipBlock::state)
	).apply(instance, ShipBlock::new));
}
