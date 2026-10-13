package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/** Ocean mount; riders still need their own air supply or Water Breathing. */
public final class Seahorse extends PrototypeMount {
    public Seahorse(EntityType<? extends Seahorse> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, .05);
    }
    @Override protected boolean canFly() { return false; }
    @Override protected boolean canSwim() { return true; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected float groundSpeed() { return .05F; }
    @Override protected double swimmingSpeed() { return .38; }
    @Override public boolean checkSpawnObstruction(LevelReader level) { return level.isUnobstructed(this); }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_swimming_mounted"; }
}
