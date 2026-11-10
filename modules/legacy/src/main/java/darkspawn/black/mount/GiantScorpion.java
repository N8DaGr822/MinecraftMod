package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Desert mount with an owner-controlled poison sting. */
public final class GiantScorpion extends PrototypeMount {
    public GiantScorpion(EntityType<? extends GiantScorpion> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 44).add(Attributes.MOVEMENT_SPEED, .3);
    }
    @Override protected boolean canFly() { return false; }
    @Override protected float groundSpeed() { return .3F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_ground_mounted"; }
}
