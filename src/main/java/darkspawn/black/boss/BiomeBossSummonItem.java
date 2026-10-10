package darkspawn.black.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

public final class BiomeBossSummonItem extends Item {
	private final BossProfile profile;
	public BiomeBossSummonItem(Properties properties, BossProfile profile) { super(properties); this.profile = profile; }
	public BossProfile profile() { return profile; }
	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getPlayer() instanceof ServerPlayer player) || !(context.getLevel() instanceof ServerLevel level)) { return InteractionResult.SUCCESS; }
		return summon(level, player, context.getClickedPos(), context.getItemInHand());
	}
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (profile != BossProfile.KRAKEN) { return InteractionResult.PASS; }
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) { return InteractionResult.SUCCESS; }
		var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
		if (hit.getType() != HitResult.Type.BLOCK) { return fail(serverPlayer, "kraken_water_required"); }
		return summon(server, serverPlayer, hit.getBlockPos(), player.getItemInHand(hand));
	}
	private InteractionResult summon(ServerLevel level, ServerPlayer player, BlockPos clicked, ItemStack stack) {
		if (!player.isAlive() || player.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL) { return fail(player, "summon_unavailable"); }
		ServerLevel end = level.getServer().getLevel(Level.END);
		if (end == null || end.getDragonFight() == null || !end.getDragonFight().hasPreviouslyKilledDragon()) { return fail(player, "dragon_required"); }
		// Ship Ritual: Place the Kraken ahead of the casting boat, outside the boat's own collision box.
		if (profile == BossProfile.KRAKEN) {
			var forward = player.getLookAngle().multiply(1, 0, 1);
			if (forward.lengthSqr() < 0.01) { forward = new net.minecraft.world.phys.Vec3(player.getDirection().getStepX(), 0, player.getDirection().getStepZ()); }
			forward = forward.normalize().scale(18);
			clicked = BlockPos.containing(player.getX() + forward.x, clicked.getY(), player.getZ() + forward.z);
		}
		BlockPos pos = clicked.above();
		boolean caveFallback = profile == BossProfile.CAVE_CRAWLER && pos.getY() < 48 && !level.canSeeSky(pos) && !level.getBiome(pos).is(Biomes.DEEP_DARK);
		if (!level.dimension().equals(profile.dimension()) || !(level.getBiome(pos).is(profile.biomes()) || caveFallback)
				|| profile != BossProfile.KRAKEN && !level.getBlockState(clicked).is(profile.altar)) {
			player.sendOverlayMessage(Component.translatable("ritual.darkspawn." + profile.id()));
			return InteractionResult.FAIL;
		}
		if (profile == BossProfile.THUNDER_BIRD && !level.isThundering()) { return fail(player, "storm_required"); }
		if (profile == BossProfile.BABA_YAGA && !MutantWolfSummonItem.isRitualNight(level.getOverworldClockTime())) { return fail(player, "night_required"); }
		if (profile.underground() && level.canSeeSky(pos)) { return fail(player, "underground_required"); }
		if (!profile.underground() && profile.dimension().equals(Level.OVERWORLD) && !level.canSeeSky(pos)) { return fail(player, "open_sky_required"); }
		if (profile == BossProfile.KRAKEN) {
			for (int x : new int[] {-5, 0, 5}) {
				for (int z : new int[] {-5, 0, 5}) {
					for (int depth = 0; depth < 7; depth++) {
						if (!level.getFluidState(clicked.offset(x, -depth, z)).is(FluidTags.WATER)) { return fail(player, "kraken_water_required"); }
					}
				}
			}
			pos = clicked.below(4);
		} else if (profile.flying()) { pos = pos.above(8); }
		if (!level.getEntitiesOfClass(BiomeBoss.class, new AABB(pos).inflate(128), boss -> boss.profile() == profile && boss.isAlive()).isEmpty()) {
			return fail(player, "family_boss_present");
		}
		BiomeBoss boss = new BiomeBoss(BossEntities.BIOME_BOSSES.get(profile), level, profile);
		boss.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		AABB box = boss.getBoundingBox();
		if (box.maxY > level.getMaxY() + 1 || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(boss, box)
				|| profile != BossProfile.KRAKEN && level.containsAnyLiquid(box)) {
			player.sendOverlayMessage(Component.translatable("message.darkspawn.ritual_space", (int) profile.width, (int) profile.height));
			return InteractionResult.FAIL;
		}
		boss.prepareEncounter(level);
		if (!level.addFreshEntity(boss)) { return InteractionResult.FAIL; }
		stack.consume(1, player);
		darkspawn.black.cooking.Culinary.award(player, "first_summon", "earned");
		player.sendOverlayMessage(Component.translatable("message.darkspawn.boss_awakened", boss.getName()));
		return InteractionResult.SUCCESS;
	}
	private static InteractionResult fail(ServerPlayer player, String key) {
		player.sendOverlayMessage(Component.translatable("message.darkspawn." + key));
		return InteractionResult.FAIL;
	}
}
