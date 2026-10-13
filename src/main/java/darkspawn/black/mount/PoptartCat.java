package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Summon-only riding prototype. */
public final class PoptartCat extends PrototypeMount {
    public PoptartCat(EntityType<? extends PoptartCat> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 28).add(Attributes.MOVEMENT_SPEED, .28F);
    }
    @Override protected boolean canFly() { return true; }
    @Override protected float groundSpeed() { return .28F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_flying_mounted"; }
}
