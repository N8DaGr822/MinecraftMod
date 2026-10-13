package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Summon-only movement prototype; no taming or cargo yet. */
public final class WoollyRhino extends PrototypeMount {
    public WoollyRhino(EntityType<? extends WoollyRhino> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, .30F);
    }
    @Override protected boolean canFly() { return false; }
    @Override protected boolean canGlide() { return false; }
    @Override protected float groundSpeed() { return .30F; }
    @Override protected String mountingMessage() { return "message.darkspawn.woolly_rhino_mounted"; }
}
