package darkspawn.black.mixin;

import darkspawn.black.pirate.ShipCollision;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ShipPlayerMovementMixin {
    @Inject(method = "canFallAtLeast", at = @At("RETURN"), cancellable = true)
    private void darkspawn$deckEdge(double x, double z, double distance, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) return;
        var player = (Player)(Object)this;
        var box = player.getBoundingBox();
        // Sneak Safety: Include moving decks in the same downward support volume as native terrain.
        var below = new AABB(box.minX+1.0E-7+x, box.minY-distance-1.0E-7, box.minZ+1.0E-7+z,
            box.maxX-1.0E-7+x, box.minY, box.maxZ-1.0E-7+z);
        if (!ShipCollision.append(player, player.level(), below, List.of()).isEmpty()) callback.setReturnValue(false);
    }
}
