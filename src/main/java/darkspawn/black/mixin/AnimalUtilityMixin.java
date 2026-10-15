package darkspawn.black.mixin;
import darkspawn.black.cooking.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Animal.class)
public abstract class AnimalUtilityMixin implements AnimalUtilityState.Holder {
    @Unique private final AnimalUtilityState darkspawn$utility=new AnimalUtilityState();
    public AnimalUtilityState darkspawn$animalUtility(){return darkspawn$utility;}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void read(ValueInput in,CallbackInfo ci){darkspawn$utility.readyAt=Math.max(0,in.getLongOr("darkspawn_utility_ready",0));darkspawn$utility.egg=Math.clamp(in.getIntOr("darkspawn_utility_egg",0),0,2);darkspawn$utility.forage=in.read("darkspawn_forage",net.minecraft.core.BlockPos.CODEC).orElse(null);darkspawn$utility.forageUntil=in.getLongOr("darkspawn_forage_until",0);}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void write(ValueOutput out,CallbackInfo ci){out.putLong("darkspawn_utility_ready",darkspawn$utility.readyAt);out.putInt("darkspawn_utility_egg",darkspawn$utility.egg);if(darkspawn$utility.forage!=null)out.store("darkspawn_forage",net.minecraft.core.BlockPos.CODEC,darkspawn$utility.forage);out.putLong("darkspawn_forage_until",darkspawn$utility.forageUntil);}
    @Inject(method="aiStep",at=@At("TAIL")) private void tick(CallbackInfo ci){AnimalUtility.tick((Animal)(Object)this);}
}
