package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/** Tavern drinks use native temporary effects; another drink refreshes duration, not amplifier. */
public final class PirateDrinks {
    public static final Item BEER = drink("pirate_beer", MobEffects.HASTE, 120, Items.BOWL, "Haste I (2:00) - mining and attack speed");
    public static final Item RUM = drink("pirate_rum", MobEffects.RESISTANCE, 60, Items.GLASS_BOTTLE, "Resistance I (1:00) - reduced incoming damage");
    private PirateDrinks() { }
    private static Item drink(String id, Holder<MobEffect> effect, int seconds, Item container, String description) {
        return Registry.register(BuiltInRegistries.ITEM, Darkspawn.id(id), new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Darkspawn.id(id))).stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(1).saturationModifier(.1F).alwaysEdible().build())
            .component(DataComponents.CONSUMABLE, Consumable.builder().animation(ItemUseAnimation.DRINK)
                .sound(SoundEvents.GENERIC_DRINK).hasConsumeParticles(false)
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(effect, seconds * 20))).build())
            .component(DataComponents.LORE, new ItemLore(List.of(Component.literal(description), Component.literal("Sold at the Rusty Anchor. Drink to refresh; buffs do not increase in strength."))))
            .usingConvertsTo(container)));
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> { entries.accept(BEER); entries.accept(RUM); });
    }
}
