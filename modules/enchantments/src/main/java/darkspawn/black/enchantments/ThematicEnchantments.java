package darkspawn.black.enchantments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import darkspawn.black.core.DarkspawnCore;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Bounded extra food pools; native loot still owns death, harvest and game-rule handling. */
public final class ThematicEnchantments {
    public static final ResourceKey<Enchantment> HUNTER=key("hunter"),BUTCHER=key("butcher"),FORAGER=key("forager"),SOULWARD=key("soulward");
    private static ResourceKey<Enchantment> key(String id){return ResourceKey.create(Registries.ENCHANTMENT,DarkspawnCore.id(id));}
    private ThematicEnchantments(){ }
    public record FoodBonus(boolean forager) implements LootItemCondition {
        public static final MapCodec<FoodBonus> CODEC=Codec.BOOL.fieldOf("forager").xmap(FoodBonus::new,FoodBonus::forager);
        public static final MapCodec<FoodBonus> TYPE=Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE,DarkspawnCore.id("thematic_food_bonus"),CODEC);
        @Override public MapCodec<FoodBonus> codec(){return TYPE;}
        @Override public boolean test(LootContext context){
            ItemStack tool;
            if(forager){
                var state=context.getOptional(LootContextParams.BLOCK_STATE);
                if(!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player p)||p.isCreative()||p.isSpectator()
                    ||state==null||!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state)
                    ||!(context.getOptional(LootContextParams.TOOL) instanceof ItemStack stack))return false;
                tool=stack;
                if(EnchantmentHelper.getItemEnchantmentLevel(context.getLevel().registryAccess().getOrThrow(Enchantments.SILK_TOUCH),tool)>0)return false;
            }else{
                var source=context.getOptional(LootContextParams.DAMAGE_SOURCE);
                var victim=context.getOptional(LootContextParams.THIS_ENTITY);
                if(source==null||!(source.getDirectEntity() instanceof Player p)||p.isCreative()||p.isSpectator()
                    ||!(victim instanceof AgeableMob animal)||animal.isBaby())return false;
                // Livestock Only: Borrowing a native loot table cannot make a modded boss eligible.
                var id=BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
                if(!id.getNamespace().equals("minecraft")||!java.util.Set.of("cow","mooshroom","pig","sheep","chicken","rabbit").contains(id.getPath()))return false;
                tool=p.getMainHandItem();
            }
            var enchantment=context.getLevel().registryAccess().getOrThrow(forager?FORAGER:BUTCHER);
            if(!enchantment.value().canEnchant(tool))return false;
            int rank=Math.clamp(EnchantmentHelper.getItemEnchantmentLevel(enchantment,tool),0,2);
            return rank>0&&context.getRandom().nextFloat()<rank*.125F;
        }
    }
    public static void initialize(){
        var condition=FoodBonus.TYPE;
        LootTableEvents.MODIFY.register((key,builder,source,registries)->{
            if(!source.isBuiltin())return;
            var item=switch(key.identifier().toString()){
                case "minecraft:entities/cow","minecraft:entities/mooshroom"->Items.BEEF;
                case "minecraft:entities/pig"->Items.PORKCHOP;
                case "minecraft:entities/sheep"->Items.MUTTON;
                case "minecraft:entities/chicken"->Items.CHICKEN;
                case "minecraft:entities/rabbit"->Items.RABBIT;
                case "minecraft:blocks/wheat"->Items.WHEAT;
                case "minecraft:blocks/carrots"->Items.CARROT;
                case "minecraft:blocks/potatoes"->Items.POTATO;
                case "minecraft:blocks/beetroots"->Items.BEETROOT;
                default->null;
            };
            if(item!=null)builder.withPool(LootPool.lootPool().add(LootItem.lootTableItem(item))
                .when(()->new FoodBonus(key.identifier().getPath().startsWith("blocks/"))));
        });
    }
}
