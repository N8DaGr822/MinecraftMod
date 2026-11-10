package darkspawn.black.noah.mixin;

import darkspawn.black.noah.NoahLegacy;
import darkspawn.black.noah.TempestProjectile;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class TempestProjectileMixin implements TempestProjectile {
    @Unique private static final EntityDataAccessor<Boolean> DARKSPAWN_TEMPEST = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BOOLEAN);

    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void darkspawn$defineAppearance(SynchedEntityData.Builder data,CallbackInfo callback) {
        data.define(DARKSPAWN_TEMPEST,false);
    }

    // Projectile Appearance: Initialize before tracking; derive again on load or item-slot replacement.
    @Inject(method={
        "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",
        "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
        "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V",
        "readAdditionalSaveData"
    },at=@At("TAIL"),require=4)
    private void darkspawn$restoreAppearance(CallbackInfo callback) { darkspawn$syncAppearance(); }

    @Inject(method="tick",at=@At("HEAD"))
    private void darkspawn$refreshAppearance(CallbackInfo callback) { darkspawn$syncAppearance(); }

    @Unique private void darkspawn$syncAppearance() {
        var trident=(ThrownTrident)(Object)this;
        if(!trident.level().isClientSide())trident.getEntityData().set(DARKSPAWN_TEMPEST,trident.getWeaponItem().is(NoahLegacy.TEMPEST));
    }

    @Override public boolean darkspawn$isTempest() {
        return ((ThrownTrident)(Object)this).getEntityData().get(DARKSPAWN_TEMPEST);
    }
}
