package darkspawn.black.forsaken;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class ContainmentKeyItem extends Item {
	public ContainmentKeyItem(Properties properties) { super(properties); }
	@Override public InteractionResult useOn(UseOnContext context) {
		if (!context.getLevel().getBlockState(context.getClickedPos()).is(Forsaken.GATE)) return InteractionResult.PASS;
		if (context.getPlayer() instanceof ServerPlayer player) Forsaken.activate(player, context.getClickedPos());
		return InteractionResult.SUCCESS;
	}
}
