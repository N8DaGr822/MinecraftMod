package darkspawn.black.boss;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class VoidPlatform extends Entity {
	private long expiresAt;

	public VoidPlatform(EntityType<? extends VoidPlatform> type, Level level) {
		super(type, level);
		blocksBuilding = true;
		expiresAt = level.getGameTime() + 1200;
	}
	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) { }
	@Override
	public boolean canBeCollidedWith(Entity other) { return isAlive(); }
	@Override
	public boolean isPickable() { return false; }
	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server && server.getGameTime() >= expiresAt
				&& server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(2, 4, 2)).isEmpty()) {
			// Platform Safety: Platforms appear above solid island ground and never vanish under a standing player.
			discard();
		}
	}
	@Override
	protected void addAdditionalSaveData(ValueOutput output) { output.putLong("expires", expiresAt); }
	@Override
	protected void readAdditionalSaveData(ValueInput input) { expiresAt = input.getLongOr("expires", level().getGameTime() + 1200); }
}
