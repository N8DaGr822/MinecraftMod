package darkspawn.black.mount;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Original local block states, independent of the aircraft's current heading. */
public record AirframeBlock(BlockPos pos, BlockState state) {
    public static final Codec<AirframeBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(AirframeBlock::pos),
        BlockState.CODEC.fieldOf("state").forGetter(AirframeBlock::state)
    ).apply(instance, AirframeBlock::new));
}
