package darkspawn.black.forsaken;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public final class PulseBladeItem extends Item {
	public static final int CHARGE_TICKS = 30, COOLDOWN_TICKS = 160;
	public PulseBladeItem(Properties properties) { super(properties); }
	@Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }
	@Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) return InteractionResult.FAIL;
		player.startUsingItem(hand); return InteractionResult.CONSUME;
	}
	@Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		int charge = getUseDuration(stack, entity) - remaining;
		if (level instanceof ServerLevel server && charge <= CHARGE_TICKS && charge % 5 == 0) {
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 1, entity.getZ(), 4, .35, .35, .35, .01);
			if (charge == CHARGE_TICKS) server.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .8F, 1.4F);
		}
	}
	@Override public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
		if (!(entity instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
			|| player.getMainHandItem() != stack || player.getUseItem() != stack || player.getCooldowns().isOnCooldown(stack)
			|| getUseDuration(stack, entity) - remaining < CHARGE_TICKS) return false;
		var server = player.level();
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		// Clearing Pulse: Only visible, non-allied infected qualify; neither terrain nor other entity types are touched.
		server.getEntitiesOfClass(Infected.class, player.getBoundingBox().inflate(4), target -> target.isAlive()
			&& player.distanceToSqr(target) <= 16 && !player.isAlliedTo(target) && player.hasLineOfSight(target)).stream()
			.sorted(java.util.Comparator.comparingDouble(player::distanceToSqr)).limit(16).forEach(target -> {
				if (target.hurtServer(server, player.damageSources().indirectMagic(player, player), 10)) {
					target.knockback(.8, player.getX() - target.getX(), player.getZ() - target.getZ(), player.damageSources().indirectMagic(player, player), 10);
				}
			});
		for (int i = 0; i < 32; i++) {
			double angle = i * Math.PI / 16;
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX() + Math.cos(angle) * 4, player.getY() + .8,
				player.getZ() + Math.sin(angle) * 4, 1, 0, .15, 0, 0);
		}
		server.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1, .6F);
		stack.hurtAndBreak(8, player, InteractionHand.MAIN_HAND); return true;
	}
}
