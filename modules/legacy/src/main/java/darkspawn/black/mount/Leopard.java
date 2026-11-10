package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Jungle mount with a faster stride and a higher grounded jump. */
public final class Leopard extends PrototypeMount {
    public Leopard(EntityType<? extends Leopard> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 32).add(Attributes.MOVEMENT_SPEED, .4)
            .add(Attributes.JUMP_STRENGTH, .65).add(Attributes.SAFE_FALL_DISTANCE, 6);
    }
    @Override protected boolean canFly() { return false; }
    @Override protected float groundSpeed() { return .4F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
