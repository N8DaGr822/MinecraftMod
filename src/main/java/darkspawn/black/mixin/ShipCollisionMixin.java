package darkspawn.black.mixin;

import darkspawn.black.pirate.PirateShip;
import darkspawn.black.pirate.ShipCollision;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ShipCollisionMixin {
    @Unique private PirateShip darkspawn$deck;
    @Unique private Vec3 darkspawn$deckPosition;
    @Unique private float darkspawn$deckYaw;

    @Inject(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void darkspawn$shipShapes(Entity entity, Level level, List<VoxelShape> shapes, AABB area, CallbackInfoReturnable<List<VoxelShape>> callback) {
        callback.setReturnValue(ShipCollision.append(entity, level, area, callback.getReturnValue()));
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void darkspawn$carryDeck(CallbackInfo callback) {
        var entity = (Entity)(Object)this;
        // Deck Transport: Follow translation and yaw while grounded; jumping and boarding release support.
        if (darkspawn$deck != null && !darkspawn$deck.isRemoved() && !darkspawn$deck.docked()
                && darkspawn$deck.level() == entity.level() && entity.onGround() && !entity.isPassenger()) {
            var target = ShipCollision.carriedPosition(entity, darkspawn$deckPosition, darkspawn$deckYaw, darkspawn$deck);
            var movement = target.subtract(entity.position());
            if (movement.lengthSqr() < 4) {
                entity.move(MoverType.SELF, movement);
                // Ground Support: Horizontal carry clears native grounding; restore only over an actual deck.
                if (ShipCollision.supporting(entity) != null) entity.setOnGround(true);
            }
        }
        darkspawn$deck = entity.onGround() ? ShipCollision.supporting(entity) : null;
        if (darkspawn$deck != null) {
            darkspawn$deckPosition = darkspawn$deck.position();
            darkspawn$deckYaw = darkspawn$deck.getYRot();
        }
    }
}
