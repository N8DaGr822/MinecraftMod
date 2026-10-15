package darkspawn.black.mixin;
import darkspawn.black.cooking.*;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Chicken.class)
public abstract class ChickenUtilityMixin {
    @Inject(method="aiStep",at=@At("HEAD")) private void specialEgg(CallbackInfo ci){
        var chicken=(Chicken)(Object)this;var s=AnimalUtility.state(chicken);
        if(chicken.level() instanceof net.minecraft.server.level.ServerLevel level&&chicken.isAlive()&&!chicken.isBaby()&&!chicken.isChickenJockey()&&chicken.eggTime<=1&&s.egg!=0){
            int egg=s.egg;s.egg=0;chicken.eggTime=6001+chicken.getRandom().nextInt(6000);
            chicken.spawnAtLocation(level,new net.minecraft.world.item.ItemStack(egg==1?AnimalUtility.RICH_EGG:AnimalUtility.HERBED_EGG));chicken.playSound(net.minecraft.sounds.SoundEvents.CHICKEN_EGG,1,1);
        }
    }
}
