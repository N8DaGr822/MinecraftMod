package darkspawn.black.endgame;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import darkspawn.black.boss.*;
import darkspawn.black.ecosystem.*;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.Vec3;

/** One bounded cooperative gauntlet; entry consent is expressed by holding a Nexus Key. */
public final class Nexus {
    public static final String TAG="darkspawn_nexus";
    public static final ResourceKey<Level> LEVEL=ResourceKey.create(Registries.DIMENSION,Darkspawn.id("nexus"));
    public static final Item KEY=Registry.register(BuiltInRegistries.ITEM,Darkspawn.id("nexus_key"),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Darkspawn.id("nexus_key"))).stacksTo(1).rarity(Rarity.EPIC)){
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer server){if(level.dimension().equals(LEVEL))leave(server);else start(server);}
            return InteractionResult.SUCCESS;
        }
    });
    public static final class Run extends SavedData {
        public static final Codec<Run> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.INT.optionalFieldOf("stage",0).forGetter(r->r.stage),Codec.LONG.optionalFieldOf("deadline",0L).forGetter(r->r.deadline),
            Codec.unboundedMap(Codec.STRING,GlobalPos.CODEC).optionalFieldOf("returns",Map.of()).forGetter(r->r.returns),
            UUIDUtil.CODEC.listOf().optionalFieldOf("targets",List.of()).forGetter(r->List.copyOf(r.targets)),
            UUIDUtil.CODEC.listOf().optionalFieldOf("claimed",List.of()).forGetter(r->List.copyOf(r.claimed)),
            UUIDUtil.CODEC.listOf().optionalFieldOf("members",List.of()).forGetter(r->List.copyOf(r.members)),
            Codec.BOOL.optionalFieldOf("built",false).forGetter(r->r.built)).apply(i,Run::new));
        public static final SavedDataType<Run> TYPE=new SavedDataType<>(Darkspawn.id("nexus_run"),()->new Run(0,0,Map.of(),List.of(),List.of(),List.of(),false),CODEC,null);
        private int stage;
        private long deadline;
        private boolean built;
        private final Map<String,GlobalPos> returns=new LinkedHashMap<>();
        private final Set<UUID> targets=new HashSet<>(),claimed=new HashSet<>(),members=new HashSet<>();
        private Run(int stage,long deadline,Map<String,GlobalPos> returns,List<UUID> targets,List<UUID> claimed,List<UUID> members,boolean built){this.stage=Math.clamp(stage,0,7);this.deadline=deadline;this.returns.putAll(returns);this.targets.addAll(targets);this.claimed.addAll(claimed);this.members.addAll(members);this.built=built;}
        public int stage(){return stage;}
    }
    private Nexus() { }
    public static Run run(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(Run.TYPE);}
    public static boolean qualified(ServerPlayer player){return WorldAwakening.get(player.level()).allDefeated()&&darkspawn.black.health.BossHearts.progress(player).savedBosses().size()>=15;}
    public static boolean recover(ServerPlayer player){
        if(!traveler(player)||!player.level().dimension().equals(Level.OVERWORLD))return false;
        var run=run(player.level());var level=player.level().getServer().getLevel(LEVEL);
        if(level==null||!run.returns.containsKey(player.getUUID().toString())||run.stage>0&&run.stage<7&&!run.members.contains(player.getUUID()))return false;
        var target=darkspawn.black.pirate.Pirate.safePosition(player,level,new BlockPos(0,64,24));
        if(target==null)return false;
        if(player.teleport(new TeleportTransition(level,target,Vec3.ZERO,180,0,TeleportTransition.PLACE_PORTAL_TICKET))==null)return false;player.fallDistance=0;message(player,"Nexus recovery visit. /nexusleave returns to your saved entrance.");return true;
    }
    private static boolean traveler(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&!p.isVehicle()&&p.containerMenu==p.inventoryMenu&&p.containerMenu.getCarried().isEmpty()&&!p.isSleeping()&&!p.isFallFlying()&&p.getShoulderEntityLeft().isEmpty()&&p.getShoulderEntityRight().isEmpty();}
    private static void message(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
    private static void arena(ServerLevel level,Run run){
        if(run.built)return;
        // Dedicated Arena: Build once in this new dimension; retries never overwrite player blocks or dropped equipment.
        for(int x=-36;x<=36;x++)for(int z=-36;z<=36;z++){
            level.setBlock(new BlockPos(x,63,z),Blocks.DEEPSLATE_BRICKS.defaultBlockState(),3);
            if(Math.abs(x)==36||Math.abs(z)==36)for(int y=64;y<72;y++)level.setBlock(new BlockPos(x,y,z),Blocks.BEDROCK.defaultBlockState(),3);
            if(x%12==0&&z%12==0)level.setBlock(new BlockPos(x,63,z),Blocks.SEA_LANTERN.defaultBlockState(),3);
        }
        run.built=true;run.setDirty();
    }
    public static boolean start(ServerPlayer captain){
        var server=captain.level().getServer();var level=server.getLevel(LEVEL);var run=run(captain.level());
        if(!traveler(captain)||!captain.level().dimension().equals(Level.OVERWORLD)||level==null||level.getDifficulty()==Difficulty.PEACEFUL)return false;
        if(run.stage!=0||!level.players().isEmpty()){message(captain,"The shared Nexus is occupied. Finish or exit the active run first.");return false;}
        if(!captain.isCreative()&&!qualified(captain)){message(captain,"Nexus entry: all 16 boss families defeated in this world and 15 different hearts consumed by the captain.");return false;}
        if(!darkspawn.black.pirate.Ships.pay(captain,Items.NETHER_STAR,1,false)){message(captain,"Starting the Nexus costs one Nether Star. Keys are reusable.");return false;}
        var group=new ArrayList<ServerPlayer>();group.add(captain);
        captain.level().players().stream().filter(p->p!=captain&&traveler(p)&&p.distanceToSqr(captain)<=12*12&&(p.getMainHandItem().is(KEY)||p.getOffhandItem().is(KEY))).limit(5).forEach(group::add);
        arena(level,run);
        // Arrival Safety: Player-modified pads need dry air and safe support across the entire arrival footprint before charging.
        for(int i=0;i<group.size();i++){
            var player=group.get(i);var box=player.getBoundingBox().move(new Vec3(-10+i*4,64,24).subtract(player.position()));
            boolean safe=level.getWorldBorder().isWithinBounds(box)&&level.noCollision(player,box);
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,63,box.minZ),BlockPos.containing(box.maxX-.001,box.maxY-.001,box.maxZ-.001))){
                var block=level.getBlockState(pos);
                if(pos.getY()==63?(!block.isSolidRender()||block.is(Blocks.MAGMA_BLOCK)||block.is(Blocks.CACTUS)):!block.isAir())safe=false;
            }
            if(!safe){message(captain,"Nexus arrival pads need clear, dry space over safe solid floors. Repair them before entering.");return false;}
        }
        darkspawn.black.pirate.Ships.pay(captain,Items.NETHER_STAR,1,true);
        run.targets.clear();run.claimed.clear();run.members.clear();group.forEach(p->run.members.add(p.getUUID()));run.stage=1;run.deadline=server.overworld().getGameTime()+24000;
        for(int i=0;i<group.size();i++){
            var player=group.get(i);run.returns.put(player.getUUID().toString(),GlobalPos.of(player.level().dimension(),player.blockPosition()));
            player.teleport(new TeleportTransition(level,new Vec3(-10+i*4,64,24),Vec3.ZERO,180,0,TeleportTransition.PLACE_PORTAL_TICKET));player.fallDistance=0;
            message(player,"Nexus: five regional waves, then the Nexus Warden. Use your key or /nexusleave to retreat. Twenty-minute run limit.");
        }
        spawnWave(level,run);run.setDirty();return true;
    }
    public static boolean leave(ServerPlayer player){
        if(!player.level().dimension().equals(LEVEL))return false;
        var server=player.level().getServer();var run=run(player.level());var origin=run.returns.get(player.getUUID().toString());
        var level=origin==null?null:server.getLevel(origin.dimension());
        var target=level==null?null:darkspawn.black.pirate.Pirate.safePosition(player,level,origin.pos());
        if(target==null){level=server.overworld();target=darkspawn.black.pirate.Pirate.safePosition(player,level,level.getRespawnData().pos());}
        if(target==null){message(player,"Both your saved entrance and world spawn are obstructed. Clear an arrival area before returning.");return false;}
        if(player.teleport(new TeleportTransition(level,target,Vec3.ZERO,player.getYRot(),0,TeleportTransition.PLACE_PORTAL_TICKET))==null)return false;
        player.fallDistance=0;return true;
    }

    private static void spawnWave(ServerLevel level,Run run){
        if(run.stage==6){
            var boss=new BiomeBoss(BossEntities.BIOME_BOSSES.get(BossProfile.VOID_EYE),level,BossProfile.VOID_EYE);boss.setPos(0,64,-12);boss.addTag(TAG);boss.prepareEncounter(level);
            boss.getAttribute(BossEntities.MAX_HEALTH).setBaseValue(boss.getMaxHealth()*1.5);boss.setHealth(boss.getMaxHealth());boss.setCustomName(Component.literal("Nexus Warden"));boss.setCustomNameVisible(true);
            if(level.addFreshEntity(boss))run.targets.add(boss.getUUID());
        }else{
            RegionalKind[][] waves={{RegionalKind.BONE_RAPTOR,RegionalKind.TYRANT_SKULL},{RegionalKind.SHADOW_GUARDIAN,RegionalKind.SHADOW_SPITTER},{RegionalKind.SPOREWALKER,RegionalKind.MYCELIUM_CRAWLER},{RegionalKind.CRIMSON_RAVAGER,RegionalKind.FUNGAL_IMP},{RegionalKind.VOIDLING,RegionalKind.WATCHER}};
            int count=3+Math.min(6,level.players().size());var kinds=waves[run.stage-1];
            for(int i=0;i<count;i++){
                var mob=RegionalEcosystems.MOBS.get(kinds[i%kinds.length]).create(level,EntitySpawnReason.EVENT);if(mob==null)continue;
                mob.setPos(-20+i*5,64,-12);mob.setPersistenceRequired();mob.addTag(TAG);
                var target=level.getNearestPlayer(mob,64);if(target!=null)mob.setTarget(target);
                if(level.addFreshEntity(mob))run.targets.add(mob.getUUID());
            }
        }
        if(run.targets.isEmpty()){run.stage=7;messageAll(level,"Nexus spawn failed. Retreat and retry; no victory awarded.");}
        else messageAll(level,run.stage==6?"The Nexus Warden awakens!":"Nexus wave "+run.stage+" / 5");
        run.setDirty();
    }
    private static void messageAll(ServerLevel level,String text){level.players().forEach(p->message(p,text));}
    static void tick(net.minecraft.server.MinecraftServer server){
        if(server.getTickCount()%20!=0)return;advance(server);
    }
    static void advance(net.minecraft.server.MinecraftServer server){
        var level=server.getLevel(LEVEL);if(level==null)return;var run=run(level);if(run.stage==0)return;
        boolean absent=level.players().stream().noneMatch(p->p.isAlive()&&!p.isSpectator());
        boolean departed=absent&&(run.stage==7||run.members.stream().map(id->server.getPlayerList().getPlayer(id)).anyMatch(java.util.Objects::nonNull));
        if(departed||run.stage<7&&(server.overworld().getGameTime()>=run.deadline||level.getDifficulty()==Difficulty.PEACEFUL)){
            var cleanup=new ArrayList<Entity>();level.getAllEntities().forEach(cleanup::add);for(var entity:cleanup)if(entity.entityTags().contains(TAG))entity.discard();
            run.stage=level.players().isEmpty()?0:7;run.targets.clear();run.setDirty();messageAll(level,"Nexus run ended. Use your key or /nexusleave to return.");return;
        }
        if(absent)return;
        // Advance only from recorded deaths, never from unloaded, missing or discarded entities.
        if(run.stage<7&&run.targets.isEmpty()){
            run.stage++;
            if(run.stage==7){
                for(var p:level.players())if(p.isAlive()&&!p.isSpectator()&&run.members.contains(p.getUUID())&&run.claimed.add(p.getUUID())){
                    var reward=new net.minecraft.world.entity.item.ItemEntity(level,p.getX(),p.getY()+1,p.getZ(),new ItemStack(LegendaryRelics.CORE));reward.setTarget(p.getUUID());level.addFreshEntity(reward);
                }
                messageAll(level,"Nexus cleared! Each surviving participant earns one Nexus Core. Craft one of four legendary relics. Retreat to finish the run.");run.setDirty();
            }else spawnWave(level,run);
        }
    }
    public static void initialize(){
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(KEY));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(entity.level() instanceof ServerLevel level&&level.dimension().equals(LEVEL)&&entity.entityTags().contains(TAG)){var run=run(level);if(run.targets.remove(entity.getUUID()))run.setDirty();}});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(level.dimension().equals(LEVEL)&&entity.entityTags().contains(TAG)&&(run(level).stage==0||run(level).stage==7))entity.discard();
        });
        ServerTickEvents.END_SERVER_TICK.register(Nexus::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
            dispatcher.register(Commands.literal("nexusleave").executes(context->{var player=context.getSource().getPlayer();return player!=null&&leave(player)?1:0;}));
            dispatcher.register(Commands.literal("nexusrecover").executes(context->{var player=context.getSource().getPlayer();return player!=null&&recover(player)?1:0;}));
        });
    }
}
