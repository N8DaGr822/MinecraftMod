package darkspawn.black.forsaken;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class QuarantineTerminalBlock extends Block {
	private final boolean extraction;
	public QuarantineTerminalBlock(Properties properties, boolean extraction) { super(properties); this.extraction = extraction; }
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
			if (extraction) Forsaken.leave(serverPlayer, pos);
			else {
				Forsaken.state(serverPlayer).researched = true;
				Forsaken.message(serverPlayer, "researched");
				Forsaken.openFabricator(serverPlayer, pos);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
