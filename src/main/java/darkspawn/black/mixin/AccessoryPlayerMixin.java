package darkspawn.black.mixin;
import darkspawn.black.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class AccessoryPlayerMixin implements AccessoryPlayer {
    @Unique private AccessoryState darkspawn$accessories;
    // Player Construction: InventoryMenu requests these slots before the player constructor has finished.
    public AccessoryState darkspawn$accessories(){if(darkspawn$accessories==null)darkspawn$accessories=new AccessoryState();return darkspawn$accessories;}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void read(ValueInput input,CallbackInfo ci){darkspawn$accessories().read(input);}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void write(ValueOutput output,CallbackInfo ci){darkspawn$accessories().write(output);}
    @Inject(method="dropEquipment",at=@At("TAIL")) private void drop(ServerLevel level,CallbackInfo ci){
        Player p=(Player)(Object)this;if(level.getGameRules().get(GameRules.KEEP_INVENTORY)||p.isSpectator())return;
        for(int i=0;i<3;i++){var stack=darkspawn$accessories().slots.removeItemNoUpdate(i);if(!stack.isEmpty()&&!net.minecraft.world.item.enchantment.EnchantmentHelper.has(stack,net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)){var drop=p.createItemStackToDrop(stack,true,false);if(drop!=null)level.addFreshEntity(drop);}}
    }
}
