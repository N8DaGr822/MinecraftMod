package darkspawn.black.health;

import darkspawn.black.boss.BossKind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class BossHeartItem extends Item {
	private final BossKind boss;

	public BossHeartItem(Properties properties, BossKind boss) {
		super(properties);
		this.boss = boss;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			var progress = BossHearts.progress(player);
			boolean duplicate = progress.hasConsumed(boss);
			if (!progress.consume(boss)) {
				serverPlayer.sendOverlayMessage(Component.translatable(duplicate
						? "message.darkspawn.heart_duplicate" : "message.darkspawn.heart_cap"));
				return InteractionResult.FAIL;
			}
			BossHearts.apply(player);
			player.getItemInHand(hand).consume(1, player);
			player.heal(2);
			serverPlayer.sendOverlayMessage(Component.translatable("message.darkspawn.heart_used",
					10 + progress.bonusHealth() / 2, progress.goldenCapacity() / 2));
		}
		return InteractionResult.SUCCESS;
	}
}
