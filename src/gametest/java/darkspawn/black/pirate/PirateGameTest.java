package darkspawn.black.pirate;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.Darkspawn;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class PirateGameTest {
    @GameTest public void biomeIslandsStoreNativeBiomesAndLoadSettlements(GameTestHelper helper) {
        var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
        var structure=sea.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Darkspawn.id("pirate_island"))).value();
        for(int kind=9;kind<15;kind++) {
            var terrain=sea.getStructureTemplateManager().getOrCreate(Darkspawn.id("pirate/terrain_"+kind));
            helper.assertTrue(terrain.getSize().equals(new net.minecraft.core.Vec3i(129,56,129)),"Large biome terrain has bounded native footprint");
            helper.assertTrue(terrain.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.CHEST).size()==1,"One finite cache per settlement");
            helper.assertTrue(terrain.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.BED.red()).size()>=4,"Settlement provides native beds");
            var island=islandFixture(helper,sea,kind);var chunk=sea.getChunk(island.center());
            var references=new java.util.HashMap<>(chunk.getAllReferences());
            int qx=island.center().getX()>>2,qz=island.center().getZ()>>2;
            var before=chunk.getNoiseBiome(qx,16,qz);
            try {
                chunk.addReferenceForStructure(structure,chunk.getPos().pack());
                sea.getChunkSource().getGenerator().applyBiomeDecoration(sea,chunk,sea.structureManager());
                helper.assertTrue(chunk.getNoiseBiome(qx,16,qz).is(PirateIslandBiomes.biome(kind)),"Chunk stores actual native biome, including weather and mob rules");
                helper.assertTrue(chunk.getNoiseBiome(qx,-8,qz).is(PirateIslandBiomes.biome(kind)),"Island biome covers mining depths too");
            } finally {chunk.fillBiomesFromNoise((x,y,z)->before);chunk.setAllReferences(references);}
        }
        helper.succeed();
    }
    @GameTest public void biomeSettlementsProtectVisitorsAndNeverRefillGarrisons(GameTestHelper helper) {
        var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var p=player(helper,helper.absolutePos(BlockPos.ZERO));
        var difficulty=sea.getDifficulty();var rule=net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS;boolean previous=sea.getGameRules().get(rule);
        var actors=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        try {
            sea.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);sea.getGameRules().set(rule,true,sea.getServer());
            for(int kind:new int[]{9,11,13}) {
                var island=islandFixture(helper,sea,kind);
                for(int[] offset:new int[][]{{0,10},{-7,10},{7,10},{-7,-10},{7,-10}}) {
                    var pos=island.center().offset(offset[0],0,offset[1]);sea.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
                    sea.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());sea.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
                }
                seaTravel(p,sea,island.center());p.setGameMode(GameType.SURVIVAL);
                helper.assertTrue(IslandSettlements.spawn(p,island)==(kind==9?1:4),"Expected bounded settlement population");
                var mobs=sea.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(island.center()).inflate(24),m->m.entityTags().contains(IslandDiscoveries.ENCOUNTER_TAG));actors.addAll(mobs);
                helper.assertTrue(IslandSettlements.spawn(p,island)==0,"Repeated visits cannot duplicate NPCs");
                if(kind==9) {
                    var warden=mobs.getFirst();helper.assertTrue(PirateDiplomacy.contact(warden)&&IslandSettlements.sheltered(p),"Native contact protects the settlement center");
                    var attacker=net.minecraft.world.entity.EntityTypes.HUSK.create(sea,net.minecraft.world.entity.EntitySpawnReason.EVENT);
                    helper.assertTrue(PirateDiplomacy.disallows(attacker,p)&&PirateDiplomacy.disallows(attacker,warden),"Warden prevents hostile mob attacks on visitors and the service NPC");attacker.discard();
                    p.teleportTo(island.center().getX()+50,65,island.center().getZ());helper.assertTrue(!IslandSettlements.sheltered(p),"Resource wilderness remains dangerous");
                } else {
                    mobs.forEach(net.minecraft.world.entity.Entity::discard);
                    helper.assertTrue(IslandSettlements.spawn(p,island)==0,"Defeated occupation never respawns");
                    var saved=sea.getDataStorage().computeIfAbsent(IslandDiscoveries.Encounters.TYPE);
                    var copy=IslandDiscoveries.Encounters.CODEC.parse(JsonOps.INSTANCE,IslandDiscoveries.Encounters.CODEC.encodeStart(JsonOps.INSTANCE,saved).getOrThrow()).getOrThrow();
                    helper.assertTrue((copy.mask(island.center())&0xF0)==0xF0,"All four garrison slots persist through save/load");
                }
            }
        } finally {actors.forEach(net.minecraft.world.entity.Entity::discard);remove(p);sea.getServer().setDifficulty(difficulty,true);sea.getGameRules().set(rule,previous,sea.getServer());}
        helper.succeed();
    }
    @GameTest public void homePortsRequireOwnershipSaveAndLosePrivilegesOnCapture(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var island=islandFixture(helper,sea,1);seaTravel(p,sea,island.center());seaTravel(other,sea,island.center());
            var ledger=SeaLedger.get(sea);ledger.fort(new SeaLedger.Fort(island.center(),p.getUUID(),"Captain",0,0));
            helper.assertTrue(SeaServices.execute(other,20)==SeaServices.Reply.OWNER,"Visitors cannot claim the owner's port");
            helper.assertTrue(SeaServices.execute(p,20)==SeaServices.Reply.DONE&&SeaServices.execute(p,20)==SeaServices.Reply.RECORDED,"Port designation is idempotent");
            p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,48));p.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,64));p.getInventory().setItem(2,new ItemStack(Items.OAK_PLANKS,32));
            helper.assertTrue(SeaServices.execute(p,21)==SeaServices.Reply.COST&&p.getInventory().countItem(Items.IRON_INGOT)==48,"Incomplete payment leaves all resources untouched");p.getInventory().setItem(3,new ItemStack(Items.EMERALD,20));
            helper.assertTrue(SeaServices.execute(p,21)==SeaServices.Reply.DONE&&ledger.homePort(p.getUUID()).level()==1,"Drydock upgrade succeeds");
            helper.assertTrue(SeaServices.execute(p,21)==SeaServices.Reply.DONE&&HomePorts.services(p)&&!HomePorts.services(other),"Tavern service access belongs to owner");
            helper.assertTrue(SeaServices.execute(p,18)==SeaServices.Reply.DONE&&p.getInventory().countItem(PirateDrinks.BEER)==1,"Home tavern sells normal drink stock");
            var restored=SeaLedger.CODEC.parse(JsonOps.INSTANCE,SeaLedger.CODEC.encodeStart(JsonOps.INSTANCE,ledger).getOrThrow()).getOrThrow();helper.assertTrue(restored.homePort(p.getUUID()).level()==2,"Port and upgrades survive save");
            ledger.fort(new SeaLedger.Fort(island.center(),other.getUUID(),"New captain",0,0));helper.assertTrue(ledger.homePort(p.getUUID())==null&&!HomePorts.services(p),"Capture revokes old owner services");
            ledger.fort(new SeaLedger.Fort(island.center(),p.getUUID(),"Captain",0,0));helper.assertTrue(ledger.homePort(p.getUUID())==null,"Recapture cannot resurrect lost upgrades");
        } finally {remove(p);remove(other);}helper.succeed();
    }
    @GameTest public void discoveryChartsRequireLocatedCompassAndKeepNativeTarget(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));
        try {
            var compass=new ItemStack(darkspawn.black.worldgen.LandmarkCompass.ITEM);
            helper.assertTrue(darkspawn.black.worldgen.DiscoveryCharts.chart(p,compass).isEmpty(),"Blank compass cannot invent a map destination");
            var target=new BlockPos(240,65,320);compass.set(DataComponents.LODESTONE_TRACKER,new net.minecraft.world.item.component.LodestoneTracker(java.util.Optional.of(GlobalPos.of(p.level().dimension(),target)),false));
            var chart=darkspawn.black.worldgen.DiscoveryCharts.chart(p,compass);helper.assertTrue(chart.is(Items.FILLED_MAP)&&chart.get(DataComponents.MAP_ID)!=null&&chart.get(DataComponents.MAP_DECORATIONS)!=null,"Chart is a native copyable map with target decoration");
            helper.assertTrue(net.minecraft.world.item.MapItem.getSavedData(chart,p.level())!=null&&compass.get(DataComponents.LODESTONE_TRACKER).target().orElseThrow().pos().equals(target),"Map has world-saved terrain data and leaves compass intact");
        } finally {remove(p);}helper.succeed();
    }
    @GameTest public void awakenedRematchesGateProgressScaleAndPersist(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));var boss=new darkspawn.black.boss.MutantWolf(darkspawn.black.boss.BossEntities.MUTANT_WOLF,p.level());
        try {
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(darkspawn.black.boss.AwakenedRematches.SEAL,2));var kind=darkspawn.black.boss.BossKind.MUTANT_WOLF;
            helper.assertTrue(!darkspawn.black.boss.AwakenedRematches.prepare(p,boss,kind)&&p.getOffhandItem().getCount()==2,"Uncompleted family rejects seal without consuming it");
            darkspawn.black.health.BossHearts.progress(p).consume(kind);float normal=boss.getMaxHealth();
            helper.assertTrue(darkspawn.black.boss.AwakenedRematches.prepare(p,boss,kind)&&Math.abs(boss.getMaxHealth()-normal*1.5)<.1,"Awakened rematch scales health after progression");
            darkspawn.black.boss.AwakenedRematches.consume(p,boss);helper.assertTrue(p.getOffhandItem().getCount()==1,"Successful awakening consumes one seal");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());boss.saveWithoutId(saved);
            var copy=new darkspawn.black.boss.MutantWolf(darkspawn.black.boss.BossEntities.MUTANT_WOLF,p.level());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
            helper.assertTrue(copy.entityTags().contains(darkspawn.black.boss.AwakenedRematches.TAG)&&Math.abs(copy.getMaxHealth()-boss.getMaxHealth())<.1,"Awakened tag and attribute modifiers persist");copy.discard();
            boss.setPos(p.position());darkspawn.black.cooking.BossCuisineRewards.grant(boss,p.level(),java.util.Set.of(p.getUUID()),kind,192);
            var rewards=p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(3),e->e.getItem().has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
            helper.assertTrue(rewards.size()==2&&rewards.stream().noneMatch(e->e.getItem().is(darkspawn.black.boss.BossItems.HEARTS.get(kind))),"Awakened victory grants two cosmetics, no heart");rewards.forEach(net.minecraft.world.entity.Entity::discard);
            helper.assertTrue(darkspawn.black.boss.AwakenedRematches.cosmetic(kind).is(Items.LEATHER_HELMET)&&darkspawn.black.boss.AwakenedRematches.trophy(kind).is(darkspawn.black.cooking.Cuisine.TROPHIES.get(kind)),"Rematch rewards are cosmetic equipment and trophy");
        } finally {boss.discard();remove(p);}helper.succeed();
    }

    private static java.util.List<net.minecraft.world.entity.Mob> skirmishUnits(net.minecraft.server.level.ServerLevel sea, BlockPos center) {
        return sea.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(center).inflate(48),FactionSkirmishes::combatant);
    }
    @GameTest(maxTicks = 280) public void redwaterNativeCombatAndDepartureCleanup(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var difficulty=sea.getDifficulty();
        sea.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);var island=islandFixture(helper,sea,6);seaTravel(p,sea,island.center().offset(0,0,16));
        p.setGameMode(GameType.CREATIVE);
        var units=new java.util.ArrayList<net.minecraft.world.entity.Mob>();
        // Fixture Ticking: Allow entity sections to load before spawning actors into forced chunks.
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)sea.setChunkForced((island.center().getX()>>4)+x,(island.center().getZ()>>4)+z,true);
        helper.runAfterDelay(10,()->{
            try {p.setGameMode(GameType.SURVIVAL);helper.assertTrue(FactionSkirmishes.observe(p,island),"Native battle starts");units.addAll(skirmishUnits(sea,island.center()));}
            catch(Throwable failure){for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)sea.setChunkForced((island.center().getX()>>4)+x,(island.center().getZ()>>4)+z,false);remove(p);FactionSkirmishes.tick(sea);sea.getServer().setDifficulty(difficulty,true);throw failure;}
        });
        helper.runAfterDelay(240,()->{
            try {
                helper.assertTrue(units.stream().anyMatch(m->m.getHealth()<32),"Native AI actually exchanges damage between factions: "+units.stream().map(m->m.tickCount+":"+m.position()+":"+m.getTarget()).toList());
                helper.assertTrue(p.getHealth()==20,"Neutral observer remains unharmed");
                p.teleportTo(island.center().getX()+100,65,island.center().getZ());FactionSkirmishes.tick(sea);
                helper.assertTrue(units.stream().allMatch(net.minecraft.world.entity.Entity::isRemoved)&&!FactionSkirmishes.observe(p,island),"Departure cleans up all actors");helper.succeed();
            } finally {for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)sea.setChunkForced((island.center().getX()>>4)+x,(island.center().getZ()>>4)+z,false);remove(p);FactionSkirmishes.tick(sea);sea.getServer().setDifficulty(difficulty,true);}
        });
    }
    @GameTest public void redwaterRewardsOneSideOnceAndSparesBystanders(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var difficulty=sea.getDifficulty();
        try {
            sea.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);var island=islandFixture(helper,sea,6);seaTravel(p,sea,island.center().offset(0,0,16));
            helper.assertTrue(FactionSkirmishes.observe(p,island),"Redwater starts a bounded battle");var units=skirmishUnits(sea,island.center());
            helper.assertTrue(units.size()==4&&!FactionSkirmishes.observe(p,island),"Four actors, no duplicate starts");
            for(var unit:units) {
                helper.assertTrue(PirateDiplomacy.disallows(unit,p),"Neutral bystanders are spared");
                for(var other:units)helper.assertTrue(PirateDiplomacy.disallows(unit,other)==(PirateDiplomacy.faction(unit)==PirateDiplomacy.faction(other)),"Only opposite faction combatants are valid targets");
            }
            int before=Pirate.state(p).renown;
            for(var unit:units)if(PirateDiplomacy.faction(unit)==PirateProgression.Faction.FLEET)unit.hurtServer(sea,sea.damageSources().playerAttack(p),1000);
            FactionSkirmishes.tick(sea);
            helper.assertTrue(Pirate.state(p).factionStanding[0]==10&&Pirate.state(p).renown==before+3,"Helping only the winning side pays standing and renown");
            FactionSkirmishes.tick(sea);helper.assertTrue(Pirate.state(p).renown==before+3&&skirmishUnits(sea,island.center()).isEmpty()&&!FactionSkirmishes.observe(p,island),"Resolved battle cleans up and never pays or spawns again");
            var saved=sea.getDataStorage().computeIfAbsent(IslandDiscoveries.Encounters.TYPE);
            helper.assertTrue((saved.mask(island.center())&FactionSkirmishes.SPENT)!=0,"Battle start has a durable spent receipt");
        } finally {remove(p);FactionSkirmishes.tick(sea);sea.getServer().setDifficulty(difficulty,true);}helper.succeed();
    }
    @GameTest public void redwaterAbandonmentAndStaleActorsCannotReward(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var difficulty=sea.getDifficulty();
        try {
            sea.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);var island=islandFixture(helper,sea,6);seaTravel(p,sea,island.center().offset(0,0,16));
            p.setGameMode(GameType.CREATIVE);helper.assertTrue(!FactionSkirmishes.observe(p,island),"Creative visitors do not start battles");p.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(FactionSkirmishes.observe(p,island),"Survival visitor starts battle");var units=skirmishUnits(sea,island.center());units.getFirst().discard();FactionSkirmishes.tick(sea);
            helper.assertTrue(skirmishUnits(sea,island.center()).isEmpty()&&Pirate.state(p).renown==0&&!FactionSkirmishes.observe(p,island),"Missing actor aborts without reward or respawn");
            var stale=net.minecraft.world.entity.EntityTypes.PILLAGER.create(sea,net.minecraft.world.entity.EntitySpawnReason.EVENT);stale.addTag(FactionSkirmishes.TAG);stale.setPos(Vec3.atBottomCenterOf(island.center()));sea.addFreshEntity(stale);
            helper.assertTrue(stale.isRemoved(),"Reloaded orphan actors are discarded");
        } finally {remove(p);FactionSkirmishes.tick(sea);sea.getServer().setDifficulty(difficulty,true);}helper.succeed();
    }
    @GameTest public void redwaterCapsBattlesAndRejectsDoubleSidedRewards(GameTestHelper helper) {
        var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var difficulty=sea.getDifficulty();var players=new java.util.ArrayList<ServerPlayer>();
        try {
            sea.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);var base=islandFixture(helper,sea,6).center();var islands=new java.util.ArrayList<IslandDiscoveries.Island>();
            for(int i=0;i<5;i++) {
                var center=base.offset(i*256,0,0);var island=new IslandDiscoveries.Island(center,6);islands.add(island);
                for(int x:new int[]{-8,8})for(int z:new int[]{-3,3}){var pos=center.offset(x,0,z);sea.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());sea.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());sea.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());}
                var p=player(helper,helper.absolutePos(BlockPos.ZERO));players.add(p);seaTravel(p,sea,center.offset(0,0,16));
                boolean started=FactionSkirmishes.observe(p,island);if(i==0)helper.assertTrue(started,"First requested battle starts");if(i==4)helper.assertTrue(!started,"Fifth simultaneous shore battle is refused, including other running fixtures");
            }
            var p=players.getFirst();var units=skirmishUnits(sea,base);
            for(var mob:units)mob.hurtServer(sea,sea.damageSources().playerAttack(p),1);
            for(var mob:units)if(PirateDiplomacy.faction(mob)==PirateProgression.Faction.FLEET)mob.hurtServer(sea,mob.damageSources().genericKill(),1000);
            FactionSkirmishes.tick(sea);helper.assertTrue(Pirate.state(p).renown==0&&Pirate.state(p).factionStanding[0]<0,"Attacking both factions forfeits gratitude and preserves aggression penalties");
            helper.assertTrue(FactionSkirmishes.observe(players.get(4),islands.get(4)),"Resolving one battle releases a global slot");
            var saved=sea.getDataStorage().computeIfAbsent(IslandDiscoveries.Encounters.TYPE);var restored=IslandDiscoveries.Encounters.CODEC.parse(JsonOps.INSTANCE,IslandDiscoveries.Encounters.CODEC.encodeStart(JsonOps.INSTANCE,saved).getOrThrow()).getOrThrow();
            helper.assertTrue((restored.mask(base)&FactionSkirmishes.SPENT)!=0,"Spent receipt survives SavedData serialization");
        } finally {players.forEach(PirateGameTest::remove);FactionSkirmishes.tick(sea);sea.getServer().setDifficulty(difficulty,true);}helper.succeed();
    }
    @GameTest public void expansionDiscoveriesAndNavigationSurviveReload(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var state=Pirate.state(p);p.setGameMode(GameType.CREATIVE);
            for(int kind=6;kind<=8;kind++) {var island=islandFixture(helper,sea,kind);seaTravel(p,sea,island.center().offset(0,0,16));IslandDiscoveries.observe(p);helper.assertTrue((state.pirateClues&(1<<kind))!=0,"New island records its own discovery");}
            state.logboundKind=8;var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,sea.registryAccess());state.write(output);var restored=new PiratePlayerState();restored.read(TagValueInput.create(ProblemReporter.DISCARDING,sea.registryAccess(),output.buildResult()));var copied=new PiratePlayerState();copied.copyFrom(restored);
            helper.assertTrue((copied.pirateClues&448)==448&&copied.logboundKind==8,"All new clue bits and navigation kind survive save and death copying");
            helper.assertTrue(IslandDiscoveries.spawn(p,islandFixture(helper,sea,7))==0,"Pearlwater has no authored sentries");
        } finally {remove(p);}helper.succeed();
    }

    @GameTest public void promotionsPersistAndRejectUnearnedOrRemoteRequests(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);PirateHarbor.prepare(sea);seaTravel(p,sea,new BlockPos(2,65,4));
            var s=Pirate.state(p);var faction=PirateProgression.Faction.CORSAIRS;s.factionMembership=faction.id;s.factionStanding[0]=100;
            helper.assertTrue(!FactionPromotions.promote(p),"Standing alone cannot buy a promotion");
            s.factionContracts[0]=2;s.logboundVisited.add(LogboundCompass.FIRST_COVE_CENTER);
            helper.assertTrue(FactionPromotions.promote(p)&&s.factionRanks[0]==1,"Earned first rank is awarded once");
            helper.assertTrue(!FactionPromotions.promote(p)&&s.factionRanks[0]==1,"Repeated requests need the next achievements");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());s.write(output);
            var loaded=new PiratePlayerState();loaded.read(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),output.buildResult()));
            var copy=new PiratePlayerState();copy.copyFrom(loaded);loaded.factionRanks[0]=5;
            helper.assertTrue(copy.factionRanks[0]==1,"Rank persists and death-copy array is independent");
            s.harborContract=2;for(int i=0;i<4;i++)s.logboundVisited.add(new BlockPos(500+i,65,0));
            p.teleportTo(800,65,800);helper.assertTrue(!FactionPromotions.promote(p)&&s.factionRanks[0]==1,"Remote promotion is rejected");
        } finally {remove(p);}helper.succeed();
    }
    @GameTest public void navigationRequiresConsentAndKeepsPersonalProgress(GameTestHelper helper) {
        var leader=player(helper,helper.absolutePos(BlockPos.ZERO));var member=player(helper,helper.absolutePos(new BlockPos(2,0,0)));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);PirateHarbor.prepare(sea);
            seaTravel(leader,sea,new BlockPos(0,65,4));seaTravel(member,sea,new BlockPos(2,65,4));
            var party=PartyNavigation.get(sea);
            helper.assertTrue(!party.accept(member,leader),"Uninvited follower cannot join");
            helper.assertTrue(party.invite(leader,member)&&party.leader(member.getUUID())==null,"Invitation alone grants no membership");
            helper.assertTrue(party.accept(member,leader)&&PartyNavigation.guide(member)==leader,"Recipient explicitly accepts");
            helper.assertTrue(!party.invite(member,leader),"Following chains and cycles are rejected");
            var own=Pirate.state(member);own.logboundDestination=LogboundCompass.NEXT_ISLAND;own.logboundNextIsland=new BlockPos(4000,65,4000);own.logboundSecondsRemaining=123;
            Pirate.state(leader).logboundDestination=LogboundCompass.FIRST_COVE;
            member.getInventory().setItem(0,new ItemStack(LogboundCompass.ITEM));
            LogboundCompass.ITEM.inventoryTick(member.getMainHandItem(),sea,member,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            helper.assertTrue(member.getMainHandItem().get(DataComponents.LODESTONE_TRACKER).target().orElseThrow().pos().equals(LogboundCompass.FIRST_COVE_CENTER),"Follower needle points at leader's route");
            member.teleportTo(192.5,65,.5);LogboundCompass.tickNavigation(member);
            helper.assertTrue(own.logboundVisited.contains(LogboundCompass.FIRST_COVE_CENTER)&&own.logboundNextIsland.equals(new BlockPos(4000,65,4000))&&own.logboundSecondsRemaining==123&&own.harborContract==0,"Physical visit earns discovery without route or reward transfer");
            var restored=PartyNavigation.CODEC.parse(JsonOps.INSTANCE,PartyNavigation.CODEC.encodeStart(JsonOps.INSTANCE,party).getOrThrow()).getOrThrow();
            helper.assertTrue(leader.getUUID().equals(restored.leader(member.getUUID())),"Membership persists across restart");
            seaTravel(leader,helper.getLevel(),helper.absolutePos(BlockPos.ZERO));
            helper.assertTrue(PartyNavigation.guide(member)==null,"Absent guide falls back to personal route");
            helper.assertTrue(party.leave(leader.getUUID())&&party.leader(member.getUUID())==null,"Leader disbands members in another dimension");
        } finally {PartyNavigation.get(helper.getLevel()).leave(leader.getUUID());remove(leader);remove(member);}helper.succeed();
    }
    @GameTest public void tavernDrinksChargeAndReturnContainersWithTemporaryBuffs(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);PirateHarbor.prepare(sea);seaTravel(p,sea,new BlockPos(2,65,4));p.getInventory().clearContent();
            helper.assertTrue(SeaServices.execute(p,18)==SeaServices.Reply.COST,"Beer requires payment");
            p.getInventory().setItem(0,new ItemStack(Items.EMERALD,8));
            var menu=new SeaMenu(1,p.getInventory(),p.blockPosition());for(int i=0;i<18;i++)menu.clickMenuButton(p,1);
            helper.assertTrue(menu.clickMenuButton(p,2)&&menu.status(10)==0&&p.getInventory().countItem(PirateDrinks.BEER)==1,"Beer purchase does not trigger ending confirmation");
            helper.assertTrue(SeaServices.execute(p,19)==SeaServices.Reply.DONE&&p.getInventory().countItem(Items.EMERALD)==5,"Rum charges two emeralds");
            var bowl=new ItemStack(PirateDrinks.BEER).finishUsingItem(sea,p);
            var bottle=new ItemStack(PirateDrinks.RUM).finishUsingItem(sea,p);
            helper.assertTrue(bowl.is(Items.BOWL)&&bottle.is(Items.GLASS_BOTTLE),"Survival drinking returns the correct containers");
            helper.assertTrue(p.getEffect(net.minecraft.world.effect.MobEffects.HASTE).getDuration()==2400&&p.getEffect(net.minecraft.world.effect.MobEffects.RESISTANCE).getDuration()==1200,"Beer and rum provide distinct timed buffs");
            new ItemStack(PirateDrinks.RUM).finishUsingItem(sea,p);
            helper.assertTrue(p.getEffect(net.minecraft.world.effect.MobEffects.RESISTANCE).getAmplifier()==0,"Repeated rum does not stack strength");
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.getInventory().setItem(0,new ItemStack(Items.EMERALD,5));
            helper.assertTrue(SeaServices.execute(p,19)==SeaServices.Reply.SPACE&&p.getInventory().countItem(Items.EMERALD)==5,"Full inventory preserves payment");
            p.teleportTo(500,65,500);helper.assertTrue(SeaServices.execute(p,18)==SeaServices.Reply.HARBOR,"Tavern purchases require harbor presence");
        } finally {remove(p);}helper.succeed();
    }
    @GameTest public void landmarkCompassPreservesChartsOnWrongDimension(GameTestHelper helper) {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));
        try {
            var item=darkspawn.black.worldgen.LandmarkCompass.ITEM;var stack=new ItemStack(item);
            var target=net.minecraft.core.GlobalPos.of(Level.OVERWORLD,new BlockPos(100,70,100));
            stack.set(DataComponents.LODESTONE_TRACKER,new net.minecraft.world.item.component.LodestoneTracker(java.util.Optional.of(target),false));
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);PirateHarbor.prepare(sea);seaTravel(p,sea,new BlockPos(2,65,4));
            helper.assertTrue(!darkspawn.black.worldgen.LandmarkCompass.search(p,stack)&&stack.get(DataComponents.LODESTONE_TRACKER).target().orElseThrow().equals(target),"Wrong-dimension search preserves existing chart");
        } finally {remove(p);}helper.succeed();
    }
    @GameTest public void landmarkCompassFindsNativeStartAndThrottlesCopies(GameTestHelper helper) throws ReflectiveOperationException {
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));
        net.minecraft.world.level.chunk.ChunkAccess fixture=null;
        net.minecraft.world.level.levelgen.structure.Structure structure=null;
        net.minecraft.world.level.levelgen.structure.StructureStart previous=null;
        var options=helper.getLevel().getServer().getWorldGenSettings().options();
        var structures=net.minecraft.world.level.levelgen.WorldOptions.class.getDeclaredField("generateStructures");structures.setAccessible(true);boolean previousGeneration=options.generateStructures();
        java.util.Map<net.minecraft.world.level.levelgen.structure.Structure,java.util.List<net.minecraft.world.level.levelgen.structure.placement.StructurePlacement>> placements=null;
        java.util.List<net.minecraft.world.level.levelgen.structure.placement.StructurePlacement> previousPlacements=null;
        try {
            var nether=helper.getLevel().getServer().getLevel(Level.NETHER);
            var holder=nether.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Darkspawn.id("soul_reliquary")));
            structure=holder.value();var generation=nether.getChunkSource().getGeneratorState();
            // GameTest worlds omit native placements: install only this saved-start fixture and restore it below.
            structures.setBoolean(options,true);generation.ensureStructuresGenerated();
            var field=net.minecraft.world.level.chunk.ChunkGeneratorStructureState.class.getDeclaredField("placementsForStructure");field.setAccessible(true);
            @SuppressWarnings("unchecked") var map=(java.util.Map<net.minecraft.world.level.levelgen.structure.Structure,java.util.List<net.minecraft.world.level.levelgen.structure.placement.StructurePlacement>>)field.get(generation);
            placements=map;previousPlacements=placements.get(structure);
            var placement=(net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)nether.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET,Darkspawn.id("soul_reliquary"))).value().placement();
            placements.put(structure,java.util.List.of(placement));
            var chunk=placement.getPotentialStructureChunk(generation.getLevelSeed(),0,0);
            var origin=new BlockPos(chunk.getMiddleBlockX()-12,40,chunk.getMiddleBlockZ()-12);
            fixture=nether.getChunk(chunk.x(),chunk.z(),net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
            previous=fixture.getAllStarts().get(structure);
            var piece=new darkspawn.black.worldgen.BossLandmarkStructure.LandmarkPiece(nether.getStructureTemplateManager(),Darkspawn.id("landmarks/soul_reliquary_0"),origin,Rotation.NONE);
            var start=new net.minecraft.world.level.levelgen.structure.StructureStart(structure,chunk,0,new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
            fixture.setStartForStructure(structure,start);
            seaTravel(p,nether,new BlockPos(chunk.getMiddleBlockX(),65,chunk.getMiddleBlockZ()));
            var stack=new ItemStack(darkspawn.black.worldgen.LandmarkCompass.ITEM);
            for(int i=0;i<16&&!darkspawn.black.worldgen.LandmarkCompass.selected(stack).site().equals("soul_reliquary");i++)darkspawn.black.worldgen.LandmarkCompass.cycle(stack);
            helper.assertTrue(darkspawn.black.worldgen.LandmarkCompass.search(p,stack),"Search finds a valid native start at its actual candidate chunk");
            helper.assertTrue(stack.get(DataComponents.LODESTONE_TRACKER).target().orElseThrow().equals(net.minecraft.core.GlobalPos.of(Level.NETHER,start.getBoundingBox().getCenter())),"Compass keeps native dimension and vertical target");
            helper.assertTrue(!darkspawn.black.worldgen.LandmarkCompass.search(p,stack.copy()),"Copying an item cannot bypass the per-player search cooldown");
            helper.assertTrue(start.getReferences()==0,"Search does not consume another player's exploration reference");
        } finally {
            if(fixture!=null&&structure!=null)fixture.setStartForStructure(structure,previous==null?net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START:previous);
            if(placements!=null&&structure!=null){if(previousPlacements==null)placements.remove(structure);else placements.put(structure,previousPlacements);}
            structures.setBoolean(options,previousGeneration);remove(p);
        }
        helper.succeed();
    }
	private static ServerPlayer player(GameTestHelper helper, BlockPos pos) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "pirate-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return gameMode == null ? GameType.SURVIVAL : super.gameMode(); }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5); ready(player); return player;
	}
	private static void ready(ServerPlayer player) {
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket()); player.hasChangedDimension(); player.setOnGround(true);
		Pirate.state(player).lastTravel = Long.MIN_VALUE;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	private static BlockPos gate(GameTestHelper helper, int offset) {
		var center = helper.absolutePos(new BlockPos(offset, 3, 1)); var level = helper.getLevel();
		for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
			level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
			for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		level.setBlockAndUpdate(center, Pirate.GATE.defaultBlockState()); return center;
	}
	@GameTest public void preDragonRoundTripPreservesInventoryAndSeparateEntrances(GameTestHelper helper) {
		var first = gate(helper, 0); var second = gate(helper, 16);
		var player = player(helper, first.south()); var other = player(helper, second.south());
		var end = helper.getLevel().getServer().getLevel(Level.END); var previous = end.getDragonFight();
		try {
			end.setDragonFight(EnderDragonFight.createDefault());
			for (var p : new ServerPlayer[] { player, other }) {
				p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
				p.getInventory().getItem(0).set(DataComponents.CUSTOM_NAME, Component.literal("Voyage supplies"));
				helper.assertTrue(Pirate.enter(p, p == player ? first : second), "Sea gate requires no dragon victory or frame");
				helper.assertTrue(p.level().dimension().equals(Pirate.LEVEL), "Arrives in Pirate Seas");
				helper.assertTrue(!Pirate.leave(p, Pirate.EXIT), "Immediate return is rejected during arrival handshake/cooldown");
			}
			helper.assertTrue(Pirate.state(player).entrance.pos().equals(first) && Pirate.state(other).entrance.pos().equals(second), "Shared harbor preserves individual entrances");
			for (var p : new ServerPlayer[] { player, other }) {
				p.teleportTo(.5, 65, -2.5); ready(p);
				helper.assertTrue(Pirate.leave(p, Pirate.EXIT), "Harbor terminal returns traveler");
				helper.assertTrue(p.level() == helper.getLevel() && p.position().distanceToSqr(Vec3.atCenterOf(p == player ? first : second)) < 40, "Returns beside the correct sea gate");
				helper.assertTrue(p.getInventory().getItem(0).getCount() == 7 && p.getInventory().getItem(0).getHoverName().getString().equals("Voyage supplies"), "Item count and components survive both transitions");
			}
		} finally { end.setDragonFight(previous); remove(player); remove(other); }
		helper.succeed();
	}
	@GameTest public void invalidTravelAndBlockedArrivalsPreserveState(GameTestHelper helper) {
		var gate = gate(helper, 0); var player = player(helper, gate.south());
		try {
			player.inventoryMenu.setCarried(new ItemStack(Items.EMERALD));
			helper.assertTrue(!Pirate.enter(player, gate), "Carried inventory rejects travel"); player.inventoryMenu.setCarried(ItemStack.EMPTY);
			var trash = player.inventoryMenu.getSlot(darkspawn.black.inventory.InventoryExpansion.TRASH_MENU_SLOT);
			trash.set(new ItemStack(Items.DIAMOND));
			helper.assertTrue(!Pirate.enter(player, gate) && trash.getItem().is(Items.DIAMOND), "Staged trash is never lost in transit"); trash.set(ItemStack.EMPTY);
			player.setOnGround(false); helper.assertTrue(!Pirate.enter(player, gate), "Airborne departure rejected"); ready(player);
			helper.assertTrue(!Pirate.enter(player, gate.east(20)) && Pirate.state(player).entrance == null, "Invalid and distant gate cannot replace return state");
			var blocked = gate.east(30);
			for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -3; y <= 4; y++) helper.getLevel().setBlockAndUpdate(blocked.offset(x,y,z), Blocks.STONE.defaultBlockState());
			helper.assertTrue(Pirate.safePosition(player, helper.getLevel(), blocked) == null && helper.getLevel().getBlockState(blocked).is(Blocks.STONE), "Blocked arrival never excavates player blocks");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void returnLinkPersistsAndSurvivesDeath(GameTestHelper helper) {
		var gate = gate(helper, 0); var player = player(helper, gate.south()); var other = player(helper, gate.south(2));
		try {
			Pirate.state(player).entrance = GlobalPos.of(Level.OVERWORLD, gate);
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess()); player.saveWithoutId(output);
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING, other.registryAccess(), output.buildResult()));
			helper.assertTrue(Pirate.state(other).entrance.equals(Pirate.state(player).entrance), "Save/load preserves return link");
			Pirate.state(other).entrance = null;
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, other, false);
			helper.assertTrue(Pirate.state(other).entrance.equals(Pirate.state(player).entrance), "Respawn copy preserves return without changing vanilla death inventory");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}
	@GameTest public void missingEntranceUsesSafeOverworldRecovery(GameTestHelper helper) {
		var gate = gate(helper, 0); var player = player(helper, gate.south());
		try {
			helper.assertTrue(Pirate.enter(player, gate), "Enter before recovery check");
			player.teleportTo(.5,65,-2.5); ready(player);
			Pirate.state(player).entrance = GlobalPos.of(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Darkspawn.id("missing_sea_gate_world")), gate);
			helper.assertTrue(Pirate.leave(player, Pirate.EXIT) && player.level().dimension().equals(Level.OVERWORLD), "Missing return dimension falls back to safe Overworld spawn");
		} finally { remove(player); }
		helper.succeed();
	}
	@GameTest public void oceanAndNativeIslandPlacementSupportExploration(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Pirate.LEVEL);
		var origin = new BlockPos(4096,40,4096); level.getChunk(origin);
		helper.assertTrue(level.getBlockState(origin.below()).is(Blocks.SAND), "Ocean floor lies at Y39");
		helper.assertTrue(level.getBlockState(origin).is(Blocks.WATER) && level.getBlockState(origin.above(23)).is(Blocks.WATER) && level.getBlockState(origin.above(24)).isAir(), "Navigable ocean is 24 blocks deep with air above Y63");
		var piece = new darkspawn.black.worldgen.BossLandmarkStructure.LandmarkPiece(level.getStructureTemplateManager(), Darkspawn.id("pirate/island_1"), origin, Rotation.NONE);
		var bounds = piece.getBoundingBox();
		for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) level.getChunk(x,z);
		piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(7),bounds,new net.minecraft.world.level.ChunkPos(256,256),origin);
		helper.assertTrue(level.getBlockState(origin.offset(24,24,24)).is(Blocks.STONE_BRICKS), "Native template placement creates dry fort floor");
		var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(origin.offset(23,25,19));
		helper.assertTrue(chest != null, "Native island placement installs loot container"); chest.unpackLootTable(null);
		helper.assertTrue(!chest.isEmpty(), "Fort treasure table resolves to real supplies");
		chest.clearContent(); PirateHarbor.prepare(level); chest.unpackLootTable(null);
		helper.assertTrue(chest.isEmpty(), "Harbor visits never refill island treasure");
		helper.succeed();
	}
	@GameTest public void harborAndFirstIslandArePersistentWithFiniteLoot(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Pirate.LEVEL); PirateHarbor.prepare(level);
		helper.assertTrue(level.getBlockState(Pirate.EXIT).is(Pirate.RETURN), "Shared harbor contains return terminal");
		helper.assertTrue(level.getBlockState(new BlockPos(192,64,0)).is(Blocks.OAK_PLANKS), "First cove is guaranteed east of harbor");
		var chestPos = new BlockPos(-9,65,-6);
		var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestPos);
		helper.assertTrue(chest != null, "Chandlery contains starter supplies");
		chest.unpackLootTable(null);
		var contents = new java.util.ArrayList<ItemStack>();
		for (int i = 0; i < chest.getContainerSize(); i++) contents.add(chest.getItem(i).copy());
		helper.assertTrue(contents.stream().anyMatch(stack -> stack.is(Items.OAK_BOAT)), "Finite boat supply is usable");
		var edited = new BlockPos(5,65,0); var before = level.getBlockState(edited);
		try {
			chest.clearContent(); level.setBlockAndUpdate(edited, Blocks.GOLD_BLOCK.defaultBlockState()); PirateHarbor.prepare(level);
			helper.assertTrue(chest.isEmpty() && level.getBlockState(edited).is(Blocks.GOLD_BLOCK), "Revisit neither restocks loot nor resets builds");
			var data = level.getDataStorage().computeIfAbsent(PirateHarbor.TYPE);
			var encoded = PirateHarbor.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
			helper.assertTrue(encoded.getAsJsonObject().get("initialized").getAsBoolean(), "Harbor initialization is saved");
		} finally { level.setBlockAndUpdate(edited, before); for (int i = 0; i < contents.size(); i++) chest.setItem(i,contents.get(i)); }
		helper.succeed();
	}
	@GameTest public void templatesAndNativeIslandStartsAreValid(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Pirate.LEVEL); var access = level.registryAccess();
		for(int kind=0;kind<9;kind++) {
			var terrain=level.getStructureTemplateManager().getOrCreate(Darkspawn.id("pirate/terrain_"+kind));
			helper.assertTrue(terrain.getSize().equals(new net.minecraft.core.Vec3i(113,56,113)),"Expanded hinterland loads as a bounded native template");
			helper.assertTrue(!terrain.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.IRON_ORE).isEmpty(),"Hinterland contains real mineable ore");
		}
		for (String name : new String[] {"harbor", "island_0", "island_1", "island_2", "island_3", "island_4", "island_5", "island_6", "island_7", "island_8"}) {
			var template = level.getStructureTemplateManager().getOrCreate(Darkspawn.id("pirate/" + name));
			helper.assertTrue(template.getSize().equals(new net.minecraft.core.Vec3i(49,40,49)), "Bounded island footprint");
			var chests = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST);
			helper.assertTrue(chests.size() == 1 && chests.getFirst().nbt().getStringOr("LootTable", "").startsWith("darkspawn:chests/pirate_"), "Each destination has one dedicated finite cache");
		}
		var generator = level.getChunkSource().getGenerator();
		var holder = access.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, Darkspawn.id("pirate_island")));
		var structure = holder.value();
		var noise = (net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) net.minecraft.world.level.levelgen.presets.WorldPresets.createNormalWorldDimensions(access).overworld();
		for (long seed : new long[] {1,42,-7319}) {
			var random = net.minecraft.world.level.levelgen.RandomState.create(access.lookupOrThrow(net.minecraft.core.registries.Registries.NOISE),seed,noise.generatorSettings().value());
			var climate = random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED);
			for (int x : new int[] {0,12,32}) {
				var chunk = new net.minecraft.world.level.ChunkPos(x,0);
				var start = structure.generate(holder,Pirate.LEVEL,access,generator,generator.getBiomeSource(),climate,random,level.getStructureTemplateManager(),seed,chunk,0,level,structure.biomes()::contains);
				helper.assertTrue(start.isValid() == (x == 32), "Generation reserves harbor/first cove while allowing distant islands");
				if (start.isValid()) {
					var context = net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
					var saved = start.createTag(context,chunk);
					var loaded = net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(context,saved,seed);
					helper.assertTrue(loaded != null && loaded.isValid() && loaded.getBoundingBox().equals(start.getBoundingBox()), "Island starts survive native serialization");
				}
			}
		}
		helper.succeed();
	}

	@GameTest public void logboundCompassCalibratesOnlyInPirateSeasAndUsesPersonalState(GameTestHelper helper) {
		var player = player(helper, helper.absolutePos(BlockPos.ZERO));
		var other = player(helper, helper.absolutePos(new BlockPos(2,0,0)));
		try {
			var compass = new ItemStack(LogboundCompass.ITEM);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, compass);
			LogboundCompass.ITEM.use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
			helper.assertTrue(Pirate.state(player).logboundDestination.isEmpty(), "Overworld use cannot start a route");
			var sea = helper.getLevel().getServer().getLevel(Pirate.LEVEL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea, new Vec3(.5,65,.5), Vec3.ZERO, 0,0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			LogboundCompass.ITEM.use(sea, player, net.minecraft.world.InteractionHand.MAIN_HAND);
			var tracker = compass.get(DataComponents.LODESTONE_TRACKER);
			helper.assertTrue(tracker.target().orElseThrow().equals(GlobalPos.of(Pirate.LEVEL, LogboundCompass.FIRST_COVE_CENTER)) && !tracker.tracked(), "Needle targets the cove without a lodestone or remote chunk lookup");
			helper.assertTrue(!Pirate.state(player).logboundArrived, "Calibrating at harbor does not discover the island");
			var shared = compass.copy();
			LogboundCompass.ITEM.inventoryTick(shared, other.level(), other, null);
			helper.assertTrue(shared.get(DataComponents.LODESTONE_TRACKER).target().isEmpty() && Pirate.state(other).logboundDestination.isEmpty(), "A shared compass cannot transfer the original holder's route");
			var replacement = new ItemStack(LogboundCompass.ITEM);
			LogboundCompass.ITEM.inventoryTick(replacement, sea, player, null);
			helper.assertTrue(replacement.get(DataComponents.LODESTONE_TRACKER).equals(tracker), "Replacement compass resumes its holder's saved destination");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}
	@GameTest public void logboundArrivalRequiresActiveRouteDimensionAndSurfaceBoundary(GameTestHelper helper) {
		var player = player(helper, helper.absolutePos(BlockPos.ZERO));
		var other = player(helper, helper.absolutePos(new BlockPos(2,0,0)));
		try {
			Pirate.state(player).logboundDestination = LogboundCompass.FIRST_COVE;
			player.teleportTo(192.5,65,.5);
			helper.assertTrue(!LogboundCompass.observeArrival(player), "Matching coordinates in another dimension do not count");
			var sea = helper.getLevel().getServer().getLevel(Pirate.LEVEL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea, new Vec3(135.5,65,.5), Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			helper.assertTrue(!LogboundCompass.observeArrival(player), "Outside the island's west edge does not count");
			player.teleportTo(192.5,97,.5);
			helper.assertTrue(!LogboundCompass.observeArrival(player), "Flying above the island envelope does not count");
			player.teleportTo(192.5,39,.5);
			helper.assertTrue(!LogboundCompass.observeArrival(player), "Mining below the island foundation does not count");
			player.teleportTo(192.5,65,57.5);
			helper.assertTrue(!LogboundCompass.observeArrival(player), "Outside the south edge does not count");
			player.teleportTo(136.5,40,-55.5);
			player.setGameMode(GameType.SPECTATOR);
			helper.assertTrue(!LogboundCompass.observeArrival(player), "Spectators cannot earn discoveries");
			player.setGameMode(GameType.SURVIVAL);
			helper.assertTrue(LogboundCompass.observeArrival(player), "Entering the boundary records arrival");
			helper.assertTrue(!LogboundCompass.observeArrival(player) && Pirate.state(player).logboundArrived, "Arrival is recorded once even without carrying a compass");
			player.teleportTo(226.5,51,.5);int study=Pirate.state(player).logboundSecondsRemaining;
			LogboundCompass.tickNavigation(player);
			helper.assertTrue(Pirate.state(player).logboundSecondsRemaining==study-1,"Exploring a hinterland cave advances island study");
			other.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(192.5,65,.5),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			helper.assertTrue(!LogboundCompass.observeArrival(other) && !Pirate.state(other).logboundArrived, "An uncalibrated second player has independent discovery state");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}
	@GameTest public void logboundProgressSurvivesSaveAndDeathWithLegacyDefaults(GameTestHelper helper) {
		var player = player(helper, helper.absolutePos(BlockPos.ZERO));
		var other = player(helper, helper.absolutePos(new BlockPos(2,0,0)));
		try {
			Pirate.state(player).logboundDestination = LogboundCompass.FIRST_COVE;
			Pirate.state(player).logboundArrived = true;
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess()); player.saveWithoutId(output);
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING, other.registryAccess(), output.buildResult()));
			helper.assertTrue(Pirate.state(other).logboundDestination.equals(LogboundCompass.FIRST_COVE) && Pirate.state(other).logboundArrived, "Native save/load preserves destination and discovery");
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING, other.registryAccess(), new net.minecraft.nbt.CompoundTag()));
			helper.assertTrue(Pirate.state(other).logboundDestination.isEmpty() && !Pirate.state(other).logboundArrived, "Older saves start with no route or discovery");
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, other, false);
			helper.assertTrue(Pirate.state(other).logboundDestination.equals(LogboundCompass.FIRST_COVE) && Pirate.state(other).logboundArrived, "Respawn preserves knowledge independently of dropped items");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}

	@GameTest public void logboundStudyPausesOutsideIslandAndPersists(GameTestHelper helper) {
		var player = player(helper, helper.absolutePos(BlockPos.ZERO));
		var other = player(helper, helper.absolutePos(new BlockPos(2,0,0)));
		try {
			var state = Pirate.state(player); state.logboundDestination = LogboundCompass.FIRST_COVE;
			var sea = helper.getLevel().getServer().getLevel(Pirate.LEVEL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(192.5,65,.5),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			LogboundCompass.tickNavigation(player);
			helper.assertTrue(state.logboundArrived && state.logboundSecondsRemaining == 540, "Arrival starts a full nine minute study period");
			LogboundCompass.tickNavigation(player);
			helper.assertTrue(state.logboundSecondsRemaining == 539, "One scheduled second of island study is counted");
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(LogboundCompass.ITEM));
			for (int n=0;n<5;n++) LogboundCompass.ITEM.use(sea,player,net.minecraft.world.InteractionHand.MAIN_HAND);
			helper.assertTrue(state.logboundSecondsRemaining == 539 && !LogboundCompass.advanceRoute(player), "Repeated use cannot accelerate calibration or unlock early");
			player.teleportTo(250,65,0); LogboundCompass.tickNavigation(player);
			helper.assertTrue(state.logboundSecondsRemaining == 539, "Leaving the boundary pauses study");
			player.teleportTo(192.5,65,.5); player.setGameMode(GameType.SPECTATOR); LogboundCompass.tickNavigation(player);
			helper.assertTrue(state.logboundSecondsRemaining == 539, "Spectators do not advance study");
			player.setGameMode(GameType.SURVIVAL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(helper.getLevel(),new Vec3(192.5,65,.5),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			LogboundCompass.tickNavigation(player);
			helper.assertTrue(state.logboundSecondsRemaining == 539, "Other dimensions pause study");
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess()); player.saveWithoutId(output);
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),output.buildResult()));
			helper.assertTrue(Pirate.state(other).logboundSecondsRemaining == 539 && Pirate.state(other).logboundCoveDiscovered, "Save/load retains remaining active time and cove discovery");
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player,other,false);
			helper.assertTrue(Pirate.state(other).logboundSecondsRemaining == 539, "Death copy does not reset the countdown");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}
	@GameTest(maxTicks=100) public void logboundCompletionChartsRealIslandAndRetainsBothDiscoveries(GameTestHelper helper) throws ReflectiveOperationException {
		var player = player(helper,helper.absolutePos(BlockPos.ZERO));
		var other = player(helper,helper.absolutePos(new BlockPos(2,0,0)));
		// Native GameTest worlds disable structures; scope this fixture override to the synchronous lookup.
		var options = helper.getLevel().getServer().getWorldGenSettings().options();
		var structures = net.minecraft.world.level.levelgen.WorldOptions.class.getDeclaredField("generateStructures");
		structures.setAccessible(true);
		boolean previous = options.generateStructures();
		try {
			var sea = helper.getLevel().getServer().getLevel(Pirate.LEVEL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(192.5,65,.5),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			var state = Pirate.state(player); state.logboundDestination = LogboundCompass.FIRST_COVE; state.logboundArrived = true; state.logboundSecondsRemaining = 1;
			structures.setBoolean(options, false);
			LogboundCompass.tickNavigation(player);
			helper.assertTrue(state.logboundSecondsRemaining == 0 && state.logboundDestination.equals(LogboundCompass.FIRST_COVE), "Disabled structures preserve completed calibration and current route");
			helper.assertTrue(!LogboundCompass.advanceRoute(player), "Repeated failed lookup is throttled");
			structures.setBoolean(options, true); state.logboundSearchAfter = Long.MIN_VALUE;
			helper.assertTrue(LogboundCompass.advanceRoute(player), "Explicit retry charts an island after structure generation becomes available");
			helper.assertTrue(state.logboundDestination.equals(LogboundCompass.NEXT_ISLAND) && state.logboundNextIsland != null && !state.logboundArrived, "Finishing study charts a second destination: structures=" + sea.getServer().getWorldGenSettings().options().generateStructures() + ", remaining=" + state.logboundSecondsRemaining + ", route=" + state.logboundDestination);
			var target = state.logboundNextIsland;
			var structure = player.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,Darkspawn.id("pirate_island"))).value();
			var start = sea.getChunk(target).getAllStarts().get(structure);
			helper.assertTrue(start != null && start.isValid() && start.getBoundingBox().isInside(target), "Chart points inside a native seeded island, not an arbitrary ocean coordinate");
			var compass = new ItemStack(LogboundCompass.ITEM); LogboundCompass.ITEM.inventoryTick(compass,sea,player,null);
			helper.assertTrue(compass.get(DataComponents.LODESTONE_TRACKER).target().orElseThrow().pos().equals(target), "Needle follows the persisted second island");
			helper.assertTrue(Pirate.state(other).logboundNextIsland == null, "Another player's route is unaffected");
			player.teleportTo(target.getX()+.5,65,target.getZ()+.5);
			helper.assertTrue(LogboundCompass.observeArrival(player) && !LogboundCompass.observeArrival(player) && state.logboundCoveDiscovered, "Second arrival is once-only and preserves the first discovery");
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());player.saveWithoutId(output);
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),output.buildResult()));
			helper.assertTrue(Pirate.state(other).logboundNextIsland.equals(target) && Pirate.state(other).logboundArrived && Pirate.state(other).logboundCoveDiscovered, "Both discoveries and destination survive reload");
			helper.assertTrue(!LogboundCompass.advanceRoute(player), "Second island cannot reroll before its study period finishes");
			var legacy=output.buildResult().copy();legacy.remove("darkspawn_logbound_route_version");legacy.remove("darkspawn_logbound_visited");legacy.putInt("darkspawn_logbound_seconds",0);
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),legacy));
			helper.assertTrue(Pirate.state(other).logboundSecondsRemaining==540&&Pirate.state(other).logboundVisited.contains(target)&&Pirate.state(other).logboundVisited.contains(LogboundCompass.FIRST_COVE_CENTER),"Legacy completed two-stop saves resume with both discoveries and a full new study period");
			helper.assertTrue(state.logboundSecondsRemaining==540 && state.logboundVisited.contains(target) && state.logboundVisited.contains(LogboundCompass.FIRST_COVE_CENTER),"Every discovery starts a fresh study period and records visited coordinates");
			state.logboundSecondsRemaining=1;state.logboundSearchAfter=Long.MIN_VALUE;
			LogboundCompass.tickNavigation(player);
			helper.assertTrue(!state.logboundNextIsland.equals(target)&&!state.logboundVisited.contains(state.logboundNextIsland)&&!state.logboundArrived,"Third stop is a new island, not the nearest previously visited destination");
			var third=state.logboundNextIsland;
			player.teleportTo(third.getX()+.5,65,third.getZ()+.5);LogboundCompass.observeArrival(player);
			state.logboundSecondsRemaining=117;
			output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());player.saveWithoutId(output);
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),output.buildResult()));
			helper.assertTrue(Pirate.state(other).logboundVisited.equals(state.logboundVisited)&&Pirate.state(other).logboundSecondsRemaining==117,"Longer-route history and partial study survive reload");
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player,other,false);
			Pirate.state(other).logboundVisited.clear();
			helper.assertTrue(state.logboundVisited.size()==3,"Death copy owns an independent history list");
		} finally { structures.setBoolean(options, previous); remove(player); remove(other); }
		helper.succeed();
	}

	private static IslandDiscoveries.Island islandFixture(GameTestHelper helper,net.minecraft.server.level.ServerLevel sea,int kind) {
		// Test Isolation: The server retains dimension SavedData between runs; use this test's unique region.
		var base=helper.absolutePos(BlockPos.ZERO);
		var center=new BlockPos(Math.floorMod(base.getX(),10000)*1024+kind*128,65,Math.floorMod(base.getZ(),10000)*1024);
		var origin=center.offset(-24,-25,-24);
		var id=Darkspawn.id("pirate/island_"+kind);
		var template=sea.getStructureTemplateManager().getOrCreate(id);
		template.placeInWorld(sea,origin,origin,new StructurePlaceSettings(),sea.getRandom(),2);
		var structure=sea.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,Darkspawn.id("pirate_island"))).value();
		var piece=new darkspawn.black.worldgen.BossLandmarkStructure.LandmarkPiece(sea.getStructureTemplateManager(),id,origin,Rotation.NONE);
		sea.getChunk(center).setStartForStructure(structure,new net.minecraft.world.level.levelgen.structure.StructureStart(structure,new net.minecraft.world.level.ChunkPos(center.getX()>>4,center.getZ()>>4),0,new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece))));
		return new IslandDiscoveries.Island(center,kind);
	}
	@GameTest public void islandContractsRequireNativeObjectivesAndPayOnce(GameTestHelper helper) {
		var player=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));
		try {
			var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(0,65,0),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			player.setGameMode(GameType.CREATIVE); // Objective fixture avoids starting unrelated encounter groups.
			for(int i=0;i<3;i++)helper.assertTrue(HarborGuide.islandContract(player,i,4)&&!HarborGuide.islandContract(player,i,5),"Accept requires a later objective before payment");
			var wreck=islandFixture(helper,sea,2);var chest=wreck.center().offset(3,0,-4);
			helper.assertTrue(!IslandDiscoveries.inspect(player,chest),"Remote cache interaction is rejected");
			player.teleportTo(chest.getX(),65,chest.getZ()+1);
			helper.assertTrue(IslandDiscoveries.at(sea,chest).kind()==2,"Loaded native start identifies wreck template");
			player.setGameMode(GameType.SPECTATOR);helper.assertTrue(!IslandDiscoveries.inspect(player,chest),"Spectators cannot complete objectives");player.setGameMode(GameType.CREATIVE);
			helper.assertTrue(IslandDiscoveries.inspect(player,chest)&&!IslandDiscoveries.inspect(player,chest),"Cargo inspection counts once, without consuming loot");
			var shrine=islandFixture(helper,sea,4);chest=shrine.center().offset(0,0,-6);player.teleportTo(chest.getX(),65,chest.getZ()+1);
			helper.assertTrue(IslandDiscoveries.inspect(player,chest),"Relic cache completes the independent shrine objective");
			var beacon=islandFixture(helper,sea,3);player.teleportTo(beacon.center().getX(),65,beacon.center().getZ());IslandDiscoveries.observe(player);
			helper.assertTrue((Pirate.state(player).islandObjectives&4)==0,"Beacon ground floor does not count as investigation");
			player.teleportTo(beacon.center().getX()-3,77,beacon.center().getZ()-4);IslandDiscoveries.observe(player);
			helper.assertTrue(Pirate.state(player).islandObjectives==7&&Pirate.state(other).islandObjectives==0,"All three objectives are personal");
			helper.assertTrue(!HarborGuide.islandContract(player,2,5),"Reward requires harbor return");player.teleportTo(0,65,0);player.setGameMode(GameType.SURVIVAL);
			for(int i=0;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
			helper.assertTrue(!HarborGuide.islandContract(player,0,5)&&Pirate.state(player).islandContracts[0]==1,"Full inventory retains reward");player.getInventory().clearContent();
			for(int i=0;i<3;i++)helper.assertTrue(HarborGuide.islandContract(player,i,5)&&!HarborGuide.islandContract(player,i,5)&&!HarborGuide.islandContract(player,i,4),"Each contract pays once and cannot restart");
			helper.assertTrue(Ships.pay(player,Items.EMERALD,20,false)&&!Ships.pay(player,Items.EMERALD,21,false),"Rewards total exactly 6 + 8 + 6 emeralds");
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());player.saveWithoutId(output);
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),output.buildResult()));
			helper.assertTrue(java.util.Arrays.equals(Pirate.state(other).islandContracts,new int[]{2,2,2})&&Pirate.state(other).islandObjectives==7,"Native save preserves contract and objective states");
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player,other,false);Pirate.state(other).islandContracts[0]=0;
			helper.assertTrue(Pirate.state(player).islandContracts[0]==2,"Respawn contract array is independent");
		} finally {remove(player);remove(other);}
		helper.succeed();
	}
	@GameTest public void encountersPersistAndRespectSafeCoveAndMobRule(GameTestHelper helper) {
		var player=player(helper,helper.absolutePos(BlockPos.ZERO));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
		var rule=net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS;boolean previous=sea.getGameRules().get(rule);
		var island=islandFixture(helper,sea,1);
		try {
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,Vec3.atBottomCenterOf(island.center()),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			sea.getGameRules().set(rule,false,sea.getServer());
			helper.assertTrue(IslandDiscoveries.spawn(player,island)==0,"Mob spawning rule is respected");sea.getGameRules().set(rule,true,sea.getServer());
			helper.assertTrue(IslandDiscoveries.spawn(player,new IslandDiscoveries.Island(LogboundCompass.FIRST_COVE_CENTER,0))==0,"Guaranteed starter cove remains safe");
			helper.assertTrue(IslandDiscoveries.spawn(player,island)==2,"Fort spawns a bounded pair of sentries on native terrain");
			var saved=sea.getDataStorage().computeIfAbsent(IslandDiscoveries.Encounters.TYPE);
			helper.assertTrue(saved.mask(island.center())==3&&IslandDiscoveries.spawn(player,island)==0,"Shared island cannot duplicate its group");
			var copy=IslandDiscoveries.Encounters.CODEC.parse(JsonOps.INSTANCE,IslandDiscoveries.Encounters.CODEC.encodeStart(JsonOps.INSTANCE,saved).getOrThrow()).getOrThrow();
			helper.assertTrue(copy.mask(island.center())==3,"World save codec retains completed encounter slots");
			for(var mob:sea.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(island.center()).inflate(24),e->e.entityTags().contains(IslandDiscoveries.ENCOUNTER_TAG))) {
				helper.assertTrue(mob.getType()==net.minecraft.world.entity.EntityTypes.PILLAGER&&mob.getMainHandItem().is(Items.CROSSBOW),"Fort sentries have their ranged weapon");mob.discard();
			}
			helper.assertTrue(IslandDiscoveries.spawn(player,island)==0,"Cleared encounters never respawn on revisit");
            var camp=islandFixture(helper,sea,5);player.teleportTo(camp.center().getX(),65,camp.center().getZ());
            helper.assertTrue(IslandDiscoveries.spawn(player,camp)==2,"Corsair outpost has two native Piglin scavengers");
            for(var mob:sea.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(camp.center()).inflate(24),e->e.entityTags().contains(IslandDiscoveries.ENCOUNTER_TAG))) {
                var savedMob=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());mob.saveWithoutId(savedMob);
                helper.assertTrue(mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin&&savedMob.buildResult().getBooleanOr("IsImmuneToZombification",false),"Corsair encounter does not zombify in Pirate Seas");mob.discard();
            }
		} finally {sea.getGameRules().set(rule,previous,sea.getServer());remove(player);}
		helper.succeed();
	}
	@GameTest public void navigationReadoutAndJournalRetainIslandKnowledge(GameTestHelper helper) {
		var player=player(helper,helper.absolutePos(BlockPos.ZERO));
		try {
			var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(192.5,65,.5),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			var state=Pirate.state(player);state.logboundDestination=LogboundCompass.FIRST_COVE;LogboundCompass.tickNavigation(player);
			helper.assertTrue(LogboundCompass.status(player).equals("Castaway Cove | 0 blocks | Discovered 1 | Study 9:00"),"Readout identifies destination, horizontal distance, discoveries and study time");
			player.teleportTo(300.5,65,.5);helper.assertTrue(LogboundCompass.status(player).contains("108 blocks | Discovered 1 | Study 9:00 (paused)"),"Leaving island explicitly shows paused study");
			var notes=darkspawn.black.journal.Journal.snapshot(player).entries();
			helper.assertTrue(notes.stream().anyMatch(e->e.id().equals("pirate/island_0")&&e.discovered()&&e.body().contains("builders")),"First arrival adds a readable Journal clue");
			helper.assertTrue(notes.stream().anyMatch(e->e.id().equals("pirate/island_4")&&!e.discovered()),"Unvisited lore remains unknown");
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());player.saveWithoutId(output);var restored=new PiratePlayerState();restored.read(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),output.buildResult()));
			helper.assertTrue(restored.pirateClues==state.pirateClues,"Journal clue bits survive native save/load");
			PirateHarbor.prepare(sea);player.teleportTo(2,65,4);var menu=new HarborMenu(1,player.getInventory(),null,HarborGuide.BOARD_POS);
			helper.assertTrue(menu.clickMenuButton(player,8)&&menu.status(4)==1&&menu.clickMenuButton(player,4)&&state.islandContracts[0]==1,"Board cycles and accepts the selected contract");
			helper.assertTrue(menu.clickMenuButton(player,6)&&state.islandContracts[0]==0,"Board abandons only selected contract");
		} finally {remove(player);}
		helper.succeed();
	}
    @GameTest public void threeFactionContractsKeepIndependentStandingAndPersist(GameTestHelper helper) {
        var player=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var state=Pirate.state(player);
            player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(2,65,4),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            PirateHarbor.prepare(sea);var menu=new HarborMenu(1,player.getInventory(),null,HarborGuide.BOARD_POS);
            for(int i=0;i<4;i++)menu.clickMenuButton(player,8);
            helper.assertTrue(menu.status(4)==4&&menu.clickMenuButton(player,4)&&state.factionContracts[0]==1,"Board exposes and accepts Corsair contract");
            helper.assertTrue(!PirateProgression.contract(player,0,5),"No reward before activity");
            helper.assertTrue(PirateProgression.contract(player,0,6)&&PirateProgression.contract(player,0,4),"Faction activity can be abandoned before claiming");
            for(int index=1;index<3;index++)helper.assertTrue(PirateProgression.contract(player,index,4),"Other factions accept independently");
            for(var faction:PirateProgression.Faction.values()) {
                var island=islandFixture(helper,sea,faction.island);
                var chest=island.center().offset(faction==PirateProgression.Faction.CORSAIRS?1:faction==PirateProgression.Faction.FLEET?-1:0,0,faction==PirateProgression.Faction.FLEET?-5:faction==PirateProgression.Faction.CORSAIRS?-4:-6);
                helper.assertTrue(!IslandDiscoveries.inspect(player,chest),"Remote objective cannot be spoofed");
                player.teleportTo(chest.getX(),65,chest.getZ()+1);
                player.setGameMode(GameType.SPECTATOR);helper.assertTrue(!IslandDiscoveries.inspect(player,chest),"Spectators cannot earn faction progress");player.setGameMode(GameType.SURVIVAL);
                if(faction==PirateProgression.Faction.DROWNED){
                    IslandDiscoveries.inspect(player,chest);helper.assertTrue((state.factionObjectives&4)==0,"Discovering lore does not bypass the Drowned offering");
                    player.getInventory().setItem(0,new ItemStack(Items.PRISMARINE_SHARD,5));
                }
                helper.assertTrue(IslandDiscoveries.inspect(player,chest)&&!IslandDiscoveries.inspect(player,chest),"Objective completes exactly once at its native cache: "+faction+" / "+IslandDiscoveries.at(sea,chest));
                if(faction==PirateProgression.Faction.DROWNED)helper.assertTrue(player.getInventory().getItem(0).getCount()==1,"Offering consumes exactly four shards, once");
                helper.assertTrue(!PirateProgression.contract(player,faction.ordinal(),5),"Hand-in requires return to harbor");
                player.teleportTo(2,65,4);
                helper.assertTrue(PirateProgression.contract(player,faction.ordinal(),5)&&!PirateProgression.contract(player,faction.ordinal(),5)&&!PirateProgression.contract(player,faction.ordinal(),6),"Faction commendation cannot repeat or reset");
                helper.assertTrue(PirateProgression.standing(state,faction)==25&&PirateProgression.crewOffer(state,faction.role),"Trusted standing unlocks that faction crew offer");
                for(var future:PirateProgression.Faction.values())if(future.ordinal()>faction.ordinal())helper.assertTrue(PirateProgression.standing(state,future)==future.initial,"Other standings are not changed");
            }
            for(int i=0;i<11;i++)menu.clickMenuButton(player,8);
            helper.assertTrue(menu.status(4)==0&&menu.clickMenuButton(player,7)&&menu.status(4)==14,"Board wraps all fifteen activities");
            helper.assertTrue(java.util.Arrays.equals(Pirate.state(other).factionStanding,new int[]{0,0,-25}),"Other player retains defaults");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());player.saveWithoutId(output);
            Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),output.buildResult()));
            helper.assertTrue(java.util.Arrays.equals(Pirate.state(other).factionStanding,new int[]{25,25,25})&&Pirate.state(other).factionObjectives==7,"Native saves preserve standing and objectives");
            net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player,other,false);Pirate.state(other).factionStanding[0]=-100;Pirate.state(other).factionContracts[0]=0;
            helper.assertTrue(state.factionStanding[0]==25&&state.factionContracts[0]==2,"Respawn owns independent relationship and contract arrays");
            Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),new net.minecraft.nbt.CompoundTag()));
            helper.assertTrue(java.util.Arrays.equals(Pirate.state(other).factionStanding,new int[]{0,0,-25})&&Pirate.state(other).factionObjectives==0,"Legacy saves use safe faction defaults");
            helper.assertTrue(darkspawn.black.journal.Journal.snapshot(player).entries().stream().anyMatch(e->e.id().equals("pirate/progression")&&e.body().contains("Illager Iron Fleet: 25")),"Journal exposes actual standings");
        } finally {remove(player);remove(other);}
        helper.succeed();
    }
    @GameTest public void independentCaptainMilestonesAndStandingBounds(GameTestHelper helper) {
        var state=new PiratePlayerState();
        state.harborContract=2;state.islandContracts=new int[]{2,2,2};
        for(int i=0;i<8;i++)state.logboundVisited.add(new BlockPos(i*256,65,0));
        helper.assertTrue(PirateProgression.captainTier(state)==2&&state.navalVictories==0&&java.util.Arrays.equals(state.factionContracts,new int[]{0,0,0}),"Independent exploration and four ordinary contracts unlock the largest ship");
        helper.assertTrue(PirateProgression.crewCost(state,PirateShip.LOOKOUT)==10&&PirateProgression.crewCost(state,PirateShip.GUNNER)==6,"Independent captains earn crew discounts");
        state.logboundVisited.clear();for(int i=0;i<8;i++)state.logboundVisited.add(BlockPos.ZERO);
        helper.assertTrue(PirateProgression.captainTier(state)==0,"Duplicate coordinates cannot manufacture discoveries");
        state.navalVictories=5;helper.assertTrue(PirateProgression.captainTier(state)==1,"Existing five-victory progression is preserved");
        state.navalVictories=12;helper.assertTrue(PirateProgression.captainTier(state)==2,"Independent naval victories also unlock third tier");
        PirateProgression.addStanding(state,PirateProgression.Faction.FLEET,Integer.MAX_VALUE);helper.assertTrue(state.factionStanding[1]==100&&state.factionStanding[0]==0,"Standing clamps upward without affecting rivals");
        PirateProgression.addStanding(state,PirateProgression.Faction.FLEET,Integer.MIN_VALUE);helper.assertTrue(state.factionStanding[1]==-100,"Standing clamps downward without overflow");
        helper.assertTrue(!PirateProgression.crewOffer(state,PirateShip.GUNNER),"Standing alone cannot bypass activity completion");helper.succeed();
    }
    @GameTest public void membershipConsequencesAmendsAndPersistence(GameTestHelper helper) {
        var player=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);PirateHarbor.prepare(sea);
            player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(2,65,4),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            var state=Pirate.state(player);var corsairs=PirateProgression.Faction.CORSAIRS;var fleet=PirateProgression.Faction.FLEET;
            helper.assertTrue(!PirateDiplomacy.membership(player,corsairs,9),"Joining requires Trusted standing");state.factionStanding=new int[]{25,25,25};state.factionContracts=new int[]{2,2,2};
            var menu=new HarborMenu(1,player.getInventory(),null,HarborGuide.BOARD_POS);for(int i=0;i<4;i++)menu.clickMenuButton(player,8);
            helper.assertTrue(menu.clickMenuButton(player,9)&&state.factionMembership.equals(corsairs.id)&&menu.status(9)==1,"Board joins selected faction and synchronizes membership");
            helper.assertTrue(PirateProgression.crewCost(state,PirateShip.MATE)==6&&!PirateDiplomacy.membership(player,fleet,9),"Only member offer gains extra discount; second membership rejected");
            helper.assertTrue(menu.clickMenuButton(player,10)&&state.factionMembership.isEmpty()&&state.factionStanding[0]==0&&state.factionStanding[1]==25,"Leaving loses exactly 25 standing with only former faction");
            helper.assertTrue(!PirateDiplomacy.membership(player,corsairs,10)&&state.factionStanding[0]==0,"Repeated leave cannot deduct again");
            helper.assertTrue(!PirateDiplomacy.membership(player,corsairs,11),"Amends require payment");player.getInventory().setItem(0,new ItemStack(Items.GOLD_INGOT,40));
            for(int i=0;i<5;i++)helper.assertTrue(PirateDiplomacy.membership(player,corsairs,11),"Paid reconciliation restores five standing at a time");
            helper.assertTrue(state.factionStanding[0]==25&&player.getInventory().getItem(0).isEmpty()&&!PirateDiplomacy.membership(player,corsairs,11),"Amends consume exactly forty gold and cap at Trusted");
            helper.assertTrue(PirateDiplomacy.membership(player,fleet,9),"May join another faction after leaving");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());player.saveWithoutId(output);Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),output.buildResult()));
            helper.assertTrue(Pirate.state(other).factionMembership.equals(fleet.id),"Membership survives native save");
            net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player,other,false);helper.assertTrue(Pirate.state(other).factionMembership.equals(fleet.id),"Membership survives death copy");
            player.teleportTo(300,65,0);helper.assertTrue(!menu.clickMenuButton(player,10)&&state.factionMembership.equals(fleet.id),"Remote stale menu cannot resign membership");
            Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),new net.minecraft.nbt.CompoundTag()));helper.assertTrue(Pirate.state(other).factionMembership.isEmpty(),"Old saves remain independent");
        } finally {remove(player);remove(other);}helper.succeed();
    }
    @GameTest public void friendlyOutpostContactsPersistAndValidateLocalServices(GameTestHelper helper) {
        var player=player(helper,helper.absolutePos(BlockPos.ZERO));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
        var contacts=new java.util.ArrayList<net.minecraft.world.entity.Mob>();var oldDifficulty=sea.getDifficulty();
        try {
            sea.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
            player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(300,65,0),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            for(var faction:PirateProgression.Faction.values()) {
                var island=islandFixture(helper,sea,faction.island);player.teleportTo(island.center().getX(),65,island.center().getZ());
                PirateDiplomacy.observe(player,island);PirateDiplomacy.observe(player,island);
                var found=sea.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(island.center()).inflate(24),PirateDiplomacy::contact);
                helper.assertTrue(found.size()==1&&(sea.getDataStorage().computeIfAbsent(IslandDiscoveries.Encounters.TYPE).mask(island.center())&4)!=0,"Each outpost contact is created and saved once");
                var contact=found.getFirst();contacts.add(contact);player.teleportTo(contact.getX()+1,65,contact.getZ());
                helper.assertTrue(!contact.canAttack(player)&&!contact.hurtServer(sea,sea.damageSources().playerAttack(player),5),"Contact is friendly and protected even before trust");
                var menu=new HarborMenu(1,player.getInventory(),contact,null);
                helper.assertTrue(menu.stillValid(player)&&menu.status(4)==4+faction.ordinal()&&menu.clickMenuButton(player,4),"Contact offers its own faction activity locally");
                helper.assertTrue(!menu.clickMenuButton(player,3)&&menu.clickMenuButton(player,8)&&menu.status(4)==7+faction.ordinal()&&menu.clickMenuButton(player,8)&&menu.status(4)==4+faction.ordinal(),"Contact cycles only its own first and repeat contracts and cannot issue compasses");
                player.teleportTo(contact.getX()+20,65,contact.getZ());helper.assertTrue(!menu.stillValid(player)&&!menu.clickMenuButton(player,11),"Contact services reject distant packets");
            }
            sea.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
            for(var contact:contacts){
                contact.checkDespawn();helper.assertTrue(!contact.isRemoved(),"Friendly contact survives Peaceful difficulty");
                var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,sea.registryAccess());contact.saveWithoutId(output);
                var tag=output.buildResult();tag.putString("id",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(contact.getType()).toString());
                var restored=net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,sea,new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.LOAD,false),entity->entity);
                helper.assertTrue(restored!=null&&PirateDiplomacy.contact(restored),"Peaceful native save/load preserves friendly contacts");restored.discard();
                tag.remove("Tags");var ordinary=net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,sea,new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.LOAD,false),entity->entity);
                helper.assertTrue((ordinary!=null)==contact.getType().canSpawn(sea),"Untagged species retain their native Peaceful load rules");if(ordinary!=null)ordinary.discard();
            }
        } finally {sea.getServer().setDifficulty(oldDifficulty,true);contacts.forEach(net.minecraft.world.entity.Entity::discard);remove(player);}helper.succeed();
    }
    @GameTest public void guardDiplomacyTargetsHostilePlayersAndLeavesVanillaUnchanged(GameTestHelper helper) {
        var player=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));
        var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var island=islandFixture(helper,sea,1);
        var guard=net.minecraft.world.entity.EntityTypes.PILLAGER.create(sea,net.minecraft.world.entity.EntitySpawnReason.EVENT);
        var vanilla=net.minecraft.world.entity.EntityTypes.PILLAGER.create(sea,net.minecraft.world.entity.EntitySpawnReason.EVENT);
        try {
            var center=island.center();player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,Vec3.atBottomCenterOf(center.offset(0,0,2)),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            other.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,Vec3.atBottomCenterOf(center.offset(0,0,4)),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));ready(player);ready(other);
            guard.setPos(Vec3.atBottomCenterOf(center));guard.setNoAi(true);guard.addTag(PirateDiplomacy.FACTION_TAG+PirateProgression.Faction.FLEET.id);sea.addFreshEntity(guard);
            Pirate.state(other).factionStanding[1]=-10;
            helper.assertTrue(!guard.canAttack(player)&&guard.canAttack(other)&&vanilla.canAttack(player),"Native targeting respects personal standing only for tagged guards");
            guard.setTarget(player);helper.assertTrue(guard.getTarget()==null,"Direct target assignment cannot bypass neutrality");PirateDiplomacy.updateGuard(guard);helper.assertTrue(guard.getTarget()==other,"Guard selects hostile player rather than neutral neighbor");
            helper.assertTrue(!net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(player,sea.damageSources().mobAttack(guard),3),"Damage backstop protects neutral player from stale attacks");
            Pirate.state(other).factionStanding[1]=0;PirateDiplomacy.updateGuard(guard);helper.assertTrue(guard.getTarget()==null,"Repaired standing clears existing target");
            helper.assertTrue(guard.hurtServer(sea,sea.damageSources().playerAttack(player),1)&&Pirate.state(player).factionStanding[1]==-10,"Actual aggression costs standing");
            guard.hurtServer(sea,sea.damageSources().playerAttack(player),2);helper.assertTrue(Pirate.state(player).factionStanding[1]==-10,"Repeated hits in one second do not multiply the penalty");
            guard.setPos(0,65,0);helper.assertTrue(!guard.canAttack(player)&&!guard.hurtServer(sea,sea.damageSources().playerAttack(player),5),"Harbor protects tagged guards and prevents their attacks");
        } finally {guard.discard();vanilla.discard();remove(player);remove(other);}helper.succeed();
    }
	@GameTest public void harborGuideMigrationAndMenuEnforceDistance(GameTestHelper helper) {
		var player=player(helper,helper.absolutePos(BlockPos.ZERO));
		var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(0,65,0),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
		PirateHarbor.prepare(sea);
		for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)sea.setChunkForced(x,z,true);
		// Native Entity Loading: Wait for saved entity visibility in every harbor chunk.
		helper.startSequence().thenWaitUntil(()->{
			for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)helper.assertTrue(sea.isPositionTickingWithEntitiesLoaded(new net.minecraft.world.level.ChunkPos(x,z).pack()),"Harbor chunks are entity-ready");
		}).thenExecute(()->{
		try {
			var area=new net.minecraft.world.phys.AABB(-10,60,-10,10,75,10);
			var guides=sea.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,area,e->e.entityTags().contains(HarborGuide.BRIGGS_TAG));
			helper.assertTrue(guides.size()==1,"Harbor migration creates one Briggs; found "+guides.size());
			PirateHarbor.prepare(sea);
			helper.assertTrue(sea.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,area,e->e.entityTags().contains(HarborGuide.BRIGGS_TAG)).size()==1,"Revisiting never duplicates the guide");
			helper.assertTrue(sea.getBlockState(HarborGuide.BOARD_POS).is(HarborGuide.BOARD),"Migration installs the contract board");
			var guide=guides.getFirst();player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,guide.position().add(1,0,0),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			var menu=new HarborMenu(1,player.getInventory(),guide,null);
			helper.assertTrue(menu.stillValid(player)&&menu.clickMenuButton(player,0),"Nearby player can talk to Briggs");
			player.teleportTo(30,65,0);
			helper.assertTrue(!menu.stillValid(player)&&!menu.clickMenuButton(player,3)&&!Pirate.state(player).logboundCompassIssued,"Stale remote menu cannot issue items");
		} finally {for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)sea.setChunkForced(x,z,false);remove(player);}
		helper.succeed();
		});
	}
	@GameTest public void briggsCompassIsFreeOnceWithPaidReplacementAndNoFullInventoryLoss(GameTestHelper helper) {
		var player=player(helper,helper.absolutePos(BlockPos.ZERO));
		try {
			var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(0,65,0),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			for(int i=0;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
			helper.assertTrue(!HarborGuide.compass(player)&&!Pirate.state(player).logboundCompassIssued,"Full inventory does not consume the free issuance");
			player.getInventory().clearContent();
			helper.assertTrue(HarborGuide.compass(player)&&Pirate.state(player).logboundCompassIssued,"First compass is free");
			helper.assertTrue(player.getInventory().getItem(0).get(DataComponents.LODESTONE_TRACKER).target().isPresent(),"Briggs supplies a calibrated compass");
			helper.assertTrue(!HarborGuide.compass(player),"Repeated interaction cannot grant another compass");
			player.getInventory().clearContent();Pirate.state(player).logboundSecondsRemaining=123;
			helper.assertTrue(!HarborGuide.compass(player),"Replacement requires an ordinary compass");
			player.getInventory().setItem(0,new ItemStack(Items.COMPASS));
			helper.assertTrue(HarborGuide.compass(player)&&!Ships.pay(player,Items.COMPASS,1,false)&&Pirate.state(player).logboundSecondsRemaining==123,"Replacement consumes payment and preserves progress");
		} finally {remove(player);}
		helper.succeed();
	}
	@GameTest public void harborSurveyIsOptionalPersonalAndPaysOnlyOnce(GameTestHelper helper) {
		var player=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));
		try {
			var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(0,65,0),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			helper.assertTrue(HarborGuide.contract(player,4)&&HarborGuide.contract(player,6)&&HarborGuide.contract(player,4),"Optional survey supports abandoning and accepting again");
			helper.assertTrue(!HarborGuide.contract(player,5),"Cannot claim without visiting the cove");
			player.teleportTo(192,65,0);HarborGuide.observeContract(player);
			helper.assertTrue(Pirate.state(player).harborSurveyed&&!Pirate.state(other).harborSurveyed&&!HarborGuide.contract(player,5),"Personal objective completes at the cove but reward requires harbor return");
			player.teleportTo(0,65,0);
			for(int i=0;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
			helper.assertTrue(!HarborGuide.contract(player,5)&&Pirate.state(player).harborContract==1,"Full inventory preserves an unclaimed reward");
			player.getInventory().clearContent();
			helper.assertTrue(HarborGuide.contract(player,5)&&Ships.pay(player,Items.EMERALD,4,false)&&!Ships.pay(player,Items.EMERALD,5,false),"Survey pays exactly four emeralds");
			helper.assertTrue(!HarborGuide.contract(player,5)&&!HarborGuide.contract(player,4)&&!HarborGuide.contract(player,6),"Completed contract cannot be replayed or reset");
			Pirate.state(player).logboundCompassIssued=true;
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());player.saveWithoutId(output);
			Pirate.state(other).read(TagValueInput.create(ProblemReporter.DISCARDING,other.registryAccess(),output.buildResult()));
			helper.assertTrue(Pirate.state(other).harborContract==2&&Pirate.state(other).harborSurveyed&&Pirate.state(other).logboundCompassIssued,"Issuance and completed contract persist through native saves");
			net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player,other,false);
			helper.assertTrue(Pirate.state(other).harborContract==2&&Pirate.state(other).logboundCompassIssued,"Death retains reward and issuance history");
		} finally {remove(player);remove(other);}
		helper.succeed();
	}
    @GameTest public void repeatVoyagesRequireFreshObjectivesAndPersistCooldown(GameTestHelper helper) {
        var player=player(helper,helper.absolutePos(BlockPos.ZERO));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
        try {
            player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(2,65,4),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            PirateHarbor.prepare(sea);var state=Pirate.state(player);var board=new HarborMenu(1,player.getInventory(),null,HarborGuide.BOARD_POS);
            for(int i=0;i<7;i++)board.clickMenuButton(player,8);
            helper.assertTrue(board.status(4)==7&&board.clickMenuButton(player,4)&&state.repeatContracts[0]==1,"Board selects repeat cargo activity");
            helper.assertTrue(!PirateActivities.contract(player,0,5),"No repeat reward before objective");
            var wreck=islandFixture(helper,sea,2);var cache=wreck.center().offset(3,0,-4);
            helper.assertTrue(!IslandDiscoveries.inspect(player,cache),"Remote cache inspection cannot earn progress");
            player.teleportTo(cache.getX(),65,cache.getZ()+1);
            helper.assertTrue(IslandDiscoveries.inspect(player,cache)&&!IslandDiscoveries.inspect(player,cache)&&state.repeatContracts[0]==2,"Shared cargo cache records one personal completion after acceptance");
            helper.assertTrue(!PirateActivities.contract(player,0,5),"Cargo requires valid hand-in location");player.teleportTo(2,65,4);
            for(int i=0;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            helper.assertTrue(!PirateActivities.contract(player,0,5)&&state.repeatContracts[0]==2&&state.factionStanding[0]==0,"Full inventory preserves completion and reputation");player.getInventory().clearContent();
            helper.assertTrue(PirateActivities.contract(player,0,5)&&state.factionStanding[0]==10&&player.getInventory().getItem(0).getCount()==6&&!PirateActivities.contract(player,0,5),"One completion pays six emeralds and ten reputation only once");
            helper.assertTrue(!PirateActivities.contract(player,0,4)&&PirateProgression.completedContracts(state)==0,"Cooldown blocks farming and repeats do not inflate captain contract milestones");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,sea.registryAccess());state.write(output);var loaded=new PiratePlayerState();loaded.read(TagValueInput.create(ProblemReporter.DISCARDING,sea.registryAccess(),output.buildResult()));var copy=new PiratePlayerState();copy.copyFrom(loaded);
            helper.assertTrue(copy.repeatAvailableAt[0]==state.repeatAvailableAt[0]&&copy.repeatContracts!=state.repeatContracts,"Cooldown and separate arrays survive save and death copying");
            state.repeatAvailableAt[0]=sea.getServer().overworld().getGameTime();helper.assertTrue(PirateActivities.contract(player,0,4)&&state.repeatContracts[0]==1&&!PirateActivities.contract(player,0,5),"After cooldown a fresh objective is required");
            helper.assertTrue(PirateActivities.contract(player,2,4),"Drowned repeat accepts independently without membership");var shrine=islandFixture(helper,sea,4);var relic=shrine.center().offset(0,0,-6);player.teleportTo(relic.getX(),65,relic.getZ()+1);
            IslandDiscoveries.inspect(player,relic);helper.assertTrue(state.repeatContracts[2]==1,"Repeat shrine expedition still requires offering despite free lore discovery");player.getInventory().setItem(1,new ItemStack(Items.PRISMARINE_SHARD,5));
            helper.assertTrue(IslandDiscoveries.inspect(player,relic)&&!IslandDiscoveries.inspect(player,relic)&&player.getInventory().getItem(1).getCount()==1,"Repeat offering consumes exactly four shards once");
            player.teleportTo(2,65,4);helper.assertTrue(PirateActivities.contract(player,2,5)&&state.factionStanding[2]==-15,"Repeat shrine improves only Drowned standing");
        } finally {remove(player);}helper.succeed();
    }
    @GameTest public void garrickRescueIsPersonalAndFreeChoicePersists(GameTestHelper helper) {
        var player=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
        net.minecraft.world.entity.Entity captive=null,second=null;
        try {
            for(var captain:java.util.List.of(player,other))captain.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(2,65,4),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            helper.assertTrue(PirateActivities.rescueOrder(player,4)&&PirateActivities.rescueOrder(other,4),"Each captain accepts their own rescue");var island=islandFixture(helper,sea,1);var state=Pirate.state(player);
            for(var captain:java.util.List.of(player,other)){captain.teleportTo(island.center().getX(),65,island.center().getZ());PirateActivities.observe(captain,island);}
            captive=sea.getEntity(state.garrickCaptive);second=sea.getEntity(Pirate.state(other).garrickCaptive);
            helper.assertTrue(captive!=null&&second!=null&&captive!=second,"Both captains get distinct durable captive identities");var id=captive.getUUID();PirateActivities.observe(player,island);helper.assertTrue(id.equals(state.garrickCaptive),"Repeated observation does not duplicate captive");
            player.teleportTo(captive.getX()+1,captive.getY(),captive.getZ());other.teleportTo(captive.getX()+1,captive.getY(),captive.getZ());
            helper.assertTrue(!PirateActivities.rescue(other,captive)&&!PirateActivities.rescue(player,captive),"Other captain and missing papers cannot free this captive");
            var cache=island.center().offset(-1,0,-5);player.teleportTo(cache.getX(),65,cache.getZ()+1);
            helper.assertTrue(IslandDiscoveries.inspect(player,cache)&&state.garrickRescue==2,"Fort dispatch chest grants personal release papers");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,sea.registryAccess());state.write(output);var restored=new PiratePlayerState();restored.read(TagValueInput.create(ProblemReporter.DISCARDING,sea.registryAccess(),output.buildResult()));
            helper.assertTrue(restored.garrickRescue==2&&restored.garrickCaptive.equals(id)&&restored.garrickPrison.equals(island.center()),"Papers, captive identity and fort location survive reload");
            player.teleportTo(captive.getX()+1,captive.getY(),captive.getZ());helper.assertTrue(PirateActivities.rescue(player,captive)&&state.garrickRescue==3&&captive.isRemoved()&&!PirateActivities.rescue(player,captive),"Personal rescue completes exactly once");
            helper.assertTrue(!PirateActivities.rescueOrder(player,6),"Resolve officer choice only at harbor");player.teleportTo(2,65,4);
            helper.assertTrue(!PirateActivities.rescueOrder(player,5)&&state.garrickRescue==3,"Missing ship preserves recruitment choice");
            helper.assertTrue(PirateActivities.rescueOrder(player,6)&&state.garrickRescue==4&&!PirateActivities.rescueOrder(player,4)&&!PirateActivities.rescueOrder(player,5),"Freeing without hiring is a final personal choice");
            var deathCopy=new PiratePlayerState();deathCopy.copyFrom(state);helper.assertTrue(deathCopy.garrickRescue==4&&Pirate.state(other).garrickRescue==1&&!second.isRemoved(),"Choice persists through death without changing another captain's rescue");
        } finally {if(captive!=null)captive.discard();if(second!=null)second.discard();remove(player);remove(other);}helper.succeed();
    }

    @GameTest public void firstSealAndOfficerCommissionsArePersonalAndPersistent(GameTestHelper helper) {
        var player=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);
        try {
            player.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,new Vec3(2,65,4),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));PirateHarbor.prepare(sea);var state=Pirate.state(player);
            helper.assertTrue(!PirateOfficers.contract(player,11,4),"Story follow-up requires discovered clue");var shrine=islandFixture(helper,sea,4);var chest=shrine.center().offset(0,0,-6);
            helper.assertTrue(!IslandDiscoveries.inspect(player,chest)&&!state.firstSealClue,"Remote lore discovery rejected");player.teleportTo(chest.getX(),65,chest.getZ()+1);
            helper.assertTrue(IslandDiscoveries.inspect(player,chest)&&state.firstSealClue&&!Pirate.state(other).firstSealClue,"Inspecting the native relic cache discovers personal lore without payment");
            helper.assertTrue(darkspawn.black.journal.Journal.snapshot(player).entries().stream().anyMatch(e->e.id().equals("pirate/first_seal")&&e.discovered()&&e.body().contains("bindings")),"Journal exposes the discovered Seven Seals entry");
            var beacon=islandFixture(helper,sea,3);player.teleportTo(beacon.center().getX(),78,beacon.center().getZ());PirateOfficers.observe(player,beacon);helper.assertTrue(state.sealContract==0,"Prior exploration cannot auto-complete an unaccepted follow-up");player.teleportTo(2,65,4);
            var menu=new HarborMenu(1,player.getInventory(),null,HarborGuide.BOARD_POS);for(int i=0;i<11;i++)menu.clickMenuButton(player,8);
            helper.assertTrue(menu.status(4)==11&&menu.clickMenuButton(player,4)&&state.sealContract==1,"Board accepts optional story follow-up");
            for(int selected=12;selected<=14;selected++)helper.assertTrue(PirateOfficers.contract(player,selected,4),"Independent officer commission accepts");
            player.teleportTo(beacon.center().getX(),65,beacon.center().getZ());PirateOfficers.observe(player,beacon);helper.assertTrue(state.sealContract==1&&state.officerContracts[0]==1,"Beacon ground level cannot satisfy lantern objective");
            player.teleportTo(beacon.center().getX(),78,beacon.center().getZ());PirateOfficers.observe(player,beacon);helper.assertTrue(state.sealContract==2&&state.officerContracts[0]==2,"Lantern records story signal and navigator bearings");
            for(int kind:new int[]{5,2}) {var island=islandFixture(helper,sea,kind);var cache=island.center().offset(kind==5?1:3,0,-4);player.teleportTo(cache.getX(),65,cache.getZ()+1);helper.assertTrue(IslandDiscoveries.inspect(player,cache),"Native cache records officer objective");}
            helper.assertTrue(!PirateOfficers.contract(player,13,5),"Officer hand-in rejects remote location");player.teleportTo(2,65,4);player.getInventory().clearContent();
            helper.assertTrue(!PirateOfficers.contract(player,13,5)&&state.officerContracts[1]==2,"Bram's supplies required before recruitment unlock");
            player.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,8));helper.assertTrue(!PirateOfficers.contract(player,13,5)&&player.getInventory().getItem(0).getCount()==8,"Missing planks do not partially consume iron");player.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,16));
            for(int selected=12;selected<=14;selected++)helper.assertTrue(PirateOfficers.contract(player,selected,5)&&!PirateOfficers.contract(player,selected,5),"Each commission unlocks exactly once");
            helper.assertTrue(player.getInventory().getItem(0).isEmpty()&&player.getInventory().getItem(1).isEmpty(),"Bram consumes exactly the agreed supplies");
            helper.assertTrue(PirateOfficers.contract(player,11,5)&&!PirateOfficers.contract(player,11,5)&&player.getInventory().getItem(0).getCount()==8&&PirateProgression.completedContracts(state)==4,"Story reward pays once and unique commissions count once toward captaincy");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,sea.registryAccess());state.write(output);var restored=new PiratePlayerState();restored.read(TagValueInput.create(ProblemReporter.DISCARDING,sea.registryAccess(),output.buildResult()));var copied=new PiratePlayerState();copied.copyFrom(restored);
            helper.assertTrue(copied.firstSealClue&&copied.sealContract==3&&java.util.Arrays.equals(copied.officerContracts,new int[]{3,3,3})&&copied.officerContracts!=state.officerContracts,"Story knowledge and officer availability survive save and death copying");
        } finally {remove(player);remove(other);}helper.succeed();
    }


    private static void seaTravel(ServerPlayer p,net.minecraft.server.level.ServerLevel sea,BlockPos at){p.teleport(new net.minecraft.world.level.portal.TeleportTransition(sea,Vec3.atBottomCenterOf(at),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));ready(p);}
    @GameTest public void seaServicesChargeOnceAndPreserveRecoveryReceipts(GameTestHelper helper){
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);PirateHarbor.prepare(sea);seaTravel(p,sea,new BlockPos(2,65,4));var state=Pirate.state(p);p.getInventory().clearContent();
            helper.assertTrue(SeaServices.execute(p,0)==SeaServices.Reply.COST,"Missing currency gives precise failure");
            p.getInventory().setItem(0,new ItemStack(Items.EMERALD,32));
            helper.assertTrue(SeaServices.execute(p,0)==SeaServices.Reply.DONE&&p.getInventory().countItem(Items.EMERALD)==31&&p.getInventory().countItem(Items.COOKED_COD)==8,"Tavern transaction conserves exact payment and supply");
            state.renown=50;helper.assertTrue(SeaServices.execute(p,1)==SeaServices.Reply.DONE&&p.getInventory().countItem(Items.EMERALD)==30,"Renown discount applies on server");
            var ledger=SeaLedger.get(sea);ledger.lose(p.getUUID(),15);state.officerContracts=new int[]{4,4,4};state.garrickRescue=5;
            helper.assertTrue(SeaServices.execute(p,7)==SeaServices.Reply.DONE&&state.officerContracts[0]==3&&ledger.lost(p.getUUID())==14&&p.getInventory().countItem(Items.EMERALD)==22,"Paid officer recovery restores recruitment without resetting completed commission");
            helper.assertTrue(SeaServices.execute(p,7)==SeaServices.Reply.NO_RESCUE&&p.getInventory().countItem(Items.EMERALD)==22,"Repeated recovery cannot charge or duplicate");
            helper.assertTrue(SeaServices.execute(p,10)==SeaServices.Reply.DONE&&state.garrickRescue==3&&ledger.lost(p.getUUID())==6,"Garrick recovery preserves his completed rescue choice");
            var encoded=SeaLedger.CODEC.encodeStart(JsonOps.INSTANCE,ledger).getOrThrow();var restored=SeaLedger.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow();helper.assertTrue(restored.lost(p.getUUID())==6,"World rescue claims survive serialization without requiring online players");
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.getInventory().setItem(0,new ItemStack(Items.EMERALD,16));
            helper.assertTrue(SeaServices.execute(p,0)==SeaServices.Reply.SPACE&&p.getInventory().countItem(Items.EMERALD)==16,"Full inventory never consumes payment");
            helper.assertTrue(SeaServices.execute(p,5)==SeaServices.Reply.DONE&&SeaServices.execute(p,5)==SeaServices.Reply.GHOST_ACTIVE,"Ghost voyage is idempotent");
            var menu=new HarborMenu(1,p.getInventory(),null,HarborGuide.BOARD_POS);menu.clickMenuButton(p,5);helper.assertTrue(menu.status(13)==HarborFeedback.ACCEPT.ordinal(),"Contract failure identifies unaccepted work");
            menu.clickMenuButton(p,9);helper.assertTrue(menu.status(13)==HarborFeedback.FACTION.ordinal(),"Diplomacy failure identifies missing faction selection");
            p.teleportTo(300,65,0);helper.assertTrue(SeaServices.execute(p,7)==SeaServices.Reply.HARBOR,"Remote services rejected");
        } finally {remove(p);}helper.succeed();
    }
    @GameTest public void sevenSealsRequireDiscoveriesAndKeepPersonalEndings(GameTestHelper helper){
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));var other=player(helper,helper.absolutePos(new BlockPos(2,0,0)));
        try {
            var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var shrine=islandFixture(helper,sea,4);seaTravel(p,sea,shrine.center());seaTravel(other,sea,shrine.center());var state=Pirate.state(p);
            helper.assertTrue(SevenSeals.order(p,14)==SeaServices.Reply.FIRST&&!SevenSeals.discover(p,1),"First binding gates later discoveries");state.sealContract=3;SevenSeals.discover(p,0);
            for(int kind:new int[]{1,2,5}){var island=islandFixture(helper,sea,kind);var cache=island.center().offset(kind==1?-1:kind==2?3:1,0,kind==1?-5:-4);p.teleportTo(cache.getX(),65,cache.getZ()+1);IslandDiscoveries.inspect(p,cache);}
            helper.assertTrue(state.seals==15,"Three native caches unlock three distinct seals");p.teleportTo(shrine.center().getX(),65,shrine.center().getZ());p.getInventory().setItem(0,new ItemStack(Items.PRISMARINE_SHARD,16));
            helper.assertTrue(SevenSeals.order(p,14)==SeaServices.Reply.DONE&&SevenSeals.order(p,14)==SeaServices.Reply.RECORDED&&p.getInventory().countItem(Items.PRISMARINE_SHARD)==8,"Fifth offering charges once");
            helper.assertTrue(SevenSeals.order(p,15)==SeaServices.Reply.SEALS,"Incomplete ending rejected");SevenSeals.discover(p,5);var beacon=islandFixture(helper,sea,3);p.teleportTo(beacon.center().getX(),78,beacon.center().getZ());SevenSeals.observe(p,beacon);helper.assertTrue(state.seals==127,"Final beacon requires first six seals");
            p.teleportTo(shrine.center().getX(),65,shrine.center().getZ());
            for(int slot=0;slot<35;slot++)p.getInventory().setItem(slot,new ItemStack(Items.STONE,64));p.getInventory().setItem(35,ItemStack.EMPTY);
            helper.assertTrue(SevenSeals.order(p,15)==SeaServices.Reply.ENDING_SPACE&&state.ending==0,"One free slot cannot consume a two-slot ending reward");p.getInventory().clearContent();
            helper.assertTrue(SevenSeals.order(p,15)==SeaServices.Reply.DONE&&state.ending==1&&state.infamy==40,"King grants personal ending and infamy");int gold=p.getInventory().countItem(Items.EMERALD);
            helper.assertTrue(SevenSeals.order(p,17)==SeaServices.Reply.ENDED&&p.getInventory().countItem(Items.EMERALD)==gold,"Ending cannot be switched or rewarded twice");
            var second=Pirate.state(other);second.sealContract=3;second.seals=127;helper.assertTrue(SevenSeals.order(other,17)==SeaServices.Reply.DONE&&second.ending==3&&state.ending==1,"Shared shrine allows independent final choices");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());p.saveWithoutId(output);var restored=new PiratePlayerState();restored.read(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),output.buildResult()));var copy=new PiratePlayerState();copy.copyFrom(restored);helper.assertTrue(copy.seals==127&&copy.ending==1&&copy.infamy==40,"All discoveries and final choice survive save and death copy");
        }finally{remove(p);remove(other);}helper.succeed();
    }
    @GameTest public void fortCapturePermitsOfflineTakeoverAndBoundsRewards(GameTestHelper helper){
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var difficulty=sea.getDifficulty();
        try {
            sea.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);var fort=islandFixture(helper,sea,1);seaTravel(p,sea,fort.center());var ledger=SeaLedger.get(sea);var offline=UUID.randomUUID();ledger.fort(new SeaLedger.Fort(fort.center(),offline,"Offline captain",0,0));
            var marker=fort.center().offset(0,-1,0);var block=sea.getBlockState(marker);
            helper.assertTrue(SeaTerritory.order(p,11)==SeaServices.Reply.CAPTURE,"Offline ownership permits a takeover challenge");
            SeaTerritory.tick(sea);helper.assertTrue(SeaTerritory.progress(p)==0,"Living garrison prevents capture");
            var rival=player(helper,helper.absolutePos(new BlockPos(3,0,0)));
            try {seaTravel(rival,sea,fort.center().east(3));
                for(var mob:sea.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(fort.center()).inflate(30),m->m.entityTags().contains(SeaTerritory.DEFENDER))){helper.assertTrue(PirateDiplomacy.disallows(mob,rival)&&!PirateDiplomacy.disallows(mob,p),"Garrison attacks its claimant, not bystanders");mob.discard();}
                SeaTerritory.tick(sea);helper.assertTrue(SeaTerritory.progress(p)==0,"Rival captain contests a cleared fort");
            }finally{remove(rival);}
            for(int i=0;i<60;i++)SeaTerritory.tick(sea);
            helper.assertTrue(ledger.fort(fort.center()).owner().equals(p.getUUID())&&Pirate.state(p).infamy==10&&sea.getBlockState(marker).equals(block),"Takeover updates owner and infamy without replacing builds");
            helper.assertTrue(SeaTerritory.order(p,13)==SeaServices.Reply.SUPPLY_WAIT,"Capture starts supply cooldown");p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,16));p.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,48));
            helper.assertTrue(SeaTerritory.order(p,12)==SeaServices.Reply.DONE&&SeaTerritory.order(p,12)==SeaServices.Reply.DONE&&SeaTerritory.order(p,12)==SeaServices.Reply.MAX_DEFENSE,"Fortifications cost supplies and stop at two");
            var saved=ledger.fort(fort.center());ledger.fort(new SeaLedger.Fort(saved.center(),saved.owner(),saved.name(),saved.defenses(),0));
            helper.assertTrue(SeaTerritory.order(p,13)==SeaServices.Reply.DONE&&SeaTerritory.order(p,13)==SeaServices.Reply.SUPPLY_WAIT&&p.getInventory().countItem(Items.EMERALD)==4,"Fort payout is once per cooldown");
            var restored=SeaLedger.CODEC.parse(JsonOps.INSTANCE,SeaLedger.CODEC.encodeStart(JsonOps.INSTANCE,ledger).getOrThrow()).getOrThrow();helper.assertTrue(restored.fort(fort.center()).equals(ledger.fort(fort.center())),"Territory ownership and timers persist");
        }finally{remove(p);SeaTerritory.tick(sea);sea.getServer().setDifficulty(difficulty,true);}helper.succeed();
    }

    @GameTest public void liberatorBattleRequiresPersonalVictoryAndCleansUp(GameTestHelper helper){
        var p=player(helper,helper.absolutePos(BlockPos.ZERO));var sea=helper.getLevel().getServer().getLevel(Pirate.LEVEL);var difficulty=sea.getDifficulty();
        try {
            sea.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);var shrine=islandFixture(helper,sea,4);seaTravel(p,sea,shrine.center());var state=Pirate.state(p);state.sealContract=3;state.seals=127;
            helper.assertTrue(SevenSeals.order(p,16)==SeaServices.Reply.BATTLE&&state.ending==0,"Liberation summons a personal adversary before granting rewards");
            helper.assertTrue(SevenSeals.order(p,16)==SeaServices.Reply.BATTLE_ACTIVE,"Repeated summon cannot create extra bosses");var ledger=SeaLedger.get(sea);var receipt=ledger.battle(p.getUUID());var mob=(net.minecraft.world.entity.Mob)sea.getEntity(receipt.entity());helper.assertTrue(mob!=null&&mob.getHealth()==120,"Bound Admiral spawns with intended health");
            var encoded=SeaLedger.CODEC.encodeStart(JsonOps.INSTANCE,ledger).getOrThrow();helper.assertTrue(SeaLedger.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow().battle(p.getUUID()).equals(receipt),"Battle receipt survives restart");
            mob.hurtServer(sea,mob.damageSources().genericKill(),1000);helper.assertTrue(ledger.battle(p.getUUID()).defeated(),"Defeat records victory for its captain");
            helper.assertTrue(SevenSeals.order(p,16)==SeaServices.Reply.DONE&&state.ending==2&&p.getInventory().countItem(Items.TRIDENT)==1,"Liberation pays once after real entity defeat");
            helper.assertTrue(SevenSeals.order(p,16)==SeaServices.Reply.ENDED&&p.getInventory().countItem(Items.TRIDENT)==1,"Repeated liberation cannot duplicate trophy");
            mob.discard();
        }finally{remove(p);SevenSeals.tick();sea.getServer().setDifficulty(difficulty,true);}helper.succeed();
    }
}
