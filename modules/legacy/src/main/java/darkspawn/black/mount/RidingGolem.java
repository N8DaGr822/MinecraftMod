package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Naturally spawning riding prototype. */
public final class RidingGolem extends PrototypeMount {
    public RidingGolem(EntityType<? extends RidingGolem> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, .18F);
    }
    @Override protected boolean canFly() { return false; }
    @Override protected float groundSpeed() { return .18F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
