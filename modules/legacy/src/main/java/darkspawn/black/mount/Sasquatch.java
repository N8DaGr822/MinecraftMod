package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Naturally spawning riding prototype. */
public final class Sasquatch extends PrototypeMount {
    public Sasquatch(EntityType<? extends Sasquatch> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.MOVEMENT_SPEED, .30F);
    }
    @Override protected boolean canFly() { return false; }
    @Override protected float groundSpeed() { return .30F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
