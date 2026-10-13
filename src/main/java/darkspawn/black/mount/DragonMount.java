package darkspawn.black.mount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Summon-only riding prototype. */
public final class DragonMount extends PrototypeMount {
    public DragonMount(EntityType<? extends DragonMount> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, .25F);
    }
    @Override protected boolean canFly() { return true; }
    @Override protected float groundSpeed() { return .25F; }
    @Override protected String mountingMessage() { return "message.darkspawn.mount_flying_mounted"; }
}
