package darkspawn.black.ecosystem;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class ChargedFeatherItem extends Item {
	public ChargedFeatherItem(Properties properties) { super(properties); }

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var level = context.getLevel();
		var player = context.getPlayer();
		var pos = context.getClickedPos();
		if (player == null || !player.isAlive() || player.isSpectator() || !level.dimension().equals(Level.OVERWORLD)
				|| !level.isThundering() || !level.canSeeSky(pos.above())
				|| level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY() + 1
				|| !level.getBlockState(pos).is(Blocks.COPPER_BLOCK.weathering().unaffected())) { return InteractionResult.PASS; }
		if (!(level instanceof ServerLevel server)) { return InteractionResult.CONSUME; }
		// Weather Ritual: One accepted use exchanges one feather; the copper block and terrain stay intact.
		context.getItemInHand().consume(1, player);
		var reward = new ItemStack(RegionalEcosystems.STORMFORGED_FEATHER);
		if (!player.getInventory().add(reward)) { player.spawnAtLocation(server, reward); }
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + .5, pos.getY() + 1.2, pos.getZ() + .5, 30, .4, .4, .4, .05);
		return InteractionResult.SUCCESS_SERVER;
	}
}
