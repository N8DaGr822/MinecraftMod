package darkspawn.black.boss;

import darkspawn.black.Darkspawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class TreeSpiritSummonItem extends Item {
	public static final TagKey<Biome> FORESTS = TagKey.create(Registries.BIOME, Darkspawn.id("tree_spirit_forests"));

	public TreeSpiritSummonItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getPlayer() instanceof ServerPlayer player) || !(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		if (!player.isAlive() || player.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL) {
			return fail(player, "summon_unavailable");
		}
		ServerLevel end = level.getServer().getLevel(Level.END);
		if (end == null || end.getDragonFight() == null || !end.getDragonFight().hasPreviouslyKilledDragon()) {
			return fail(player, "dragon_required");
		}
		BlockPos pos = context.getClickedPos().above();
		if (!level.dimension().equals(Level.OVERWORLD) || !level.getBiome(pos).is(FORESTS)
				|| !level.getBlockState(context.getClickedPos()).is(Blocks.MOSS_BLOCK)) {
			return fail(player, "forest_ritual_required");
		}
		AABB arena = new AABB(pos).inflate(128);
		if (!level.getEntitiesOfClass(AncientTreeSpirit.class, arena, boss -> boss.isAlive()).isEmpty()) {
			return fail(player, "boss_already_present");
		}
		AncientTreeSpirit boss = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
		boss.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		// Summon Validation: Never carve an arena through builds or consume the item on a failed spawn.
		if (!level.canSeeSky(pos) || boss.getBoundingBox().maxY > level.getMaxY() + 1
				|| !level.getWorldBorder().isWithinBounds(boss.getBoundingBox())
				|| !level.noCollision(boss, boss.getBoundingBox()) || level.containsAnyLiquid(boss.getBoundingBox())) {
			return fail(player, "boss_needs_space");
		}
		boss.prepareEncounter(level);
		if (!level.addFreshEntity(boss)) {
			return InteractionResult.FAIL;
		}
		context.getItemInHand().consume(1, player);
		player.sendOverlayMessage(Component.translatable("message.darkspawn.tree_awakened"));
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult fail(ServerPlayer player, String key) {
		player.sendOverlayMessage(Component.translatable("message.darkspawn." + key));
		return InteractionResult.FAIL;
	}
}
