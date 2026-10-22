package darkspawn.black.pirate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

/** Boarding Sentries: Stationed archers avoid native navigation over virtual ship blocks. */
public final class BoardingDefender extends Skeleton {
    public BoardingDefender(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        setNoAi(true); setNoGravity(true); setPersistenceRequired(); xpReward = 0;
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return Boarding.canTarget(this, source.getEntity()) && super.hurtServer(level, source, amount);
    }
    @Override protected void dropAllDeathLoot(ServerLevel level, DamageSource source) { }
}
