package darkspawn.black.legacy;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.MountCompanions;
import darkspawn.black.mount.Mounts;
import darkspawn.black.mount.Thalassar;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** The Legacy of Noah: admin-gifted relics with persistent first-use bonds and server-owned powers. */
public final class NoahLegacy {
    private static final String OWNER="NoahOwner";
    public static final Map<ArmorType,Item> ARMOR=new EnumMap<>(ArmorType.class);
    private static Item.Properties properties(String id,String lore) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Darkspawn.id(id))).rarity(Rarity.EPIC).fireResistant()
            .component(DataComponents.LORE,new ItemLore(List.of(Component.literal("The Legacy of Noah • Prince of the Tides"),Component.translatable("tooltip.darkspawn."+lore),Component.translatable("tooltip.darkspawn.noah_bond"))));
    }
    private static Item register(String id,Item item) { return Registry.register(BuiltInRegistries.ITEM,Darkspawn.id(id),item); }
    public static final Item TEMPEST=register("noahs_tempest",new TridentItem(properties("noahs_tempest","noahs_tempest").durability(2031).enchantable(20).repairable(Items.PRISMARINE_SHARD).component(DataComponents.WEAPON,new Weapon(1))
        .attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,12,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED,new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,-2.7,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build())) {
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            var stack=player.getItemInHand(hand);
            if(player instanceof ServerPlayer server) {
                if(!bind(stack,server))return InteractionResult.FAIL;
                if(player.isShiftKeyDown()) {
                    var data=NoahLedger.get(server.level()).entry(player.getUUID());int mode=(data.getIntOr("Mode",0)+1)%3;
                    data.putInt("Mode",mode);NoahLedger.get(server.level()).setDirty();message(server,"Selected: "+powerName(mode));return InteractionResult.SUCCESS;
                }
            } else if(player.isShiftKeyDown())return InteractionResult.SUCCESS;
            return super.use(level,player,hand);
        }
    });
    public static final Item CONCH=register("tideborn_conch",new Item(properties("tideborn_conch","tideborn_conch").stacksTo(1)) {
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            if(player instanceof ServerPlayer server&&bind(player.getItemInHand(hand),server))summon(server,player.isShiftKeyDown());
            return InteractionResult.SUCCESS;
        }
    });
    public static final Item GIFT=register("legacy_of_noah",new Item(properties("legacy_of_noah","legacy_of_noah").stacksTo(1)) {
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            if(player instanceof ServerPlayer server&&server.isAlive()&&!server.isSpectator()) {
                player.getItemInHand(hand).shrink(1);
                for(var item:relics()) {var stack=new ItemStack(item);bind(stack,server);if(!player.getInventory().add(stack))player.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}
                var guide=guide();if(!player.getInventory().add(guide))player.drop(guide,false,net.minecraft.util.Prediction.SERVER_ONLY);
                message(server,"Noah, Heir of Poseidon — the sea remembers its prince.");
            }
            return InteractionResult.SUCCESS;
        }
    });
    private NoahLegacy() { }
    private static ItemStack guide() {
        var book=new ItemStack(Items.WRITTEN_BOOK);
        var pages=List.of(
            "THE LEGACY OF NOAH\n\nWhen oceans raged and storms answered no mortal hand, Poseidon sought an heir worthy of the sea's divine power.",
            "From the crashing tides, he chose Noah: brave of heart and steadfast before the fury of the deep.\n\nA crown, armor, storm trident and royal steed were entrusted to him.",
            "NOAH, HEIR OF POSEIDON\nPrince of the Tides\n\nWhere he calls, storms answer.\nWhere he rides, the deep remembers its prince.",
            "DIVINE REGALIA\n\nWear all four pieces for Blessing of the Tideborn: sea creatures spare you, the depths heal you, and attackers face lightning.\n\nSneak to dive through the water surface.",
            "NOAH'S TEMPEST\n\nUse normally to throw. Sneak-use selects Tidal Burst, Whirlpool Call or Judgment of the Deep.\n\nK casts. B unleashes Wrath of the Seventh Sea. Keys can be changed in Controls.",
            "THALASSAR\n\nUse the conch to summon or recall. Sneak-use lets him rest. Empty-hand use mounts him.\n\nWASD: steer\nSpace / X: swim up / down\nG: Abyssal Dash\nDismount before recall.",
            "NOAH'S DOMINION\n\nWear the full regalia, hold Tempest and ride Thalassar. Your powers recharge faster and B unlocks the ultimate.\n\nIf Thalassar falls, he returns after 30 seconds.");
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(net.minecraft.server.network.Filterable.passThrough("The Legacy of Noah"),"Poseidon",0,
            pages.stream().map(text->net.minecraft.server.network.Filterable.passThrough((Component)Component.literal(text))).toList(),true));
        return book;
    }
    public static List<Item> relics() { var items=new ArrayList<>(ARMOR.values());items.add(TEMPEST);items.add(CONCH);return items; }
    public static boolean bound(ItemStack stack,Player player) { return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr(OWNER,"").equals(player.getUUID().toString()); }
    public static boolean bind(ItemStack stack,ServerPlayer player) {
        if(!player.isAlive()||player.isSpectator()||!relics().contains(stack.getItem()))return false;
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();String owner=tag.getStringOr(OWNER,"");
        if(!owner.isEmpty()) {if(bound(stack,player))return true;message(player,"This relic answers another heir.");return false;}
        tag.putString(OWNER,player.getUUID().toString());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        award(player,"chosen");
        if(stack.is(TEMPEST)) {
            var enchantments=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.enchant(enchantments.getOrThrow(Enchantments.LOYALTY),3);
            stack.enchant(enchantments.getOrThrow(Enchantments.IMPALING),5);
        }
        return true;
    }
    public static boolean wearing(Player player,ArmorType type) { var stack=player.getItemBySlot(type.getSlot());return stack.is(ARMOR.get(type))&&bound(stack,player); }
    public static boolean fullSet(Player player) { return !player.isSpectator()&&player.isAlive()&&ARMOR.keySet().stream().allMatch(t->wearing(player,t)); }
    public static boolean dominion(ServerPlayer player) { return fullSet(player)&&player.getMainHandItem().is(TEMPEST)&&bound(player.getMainHandItem(),player)
        &&player.getVehicle() instanceof Thalassar mount&&mount.isOwnedBy(player)&&mount.isAlive(); }
    public static boolean seaProtected(Mob mob,LivingEntity target) {
        return target instanceof Player player&&fullSet(player)&&(mob.getType()==EntityTypes.DROWNED||mob.getType()==EntityTypes.GUARDIAN||mob.getType()==EntityTypes.ELDER_GUARDIAN);
    }
    public static boolean hostile(ServerPlayer player,LivingEntity entity) {
        if(!(entity instanceof Enemy)||!entity.isAlive()||entity.isSpectator()||entity instanceof Player||player.isAlliedTo(entity)||entity.isAlliedTo(player)
            ||entity instanceof OwnableEntity owned&&owned.getOwnerReference()!=null||darkspawn.black.pirate.PirateShip.loyalCrew(entity)||darkspawn.black.pirate.PirateDiplomacy.contact(entity))return false;
        if(entity instanceof darkspawn.black.ecosystem.RegionalMob mob&&mob.kind().temper!=darkspawn.black.ecosystem.RegionalKind.Temper.HOSTILE)return false;
        if(entity instanceof NeutralMob&&entity instanceof Mob mob&&mob.getTarget()!=player&&mob.getTarget()!=player.getVehicle())return false;
        return entity.getType().getCategory()==MobCategory.MONSTER&&player.hasLineOfSight(entity);
    }
    private static void effect(Player p,net.minecraft.core.Holder<MobEffect> effect,int amplifier) {p.addEffect(new MobEffectInstance(effect,effect==MobEffects.NIGHT_VISION?240:45,amplifier,true,false));}
    private static void update(ServerPlayer player) {
        if(!player.isAlive()||player.isSpectator())return;
        if(ARMOR.entrySet().stream().noneMatch(e->player.getItemBySlot(e.getKey().getSlot()).is(e.getValue())))return;
        for(var entry:ARMOR.entrySet()) {
            var stack=player.getItemBySlot(entry.getKey().getSlot());
            if(stack.is(entry.getValue())&&!bind(stack,player)) {
                player.setItemSlot(entry.getKey().getSlot(),ItemStack.EMPTY);
                if(!player.getInventory().add(stack))player.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        boolean wet=player.isInWater()||player.level().isRainingAt(player.blockPosition());
        long now=NoahLedger.time(player.level());var ledger=NoahLedger.get(player.level());var data=ledger.entry(player.getUUID());
        if(data.getLongOr("UltimateUntil",0)>now&&dominion(player)&&now%10==0)burst(player,3);
        if(now%20!=0)return;
        if(wearing(player,ArmorType.HELMET)) {effect(player,MobEffects.WATER_BREATHING,0);if(player.isUnderWater())effect(player,MobEffects.NIGHT_VISION,0);}
        if(wet&&wearing(player,ArmorType.CHESTPLATE)){effect(player,MobEffects.RESISTANCE,0);effect(player,MobEffects.STRENGTH,1);}
        if(player.isInWater()&&wearing(player,ArmorType.LEGGINGS))effect(player,MobEffects.DOLPHINS_GRACE,0);
        if(wet&&wearing(player,ArmorType.BOOTS))effect(player,MobEffects.SPEED,1);
        if(fullSet(player)) {
            if(!data.getBooleanOr("Awakened",false)) {
                data.putBoolean("Awakened",true);ledger.setDirty();award(player,"crowned");
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("Prince of the Tides")));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("Noah, Heir of Poseidon")));
            }
            if(player.isInWater()){effect(player,MobEffects.CONDUIT_POWER,0);player.heal(1);}
            for(var mob:player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(24),m->seaProtected(m,player)))if(mob.getTarget()==player)mob.setTarget(null);
        }
        if(dominion(player)) {
            award(player,"heir");
            particles(player.level(),player.position(),12);
            for(var mob:player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(6),m->hostile(player,m)))mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,30,0));
        }
    }
    public static String powerName(int mode) {return switch(mode){case 1->"Whirlpool Call";case 2->"Judgment of the Deep";default->"Tidal Burst";};}
    public static boolean cast(ServerPlayer player,boolean ultimate) {
        if(!player.getMainHandItem().is(TEMPEST)||!bind(player.getMainHandItem(),player))return false;
        var ledger=NoahLedger.get(player.level());var data=ledger.entry(player.getUUID());long now=NoahLedger.time(player.level());
        String key=ultimate?"UltimateReady":"PowerReady";
        if(now<data.getLongOr(key,0)){message(player,"The tide gathers: "+((data.getLongOr(key,0)-now+19)/20)+"s");return false;}
        if(ultimate&&!dominion(player)){message(player,"Wrath of the Seventh Sea requires the full regalia, Tempest and riding Thalassar.");return false;}
        int mode=Math.floorMod(data.getIntOr("Mode",0),3);
        data.putLong(key,now+(ultimate?1200:dominion(player)?50:100));
        if(ultimate)data.putLong("UltimateUntil",now+100);ledger.setDirty();
        burst(player,ultimate?3:mode);message(player,ultimate?"Noah's Dominion — Wrath of the Seventh Sea!":powerName(mode));return true;
    }
    private static void burst(ServerPlayer player,int mode) {
        award(player,mode==3?"legacy":"storm");
        var level=player.level();var origin=player.getEyePosition();var look=player.getLookAngle();
        var center=mode==1?origin.add(look.scale(4)):player.position();
        for(var target:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(10),m->hostile(player,m))) {
            var delta=target.getBoundingBox().getCenter().subtract(origin);
            if(delta.lengthSqr()>100||(mode==0&&delta.normalize().dot(look)<.65)||(mode==1&&target.distanceToSqr(center)>25))continue;
            if(mode==1) {target.setDeltaMovement(target.getDeltaMovement().add(center.subtract(target.position()).normalize().scale(.65)));target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,60,2));}
            else {
                target.hurtServer(level,player.damageSources().source(DamageTypes.INDIRECT_MAGIC,player,player),mode==3?6:mode==2?12:10);
                if(mode==0)target.knockback(1.4,player.getX()-target.getX(),player.getZ()-target.getZ(),player.damageSources().source(DamageTypes.INDIRECT_MAGIC,player,player),10);
                else lightning(level,target.position());
            }
        }
        particles(level,center,40);level.playSound(null,player.blockPosition(),mode>=2?SoundEvents.TRIDENT_THUNDER.value():SoundEvents.CONDUIT_ACTIVATE,SoundSource.PLAYERS,.7F,1F);
    }
    public static void guardianSurge(ServerPlayer player,Thalassar mount) {
        for(var target:player.level().getEntitiesOfClass(LivingEntity.class,mount.getBoundingBox().inflate(4),m->hostile(player,m))) {
            target.hurtServer(player.level(),player.damageSources().source(DamageTypes.INDIRECT_MAGIC,player,player),8);target.knockback(1,mount.getX()-target.getX(),mount.getZ()-target.getZ(),player.damageSources().source(DamageTypes.INDIRECT_MAGIC,player,player),8);
        }
        particles(player.level(),mount.position(),25);message(player,"Abyssal Dash — Guardian Surge");
    }
    private static void lightning(ServerLevel level,Vec3 at) {
        var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
        if(bolt!=null){bolt.setPos(at);bolt.setVisualOnly(true);level.addFreshEntity(bolt);}
    }
    private static void particles(ServerLevel level,Vec3 at,int count) {level.sendParticles(ParticleTypes.NAUTILUS,at.x,at.y+1,at.z,count,1,.6,1,.1);}
    private static void message(ServerPlayer player,String text) {player.sendOverlayMessage(Component.literal(text));}
    public static void award(ServerPlayer player,String path) {
        var advancement=player.level().getServer().getAdvancements().get(Darkspawn.id("noah/"+path));
        if(advancement!=null)player.getAdvancements().award(advancement,"earned");
    }
    public static void track(Thalassar mount) {
        if(!(mount.level() instanceof ServerLevel level)||mount.getOwnerReference()==null)return;
        var ledger=NoahLedger.get(level);var data=ledger.entry(mount.getOwnerReference().getUUID());
        String id=mount.getUUID().toString();
        if(!data.getStringOr("Mount","").equals(id))return;
        var pos=mount.blockPosition();String dim=level.dimension().identifier().toString();
        if(data.getIntOr("X",Integer.MIN_VALUE)!=pos.getX()||data.getIntOr("Y",Integer.MIN_VALUE)!=pos.getY()||data.getIntOr("Z",Integer.MIN_VALUE)!=pos.getZ()||!dim.equals(data.getStringOr("Dimension",""))) {
            data.putInt("X",pos.getX());data.putInt("Y",pos.getY());data.putInt("Z",pos.getZ());data.putString("Dimension",dim);ledger.setDirty();
        }
    }
    public static boolean rest(Thalassar mount,boolean defeated) {
        var level=(ServerLevel)mount.level();var ledger=NoahLedger.get(level);var data=ledger.entry(mount.getOwnerReference().getUUID());
        if(!data.getStringOr("Mount","").equals(mount.getUUID().toString()))return false;
        mount.ejectPassengers();if(defeated){mount.setHealth(mount.getMaxHealth());mount.clearFire();mount.removeAllEffects();}
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());mount.saveWithoutId(saved);
        data.put("Snapshot",saved.buildResult());data.remove("Mount");data.putLong("MountReady",NoahLedger.time(level)+(defeated?600:0));ledger.setDirty();mount.discard();
        return true;
    }
    public static boolean summon(ServerPlayer player,boolean dismiss) {
        if(!player.isAlive()||player.isSpectator()||!player.mayBuild())return false;
        var level=player.level();var ledger=NoahLedger.get(level);var data=ledger.entry(player.getUUID());long now=NoahLedger.time(level);
        if(now<data.getLongOr("ConchReady",0))return false;
        data.putLong("ConchReady",now+20);ledger.setDirty();
        if(now<data.getLongOr("MountReady",0)){message(player,"Thalassar rests in the deep: "+((data.getLongOr("MountReady",0)-now+19)/20)+"s");return false;}
        Thalassar active=null;String activeId=data.getStringOr("Mount","");
        if(!activeId.isEmpty()) {
            UUID id;try{id=UUID.fromString(activeId);}catch(IllegalArgumentException invalid){return false;}
            for(var world:level.getServer().getAllLevels())if(world.getEntity(id) instanceof Thalassar found)active=found;
            if(active==null) {
                var dim=Identifier.tryParse(data.getStringOr("Dimension",""));var world=dim==null?null:level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,dim));
                if(world!=null)world.getChunkAt(new BlockPos(data.getIntOr("X",0),data.getIntOr("Y",64),data.getIntOr("Z",0)));
                message(player,"Calling across the deep. Use the conch again when Thalassar's waters have loaded.");return false;
            }
            if(!active.isOwnedBy(player)||active.isVehicle()||active.isLeashed()){message(player,"Dismount and unleash Thalassar before calling him.");return false;}
            if(darkspawn.black.oneblock.OneBlock.island(active.level())!=darkspawn.black.oneblock.OneBlock.island(level)){message(player,"Rest Thalassar before crossing the OneBlock boundary.");return false;}
        }
        if(dismiss){if(active==null)return false;rest(active,false);message(player,"Thalassar rests beneath the tides.");return true;}
        var mount=Mounts.THALASSAR.create(level,EntitySpawnReason.COMMAND);if(mount==null)return false;
        Vec3 destination=null;
        for(int radius=2;radius<=4&&destination==null;radius++)for(int dy=1;dy>=-2&&destination==null;dy--)for(int side=0;side<4;side++) {
            var pos=player.blockPosition().offset(side==0?radius:side==1?-radius:0,dy,side==2?radius:side==3?-radius:0);
            mount.setPos(Vec3.atBottomCenterOf(pos));
            if(level.hasChunkAt(pos)&&level.mayInteract(player,pos)&&MountCompanions.safePlacement(level,mount)){destination=mount.position();break;}
        }
        if(destination==null){mount.discard();message(player,"Thalassar needs open water or safe ground nearby.");return false;}
        if(active!=null)rest(active,false);
        data.getCompound("Snapshot").ifPresent(saved->mount.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved.copy())));
        mount.bond(player);mount.setPos(destination);mount.setDeltaMovement(Vec3.ZERO);mount.setCustomName(Component.translatable("entity.darkspawn.thalassar"));mount.setCustomNameVisible(true);
        if(!level.addFreshEntity(mount)){mount.discard();return false;}
        data.remove("Snapshot");data.putString("Mount",mount.getUUID().toString());ledger.setDirty();track(mount);mount.triggerAnim("action","summon");
        particles(level,destination,40);level.playSound(null,player.blockPosition(),SoundEvents.CONDUIT_ACTIVATE,SoundSource.PLAYERS,1,.8F);message(player,"Thalassar answers the Prince of the Tides.");return true;
    }
    public static void initialize() {
        var material=new ArmorMaterial(50,Map.of(ArmorType.HELMET,3,ArmorType.CHESTPLATE,8,ArmorType.LEGGINGS,6,ArmorType.BOOTS,3),20,SoundEvents.ARMOR_EQUIP_NETHERITE,4,.15F,ItemTags.REPAIRS_NETHERITE_ARMOR,ResourceKey.create(EquipmentAssets.ROOT_ID,Darkspawn.id("noah")));
        for(var type:List.of(ArmorType.HELMET,ArmorType.CHESTPLATE,ArmorType.LEGGINGS,ArmorType.BOOTS)) {
            String id="noah_"+type.getName();ARMOR.put(type,register(id,new Item(properties(id,id).humanoidArmor(material,type))));
        }
        PayloadTypeRegistry.serverboundPlay().register(NoahPowerPayload.TYPE,NoahPowerPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(NoahPowerPayload.TYPE,(payload,context)->cast(context.player(),payload.ultimate()));
        ServerTickEvents.END_SERVER_TICK.register(server->server.getPlayerList().getPlayers().forEach(NoahLegacy::update));
        AttackEntityCallback.EVENT.register((player,level,hand,target,hit)->player instanceof ServerPlayer p&&player.getItemInHand(hand).is(TEMPEST)&&!bind(player.getItemInHand(hand),p)?InteractionResult.FAIL:InteractionResult.PASS);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->{
            if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return true;
            if(target instanceof Player p&&(source.is(DamageTypeTags.IS_FALL)&&wearing(p,ArmorType.BOOTS)||source.is(DamageTypes.DROWN)&&fullSet(p)))return false;
            return !(source.getEntity() instanceof Mob mob&&seaProtected(mob,target));
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,damage,blocked)->{
            if(damage<=0||!(target instanceof ServerPlayer p)||!fullSet(p)||!(source.getEntity() instanceof LivingEntity attacker)||!hostile(p,attacker))return;
            var ledger=NoahLedger.get(p.level());var data=ledger.entry(p.getUUID());long now=NoahLedger.time(p.level());
            if(now<data.getLongOr("RetaliateReady",0))return;data.putLong("RetaliateReady",now+100);ledger.setDirty();
            lightning(p.level(),attacker.position());attacker.hurtServer(p.level(),p.damageSources().source(DamageTypes.INDIRECT_MAGIC,p,p),6);
        });
    }
}
