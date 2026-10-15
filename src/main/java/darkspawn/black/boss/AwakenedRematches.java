package darkspawn.black.boss;

import darkspawn.black.Darkspawn;
import darkspawn.black.health.BossHearts;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

/** Offhand seals opt into harder native rituals without bypassing progression or spawning extra hearts. */
public final class AwakenedRematches {
    public static final String TAG="darkspawn_awakened_rematch";
    public static final Item SEAL=Registry.register(BuiltInRegistries.ITEM,Darkspawn.id("awakening_seal"),new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM,Darkspawn.id("awakening_seal"))).stacksTo(16)
        .component(DataComponents.LORE,new ItemLore(java.util.List.of(Component.literal("Hold in offhand during a completed boss family's normal summon ritual."),Component.literal("Awakened: +50% health, +4 armor, +10% movement. Cosmetic victory rewards."))))));
    private AwakenedRematches() { }
    public static boolean requested(ServerPlayer player){return player.getOffhandItem().is(SEAL);}
    public static boolean eligible(ServerPlayer player,BossKind kind) {
        var advancement=player.level().getServer().getAdvancements().get(Darkspawn.id("progress/defeat_"+kind.id()));
        return BossHearts.progress(player).hasConsumed(kind)||advancement!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
    public static boolean prepare(ServerPlayer player,LivingEntity boss,BossKind kind) {
        if(!requested(player))return true;
        if(!eligible(player,kind)){player.sendSystemMessage(Component.literal("Defeat this boss family normally before using an Awakening Seal."));return false;}
        boss.addTag(TAG);
        boss.getAttribute(BossEntities.MAX_HEALTH).addOrReplacePermanentModifier(new AttributeModifier(Darkspawn.id("awakened_health"),.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        boss.getAttribute(Attributes.ARMOR).addOrReplacePermanentModifier(new AttributeModifier(Darkspawn.id("awakened_armor"),4,AttributeModifier.Operation.ADD_VALUE));
        if(boss.getAttribute(Attributes.MOVEMENT_SPEED)!=null)boss.getAttribute(Attributes.MOVEMENT_SPEED).addOrReplacePermanentModifier(new AttributeModifier(Darkspawn.id("awakened_stride"),.1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        boss.setHealth(boss.getMaxHealth());boss.setCustomName(Component.literal("Awakened ").append(boss.getName()));boss.setCustomNameVisible(true);return true;
    }
    public static void consume(ServerPlayer player,LivingEntity boss){if(boss.entityTags().contains(TAG))player.getOffhandItem().consume(1,player);}
    public static ItemStack trophy(BossKind kind) {
        var stack=new ItemStack(darkspawn.black.cooking.Cuisine.TROPHIES.get(kind));
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Awakened "+kind.id().replace('_',' ')+" Trophy"));stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        stack.set(DataComponents.LORE,new ItemLore(java.util.List.of(Component.literal("Earned in an awakened rematch. Display trophy; no combat bonus."))));return stack;
    }
    public static ItemStack cosmetic(BossKind kind) {
        var stack=new ItemStack(Items.LEATHER_HELMET);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Awakened Victor's Laurel: "+kind.id().replace('_',' ')));
        stack.set(DataComponents.DYED_COLOR,new DyedItemColor(0x704020+kind.ordinal()*0x070809));stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        stack.set(DataComponents.LORE,new ItemLore(java.util.List.of(Component.literal("Cosmetic victory colors. Ordinary leather armor; no new combat power."))));return stack;
    }
    public static void initialize(){net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries->entries.accept(SEAL));}
}
