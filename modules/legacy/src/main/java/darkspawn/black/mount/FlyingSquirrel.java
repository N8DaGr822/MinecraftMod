package darkspawn.black.mount;

import darkspawn.black.mountkit.PrototypeMount;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Naturally spawning movement prototype; no taming or cargo yet. */
public final class FlyingSquirrel extends PrototypeMount {
    @Override public net.minecraft.world.item.Item tamingFood() { return net.minecraft.world.item.Items.SWEET_BERRIES; }
    public FlyingSquirrel(EntityType<? extends FlyingSquirrel> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return PrototypeMount.attributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, .25F);
    }
    @Override public boolean canFly() { return false; }
    @Override public boolean canGlide() { return true; }
    @Override public float groundSpeed() { return .25F; }
    @Override protected String mountingMessage() { return "message.darkspawn.flying_squirrel_mounted"; }
}
