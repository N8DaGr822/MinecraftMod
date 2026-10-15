package darkspawn.black.endgame;

import darkspawn.black.Darkspawn;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Four scarce alternatives earned through the Nexus; existing empowerment and armor remain relevant. */
public final class LegendaryRelics {
    private static Item.Properties properties(String id){return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Darkspawn.id(id))).rarity(Rarity.EPIC).fireResistant();}
    private static Item register(String id,Item item){return Registry.register(BuiltInRegistries.ITEM,Darkspawn.id(id),item);}
    public static final Item CORE=register("nexus_core",new Item(properties("nexus_core")));
    public static final Item TITANBREAKER=register("titanbreaker",new MaceItem(properties("titanbreaker").durability(750).component(DataComponents.TOOL,MaceItem.createToolProperties()).attributes(MaceItem.createAttributes()).enchantable(15).repairable(Items.NETHERITE_INGOT).component(DataComponents.WEAPON,new Weapon(1))));
    public static final Item VOIDBOW=register("voidbow",new BowItem(properties("voidbow").durability(768).enchantable(10).repairable(Items.NETHERITE_INGOT)){
        @Override protected void shootProjectile(LivingEntity shooter,Projectile projectile,int index,float power,float uncertainty,float angle,LivingEntity target){
            super.shootProjectile(shooter,projectile,index,power,uncertainty,angle,target);
            if(projectile instanceof AbstractArrow arrow){arrow.setNoGravity(true);arrow.addTag("darkspawn_void_arrow");arrow.addTag("darkspawn_void_until_"+(shooter.level().getGameTime()+100));}
        }
    });
    public static final Item CARAPACE=register("queens_carapace",new ShieldItem(properties("queens_carapace").durability(672).repairable(Items.NETHERITE_INGOT).equippableUnswappable(EquipmentSlot.OFFHAND)
        .component(DataComponents.BANNER_PATTERNS,net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY)
        .delayedComponent(DataComponents.BLOCKS_ATTACKS,context->new BlocksAttacks(.25F,1F,List.of(new BlocksAttacks.DamageReduction(90F,Optional.empty(),0F,1F)),new BlocksAttacks.ItemDamageFunction(3F,1F,1F),Optional.of(context.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)))));
    public static final Item LANTERN=register("soul_lantern_relic",new Item(properties("soul_lantern_relic").stacksTo(1)){
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer server&&player.isAlive()&&!player.isSpectator()&&!player.getCooldowns().isOnCooldown(player.getItemInHand(hand))){
                for(var mob:level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(16),e->e instanceof Enemy&&e.isAlive()&&player.hasLineOfSight(e)))
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING,200,0));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240,0));player.getCooldowns().addCooldown(player.getItemInHand(hand),200);
                server.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul Lantern: nearby visible hostile souls revealed for ten seconds."));
            }
            return InteractionResult.SUCCESS;
        }
    });
    private LegendaryRelics() { }
    public static void initialize(){
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(e->{for(var item:List.of(CORE,TITANBREAKER,VOIDBOW,CARAPACE,LANTERN))e.accept(item);});
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim,source,base,damage,blocked)->{
            // Direct melee only: projectiles and reflected damage cannot recursively trigger the heavy strike.
            if(damage>0&&victim instanceof Enemy&&source.getDirectEntity() instanceof ServerPlayer player&&player.getMainHandItem().is(TITANBREAKER)&&player.getOffhandItem().isEmpty())victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,40,1));
            if(blocked&&victim instanceof ServerPlayer player&&player.getUseItem().is(CARAPACE)&&source.getDirectEntity() instanceof LivingEntity attacker&&attacker instanceof Enemy)attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,60,1));
        });
    }
}
