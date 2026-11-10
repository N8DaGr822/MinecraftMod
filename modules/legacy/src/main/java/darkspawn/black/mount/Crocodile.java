package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Swamp mount with ground travel and controlled swimming. */
public final class Crocodile extends PrototypeMount {
    public Crocodile(EntityType<? extends Crocodile> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 48).add(Attributes.MOVEMENT_SPEED, .25);
    }
    @Override protected boolean canFly() { return false; }
    @Override protected boolean canSwim() { return true; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected double swimmingSpeed() { return .32; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_swimming_mounted"; }
}
