package darkspawn.black.legacy;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.mount.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class NoahGameTest {
    private static ServerPlayer player(GameTestHelper helper) {
        var level=helper.getLevel();var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"noah-test"),false);
        var p=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation()) {
            @Override public GameType gameMode(){return GameType.SURVIVAL;}
            @Override public boolean isClientAuthoritative(){return false;}
        };
        var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);level.getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL);p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        var at=helper.absolutePos(new BlockPos(3,3,3)).atY(260);
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++) {
            level.getChunk((at.getX()+x)>>4,(at.getZ()+z)>>4);level.setBlock(at.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            for(int y=0;y<6;y++)level.setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        }
        p.setPos(Vec3.atBottomCenterOf(at));p.setOnGround(true);p.getInventory().clearContent();return p;
    }
    private static void cleanup(ServerPlayer p) {
        for(var mount:p.level().getEntitiesOfClass(Thalassar.class,p.getBoundingBox().inflate(40)))mount.discard();
        for(var mob:p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(15)))mob.discard();
        p.stopRiding();p.level().getServer().getPlayerList().remove(p);p.discard();
    }
    private static ItemStack relic(ServerPlayer p,Item item) {var stack=new ItemStack(item);NoahLegacy.bind(stack,p);return stack;}
    private static void armor(ServerPlayer p) {NoahLegacy.ARMOR.forEach((type,item)->p.setItemSlot(type.getSlot(),relic(p,item)));}
    private static Thalassar mount(ServerPlayer p) {return p.level().getEntitiesOfClass(Thalassar.class,p.getBoundingBox().inflate(12)).stream().filter(m->m.isOwnedBy(p)).findFirst().orElseThrow();}
    private static void ready(ServerPlayer p) {NoahLedger.get(p.level()).entry(p.getUUID()).putLong("ConchReady",0);}

    @GameTest public void giftBindsSixRelicsAndRejectsOtherOwners(GameTestHelper helper) {
        var p=player(helper);var other=player(helper);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(NoahLegacy.GIFT));NoahLegacy.GIFT.use(p.level(),p,InteractionHand.MAIN_HAND);
            for(var item:NoahLegacy.relics()) {
                helper.assertTrue(p.getInventory().countItem(item)==1,"Gift delivers exactly one of each relic");
                var stack=ItemStack.EMPTY;
                // Component-aware inventories need a direct scan for the bonded stack.
                for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))stack=p.getInventory().getItem(i);
                helper.assertTrue(NoahLegacy.bound(stack,p)&&!NoahLegacy.bind(stack.copy(),other),"First user owns the copied relic too");
            }
        } finally {cleanup(p);cleanup(other);}helper.succeed();
    }
    @GameTest public void powersRespectCooldownAndNonHostileTargets(GameTestHelper helper) {
        var p=player(helper);
        try {
            p.setYRot(0);p.setXRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,relic(p,NoahLegacy.TEMPEST));
            var zombie=EntityTypes.ZOMBIE.create(p.level(),EntitySpawnReason.COMMAND);zombie.setNoAi(true);zombie.setPos(p.position().add(0,0,4));p.level().addFreshEntity(zombie);
            var villager=EntityTypes.VILLAGER.create(p.level(),EntitySpawnReason.COMMAND);villager.setNoAi(true);villager.setPos(p.position().add(2,0,4));p.level().addFreshEntity(villager);float health=villager.getHealth();
            helper.assertTrue(NoahLegacy.cast(p,false)&&zombie.getHealth()<zombie.getMaxHealth(),"Tidal Burst damages a visible hostile in the cone");
            helper.assertTrue(villager.getHealth()==health&&!NoahLegacy.cast(p,false),"Villagers are spared and immediate repeat is refused");
            p.setItemInHand(InteractionHand.MAIN_HAND,relic(p,NoahLegacy.TEMPEST));helper.assertTrue(!NoahLegacy.cast(p,false),"Changing tridents does not reset owner cooldown");
            helper.assertTrue(!NoahLegacy.cast(p,true),"Ultimate requires armor and companion");
        } finally {cleanup(p);}helper.succeed();
    }
    @GameTest public void conchRecallRestRecoveryAndCopyCannotDuplicate(GameTestHelper helper) {
        var p=player(helper);
        try {
            helper.assertTrue(NoahLegacy.summon(p,false),"Summon onto clear ground");var first=mount(p);var id=first.getUUID();
            ready(p);helper.assertTrue(NoahLegacy.summon(p,false),"Recall same companion");
            helper.assertTrue(mount(p).getUUID().equals(id)&&p.level().getEntitiesOfClass(Thalassar.class,p.getBoundingBox().inflate(15)).size()==1,"Recall preserves identity with one live mount");
            ready(p);helper.assertTrue(NoahLegacy.summon(p,true)&&mountCount(p)==0,"Conch can dismiss mount");
            ready(p);helper.assertTrue(NoahLegacy.summon(p,false)&&mount(p).getUUID().equals(id),"Restored companion retains identity");
            mount(p).die(p.damageSources().generic());ready(p);helper.assertTrue(!NoahLegacy.summon(p,false),"Defeat enforces recovery cooldown");
            NoahLedger.get(p.level()).entry(p.getUUID()).putLong("MountReady",0);ready(p);
            helper.assertTrue(NoahLegacy.summon(p,false)&&mount(p).getHealth()==mount(p).getMaxHealth(),"Recovery restores companion without a permanent death");
        } finally {cleanup(p);}helper.succeed();
    }
    private static int mountCount(ServerPlayer p){return p.level().getEntitiesOfClass(Thalassar.class,p.getBoundingBox().inflate(15)).size();}
    @GameTest public void thalassarCannotBeFoodTamedAndUntrackedDeathPreservesCompanion(GameTestHelper helper) {
        var p=player(helper);
        try {
            helper.assertTrue(NoahLegacy.summon(p,false),"Conch establishes the real companion");var original=mount(p);
            var stray=Mounts.THALASSAR.create(p.level(),EntitySpawnReason.COMMAND);stray.setPos(p.position().add(4,0,0));p.level().addFreshEntity(stray);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(stray.tamingFood(),3));stray.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO);
            helper.assertTrue(p.getMainHandItem().getCount()==3&&!stray.isTamed(),"Unbound Thalassar cannot enter ordinary food taming");
            stray.bond(p);stray.die(p.damageSources().generic());
            helper.assertTrue(stray.isRemoved(),"An untracked bonded duplicate must not remain as a dying entity");
            helper.assertTrue(!original.isRemoved()&&NoahLedger.get(p.level()).entry(p.getUUID()).getStringOr("Mount","").equals(original.getUUID().toString()),"Untracked death cannot replace the conch companion");
        } finally {cleanup(p);}helper.succeed();
    }
    @GameTest public void ordinaryRecallPreservesInjuryAndBlockedRecallPreservesMount(GameTestHelper helper) {
        var p=player(helper);
        try {
            helper.assertTrue(NoahLegacy.summon(p,false),"Mount fixture");var m=mount(p);m.setHealth(23);var id=m.getUUID();
            ready(p);helper.assertTrue(NoahLegacy.summon(p,true),"Rest injured companion");ready(p);
            helper.assertTrue(NoahLegacy.summon(p,false)&&mount(p).getHealth()==23,"Ordinary rest is not a free heal");
            m=mount(p);var at=p.blockPosition();
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-2;y<=4;y++)p.level().setBlock(at.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
            ready(p);helper.assertTrue(!NoahLegacy.summon(p,false)&&!m.isRemoved()&&m.getUUID().equals(id)&&m.getHealth()==23,"No safe destination leaves the original companion intact");
        } finally {cleanup(p);}helper.succeed();
    }
    @GameTest public void chargedTempestThrowKeepsBondAndLoyalty(GameTestHelper helper) {
        var p=player(helper);
        net.minecraft.world.entity.projectile.arrow.ThrownTrident thrown=null;
        try {
            var stack=relic(p,NoahLegacy.TEMPEST);p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setXRot(90);
            NoahLegacy.TEMPEST.use(p.level(),p,InteractionHand.MAIN_HAND);
            helper.assertTrue(NoahLegacy.TEMPEST.releaseUsing(stack,p.level(),p,NoahLegacy.TEMPEST.getUseDuration(stack,p)-20),"Native charged release succeeds");
            thrown=p.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.ThrownTrident.class,p.getBoundingBox().inflate(5)).stream().filter(t->t.getOwner()==p).findFirst().orElseThrow();
            var weapon=thrown.getWeaponItem();
            helper.assertTrue(p.getInventory().countItem(NoahLegacy.TEMPEST)==0&&weapon.is(NoahLegacy.TEMPEST)&&NoahLegacy.bound(weapon,p),"Throw transfers the bonded relic into its projectile");
            var loyalty=p.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOYALTY);
            helper.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(loyalty,weapon)==3,"Thrown relic retains Loyalty III");
            // Native Return: Strike the fixture floor, then let Loyalty bring the actual projectile back.
            for(int tick=0;tick<200&&!thrown.isRemoved();tick++) {
                thrown.tick();
                if(thrown.getBoundingBox().intersects(p.getBoundingBox().inflate(.2)))thrown.playerTouch(p);
            }
            helper.assertTrue(thrown.isRemoved()&&p.getInventory().countItem(NoahLegacy.TEMPEST)==1,"Loyalty returns exactly one Tempest to its owner");
        } finally {if(thrown!=null)thrown.discard();cleanup(p);}helper.succeed();
    }
    @GameTest public void ownerRidingAndSovereignUltimate(GameTestHelper helper) {
        var p=player(helper);var stranger=player(helper);
        try {
            armor(p);p.setItemInHand(InteractionHand.MAIN_HAND,relic(p,NoahLegacy.TEMPEST));helper.assertTrue(NoahLegacy.summon(p,false),"Summon fixture");var m=mount(p);
            helper.assertTrue(!stranger.startRiding(m)&&p.startRiding(m),"Only the bonded owner may ride");
            helper.assertTrue(NoahLegacy.dominion(p)&&NoahLegacy.cast(p,true)&&!NoahLegacy.cast(p,true),"Full loadout unlocks a cooldown-limited ultimate");
            helper.assertTrue(!MountCompanions.capture(p,m,new ItemStack(MountCompanions.CAPSULE)),"General capsules cannot create a second recovery path");
            ready(p);helper.assertTrue(!NoahLegacy.summon(p,true),"Conch cannot remove an occupied mount");
        } finally {cleanup(p);cleanup(stranger);}helper.succeed();
    }
    @GameTest public void armorProtectsFallDrowningAndSeaAggro(GameTestHelper helper) {
        var p=player(helper);
        try {
            armor(p);float hp=p.getHealth();
            p.hurtServer(p.level(),p.damageSources().fall(),10);p.hurtServer(p.level(),p.damageSources().drown(),10);
            helper.assertTrue(p.getHealth()==hp,"Bound regalia prevents fall and drowning damage");
            var drowned=EntityTypes.DROWNED.create(p.level(),EntitySpawnReason.COMMAND);drowned.setPos(p.position().add(3,0,0));p.level().addFreshEntity(drowned);drowned.setTarget(p);
            helper.assertTrue(drowned.getTarget()==null&&!drowned.canAttack(p),"Sea blessing suppresses native target acquisition");
            helper.assertTrue(p.canStandOnFluid(Blocks.WATER.defaultBlockState().getFluidState()),"Boots expose native water surface support");p.setShiftKeyDown(true);
            helper.assertTrue(!p.canStandOnFluid(Blocks.WATER.defaultBlockState().getFluidState()),"Sneaking permits deliberate diving");
            p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);drowned.setTarget(p);helper.assertTrue(drowned.getTarget()==p,"Removing crown releases sea protection");
        } finally {cleanup(p);}helper.succeed();
    }
    @GameTest public void ledgerSurvivesSerializationAndUnknownMountNeverRespawns(GameTestHelper helper) {
        var p=player(helper);
        try {
            var ledger=NoahLedger.get(p.level());var data=ledger.entry(p.getUUID());data.putLong("PowerReady",123456);data.putString("Mount",UUID.randomUUID().toString());
            var json=NoahLedger.CODEC.encodeStart(JsonOps.INSTANCE,ledger).getOrThrow();var restored=NoahLedger.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
            helper.assertTrue(restored.entry(p.getUUID()).getLongOr("PowerReady",0)==123456,"Owner cooldown survives saved-data serialization");
            helper.assertTrue(!NoahLegacy.summon(p,false)&&mountCount(p)==0,"Missing or unloaded active record never creates a duplicate");
        } finally {cleanup(p);}helper.succeed();
    }
    @GameTest public void bootsSupportWaterSurfaceAndAllowDiving(GameTestHelper helper) {
        var p=player(helper);
        try {
            p.setItemSlot(EquipmentSlot.FEET,relic(p,NoahLegacy.ARMOR.get(ArmorType.BOOTS)));var at=p.blockPosition();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)p.level().setBlock(at.offset(x,-1,z),Blocks.WATER.defaultBlockState(),2);
            double surface=p.getY();p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.5,0));
            helper.assertTrue(p.getY()>surface-.2,"Native fluid collision holds boots above water");
            p.setShiftKeyDown(true);p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.6,0));
            helper.assertTrue(p.getY()<surface-.5,"Sneaking releases the surface for diving");
        } finally {cleanup(p);}helper.succeed();
    }
    @GameTest public void thalassarSwimsVerticallyAndDashUsesPersistentCooldown(GameTestHelper helper) {
        var p=player(helper);
        try {
            helper.assertTrue(NoahLegacy.summon(p,false),"Mount fixture");var m=mount(p);m.setNoAi(true);var at=m.blockPosition();
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=0;y<5;y++)p.level().setBlock(at.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
            m.setPos(Vec3.atBottomCenterOf(at.above()));m.baseTick();helper.assertTrue(p.startRiding(m),"Owner boards aquatic mount");
            helper.assertTrue(m.acceptControls(p,PrototypeMount.FORWARD|PrototypeMount.ASCEND|PrototypeMount.ABILITY),"Server accepts owner control intent");m.tick();
            var before=m.position();for(int i=0;i<3;i++)m.travel(new Vec3(0,.6,1));
            helper.assertTrue(m.getY()>before.y&&m.getZ()>before.z&&m.canBreatheUnderwater(),"Steering moves forward and up underwater");
            long ready=NoahLedger.get(p.level()).entry(p.getUUID()).getLongOr("DashReady",0);
            helper.assertTrue(ready>NoahLedger.time(p.level()),"Dash consumes a persistent owner cooldown");m.tick();
            helper.assertTrue(NoahLedger.get(p.level()).entry(p.getUUID()).getLongOr("DashReady",0)==ready,"Held ability cannot restart dash every tick");
        } finally {cleanup(p);}helper.succeed();
    }
    @GameTest public void tempestImpalingExtendsToLandMobsAndAdvancementsExist(GameTestHelper helper) {
        var p=player(helper);
        try {
            var stack=relic(p,NoahLegacy.TEMPEST);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var arrow=new net.minecraft.world.entity.projectile.arrow.ThrownTrident(p.level(),p,stack.copy());
            var mob=EntityTypes.ZOMBIE.create(p.level(),EntitySpawnReason.COMMAND);mob.setNoAi(true);mob.setPos(p.position().add(0,0,5));p.level().addFreshEntity(mob);
            mob.hurtServer(p.level(),p.damageSources().source(net.minecraft.world.damagesource.DamageTypes.TRIDENT,arrow,p),8);
            helper.assertTrue(mob.getHealth()<5,"Bound Tempest applies its impaling enchantment to land mobs");arrow.discard();
            for(var id:java.util.List.of("chosen","crowned","storm","ride","heir","legacy"))helper.assertTrue(p.level().getServer().getAdvancements().get(darkspawn.black.Darkspawn.id("noah/"+id))!=null,"Named advancement loads: "+id);
        } finally {cleanup(p);}helper.succeed();
    }
}
