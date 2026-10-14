package darkspawn.black.mount;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class MountCareGameTest {
	private static ServerPlayer player(GameTestHelper helper) { return player(helper, UUID.randomUUID()); }
	private static ServerPlayer player(GameTestHelper helper, UUID id) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "mount-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			// Mirror the local rider's authority flag to catch mounts accidentally inheriting client ownership.
			@Override protected boolean isLocalClientAuthoritative() { return true; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return player;
	}
	private static void cleanup(ServerPlayer player) {
		player.stopRiding();
		player.level().getServer().getPlayerList().remove(player);
		player.discard();
	}

    private static PrototypeMount spawn(GameTestHelper helper, EntityType<? extends PrototypeMount> type) {
        // Test Isolation: Keep live companions outside habitat tests' 96-block population checks.
        var origin=helper.absolutePos(new BlockPos(3,3,3)).atY(260);
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) {
            helper.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<16;y++) helper.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var mount=type.create(helper.getLevel(),EntitySpawnReason.COMMAND);
        mount.setPos(Vec3.atBottomCenterOf(origin)); mount.setOnGround(true);
        helper.getLevel().addFreshEntity(mount); return mount;
    }

    private static void own(PrototypeMount mount, ServerPlayer owner) {
        mount.assignOwner(owner.getUUID()); owner.teleportTo(mount.getX()+3, mount.getY(), mount.getZ());
    }
    private static net.minecraft.nbt.CompoundTag save(PrototypeMount mount) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mount.level().registryAccess());
        mount.saveWithoutId(output); return output.buildResult();
    }
    private static void load(PrototypeMount mount, net.minecraft.nbt.CompoundTag tag) {
        mount.load(TagValueInput.create(ProblemReporter.DISCARDING, mount.level().registryAccess(), tag));
    }
    @GameTest(skyAccess=true)
    public void cargoAndTransfersEnforceBothPlayersConsent(GameTestHelper helper) {
        var owner=player(helper); var next=player(helper); var stranger=player(helper);
        var mount=spawn(helper,Mounts.LEOPARD);
        try {
            own(mount,owner); next.teleportTo(owner.getX(),owner.getY(),owner.getZ()); stranger.teleportTo(owner.getX(),owner.getY(),owner.getZ());
            mount.care.cargo.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
            var menu=new MountMenu(1,owner.getInventory(),mount);
            helper.assertTrue(!MountCompanions.cargoAllowed(new net.minecraft.world.item.ItemStack(MountCompanions.CAPSULE))
                && !MountCompanions.cargoAllowed(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHULKER_BOX))
                && !MountCompanions.cargoAllowed(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUNDLE)),"Nested containers rejected");
            helper.assertTrue(!mount.care.offer(next,owner) && !mount.care.accept(next),"Cannot steal or accept without offer");
            helper.assertTrue(mount.care.offer(owner,next) && mount.isOwnedBy(owner),"Offer alone preserves ownership");
            helper.assertTrue(!owner.startRiding(mount),"Pending transfer prevents riding");
            helper.assertTrue(!MountCompanions.capture(owner,mount,new net.minecraft.world.item.ItemStack(MountCompanions.CAPSULE)),"Pending transfer prevents storage");
            var recipientMenu=new MountMenu(2,next.getInventory(),mount);
            helper.assertTrue(!recipientMenu.slots.get(0).mayPickup(next) && !mount.care.accept(stranger),"Recipient cannot take cargo before accepting; stranger cannot accept");
            helper.assertTrue(mount.care.accept(next) && mount.isOwnedBy(next) && !menu.stillValid(owner),"Acceptance revokes old owner immediately");
            helper.assertTrue(!menu.slots.get(0).mayPickup(owner) && menu.quickMoveStack(owner,0).isEmpty(),"Stale owner menu cannot remove cargo");
            helper.assertTrue(mount.care.cargo.getItem(0).getCount()==7 && !mount.care.following,"Transfer preserves cargo and selects Stay");
            mount.care.setFollowing(true); load(mount,save(mount));
            helper.assertTrue(mount.isOwnedBy(next) && mount.care.following && mount.care.cargo.getItem(0).getCount()==7 && !mount.care.offerPending(),"Care and cargo persist without stale offers");
        } finally {mount.discard();cleanup(owner);cleanup(next);cleanup(stranger);}
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void capsulesPreserveIdentityAndRejectCopiesAndUnsafeRelease(GameTestHelper helper) {
        var owner=player(helper); var stranger=player(helper); var mount=spawn(helper,Mounts.LEOPARD);
        String ticket="";
        try {
            own(mount,owner); var pos=mount.blockPosition(); var uuid=mount.getUUID();
            mount.setCustomName(Component.literal("Fern")); mount.setHealth(12); mount.care.cargo.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,5));
            var capsule=new net.minecraft.world.item.ItemStack(MountCompanions.CAPSULE);
            helper.assertTrue(MountCompanions.capture(owner,mount,capsule) && mount.isRemoved(),"Capture removes original only after saving");
            ticket=MountCompanions.token(capsule); var copy=capsule.copy();
            var ledger=MountRegistry.get(helper.getLevel());
            var encoded=MountRegistry.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,ledger).getOrThrow();
            var decoded=MountRegistry.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,encoded).getOrThrow();
            helper.assertTrue(decoded.get(ticket).mountId().equals(uuid),"Registry survives codec round trip");
            helper.assertTrue(!MountCompanions.release(stranger,capsule,pos),"Wrong owner cannot redeem");
            helper.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            helper.assertTrue(!MountCompanions.release(owner,capsule,pos) && ledger.get(ticket)!=null,"Blocked release retains record");
            helper.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            helper.assertTrue(MountCompanions.release(owner,capsule,pos),"Safe release succeeds");
            var restored=(PrototypeMount)helper.getLevel().getEntity(uuid);
            helper.assertTrue(restored!=null && restored.isOwnedBy(owner) && restored.getHealth()==12 && restored.getName().getString().equals("Fern")
                && restored.care.cargo.getItem(0).getCount()==5,"Release preserves UUID, owner, name, health and cargo");
            helper.assertTrue(!MountCompanions.release(owner,copy,pos.offset(0,0,2)),"Copied ticket cannot create second mount");
            restored.discard();
            helper.assertTrue(!MountCompanions.release(owner,copy,pos),"Spent ticket remains invalid after original disappears");
        } finally {if(!ticket.isEmpty())MountRegistry.get(helper.getLevel()).remove(ticket);mount.discard();cleanup(owner);cleanup(stranger);}
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void recoveryRequiresKitAndKeepsCargo(GameTestHelper helper) {
        var owner=player(helper); var mount=spawn(helper,Mounts.WOOLLY_RHINO); String ticket="";
        try {
            own(mount,owner); var pos=mount.blockPosition(); var uuid=mount.getUUID();
            mount.care.cargo.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,3));
            mount.hurtServer(helper.getLevel(),mount.damageSources().generic(),1000);
            helper.assertTrue(mount.isRemoved(),"Lethal damage stores owned mount immediately");
            net.minecraft.world.item.ItemStack token=net.minecraft.world.item.ItemStack.EMPTY;
            for(int i=0;i<owner.getInventory().getContainerSize();i++) if(!MountCompanions.token(owner.getInventory().getItem(i)).isEmpty())token=owner.getInventory().getItem(i);
            ticket=MountCompanions.token(token);
            helper.assertTrue(!ticket.isEmpty() && MountRegistry.get(helper.getLevel()).get(ticket).recovery(),"Owner receives recovery token");
            helper.assertTrue(!MountCompanions.release(owner,token,pos),"Recovery requires veterinary kit");
            owner.getInventory().add(new net.minecraft.world.item.ItemStack(MountCompanions.VET_KIT));
            helper.assertTrue(MountCompanions.release(owner,token,pos),"Kit restores defeated companion");
            var restored=(PrototypeMount)helper.getLevel().getEntity(uuid);
            helper.assertTrue(restored!=null && restored.getHealth()==restored.getMaxHealth() && restored.care.cargo.getItem(0).getCount()==3,"Recovery restores health and cargo");
            boolean kit=false;for(int i=0;i<owner.getInventory().getContainerSize();i++)kit|=owner.getInventory().getItem(i).is(MountCompanions.VET_KIT);
            helper.assertTrue(!kit,"Exactly one kit consumed on successful recovery");restored.discard();
        } finally {if(!ticket.isEmpty())MountRegistry.get(helper.getLevel()).remove(ticket);mount.discard();cleanup(owner);}
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void breedingPersistsGrowthAndPreventsEarlyRiding(GameTestHelper helper) {
        var owner=player(helper); var a=spawn(helper,Mounts.LEOPARD); var b=Mounts.LEOPARD.create(helper.getLevel(),EntitySpawnReason.COMMAND);
        try {
            own(a,owner); b.setPos(a.position().add(0,0,2));b.assignOwner(owner.getUUID());helper.getLevel().addFreshEntity(b);
            owner.getInventory().add(new net.minecraft.world.item.ItemStack(a.tamingFood(),5));
            a.care.setFollowing(true);helper.assertTrue(!a.care.requestBreeding(owner),"Following parent cannot breed");a.care.setFollowing(false);
            a.setHealth(5);helper.assertTrue(!a.care.requestBreeding(owner),"Injured parent cannot breed");a.setHealth(a.getMaxHealth());
            helper.assertTrue(a.care.requestBreeding(owner) && b.care.requestBreeding(owner),"Healthy same-owner Stay parents accept food");
            a.care.tick(helper.getLevel());
            var babies=helper.getLevel().getEntitiesOfClass(PrototypeMount.class,a.getBoundingBox().inflate(5),m->m.isBaby()&&m.isOwnedBy(owner));
            helper.assertTrue(babies.size()==1,"Exactly one owned baby created");var baby=babies.getFirst();
            helper.assertTrue(!owner.startRiding(baby) && baby.getBbHeight()<a.getBbHeight(),"Baby is smaller and cannot be ridden");
            helper.assertTrue(!a.care.requestBreeding(owner) && !b.care.requestBreeding(owner),"Both parents have cooldown");
            var saved=save(baby);load(baby,saved);helper.assertTrue(baby.isBaby() && baby.isOwnedBy(owner),"Growth and ownership survive save");
            saved.putInt("MountGrowth",1);load(baby,saved);baby.care.tick(helper.getLevel());
            helper.assertTrue(!baby.isBaby() && owner.startRiding(baby),"Grown companion becomes rideable");owner.stopRiding();baby.discard();
        } finally {a.discard();b.discard();cleanup(owner);}
        helper.succeed();
    }
    @GameTest(skyAccess=true,maxTicks=200)
    public void followMovesGroundAndFlyingMountsThenStayStopsThem(GameTestHelper helper) {
        var owner=player(helper);var ground=spawn(helper,Mounts.LEOPARD);
        // Follow Fixture: Other companion tests share the high platform elevation; keep this live path isolated.
        ground.setPos(ground.position().add(0,0,2048));ground.setOnGround(true);
        var flyer=Mounts.GRIFFIN.create(helper.getLevel(),EntitySpawnReason.COMMAND);
        own(ground,owner);flyer.assignOwner(owner.getUUID());flyer.setPos(ground.position().add(0,3,2));helper.getLevel().addFreshEntity(flyer);
        for(int x=-5;x<=15;x++)for(int z=-5;z<=5;z++)helper.getLevel().setBlockAndUpdate(ground.blockPosition().offset(x,-1,z),Blocks.STONE.defaultBlockState());
        owner.teleportTo(ground.getX()+10,ground.getY(),ground.getZ());
        var start=ground.position();var air=flyer.position();var chunk=new net.minecraft.world.level.ChunkPos(ground.blockPosition().getX()>>4,ground.blockPosition().getZ()>>4);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)helper.getLevel().setChunkForced(chunk.x()+x,chunk.z()+z,true);
        helper.startSequence().thenWaitUntil(()->{
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)helper.assertTrue(helper.getLevel().isPositionTickingWithEntitiesLoaded(new net.minecraft.world.level.ChunkPos(chunk.x()+x,chunk.z()+z).pack()),"Follow fixture chunks are entity-ready");
        }).thenExecute(()->{ground.care.setFollowing(true);flyer.care.setFollowing(true);}).thenExecuteAfter(35,()->{
            helper.assertTrue(ground.getX()>start.x+1,"Ground companion follows using navigation; x="+ground.getX()+" start="+start.x);
            helper.assertTrue(flyer.getX()>air.x+1,"Flying companion follows owner");
            // Stay Fixture: Neither the settling flyer nor its nearby owner may physically push the mount.
            flyer.discard();ground.care.setFollowing(false);owner.teleportTo(ground.getX()+20,ground.getY(),ground.getZ());var stopped=ground.position();
            helper.runAfterDelay(10,()->{try {
                helper.assertTrue(ground.position().distanceToSqr(stopped)<.1,"Stay stops ground movement; before="+stopped+" after="+ground.position());helper.succeed();
            }finally{ground.discard();flyer.discard();cleanup(owner);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)helper.getLevel().setChunkForced(chunk.x()+x,chunk.z()+z,false);}});
        });
    }

    @GameTest(skyAccess=true)
    public void everySpeciesSupportsConsentAndPersistentBabies(GameTestHelper helper) {
        var owner=player(helper);var next=player(helper);
        var types=List.of(Mounts.GRIFFIN,Mounts.WOOLLY_RHINO,Mounts.FLYING_SQUIRREL,Mounts.POPTART_CAT,Mounts.DRAGON_MOUNT,
            Mounts.WINGED_UNICORN,Mounts.RIDING_GOLEM,Mounts.SASQUATCH,Mounts.CERBERUS,Mounts.LEOPARD,Mounts.SEAHORSE,
            Mounts.GIANT_SCORPION,Mounts.CROCODILE,Mounts.GIANT_MOLE);
        try {for(var type:types) {
            var mount=spawn(helper,type);
            try {
                own(mount,owner);next.teleportTo(owner.getX(),owner.getY(),owner.getZ());
                helper.assertTrue(mount.care.offer(owner,next) && mount.care.accept(next),"All species transfer only on acceptance: "+type);
                mount.care.makeBaby();load(mount,save(mount));
                helper.assertTrue(mount.isBaby() && mount.isOwnedBy(next) && !next.startRiding(mount),"All species persist babies and prevent riding: "+type);
                helper.assertTrue(mount.care.offer(next,owner),"New owner can offer baby");
                load(mount,save(mount));helper.assertTrue(!mount.care.accept(owner),"Reload clears pending transfer: "+type);
            }finally{mount.discard();}
        }} finally{cleanup(owner);cleanup(next);}
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void aquaticCapsulesRequireWaterAndRespectOneBlockBoundary(GameTestHelper helper) {
        var owner=player(helper);var mount=spawn(helper,Mounts.SEAHORSE);String ticket="";
        try {
            own(mount,owner);var pos=mount.blockPosition();var uuid=mount.getUUID();var capsule=new net.minecraft.world.item.ItemStack(MountCompanions.CAPSULE);
            helper.assertTrue(MountCompanions.capture(owner,mount,capsule),"Seahorse stores normally");ticket=MountCompanions.token(capsule);
            helper.assertTrue(!MountCompanions.release(owner,capsule,pos),"Seahorse cannot be released on land");
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<4;y++)helper.getLevel().setBlockAndUpdate(pos.offset(x,y,z),Blocks.WATER.defaultBlockState());
            var registry=MountRegistry.get(helper.getLevel());var saved=registry.get(ticket);
            var foreign=registry.store(new MountRegistry.Entry(saved.owner(),saved.mountId(),saved.type(),saved.entity(),false,true));
            var copy=capsule.copy();var tag=new net.minecraft.nbt.CompoundTag();tag.putString("MountToken",foreign);
            copy.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            helper.assertTrue(!MountCompanions.release(owner,copy,pos),"OneBlock snapshot cannot export to Overworld");registry.remove(foreign);
            helper.assertTrue(MountCompanions.release(owner,capsule,pos),"Fully submerged safe placement succeeds");
            var restored=(PrototypeMount)helper.getLevel().getEntity(uuid);helper.assertTrue(restored!=null,"Same aquatic identity restored");restored.discard();
        }finally{if(!ticket.isEmpty())MountRegistry.get(helper.getLevel()).remove(ticket);mount.discard();cleanup(owner);}
        helper.succeed();
    }
    @GameTest(skyAccess=true,maxTicks=50)
    public void aquaticFollowUsesWaterAndStopsForOwnerOnLand(GameTestHelper helper) {
        var owner=player(helper);var mount=spawn(helper,Mounts.SEAHORSE);own(mount,owner);
        var origin=mount.blockPosition();
        for(int x=-2;x<=12;x++)for(int z=-2;z<=2;z++)for(int y=0;y<5;y++)helper.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.WATER.defaultBlockState());
        owner.teleportTo(mount.getX()+9,mount.getY()+1,mount.getZ());owner.doTick();var start=mount.position();mount.care.setFollowing(true);
        helper.runAfterDelay(20,()->{
            helper.assertTrue(mount.getX()>start.x+.5,"Aquatic follower swims toward submerged owner; mount="+mount.position()+" start="+start+" owner="+owner.position()+" wet="+owner.isInWater()+" input="+mount.care.followInput());
            owner.teleportTo(mount.getX()+8,mount.getY()+8,mount.getZ());owner.doTick();
            helper.runAfterDelay(2,()->{try{helper.assertTrue(mount.care.followInput().equals(Vec3.ZERO),"Seahorse does not chase owner onto land");helper.succeed();}
                finally{mount.discard();cleanup(owner);}});
        });
    }

    @GameTest(skyAccess=true)
    public void offlineDefeatReturnsRecoveryTokenOnJoin(GameTestHelper helper) {
        var mount=spawn(helper,Mounts.CERBERUS);var ownerId=UUID.randomUUID();mount.assignOwner(ownerId);
        var registry=MountRegistry.get(helper.getLevel());mount.hurtServer(helper.getLevel(),mount.damageSources().generic(),1000);
        var entry=registry.entries().entrySet().stream().filter(e->e.getValue().owner().equals(ownerId)).findFirst().orElseThrow();
        var owner=player(helper,ownerId);
        try {
            boolean delivered=false;
            for(int i=0;i<owner.getInventory().getContainerSize();i++)if(MountCompanions.token(owner.getInventory().getItem(i)).equals(entry.getKey())) {
                delivered=true;owner.getInventory().setItem(i,net.minecraft.world.item.ItemStack.EMPTY);
            }
            helper.assertTrue(delivered,"Offline owner's saved recovery is delivered on join");
            MountCompanions.reclaim(owner);MountCompanions.reclaim(owner);
            int tickets=0;for(int i=0;i<owner.getInventory().getContainerSize();i++)if(MountCompanions.token(owner.getInventory().getItem(i)).equals(entry.getKey()))tickets++;
            helper.assertTrue(tickets==1 && registry.get(entry.getKey())!=null,"Lost ticket reissues once while carried, retaining one saved mount");
        }finally{registry.remove(entry.getKey());mount.discard();cleanup(owner);}
        helper.succeed();
    }
}
