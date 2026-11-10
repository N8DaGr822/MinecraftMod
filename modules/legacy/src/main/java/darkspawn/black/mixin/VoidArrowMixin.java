package darkspawn.black.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restore gravity after the short void-flight window, including time spent unloaded. */
@Mixin(AbstractArrow.class)
public abstract class VoidArrowMixin {
    @Unique private String darkspawn$flightTag;
    @Unique private long darkspawn$flightUntil;
    @Inject(method="tick",at=@At("HEAD"))
    private void darkspawn$boundedFlight(CallbackInfo ci){
        var arrow=(AbstractArrow)(Object)this;
        if(arrow.level().isClientSide()||!arrow.entityTags().contains("darkspawn_void_arrow"))return;
        if(darkspawn$flightTag==null){
            darkspawn$flightTag=arrow.entityTags().stream().filter(t->t.startsWith("darkspawn_void_until_")).findFirst().orElse("");
            try{darkspawn$flightUntil=Long.parseLong(darkspawn$flightTag.substring("darkspawn_void_until_".length()));}catch(RuntimeException invalid){darkspawn$flightUntil=0;}
        }
        // Arrow Lifetime: Returning to vanilla gravity prevents permanent hovering entities.
        if(arrow.level().getGameTime()>=darkspawn$flightUntil){arrow.setNoGravity(false);arrow.removeTag("darkspawn_void_arrow");arrow.removeTag(darkspawn$flightTag);}
    }
}
