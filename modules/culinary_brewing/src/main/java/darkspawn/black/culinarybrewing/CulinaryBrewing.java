package darkspawn.black.culinarybrewing;

import darkspawn.black.core.DarkspawnCore;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;

/**
 * Native brewing recipes own potion effects independently of the meal timer. Each base recipe carries a load condition
 * on its reagent, so a family is brewable only when farming or husbandry supplies that ingredient.
 */
public final class CulinaryBrewing {
    public static final Map<String,Holder<Potion>> POTIONS=new LinkedHashMap<>();
    private CulinaryBrewing() { }
    public static void initialize() {
        register("herbal_restoration",MobEffects.REGENERATION,400);
        register("pepper_haste",MobEffects.HASTE,1200);
        register("dairy_guard",MobEffects.RESISTANCE,600);
        register("truffle_vigor",MobEffects.STRENGTH,600);
        register("marsh_breath",MobEffects.WATER_BREATHING,1800);
        register("frostfire_tonic",MobEffects.FIRE_RESISTANCE,1800);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries->POTIONS.values().forEach(p->entries.accept(PotionContents.createItemStack(Items.POTION,p))));
    }
    private static void register(String name,Holder<MobEffect> effect,int duration) {
        for(int variant=0;variant<3;variant++) {
            // Duration-only effects deliberately have no meaningless level-II variant.
            if(variant==2&&(effect==MobEffects.WATER_BREATHING||effect==MobEffects.FIRE_RESISTANCE))continue;
            String id=(variant==1?"long_":variant==2?"strong_":"")+name;
            POTIONS.put(id,Registry.registerForHolder(BuiltInRegistries.POTION,DarkspawnCore.id(id),new Potion(name,new MobEffectInstance(effect,variant==1?duration*2:variant==2?duration/2:duration,variant==2?1:0))));
        }
    }
}
