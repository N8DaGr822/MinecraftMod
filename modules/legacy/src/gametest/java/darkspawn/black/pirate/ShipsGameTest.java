package darkspawn.black.pirate;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.Darkspawn;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
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

public final class ShipsGameTest {
    private static PirateShip boardingPatrol(GameTestHelper h,ServerPlayer p,PirateShip attacker,BlockPos at) {
        var patrol=PirateShip.spawnEnemy(p.level(),at.offset(15,0,0));
        for(int i=0;i<8;i++)h.assertTrue(patrol.cannonDamage(attacker,Ships.SHOT),"Damage patrol with captain's ship");
        h.assertTrue(patrol.boardableBy(p),"Forty-hull patrol is boardable by its attacking captain");return patrol;
    }
    private static java.util.List<BoardingDefender> sentries(PirateShip ship) {
        return ship.level().getEntitiesOfClass(BoardingDefender.class,ship.getBoundingBox().inflate(10));
    }
    @GameTest public void boardingSoloRewardsOriginalCargoOnce(GameTestHelper h) {
        var at=location(h);var p=player(h,at.above());var ship=build(h,p,at);var patrol=boardingPatrol(h,p,ship,at);
        try {
            var treasure=new ItemStack(Items.DIAMOND,7);treasure.set(DataComponents.CUSTOM_NAME,Component.literal("Patrol cargo"));
            patrol.cargo.clearContent();patrol.cargo.setItem(0,treasure);int credit=Pirate.state(p).navalVictories;
            h.assertTrue(Boarding.form(p)==1&&Boarding.start(p)==1,"Solo party starts");var defenders=sentries(patrol);
            h.assertTrue(defenders.size()==3&&patrol.boardingSpent,"Exactly three finite solo sentries");
            h.assertTrue(!patrol.cannonDamage(ship,Ships.SHOT)&&patrol.fire(true)==0,"Reserved patrol cannot fire or be sunk by stray cannonballs");
            for(var defender:defenders)h.assertTrue(defender.hurtServer(p.level(),p.damageSources().playerAttack(p),1000),"Participant can defeat sentry");
            var loot=p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,patrol.getBoundingBox().inflate(12));
            h.assertTrue(loot.size()==1&&loot.getFirst().getItem().getCount()==7&&ItemStack.isSameItemSameComponents(loot.getFirst().getItem(),treasure),"Only exact original cargo drops, with its name intact");
            h.assertTrue(patrol.cargo.isEmpty()&&Pirate.state(p).navalVictories==credit+1,"One naval credit and empty hold");
            patrol.releaseBoardingCargo(p);h.assertTrue(loot.size()==p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,patrol.getBoundingBox().inflate(12)).size()&&Pirate.state(p).navalVictories==credit+1,"Repeated completion cannot duplicate reward");
            h.assertTrue(Boarding.form(p)==0,"Spent wreck cannot start another encounter");loot.forEach(net.minecraft.world.entity.Entity::discard);
        } finally {Boarding.leave(p);clear(patrol);Boarding.tick(p.level());clear(ship);remove(p);}h.succeed();
    }
    @GameTest public void boardingCooperativeOptInAndDamageIsolation(GameTestHelper h) {
        var at=location(h);var p=player(h,at.above());var friend=player(h,at.above());var outsider=player(h,at.above());var ship=build(h,p,at);var patrol=boardingPatrol(h,p,ship,at);
        try {
            h.assertTrue(Boarding.form(outsider)==0,"Non-attacking captain cannot reserve patrol");
            h.assertTrue(Boarding.form(p)==1&&Boarding.join(friend,p)==1,"Explicit cooperative join");
            h.assertTrue(Boarding.start(friend)==0&&Boarding.start(p)==1,"Only leader starts");var defenders=sentries(patrol);
            h.assertTrue(defenders.size()==4,"Cooperative encounter has four finite defenders");var defender=defenders.getFirst();
            h.assertTrue(!defender.hurtServer(p.level(),outsider.damageSources().playerAttack(outsider),1000)&&!defender.hurtServer(p.level(),defender.damageSources().generic(),1000),"Outsiders and environment cannot kill sentries");
            h.assertTrue(Boarding.canTarget(defender,p)&&Boarding.canTarget(defender,friend)&&!Boarding.canTarget(defender,outsider),"Sentries target opted-in players only");
            float health=outsider.getHealth();outsider.hurtServer(p.level(),defender.damageSources().mobAttack(defender),5);
            h.assertTrue(outsider.getHealth()==health,"Actual damage event protects nonparticipants");
            h.assertTrue(Boarding.join(outsider,p)==0,"No joining an active fight");
            Boarding.leave(friend);h.assertTrue(patrol.isRemoved()&&defenders.stream().allMatch(net.minecraft.world.entity.Entity::isRemoved),"Departure cancels party and discards defenders and cargo");
        } finally {Boarding.leave(p);clear(patrol);clear(ship);remove(p);remove(friend);remove(outsider);}h.succeed();
    }
    @GameTest public void boardingReloadDiscardsAttemptAndOrphanDefenders(GameTestHelper h) {
        var at=location(h);var p=player(h,at.above());var ship=build(h,p,at);var patrol=boardingPatrol(h,p,ship,at);PirateShip restored=null;
        try {
            h.assertTrue(Boarding.form(p)==1&&Boarding.start(p)==1,"Start attempt before save");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());patrol.saveWithoutId(saved);
            restored=new PirateShip(Ships.SHIP,p.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
            Boarding.leave(p);h.assertTrue(restored.boardingSpent,"Attempt survives save/load");restored.tick();h.assertTrue(restored.isRemoved(),"Reload without live party discards cargo without credit");
            var orphan=new BoardingDefender(Ships.BOARDING_DEFENDER,p.level());orphan.setPos(patrol.local(0,1,3));p.level().addFreshEntity(orphan);
            h.assertTrue(orphan.isRemoved(),"Orphan sentry is removed on entity load");
        } finally {Boarding.leave(p);clear(restored);clear(patrol);clear(ship);remove(p);}h.succeed();
    }
    @GameTest public void boardingLobbyCancelAndDisconnectHaveNoRewards(GameTestHelper h) {
        var at=location(h);var p=player(h,at.above());var friend=player(h,at.above());var ship=build(h,p,at);var patrol=boardingPatrol(h,p,ship,at);
        try {
            h.assertTrue(Boarding.form(p)==1&&Boarding.join(friend,p)==1,"Open lobby");Boarding.leave(friend);Boarding.leave(p);
            h.assertTrue(!patrol.boardingSpent&&patrol.isAlive()&&!Boarding.reserved(patrol),"Unstarted lobby does not consume patrol");
            h.assertTrue(Boarding.form(p)==1&&Boarding.join(friend,p)==1&&Boarding.start(p)==1,"Start a new lobby");
            p.level().getServer().getPlayerList().remove(friend);Boarding.tick(p.level());
            h.assertTrue(patrol.isRemoved()&&sentries(patrol).isEmpty()&&Pirate.state(p).navalVictories==0,"Disconnect cleans encounter without reward");
        } finally {Boarding.leave(p);clear(patrol);clear(ship);remove(p);remove(friend);}h.succeed();
    }
    @GameTest public void boardingArrowsRespectParticipationAndCleanup(GameTestHelper h) {
        var at=location(h);var p=player(h,at.above());var outsider=player(h,at.above());var ship=build(h,p,at);var patrol=boardingPatrol(h,p,ship,at);
        try {
            h.assertTrue(Boarding.form(p)==1&&Boarding.start(p)==1,"Start ranged combat fixture");var defender=sentries(patrol).getFirst();
            defender.performRangedAttack(p,.8F);
            var arrows=p.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,patrol.getBoundingBox().inflate(15));
            h.assertTrue(arrows.size()==1,"Native sentry bow creates a real arrow");var arrow=arrows.getFirst();
            h.assertTrue(arrow.pickup==net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED,"Sentry arrows cannot be farmed");
            float health=outsider.getHealth();outsider.hurtServer(p.level(),p.damageSources().arrow(arrow,defender),5);
            h.assertTrue(outsider.getHealth()==health,"Actual projectile damage cannot hurt outsiders");
            health=p.getHealth();p.hurtServer(p.level(),p.damageSources().arrow(arrow,defender),5);
            h.assertTrue(p.getHealth()<health,"Actual projectile damage reaches participants");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());arrow.saveWithoutId(saved);
            Boarding.leave(p);h.assertTrue(arrow.isRemoved(),"Outstanding arrows are removed on cancellation");
            var restored=new net.minecraft.world.entity.projectile.arrow.Arrow(net.minecraft.world.entity.EntityTypes.ARROW,p.level());
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));p.level().addFreshEntity(restored);
            h.assertTrue(restored.isRemoved(),"Reloaded orphan arrow is discarded even without its owner");
        } finally {Boarding.leave(p);clear(patrol);clear(ship);remove(p);remove(outsider);}h.succeed();
    }
    @GameTest public void boardingPartyCapAndBlockedDeckDoNotSpendAttempt(GameTestHelper h) {
        var at=location(h);var p=player(h,at.above());var friends=new java.util.ArrayList<ServerPlayer>();var ship=build(h,p,at);var patrol=boardingPatrol(h,p,ship,at);
        var obstruction=BlockPos.containing(patrol.local(-1,1.05,4));
        try {
            for(int i=0;i<4;i++)friends.add(player(h,at.above()));h.assertTrue(Boarding.form(p)==1,"Open lobby");
            for(int i=0;i<3;i++)h.assertTrue(Boarding.join(friends.get(i),p)==1,"Join within four-player cap");
            h.assertTrue(Boarding.join(friends.get(3),p)==0,"Fifth player refused");
            p.level().setBlock(obstruction,Blocks.STONE.defaultBlockState(),2);
            h.assertTrue(Boarding.start(p)==0&&!patrol.boardingSpent&&sentries(patrol).isEmpty(),"Blocked entry rejects atomically without consuming attempt or spawning sentries");
            p.level().setBlock(obstruction,Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(Boarding.start(p)==1,"Clear entry allows same party to start");friends.getFirst().setPos(patrol.position().add(60,1,0));Boarding.tick(p.level());
            h.assertTrue(patrol.isRemoved(),"Out-of-bounds participant ends fight");
        } finally {p.level().setBlock(obstruction,Blocks.AIR.defaultBlockState(),2);Boarding.leave(p);clear(patrol);clear(ship);remove(p);friends.forEach(ShipsGameTest::remove);}h.succeed();
    }
    @GameTest public void pilotStatusTracksAmmoFallbackReloadAndHarbor(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);
        try {
            p.level().setBlock(at.offset(-3,1,-3),Ships.CANNON.defaultBlockState(),2);
            helper.assertTrue(ship.launch(p)==null,"Launch gun fixture");
            helper.assertTrue(ship.pilotStatus().combat().startsWith("No ammo"),"Empty hold does not claim ready");
            ship.cargo.setItem(0,new ItemStack(Ships.SHOT,3));ship.cargo.setItem(1,new ItemStack(Ships.FIRE_SHOT,1));
            helper.assertTrue(ship.pilotStatus().combat().equals("Ready | Auto: fire 1"),"Automatic mode reports fire-first selection");
            helper.assertTrue(ship.fire(true)==1,"Fire the real broadside");
            helper.assertTrue(ship.pilotStatus().combat().equals("Reload 3s | Auto: solid 3"),"Reload and fallback follow actual consumed ammunition");
            ship.cycleAmmunition(p);ship.cycleAmmunition(p);
            helper.assertTrue(ship.pilotStatus().combat().endsWith("Chain 0"),"Selected ammo does not imply unavailable fallback");
            ship.setPos(.5,64,.5);helper.assertTrue(ship.pilotStatus().combat().startsWith("Harbor safe"),"Protected harbor overrides combat readiness");
        } finally {clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void deckSupportReacquiresReloadedShipWithoutStaleCarry(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);PirateShip restored=null;
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");p.stopRiding();p.setPos(ship.local(0,1,4));p.setOnGround(true);p.baseTick();
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());ship.saveWithoutId(saved);
            restored=new PirateShip(Ships.SHIP,p.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
            ship.discard();helper.assertTrue(p.level().addFreshEntity(restored),"Recreate saved ship after original entity removal");
            var before=p.position();p.baseTick();helper.assertTrue(p.position().equals(before),"Stale removed ship never carries the player");
            restored.setPos(restored.position().add(.2,0,0));p.baseTick();
            helper.assertTrue(Math.abs(p.getX()-before.x-.2)<.001,"Standing player reacquires replacement deck support");
            helper.assertTrue(p.startRiding(restored),"Owner can board the restored ship");
            helper.assertTrue(restored.controls(p,PirateShip.FORWARD),"Restored ship accepts fresh owner input");
        } finally {clear(restored);clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void sailingClearsUnusedHeightButDockingRemainsStrict(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);var bridge=at.offset(0,5,3);
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");p.level().setBlock(bridge,Blocks.STONE.defaultBlockState(),2);
            helper.assertTrue(ship.controls(p,PirateShip.FORWARD),"Captain requests sailing");var before=ship.position();ship.tick();
            helper.assertTrue(ship.getZ()>before.z,"Bare vessel sails beneath unused build height");
            ship.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(ship.dock(p)!=null&&!ship.docked(),"Docking still reserves the full restoration volume");
            helper.assertTrue(p.level().getBlockState(bridge).is(Blocks.STONE),"Bridge is preserved");
            p.level().setBlock(at.offset(0,2,2),Blocks.STONE.defaultBlockState(),2);
            helper.assertTrue(!ShipCollision.clearSailing(ship,ship.position(),0),"Low bridge cannot clip the captain even when it clears the hull");
        } finally {p.level().setBlock(bridge,Blocks.AIR.defaultBlockState(),2);p.level().setBlock(at.offset(0,2,2),Blocks.AIR.defaultBlockState(),2);clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void sailingClearsEmptyRotatedCornersButRejectsHullObstacles(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);var corner=at.offset(7,0,7);var wall=at.offset(0,0,8);
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");ship.setYRot(45);
            p.level().setBlock(corner,Blocks.STONE.defaultBlockState(),2);p.level().setBlock(corner.below(),Blocks.STONE.defaultBlockState(),2);
            helper.assertTrue(ShipCollision.clearSailing(ship,ship.position(),45),"Empty diagonal corner may contain shore, including dry ground");
            ship.setYRot(0);p.level().setBlock(wall,Blocks.STONE.defaultBlockState(),2);
            helper.assertTrue(ship.controls(p,PirateShip.FORWARD),"Captain requests forward movement");var before=ship.position();ship.tick();
            helper.assertTrue(ship.position().equals(before)&&ship.getDeltaMovement().equals(Vec3.ZERO),"Actual bow obstacle stops sailing");
            p.level().setBlock(wall,Blocks.AIR.defaultBlockState(),2);p.level().setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);
            helper.assertTrue(!ShipCollision.clearSailing(ship,ship.position(),0),"Actual hull still requires water beneath it");
        } finally {p.level().setBlock(corner,Blocks.AIR.defaultBlockState(),2);p.level().setBlock(corner.below(),Blocks.WATER.defaultBlockState(),2);p.level().setBlock(wall,Blocks.AIR.defaultBlockState(),2);p.level().setBlock(at.below(),Blocks.WATER.defaultBlockState(),2);clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void sailingRejectsCapturedMastCollision(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);var obstacle=at.offset(0,4,8);
        try {
            for(int y=1;y<=4;y++)p.level().setBlock(at.offset(0,y,7),Blocks.OAK_LOG.defaultBlockState(),2);
            helper.assertTrue(ship.launch(p)==null,"Launch mast fixture");p.level().setBlock(obstacle,Blocks.STONE.defaultBlockState(),2);
            helper.assertTrue(!ShipCollision.clearSailing(ship,ship.position().add(0,0,.1),0),"Captured mast cannot pass through overhead obstruction");
        } finally {p.level().setBlock(obstacle,Blocks.AIR.defaultBlockState(),2);clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void sailingRejectsOtherShipsAndUnloadedDestinations(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);PirateShip other=null;
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");other=PirateShip.spawnEnemy(p.level(),at.offset(24,0,0));
            helper.assertTrue(other!=null,"Other moving hull fixture");
            helper.assertTrue(!ShipCollision.clearSailing(ship,other.position(),45),"Rotated vessel cannot overlap another captured hull");
            helper.assertTrue(!ShipCollision.clearSailing(ship,ship.position().add(1000000,0,1000000),0),"Sailing cannot enter unloaded chunks");
            helper.assertTrue(!ShipCollision.clearSailing(ship,new Vec3(31000000,64,0),0),"Sailing cannot cross the world border");
        } finally {clear(other);clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void deckSupportReleasesOnJumpAndProtectsSneakingEdges(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");p.stopRiding();p.setPos(ship.local(0,1,4));p.setOnGround(true);p.baseTick();
            p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,.42,0));var airborne=p.position();
            ship.setPos(ship.position().add(.2,0,0));p.baseTick();
            helper.assertTrue(p.position().distanceTo(airborne)<.001,"Jump releases carry instead of dragging airborne player");
            p.setPos(ship.local(3.6,1,4));p.setOnGround(true);p.setShiftKeyDown(true);
            for(int i=0;i<10;i++)p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(.2,-.08,0));
            helper.assertTrue(p.getX()-ship.getX()<3.8&&Math.abs(p.getY()-ship.getY()-1)<.001,"Crouching still prevents walking off the outer edge");
        } finally {clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void rotatingDockPreservesWalkerAndRejectsSwimmer(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);ServerPlayer swimmer=null;
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");p.stopRiding();ship.setYRot(40);p.setPos(ship.local(0,1,4));
            swimmer=player(helper,at.offset(2,0,0));
            helper.assertTrue(ship.dock(p)!=null&&!ship.docked(),"Swimmer still prevents block restoration");remove(swimmer);swimmer=null;
            helper.assertTrue(ship.dock(p)==null,"Deck walker can dock through heading snap");
            helper.assertTrue(p.position().distanceTo(ship.local(0,1,4))<.001,"Walker retains local deck position after rotation");
        } finally {clear(ship);if(swimmer!=null)remove(swimmer);remove(p);}helper.succeed();
    }
    @GameTest public void largeDeckMenuRemainsValidAndClosesAfterLeaving(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);
        try {
            Pirate.state(p).navalVictories=5;p.getInventory().clearContent();for(int i=0;i<3;i++)p.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
            helper.assertTrue(ship.upgrade(p)==null&&ship.launch(p)==null,"Launch expanded vessel");ship.tick();p.stopRiding();p.setPos(ship.local(0,1,10));
            var menu=new ShipMenu(1,p.getInventory(),null,ship);
            helper.assertTrue(menu.stillValid(p),"Owned deck supports menus beyond eight blocks from center");
            p.setPos(ship.local(0,1,15));helper.assertTrue(!menu.stillValid(p),"Leaving the deck closes distant menus");
        } finally {clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void sustainedDeckTransport(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");p.stopRiding();p.setPos(ship.local(0,1,4));p.setOnGround(true);p.baseTick();
            var initial=p.position().subtract(ship.position());
            for(int i=0;i<20;i++) {ship.setPos(ship.position().add(.2,0,0));p.baseTick();p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.08,0));}
            double drift=p.position().subtract(ship.position()).subtract(initial).length();
            helper.assertTrue(drift<.01,"Standing rider should retain local position over 20 steps; drift="+drift);
            p.setShiftKeyDown(true);
            for(int i=0;i<20;i++) {ship.setYRot(ship.getYRot()+1);p.baseTick();p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.08,0));}
            helper.assertTrue(p.position().distanceTo(ship.local(0,1,4))<.01,"Crouching rider follows sustained turning without drifting");
        } finally {clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void crouchingCanWalkOnDeck(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");p.stopRiding();p.setPos(ship.local(0,1,4));p.setOnGround(true);p.setShiftKeyDown(true);
            double before=p.getX();p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(.2,-.08,0));
            helper.assertTrue(p.getX()-before>.15,"Crouching on the center of a solid moving deck should permit walking; moved="+(p.getX()-before));
        } finally {clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void walkingCaptainCanDock(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");p.stopRiding();p.setPos(ship.local(0,1,2));ship.setDeltaMovement(Vec3.ZERO);
            String error=ship.dock(p);helper.assertTrue(error==null,"A captain on the stopped deck should not be classified as an obstructing swimmer: "+error);
        } finally {clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void lateBoardingChecksSeatClearance(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);ServerPlayer guest=null;
        try {
            p.level().setBlock(at.offset(2,2,-5),Blocks.OAK_PLANKS.defaultBlockState(),2);
            helper.assertTrue(ship.launch(p)==null,"Unoccupied future seat is currently accepted at launch");
            guest=player(helper,at.offset(3,1,-5));guest.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            guest.setPose(net.minecraft.world.entity.Pose.SWIMMING);
            ship.interact(guest,net.minecraft.world.InteractionHand.MAIN_HAND,Vec3.ZERO);
            helper.assertTrue(guest.getVehicle()!=ship,"Swimming cannot bypass standing headroom at the reserved passenger seat");
        } finally {clear(ship);if(guest!=null)remove(guest);remove(p);}helper.succeed();
    }
    @GameTest public void capturedLadderRemainsClimbable(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);
        try {
            p.level().setBlock(at.offset(1,1,4),Blocks.OAK_PLANKS.defaultBlockState(),2);
            p.level().setBlock(at.offset(0,1,4),Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING,net.minecraft.core.Direction.WEST),2);
            helper.assertTrue(ship.launch(p)==null,"Ladder is an allowed captured block");p.stopRiding();p.setPos(ship.local(0,1,4));
            helper.assertTrue(p.onClimbable(),"Allowed ship ladder should retain climbing behavior while sailing");
            ship.setYRot(90);ship.tick();p.setPos(ship.local(0,1,4));helper.assertTrue(p.onClimbable(),"Ladder lookup follows ship rotation");
            p.setPos(ship.local(-2,1,4));helper.assertTrue(!p.onClimbable(),"Nearby empty deck is not climbable");
        } finally {clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void cannonMissesEmptySpaceAboveDeck(GameTestHelper helper) {
        var at=location(helper);var p=player(helper,at.above());var ship=build(helper,p,at);PirateShip attacker=null;Cannonball ball=null;
        try {
            helper.assertTrue(ship.launch(p)==null,"Launch fixture");attacker=PirateShip.spawnEnemy(p.level(),at.offset(32,0,0));
            float before=ship.hull();ball=new Cannonball(Ships.BALL,p.level());ball.setOwner(attacker);ball.setItem(new ItemStack(Ships.SHOT));
            ball.setPos(ship.getX(),ship.getY()+5,ship.getZ()-20);ball.shoot(0,0,1,1F,0);p.level().addFreshEntity(ball);
            for(int i=0;i<25&&!ball.isRemoved();i++)ball.tick();
            helper.assertTrue(ship.hull()==before,"Shot through empty air above a bare deck should miss; hull="+ship.hull()+" before="+before);
        } finally {if(ball!=null)ball.discard();clear(attacker);clear(ship);remove(p);}helper.succeed();
    }

	private static ServerPlayer player(GameTestHelper helper, BlockPos pos) {
		var level = helper.getLevel().getServer().getLevel(Pirate.LEVEL);
		for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk((pos.getX()>>4)+x,(pos.getZ()>>4)+z);
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "pirate-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(level, new Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5), Vec3.ZERO, 0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING)); ready(player); return player;
	}
	private static void ready(ServerPlayer player) {
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket()); player.hasChangedDimension(); player.setOnGround(true);
		Pirate.state(player).lastTravel = Long.MIN_VALUE;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	private static BlockPos location(GameTestHelper helper){return new BlockPos(2000+Math.floorMod(helper.absolutePos(BlockPos.ZERO).getX(),10000)*128,64,2000+Math.floorMod(helper.absolutePos(BlockPos.ZERO).getZ(),10000)*128);}
	private static void supplies(ServerPlayer player){player.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,64));player.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,64));player.getInventory().setItem(2,new ItemStack(Ships.HELM_ITEM));}
	private static PirateShip build(GameTestHelper helper,ServerPlayer player,BlockPos at){supplies(player);var ship=PirateShip.build(player,at);helper.assertTrue(ship!=null,"Paid vessel builds in clear water");return ship;}
	private static void clear(PirateShip ship){
		if(ship==null)return;
		var level=ship.level();var origin=ship.blockPosition();
		if(ship.docked())for(int x=-ship.width()/2-1;x<=ship.width()/2+1;x++)for(int z=-ship.length()/2-1;z<=ship.length()/2+1;z++)for(int y=0;y<=ship.height();y++){
			var pos=origin.offset(x,y,z);if(PirateShip.permitted(level.getBlockState(pos)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
		}
		for(var passenger:java.util.List.copyOf(ship.getPassengers())){passenger.stopRiding();if(!(passenger instanceof net.minecraft.world.entity.player.Player))passenger.discard();}ship.discard();
	}
    @GameTest public void movingDeckSupportsWalkingAndDockedBuildIsNotPickable(GameTestHelper helper) {
        var pos=location(helper);var p=player(helper,pos.above());var ship=build(helper,p,pos);
        try {
            helper.assertTrue(!ship.isPickable(),"Docked entity cannot intercept platform block targeting");
            helper.assertTrue(ship.launch(p)==null,"Launch for collision regression");
            p.stopRiding();ship.tick();
            for(float yaw:new float[]{0,45,90,180,270}) {
                ship.setYRot(yaw);ship.tick();
                var top=ship.local(2,1,4);p.setPos(top.add(0,1,0));
                p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-2,0));
                helper.assertTrue(Math.abs(p.getY()-top.y)<.001,"Moving deck catches falling player at yaw "+yaw+" actual="+p.getY()+" expected="+top.y+" box="+ship.getBoundingBox());
            }
            ship.setYRot(0);ship.tick();p.setPos(ship.local(2,1,4));p.setOnGround(true);p.baseTick();
            var before=p.position();ship.setPos(ship.position().add(.2,0,0));p.baseTick();
            helper.assertTrue(Math.abs(p.getX()-before.x-.2)<.001,"Grounded player follows translating deck");
            p.setPos(ship.local(20,2,0));p.setOnGround(false);p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-1,0));
            helper.assertTrue(Math.abs(p.getY()-ship.getY()-1)<.001,"Open water outside ship does not acquire a phantom floor");
        } finally { clear(ship);remove(p); }
        helper.succeed();
    }

    @GameTest public void advancedRefitsAndCrewCarePreserveSavesAndPayment(GameTestHelper helper) {
        var pos=location(helper);var p=player(helper,pos.above());var ship=build(helper,p,pos);
        try {
            var ledger=SeaLedger.get(p.level());ledger.fort(new SeaLedger.Fort(pos,p.getUUID(),"Test port",0,0));ledger.homePort(new SeaLedger.HomePort(p.getUUID(),pos,1));
            Pirate.state(p).navalVictories=5;p.getInventory().clearContent();for(int i=0;i<3;i++)p.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
            helper.assertTrue(ship.upgrade(p)==null,"Expanded platform prerequisite");p.getInventory().setItem(3,new ItemStack(Items.IRON_INGOT,64));p.getInventory().setItem(4,new ItemStack(Items.EMERALD,64));
            ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,3));helper.assertTrue(ship.refitClass(p,1)&&ship.maxHull()==340&&ship.classSpeed()==1.25,"Clipper trades hull for speed");
            helper.assertTrue(ship.refitClass(p,2)&&ship.maxHull()==500&&ship.cargo.getItem(0).getCount()==3,"Ironclad preserves hold and custom platform");
            helper.assertTrue(ship.recruit(p,PirateShip.WORKER),"Owned upgraded port can recruit crew");int before=p.getInventory().countItem(Items.EMERALD);
            helper.assertTrue(ship.supportCrew(p,true)&&!ship.supportCrew(p,true)&&p.getInventory().countItem(Items.EMERALD)==before-1&&ship.morale()==90,"Wages charge exactly once during coverage");
            p.getInventory().setItem(5,new ItemStack(Items.BREAD,2));helper.assertTrue(ship.supportCrew(p,false)&&ship.morale()==100&&ship.crewEfficiency()==1.15,"Provisions provide capped efficiency bonus");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());ship.saveWithoutId(saved);var restored=new PirateShip(Ships.SHIP,p.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
            helper.assertTrue(restored.shipClass()==2&&restored.morale()==100&&restored.maxHull()==500,"Class and care state survive load");restored.discard();
        } finally {clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void specialAmmunitionPreservesCostsAndDamageRules(GameTestHelper helper){
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var enemy=PirateShip.spawnEnemy(captain.level(),pos.east(40));
        try{
            captain.level().setBlockAndUpdate(pos.offset(3,1,3),Ships.CANNON.defaultBlockState());helper.assertTrue(ship.launch(captain)==null,"Launch fixture");
            ship.cargo.setItem(0,new ItemStack(Ships.SHOT,8));ship.cycleAmmunition(captain);ship.cycleAmmunition(captain);
            helper.assertTrue(ship.fire(false)==0&&ship.cargo.getItem(0).getCount()==8,"Explicit chain selection never consumes solid shot");ship.cargo.setItem(1,new ItemStack(Ships.CHAIN_SHOT,2));
            helper.assertTrue(ship.fire(false)==1&&ship.cargo.getItem(1).getCount()==1,"One shot consumes one chain round");
            float hull=enemy.hull();helper.assertTrue(enemy.cannonDamage(ship,Ships.CHAIN_SHOT)&&enemy.hull()==hull-6,"Chain has reduced hull damage");
            helper.assertTrue(enemy.cannonDamage(ship,Ships.PIERCING_SHOT)&&enemy.hull()==hull-34,"Piercing damage adds 28");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());enemy.saveWithoutId(output);
            var input=TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult());helper.assertTrue(input.getIntOr("RiggingDamage",0)==200,"Ten-second rigging penalty is saved");
            enemy.setPos(.5,64,60.5);hull=enemy.hull();helper.assertTrue(!enemy.cannonDamage(ship,Ships.PIERCING_SHOT)&&enemy.hull()==hull,"New ammunition honors harbor protection");
        }finally{enemy.discard();clear(ship);remove(captain);}helper.succeed();
    }
    @GameTest public void factionClassesKeepSizeAndRequireEarnedRank(GameTestHelper helper) {
        var pos=location(helper);var p=player(helper,pos.above());var ship=build(helper,p,pos);
        try {
            var ledger=SeaLedger.get(p.level());ledger.fort(new SeaLedger.Fort(pos,p.getUUID(),"Test port",0,0));ledger.homePort(new SeaLedger.HomePort(p.getUUID(),pos,1));
            Pirate.state(p).navalVictories=5;p.getInventory().clearContent();for(int i=0;i<3;i++)p.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
            helper.assertTrue(ship.upgrade(p)==null,"Expanded prerequisite");
            p.getInventory().setItem(3,new ItemStack(Items.IRON_INGOT,64));p.getInventory().setItem(4,new ItemStack(Items.EMERALD,64));
            helper.assertTrue(!ship.refitClass(p,3)&&p.getInventory().countItem(Items.IRON_INGOT)==64,"Unqualified refit never charges");
            for(int i=0;i<3;i++){
                var state=Pirate.state(p);var faction=PirateProgression.Faction.values()[i];state.factionMembership=faction.id;state.factionRanks[i]=3;state.factionStanding[i]=60;
                p.getInventory().setItem(3,new ItemStack(Items.IRON_INGOT,64));
                helper.assertTrue(ship.refitClass(p,i+3)&&ship.width()==11&&ship.gunLimit()==8,"Faction classes preserve size and crew limits");
            }
            helper.assertTrue(ship.cycleAmmunition(p)&&ship.cycleAmmunition(p)&&ship.ammunition()==2,"Explicit chain shot selection");
            helper.assertTrue(ship.toggleExpedition(p)&&ship.crewExpedition(),"Expedition starts docked");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());ship.saveWithoutId(saved);var restored=new PirateShip(Ships.SHIP,p.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
            helper.assertTrue(restored.shipClass()==5&&restored.ammunition()==2&&restored.crewExpedition(),"New class, ammunition and expedition survive reload");restored.discard();
        }finally{clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void dockedRoamingRecallsOriginalCrewBeforeLaunch(GameTestHelper helper) {
        var pos=location(helper);var p=player(helper,pos.above());var ship=build(helper,p,pos);
        try {
            var ledger=SeaLedger.get(p.level());ledger.fort(new SeaLedger.Fort(pos,p.getUUID(),"Test port",0,0));ledger.homePort(new SeaLedger.HomePort(p.getUUID(),pos,1));p.getInventory().setItem(0,new ItemStack(Items.EMERALD,32));
            helper.assertTrue(ship.recruit(p,PirateShip.WORKER),"One sailor joins");var sailor=ship.getPassengers().getFirst();var id=sailor.getUUID();
            helper.assertTrue(ship.toggleCrewRoaming(p),"Captain enables docked walking");ship.tickCount=20;ship.tick();
            helper.assertTrue(!sailor.isPassenger()&&ship.crew(PirateShip.WORKER)==1,"Roaming sailor remains counted in original crew");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());ship.saveWithoutId(saved);var restored=new PirateShip(Ships.SHIP,p.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
            helper.assertTrue(restored.crewRoaming(),"Roaming preference survives reload");restored.discard();
            var launchError=ship.launch(p);helper.assertTrue(launchError==null&&sailor.getVehicle()==ship&&ship.crewMembers().getFirst().getUUID().equals(id),"Launch recalls original crew: "+launchError+" | removed="+sailor.isRemoved()+" vehicle="+sailor.getVehicle());
        } finally {for(var sailor:ship.crewMembers())sailor.discard();clear(ship);remove(p);}helper.succeed();
    }
    @GameTest public void cargoPredictionUsesSyncedCapacityWithoutShipReference(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());
        captain.level().setBlockAndUpdate(pos.above(),Ships.SHIPYARD.defaultBlockState());
        try {
            // Mirror the client menu's null ship, while a valid station permits exercising transfer logic here.
            var menu=new ShipMenu(1,captain.getInventory(),pos.above(),null);
            menu.setData(0,1);menu.setData(15,36);menu.setData(16,1);
            var cargo=new ItemStack(Items.GOLD_INGOT,7);cargo.set(DataComponents.CUSTOM_NAME,Component.literal("Voyage cargo"));
            captain.getInventory().setItem(0,cargo);
            int source=-1;for(int i=54;i<menu.slots.size();i++)if(menu.slots.get(i).getContainerSlot()==0)source=i;
            helper.assertTrue(source>=0&&!menu.quickMoveStack(captain,source).isEmpty(),"Shift-click works with synced capacity and no ship reference");
            helper.assertTrue(menu.slots.get(27).getItem().getCount()==7&&menu.slots.get(27).getItem().get(DataComponents.CUSTOM_NAME).getString().equals("Voyage cargo")&&captain.getInventory().getItem(0).isEmpty(),"Predicted transfer preserves count and components");
            helper.assertTrue(!menu.slots.get(0).isActive()&&!menu.slots.get(36).isActive(),"Hidden and locked slots remain inaccessible");
        } finally {captain.level().setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());remove(captain);}helper.succeed();
    }
    @GameTest public void dismountImmediatelyClearsQueuedCannonInput(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
        try {
            captain.level().setBlockAndUpdate(pos.offset(-3,1,-3),Ships.CANNON.defaultBlockState());
            ship.cargo.setItem(0,new ItemStack(Ships.SHOT,3));
            helper.assertTrue(ship.launch(captain)==null&&ship.controls(captain,PirateShip.FORWARD|PirateShip.FIRE_LEFT),"Captain queues movement and cannon input");
            captain.stopRiding();ship.commonTick();ship.tick();
            helper.assertTrue(ship.cargo.getItem(0).getCount()==3&&ship.getDeltaMovement().horizontalDistanceSqr()==0,"Dismount cancels queued firing and thrust immediately");
        } finally {clear(ship);remove(captain);}helper.succeed();
    }
	@GameTest public void constructionAndRejectedLaunchAreAtomic(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());PirateShip ship=null;
		try{
			helper.assertTrue(PirateShip.build(captain,pos)==null&&captain.level().isEmptyBlock(pos),"Missing payment does not place a platform");
			ship=build(helper,captain,pos);helper.assertTrue(captain.getInventory().getItem(0).isEmpty()&&captain.getInventory().getItem(1).getCount()==23&&captain.getInventory().getItem(2).isEmpty(),"Exact plank and helm cost consumed once");
			captain.level().setBlockAndUpdate(pos.offset(1,1,3),Blocks.CHEST.defaultBlockState());ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,3));
			helper.assertTrue(ship.launch(captain)!=null&&ship.docked()&&captain.level().getBlockState(pos).is(Blocks.OAK_PLANKS)&&ship.cargo.getItem(0).getCount()==3,"Unsupported container rejects whole launch without losing blocks or cargo");
			captain.level().setBlockAndUpdate(pos.offset(1,1,3),Blocks.AIR.defaultBlockState());
		}finally{clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void launchDockAndBlockedDockPreserveBuildAndCargo(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
		try{
			captain.level().setBlockAndUpdate(pos.offset(-3,1,-3),Ships.CANNON.defaultBlockState());
			ship.cargo.setItem(0,new ItemStack(Items.EMERALD,11));
			String error=ship.launch(captain);helper.assertTrue(error==null,"Launch succeeds: "+error);
			helper.assertTrue(!ship.docked()&&captain.getVehicle()==ship&&captain.level().isEmptyBlock(pos)&&ship.guns()==1,"Blocks become one owned moving ship with a seated captain");
			var obstruction=pos.offset(3,2,3);captain.level().setBlockAndUpdate(obstruction,Blocks.STONE.defaultBlockState());
			helper.assertTrue(ship.dock(captain)!=null&&!ship.docked()&&captain.level().isEmptyBlock(pos),"Blocked docking restores nothing");captain.level().setBlockAndUpdate(obstruction,Blocks.AIR.defaultBlockState());
			helper.assertTrue(ship.dock(captain)==null&&ship.docked()&&captain.getVehicle()==null,"Docking restores editable world blocks and dismounts captain");
			helper.assertTrue(captain.level().getBlockState(pos.offset(-3,1,-3)).is(Ships.CANNON)&&ship.cargo.getItem(0).getCount()==11,"Custom gun and cargo survive conversion");
		}finally{clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void largerPlatformRequiresProgressionAndExactExpansionCost(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
		try{
			for(int i=0;i<3;i++)captain.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
			helper.assertTrue(ship.upgrade(captain)!=null&&ship.tier()==0&&captain.getInventory().getItem(0).getCount()==64,"Rank gate does not consume resources");
			Pirate.state(captain).navalVictories=5;helper.assertTrue(ship.upgrade(captain)==null&&ship.width()==11&&ship.length()==23&&ship.height()==8&&ship.gunLimit()==8,"Five victories unlock larger bounds");
			helper.assertTrue(captain.getInventory().getItem(0).isEmpty()&&captain.getInventory().getItem(1).isEmpty()&&captain.getInventory().getItem(2).getCount()==44,"Expansion consumes exactly 148 extra platform blocks");
			helper.assertTrue(captain.level().getBlockState(pos.offset(5,0,11)).is(Blocks.OAK_PLANKS),"Expanded corners actually exist");
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());Pirate.state(captain).write(output);
			var restored=new PiratePlayerState();restored.read(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));helper.assertTrue(restored.navalVictories==5,"Rank persists in player save");
		}finally{clear(ship);remove(captain);}helper.succeed();
	}
    @GameTest public void thirdShipTierPreservesCostBoundsCaptureAndSave(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var restored=new PirateShip(Ships.SHIP,captain.level());
        try {
            var state=Pirate.state(captain);state.harborContract=2;state.islandContracts=new int[]{2,2,2};
            for(int i=0;i<8;i++)state.logboundVisited.add(new BlockPos(i*256,65,0));
            captain.getInventory().clearContent();for(int i=0;i<3;i++)captain.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
            helper.assertTrue(ship.upgrade(captain)==null&&ship.tier()==1,"Independent milestones unlock first expansion");
            helper.assertTrue(ship.upgrade(captain)!=null&&Ships.pay(captain,Items.OAK_PLANKS,44,false),"Second expansion rejects insufficient funds without consuming remainder");
            captain.getInventory().clearContent();for(int i=0;i<4;i++)captain.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
            captain.level().setBlockAndUpdate(pos.offset(7,0,15),Blocks.STONE.defaultBlockState());
            helper.assertTrue(ship.upgrade(captain)!=null&&Ships.pay(captain,Items.OAK_PLANKS,256,false),"Blocked outer corner rejects atomically");captain.level().setBlockAndUpdate(pos.offset(7,0,15),Blocks.AIR.defaultBlockState());
            captain.level().setBlockAndUpdate(pos.above(9),Blocks.STONE.defaultBlockState());
            helper.assertTrue(ship.upgrade(captain)!=null,"New height above existing deck must also be clear");captain.level().setBlockAndUpdate(pos.above(9),Blocks.AIR.defaultBlockState());
            helper.assertTrue(ship.upgrade(captain)==null&&ship.tier()==2&&ship.width()==15&&ship.length()==31&&ship.height()==10&&ship.blockLimit()==1536&&ship.gunLimit()==12,"Third tier has real expanded bounds");
            helper.assertTrue(Ships.pay(captain,Items.OAK_PLANKS,44,false)&&!Ships.pay(captain,Items.OAK_PLANKS,45,false)&&ship.upgrade(captain)!=null,"Third tier costs exactly 212 planks and cannot expand again");
            int added=0;
            outer:for(int y=3;y<=10;y++)for(int x=-7;x<=7;x++)for(int z=-15;z<=15;z++){
                captain.level().setBlockAndUpdate(pos.offset(x,y,z),Blocks.OAK_PLANKS.defaultBlockState());if(++added==1070)break outer;
            }
            helper.assertTrue(ship.launch(captain)==null&&ship.blocks().size()==1536,"Maximum permitted build captures without losing blocks");
            var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(ship.getId(),ship.getEntityData().packDirty());
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),captain.registryAccess());
            try {
                net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);
                var decoded=net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.packedItems().equals(packet.packedItems()),"Maximum build fits and round-trips the actual native entity sync packet");
            } finally {buffer.release();}
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));
            helper.assertTrue(restored.tier()==2&&restored.blocks().equals(ship.blocks())&&restored.ownedBy(captain),"Native save retains full third-tier layout and ownership");
            helper.assertTrue(ship.dock(captain)==null&&captain.level().getBlockState(pos.offset(7,0,15)).is(Blocks.OAK_PLANKS),"Largest captured ship restores its outer deck");
        } finally {restored.discard();clear(ship);remove(captain);}helper.succeed();
    }
    @GameTest public void factionCrewOffersChargeActualPricesAndRespectRoleLimits(GameTestHelper helper) {
        var pos=new BlockPos(-48,64,40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
        var originalDifficulty=captain.level().getDifficulty();
        try {
            captain.level().getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
            var state=Pirate.state(captain);state.factionContracts=new int[]{2,2,2};state.factionStanding=new int[]{25,25,25};
            captain.getInventory().clearContent();captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,32));
            for(int role=0;role<4;role++)helper.assertTrue(ship.recruit(captain,role),"All roles retain recruitment access");
            helper.assertTrue(captain.getInventory().getItem(0).isEmpty(),"Offers charge 8 + 8 + 4 + normal 12 for lookout");
            helper.assertTrue(ship.getPassengers().stream().anyMatch(e->e.getName().getString().equals("Fleet Cannoneer")),"Earned crew has its faction title");
            helper.assertTrue(ship.getPassengers().stream().anyMatch(e->e.getType()==net.minecraft.world.entity.EntityTypes.PIGLIN)&&ship.getPassengers().stream().anyMatch(e->e.getType()==net.minecraft.world.entity.EntityTypes.PILLAGER)&&ship.getPassengers().stream().anyMatch(e->e.getType()==net.minecraft.world.entity.EntityTypes.HUSK),"Faction offers recruit actual Piglin, Illager and undead species");
            helper.assertTrue(PirateDiplomacy.membership(captain,PirateProgression.Faction.FLEET,9)&&PirateDiplomacy.membership(captain,PirateProgression.Faction.FLEET,10)&&ship.crew(PirateShip.GUNNER)==1,"Resigning keeps hired crew despite lost standing");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(saved);
            var restored=(PirateShip)net.minecraft.world.entity.EntityType.loadEntityRecursive(Ships.SHIP,saved.buildResult(),captain.level(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
            helper.assertTrue(restored!=null&&restored.crew(PirateShip.MATE)==1&&restored.crew(PirateShip.WORKER)==1&&restored.crew(PirateShip.GUNNER)==1,"Native recursive loading restores distinct-species role passengers");
            var oldDifficulty=captain.level().getDifficulty();
            try {
                captain.level().getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
                for(var entity:restored.getPassengers()) {
                    var mob=(net.minecraft.world.entity.Mob)entity;
                    helper.assertTrue(mob.entityTags().contains(PirateShip.CREW_OWNER+captain.getUUID())&&!mob.canAttack(captain),"Saved crew retain captain loyalty and cannot attack their owner");
                    mob.checkDespawn();helper.assertTrue(!mob.isRemoved(),"Recruited hostile species survive Peaceful difficulty");
                }
            } finally {captain.level().getServer().setDifficulty(oldDifficulty,true);for(var entity:java.util.List.copyOf(restored.getPassengers()))entity.discard();restored.discard();}
            captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,64));helper.assertTrue(!ship.recruit(captain,PirateShip.MATE)&&captain.getInventory().getItem(0).getCount()==64,"Unique role cap still rejects without charging");
        } finally {captain.level().getServer().setDifficulty(originalDifficulty,true);clear(ship);remove(captain);}helper.succeed();
    }
	@GameTest public void ownershipControlsAndCannonAmmunitionAreAuthoritative(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var stranger=player(helper,pos.offset(12,1,0));var ship=build(helper,captain,pos);
		try{
			helper.assertTrue(ship.launch(stranger)!=null&&ship.docked(),"Non-owner cannot launch");captain.level().setBlockAndUpdate(pos.offset(-3,1,3),Ships.CANNON.defaultBlockState());
			helper.assertTrue(ship.launch(captain)==null,"Owner launches");
			helper.assertTrue(!ship.controls(stranger,PirateShip.FORWARD)&&!ship.controls(captain,1024)&&ship.controls(captain,PirateShip.FORWARD),"Only captain may submit valid button bits");
			helper.assertTrue(ship.fire(true)==0,"No free cannon ammunition");ship.cargo.setItem(0,new ItemStack(Ships.SHOT,3));
			helper.assertTrue(ship.fire(true)==1&&ship.cargo.getItem(0).getCount()==2,"One manual gun consumes one round");
			helper.assertTrue(ship.fire(true)==0&&ship.cargo.getItem(0).getCount()==2,"Reload prevents repeated packet firing");
			var before=ship.position();ship.tick();helper.assertTrue(ship.position().distanceToSqr(before)>0,"Server control moves ship across water");
		}finally{clear(ship);remove(captain);remove(stranger);}helper.succeed();
	}
	@GameTest public void paidCrewPerformRepairsAndPersistAsPassengers(GameTestHelper helper){
		var pos=new BlockPos(40,64,40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
		try{
			captain.getInventory().setItem(3,new ItemStack(Items.EMERALD,64));
			for(int role=0;role<4;role++)helper.assertTrue(ship.recruit(captain,role),"All four crew roles can be recruited at harbor");
			helper.assertTrue(captain.getInventory().getItem(3).getCount()==20&&!ship.recruit(captain,PirateShip.MATE),"Crew costs exactly 44 emeralds and unique roles cannot duplicate");
			var legacy=ship.getPassengers().getFirst();legacy.removeTag(PirateShip.CREW_TAG);legacy.removeTag(PirateShip.CREW_OWNER+captain.getUUID());ship.tick();
            helper.assertTrue(legacy.entityTags().contains(PirateShip.CREW_OWNER+captain.getUUID())&&ship.crew(PirateShip.MATE)==1,"Legacy villagers migrate to saved captain loyalty without losing their role");
            helper.assertTrue(ship.launch(captain)==null,"Crew launch seated with captain");ship.setPos(4000.5,64,4000.5);
			var enemy=PirateShip.spawnEnemy(captain.level(),new BlockPos(4040,64,4000));ship.cannonDamage(enemy,true);enemy.discard();
			ship.cargo.setItem(0,new ItemStack(Items.OAK_PLANKS,2));float hurt=ship.hull();ship.tickCount=79;ship.commonTick();ship.tick();
			helper.assertTrue(ship.hull()>hurt&&ship.cargo.getItem(0).getCount()==1,"Shipworker consumes stocked plank and repairs net hull damage");
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);
			helper.assertTrue(output.buildResult().toString().contains("naval_role_3")&&output.buildResult().toString().contains("naval_role_0"),"Native passenger save contains named role crew");
		}finally{clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void shipSaveRestoresBuildCargoOwnershipAndCooldown(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var restored=new PirateShip(Ships.SHIP,captain.level());
		try{
			captain.level().setBlockAndUpdate(pos.offset(3,1,3),Ships.CANNON.defaultBlockState());ship.cargo.setItem(0,new ItemStack(Ships.SHOT,8));helper.assertTrue(ship.launch(captain)==null,"Launch fixture");ship.fire(false);
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));
			helper.assertTrue(restored.ownedBy(captain)&&!restored.docked()&&restored.blocks().equals(ship.blocks())&&restored.cargo.getItem(0).getCount()==7,"Native save restores exact build, cargo and owner");
			helper.assertTrue(restored.fire(false)==0,"Reload survives save/load");
		}finally{restored.discard();clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void navalCombatProtectsHarborAndAwardsNpcVictoriesOnce(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var enemy=PirateShip.spawnEnemy(captain.level(),pos.east(40));
		try{
			float hull=ship.hull();ship.setPos(.5,64,60.5);helper.assertTrue(!ship.cannonDamage(enemy,false)&&ship.hull()==hull,"Harbor vessels reject cannon damage");ship.setPos(pos.getX()+.5,64,pos.getZ()+.5);
			for(int i=0;i<10;i++)enemy.cannonDamage(ship,false);
			helper.assertTrue(enemy.isRemoved()&&Pirate.state(captain).navalVictories==1,"NPC sinking grants one victory");enemy.cannonDamage(ship,false);
			helper.assertTrue(Pirate.state(captain).navalVictories==1,"Repeated hits cannot duplicate rank rewards");
		}finally{enemy.discard();clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void playerShipDamageRespectsPvpAndDoesNotAwardRank(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var other=player(helper,pos.east(40).above());var ship=build(helper,captain,pos);var victim=build(helper,other,pos.east(40));
		var rules=captain.level().getGameRules();boolean original=rules.get(net.minecraft.world.level.gamerules.GameRules.PVP);
		try{
			rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,false,captain.level().getServer());helper.assertTrue(!victim.cannonDamage(ship,false)&&victim.hull()==200,"PVP-disabled worlds reject player-ship damage");
			rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,true,captain.level().getServer());helper.assertTrue(victim.cannonDamage(ship,false)&&victim.hull()==180,"PVP-enabled worlds permit enemy player vessels to take cannon damage");
			for(int i=0;i<9;i++)victim.cannonDamage(ship,false);
			helper.assertTrue(victim.isRemoved()&&Pirate.state(captain).navalVictories==0,"Player ship kills cannot farm rank");
		}finally{rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,original,captain.level().getServer());clear(ship);clear(victim);remove(captain);remove(other);}helper.succeed();
	}
	@GameTest(maxTicks=60) public void actualCannonballHitsShipWithoutBreakingTerrain(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var other=player(helper,pos.west(40).above());var ship=build(helper,captain,pos);var victim=build(helper,other,pos.west(40));
		captain.level().setBlockAndUpdate(pos.offset(-3,1,0),Ships.CANNON.defaultBlockState());ship.cargo.setItem(0,new ItemStack(Ships.SHOT,2));
		String error=ship.launch(captain);helper.assertTrue(error==null,"Combat launch: "+error);for(int x=-4;x<=2;x++)for(int z=-2;z<=2;z++)captain.level().setChunkForced((pos.getX()>>4)+x,(pos.getZ()>>4)+z,true);
		helper.runAfterDelay(5,()->helper.assertTrue(ship.fire(true)==1,"Cannonball leaves barrel"));
		helper.runAfterDelay(27,()->{
			try{
				helper.assertTrue(victim.hull()<200,"Real projectile collision damages target hull; PvP="+captain.level().isPvpAllowed()+" shipTicks="+ship.tickCount+" balls="+captain.level().getEntitiesOfClass(Cannonball.class,ship.getBoundingBox().inflate(100)).stream().map(ball->ball.tickCount+":"+ball.position()).toList());
				helper.assertTrue(captain.level().getBlockState(pos.west(40)).is(Blocks.OAK_PLANKS),"Impact does not explode terrain or ship blocks");
				helper.succeed();
			}finally{for(int x=-4;x<=2;x++)for(int z=-2;z<=2;z++)captain.level().setChunkForced((pos.getX()>>4)+x,(pos.getZ()>>4)+z,false);clear(ship);clear(victim);remove(captain);remove(other);}
		});
	}
    @GameTest public void patrolsRespectStandingRetaliateAndPersist(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var hostile=player(helper,pos.offset(0,1,32));var ship=build(helper,captain,pos);var other=build(helper,hostile,pos.offset(0,0,32));PirateShip patrol=null;
        try {
            helper.assertTrue(ship.launch(captain)==null&&other.launch(hostile)==null,"Both captains sail independently");
            Pirate.state(hostile).factionStanding[1]=-25;
            patrol=PirateShip.spawnPatrol(captain.level(),pos.offset(24,0,16),PirateProgression.Faction.FLEET);
            helper.assertTrue(patrol!=null&&patrol.getName().getString().contains("Iron Fleet")&&patrol.blocks().stream().anyMatch(b->net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.state().getBlock()).getPath().equals("blue_wool")),"Fleet patrol has recognizable name and blue sail");
            helper.assertTrue(!patrol.hostileTo(ship)&&patrol.hostileTo(other),"Patrol distinguishes neutral and hostile captains");float hull=ship.hull();
            helper.assertTrue(!ship.cannonDamage(patrol,false)&&ship.hull()==hull&&other.cannonDamage(patrol,false),"Projectile damage also respects personal standing");
            Pirate.state(captain).factionStanding[1]=50;
            helper.assertTrue(patrol.cannonDamage(ship,false)&&Pirate.state(captain).factionStanding[1]==40&&patrol.hostileTo(ship),"Attacking a friendly patrol costs reputation and provokes retaliation despite positive standing");
            patrol.cannonDamage(ship,false);helper.assertTrue(Pirate.state(captain).factionStanding[1]==40,"One broadside cannot apply a reputation penalty for each cannon");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());patrol.saveWithoutId(out);var restored=new PirateShip(Ships.SHIP,captain.level());
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),out.buildResult()));helper.assertTrue(restored.patrolFaction()==PirateProgression.Faction.FLEET&&restored.hostileTo(ship),"Faction and retaliation survive reload");restored.discard();
            var tag=out.buildResult();tag.putLong("ProvokedUntil",0);restored=new PirateShip(Ships.SHIP,captain.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),tag));helper.assertTrue(!restored.hostileTo(ship),"Expired provocation returns to standing rules");restored.discard();
            other.setPos(0,64,70);helper.assertTrue(!patrol.hostileTo(other)&&!other.cannonDamage(patrol,false),"Harbor protects even hostile captains");
            tag.putLong("PatrolExpiresAt",1);restored=new PirateShip(Ships.SHIP,captain.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),tag));restored.tick();helper.assertTrue(restored.isRemoved(),"Expired patrol is cleaned up after reload without reward");
            helper.assertTrue(PirateShip.spawnPatrol(captain.level(),new BlockPos(0,64,70),PirateProgression.Faction.CORSAIRS)==null,"Patrols cannot spawn in harbor");
        } finally {clear(patrol);clear(ship);clear(other);remove(captain);remove(hostile);}helper.succeed();
    }
    @GameTest public void outlawHuntOnlyCountsEligibleSinksAndPaysOnce(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);PirateShip outlaw=null,patrol=null;
        try {
            helper.assertTrue(ship.launch(captain)==null,"Captain sails for hunt");Pirate.state(captain).repeatContracts[1]=1;
            patrol=PirateShip.spawnPatrol(captain.level(),pos.offset(24,0,16),PirateProgression.Faction.CORSAIRS);helper.assertTrue(patrol!=null,"Faction patrol fixture spawns");
            for(int i=0;i<10;i++)patrol.cannonDamage(ship,false);
            helper.assertTrue(patrol.isRemoved()&&Pirate.state(captain).repeatContracts[1]==1,"Faction patrol sink does not fulfill outlaw hunt");
            outlaw=PirateShip.spawnEnemy(captain.level(),pos.offset(24,0,-16));for(int i=0;i<10;i++)outlaw.cannonDamage(ship,false);
            helper.assertTrue(outlaw.isRemoved()&&Pirate.state(captain).repeatContracts[1]==2,"Confirmed outlaw sink fulfills active hunt");int wins=Pirate.state(captain).navalVictories;
            helper.assertTrue(!outlaw.cannonDamage(ship,false)&&Pirate.state(captain).navalVictories==wins,"Duplicate sink cannot award victory twice");
            captain.stopRiding();captain.teleportTo(2,65,4);captain.getInventory().clearContent();helper.assertTrue(PirateActivities.contract(captain,1,5)&&!PirateActivities.contract(captain,1,5)&&Pirate.state(captain).factionStanding[1]==10,"Hunt pays matching standing once at harbor");
        } finally {clear(patrol);clear(outlaw);clear(ship);remove(captain);}helper.succeed();
    }
    @GameTest public void garrickRecruitmentRespectsBerthsAndNativePassengerPersistence(GameTestHelper helper) {
        var pos=new BlockPos(48,64,-40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
        try {
            var state=Pirate.state(captain);state.garrickRescue=3;captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,64));
            for(int i=0;i<4;i++)helper.assertTrue(ship.recruit(captain,PirateShip.GUNNER),"Ordinary gunners fill available berths");
            helper.assertTrue(!ship.recruitGarrick(captain)&&state.garrickRescue==3,"Full crew preserves officer reward");ship.getPassengers().getFirst().discard();
            int emeralds=captain.getInventory().getItem(0).getCount();
            helper.assertTrue(ship.recruitGarrick(captain)&&state.garrickRescue==5&&ship.crew(PirateShip.GUNNER)==4&&!ship.recruitGarrick(captain)&&captain.getInventory().getItem(0).getCount()==emeralds,"Garrick boards once without an emerald charge");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(saved);
            var restored=(PirateShip)net.minecraft.world.entity.EntityType.loadEntityRecursive(Ships.SHIP,saved.buildResult(),captain.level(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
            helper.assertTrue(restored!=null&&restored.crew(PirateShip.GUNNER)==4&&restored.getPassengers().stream().anyMatch(e->e.entityTags().contains(PirateActivities.GARRICK)&&e.getName().getString().equals("Garrick the Deserter")&&e.entityTags().contains(PirateShip.CREW_OWNER+captain.getUUID())),"Named officer retains role and captain identity through native recursive load");
            for(var sailor:java.util.List.copyOf(restored.getPassengers()))sailor.discard();restored.discard();
        } finally {clear(ship);remove(captain);}helper.succeed();
    }

    @GameTest public void patrolSpawningRespectsLoadedBudgetAndRules(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var level=captain.level();
        var watcher=player(helper,pos.offset(320,1,0));
        var spawned=new java.util.ArrayList<PirateShip>();var rule=net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS;boolean oldRule=level.getGameRules().get(rule);
        try {
            helper.assertTrue(ship.launch(captain)==null,"Captain sails before patrol spawning");var target=pos.offset(96,0,64);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.getChunk((target.getX()>>4)+x,(target.getZ()>>4)+z);
            Ships.spawnPatrols(level);spawned.addAll(level.getEntitiesOfClass(PirateShip.class,ship.getBoundingBox().inflate(192),e->e.enemy()&&e.isAlive()));
            helper.assertTrue(spawned.size()==1,"Loaded clear ocean gets one bounded encounter");Ships.spawnPatrols(level);
            helper.assertTrue(level.getEntitiesOfClass(PirateShip.class,ship.getBoundingBox().inflate(192),e->e.enemy()&&e.isAlive()).size()==1,"Existing nearby patrol prevents another spawn");spawned.getFirst().discard();
            var capPos=pos.offset(320,0,0);level.getChunk(capPos);
            for(int i=0;i<8;i++)spawned.add(PirateShip.spawnEnemy(level,capPos));
            int loaded=0;for(var entity:level.getAllEntities())if(entity instanceof PirateShip vessel&&vessel.enemy()&&vessel.isAlive())loaded++;helper.assertTrue(loaded>=8,"Cap fixture has eight registered loaded vessels, observed "+loaded);
            Ships.spawnPatrols(level);helper.assertTrue(level.getEntitiesOfClass(PirateShip.class,ship.getBoundingBox().inflate(192),e->e.enemy()&&e.isAlive()).isEmpty(),"Eight loaded NPC vessels prevent another encounter");
            for(var patrol:spawned)patrol.discard();level.getGameRules().set(rule,false,level.getServer());
            helper.assertTrue(PirateShip.spawnPatrol(level,target,PirateProgression.Faction.DROWNED)==null,"Mob spawn rule disables patrol creation");
            level.getGameRules().set(rule,true,level.getServer());var unloaded=new BlockPos(-1000000,64,-1000000);
            helper.assertTrue(level.getChunkSource().getChunkNow(unloaded.getX()>>4,unloaded.getZ()>>4)==null&&PirateShip.spawnPatrol(level,unloaded,PirateProgression.Faction.DROWNED)==null&&level.getChunkSource().getChunkNow(unloaded.getX()>>4,unloaded.getZ()>>4)==null,"Patrol attempts never load distant chunks");
        } finally {level.getGameRules().set(rule,oldRule,level.getServer());for(var patrol:spawned)clear(patrol);clear(ship);remove(captain);remove(watcher);}helper.succeed();
    }

    @GameTest public void shipIdentityCargoUpgradesAndPagesPreserveOwnership(GameTestHelper helper) {
        var pos=new BlockPos(-40,64,-40);var captain=player(helper,pos.above());var stranger=player(helper,pos.offset(12,1,0));var ship=build(helper,captain,pos);
        try {
            var menu=new ShipMenu(1,captain.getInventory(),null,ship);var foreign=new ShipMenu(2,stranger.getInventory(),null,ship);
            helper.assertTrue(menu.rename(captain,"  North Star  ")&&ship.getName().getString().equals("North Star")&&!foreign.rename(stranger,"Stolen"),"Only nearby owner may name a docked ship");
            helper.assertTrue(!menu.rename(captain,"x".repeat(33))&&!menu.rename(captain,"bad\nname")&&ship.getName().getString().equals("North Star"),"Overlong and control-character names are rejected");
            Pirate.state(captain).factionMembership="corsairs";helper.assertTrue(ship.cycleFlag(captain)&&ship.flag()==1&&!ship.cycleFlag(stranger),"Only owner selects membership flag");
            ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,3));captain.getInventory().clearContent();captain.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,48));captain.getInventory().setItem(1,new ItemStack(Items.EMERALD,12));
            helper.assertTrue(!ship.upgradeCargo(captain)&&ship.cargoSlots()==27&&captain.getInventory().getItem(0).getCount()==48,"Cargo milestone gate consumes nothing");
            Pirate.state(captain).navalVictories=5;helper.assertTrue(ship.upgradeCargo(captain)&&ship.cargoSlots()==36&&captain.getInventory().getItem(0).getCount()==32&&captain.getInventory().getItem(1).getCount()==8,"First cargo upgrade charges exactly sixteen iron and four emeralds");
            helper.assertTrue(!ship.upgradeCargo(captain),"Veteran required for final hold");menu.broadcastChanges();helper.assertTrue(!menu.slots.get(27).isActive()&&!menu.slots.get(27).mayPlace(new ItemStack(Items.DIAMOND)),"Second page is hidden and server-locked until selected");
            helper.assertTrue(menu.clickMenuButton(captain,ShipMenu.CARGO_PAGE)&&menu.slots.get(27).isActive()&&!menu.slots.get(36).isActive()&&!menu.slots.get(0).mayPickup(captain),"36-slot hold exposes exactly nine slots on page two");
            ship.cargo.setItem(1,new ItemStack(Items.GOLD_INGOT,5));captain.getInventory().setItem(2,new ItemStack(Items.GOLD_INGOT,7));int inventorySlot=-1;for(int i=54;i<menu.slots.size();i++)if(menu.slots.get(i).getContainerSlot()==2)inventorySlot=i;
            helper.assertTrue(inventorySlot>=0&&!menu.quickMoveStack(captain,inventorySlot).isEmpty()&&ship.cargo.getItem(27).is(Items.GOLD_INGOT)&&ship.cargo.getItem(27).getCount()==7&&ship.cargo.getItem(1).getCount()==5&&ship.cargo.getItem(0).getCount()==3,"Shift-click deposits into selected page without changing hidden cargo");
            helper.assertTrue(menu.quickMoveStack(captain,0).isEmpty(),"Stale hidden-slot extraction rejected");
            Pirate.state(captain).navalVictories=12;helper.assertTrue(ship.upgradeCargo(captain)&&ship.cargoSlots()==54&&captain.getInventory().getItem(0).isEmpty()&&captain.getInventory().getItem(1).isEmpty(),"Second cargo upgrade consumes thirty-two iron and eight emeralds");ship.cargo.setItem(53,new ItemStack(Items.PRISMARINE_SHARD,9));
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);var restored=new PirateShip(Ships.SHIP,captain.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));
            helper.assertTrue(restored.cargoSlots()==54&&restored.cargo.getItem(0).getCount()==3&&restored.cargo.getItem(27).getCount()==7&&restored.cargo.getItem(53).getCount()==9&&restored.flag()==1&&restored.getName().getString().equals("North Star"),"Native save preserves all cargo pages, flag and name");restored.discard();
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),captain.registryAccess());try {var payload=new ShipIdentityPayload(1,7,"North Star");ShipIdentityPayload.CODEC.encode(buffer,payload);helper.assertTrue(ShipIdentityPayload.CODEC.decode(buffer).equals(payload),"Bounded identity payload round-trips");}finally{buffer.release();}
            captain.teleportTo(pos.getX()+60,65,pos.getZ());helper.assertTrue(!menu.rename(captain,"Remote")&&!menu.clickMenuButton(captain,ShipMenu.CARGO_PAGE),"Remote menu packets cannot rename or change pages");
        } finally {clear(ship);remove(captain);remove(stranger);}helper.succeed();
    }
    @GameTest public void officersResupplyAndShantyKeepSavedCrewAndItems(GameTestHelper helper) {
        var pos=new BlockPos(40,64,40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
        try {
            helper.assertTrue(!ship.recruitOfficer(captain,0)&&!ship.resupply(captain)&&!ship.toggleShanty(captain),"Uncompleted commissions cannot hire or grant officer abilities");var state=Pirate.state(captain);state.officerContracts=new int[]{3,3,3};
            for(int i=0;i<3;i++)helper.assertTrue(ship.recruitOfficer(captain,i)&&!ship.recruitOfficer(captain,i)&&ship.crew(PirateShip.NAVIGATOR+i)==1,"Each named officer boards once in a separate role");
            captain.getInventory().clearContent();var supply=new ItemStack(Ships.SHOT,20);supply.set(DataComponents.CUSTOM_NAME,Component.literal("Marked ammunition"));captain.getInventory().setItem(0,supply);captain.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,10));
            for(int i=0;i<ship.cargoSlots();i++)ship.cargo.setItem(i,new ItemStack(Items.STONE,64));
            helper.assertTrue(!ship.resupply(captain)&&supply.getCount()==20,"Full hold applies backpressure without item loss");ship.cargo.setItem(0,supply.copyWithCount(60));
            helper.assertTrue(ship.resupply(captain)&&ship.cargo.getItem(0).getCount()==64&&supply.getCount()==16&&captain.getInventory().getItem(1).getCount()==10&&ship.cargo.getItem(27).isEmpty(),"Quartermaster transfers only fitting matching-component supplies into unlocked slots");
            ship.cargo.setItem(1,ItemStack.EMPTY);helper.assertTrue(ship.resupply(captain)&&ship.cargo.getItem(1).getCount()==16&&ship.cargo.getItem(1).get(DataComponents.CUSTOM_NAME).getString().equals("Marked ammunition")&&captain.getInventory().getItem(0).isEmpty(),"Partial supply transfers conserve components and exact counts");
            helper.assertTrue(ship.toggleShanty(captain)&&ship.shantyPlaying()&&ship.shantyAudible(),"Captain starts synchronized shanty eligibility");ship.tick();helper.assertTrue(ship.toggleShanty(captain)&&!ship.shantyPlaying()&&!ship.shantyAudible(),"Captain immediately stops synchronized playback");ship.toggleShanty(captain);
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);
            var restored=(PirateShip)net.minecraft.world.entity.EntityType.loadEntityRecursive(Ships.SHIP,output.buildResult(),captain.level(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
            helper.assertTrue(restored!=null&&restored.crew(PirateShip.NAVIGATOR)==1&&restored.crew(PirateShip.QUARTERMASTER)==1&&restored.crew(PirateShip.SINGER)==1&&restored.shantyPlaying(),"All three roles and playback choice survive native recursive save/load");
            for(var officer:java.util.List.copyOf(restored.getPassengers())){helper.assertTrue(officer.entityTags().contains(PirateShip.CREW_OWNER+captain.getUUID()),"New officers retain captain loyalty");officer.discard();}restored.discard();
            helper.assertTrue(ship.launch(captain)==null,"Three new officer roles launch as assigned crew");helper.assertTrue(!ship.resupply(captain)&&!ship.rename(captain,"Sailing rename"),"Refit and supply transfers require docking");ship.tickCount=120;ship.tick();
        } finally {clear(ship);remove(captain);}helper.succeed();
    }


    @GameTest public void sinkingRecordsNamedRecoveryAndGhostVictory(GameTestHelper helper){
        var pos=new BlockPos(40,64,40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);PirateShip ghost=null;
        try {
            var state=Pirate.state(captain);state.officerContracts=new int[]{3,3,3};state.garrickRescue=3;state.sealContract=3;state.seals=1;state.repeatContracts[1]=1;state.ghostVoyage=true;
            for(int i=0;i<3;i++)helper.assertTrue(ship.recruitOfficer(captain,i),"Named crew boards before voyage");helper.assertTrue(ship.recruitGarrick(captain),"Garrick boards before voyage");
            var menu=new ShipMenu(1,captain.getInventory(),null,ship);for(var slot:menu.slots)helper.assertTrue(slot.x>=0&&slot.y>=0&&slot.x+16<=ShipMenu.WIDTH&&slot.y+16<=ShipMenu.HEIGHT,"Every inventory slot stays inside compact screen bounds");
            helper.assertTrue(ship.launch(captain)==null,"Vessel launches");var at=location(helper);for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)captain.level().getChunk((at.getX()>>4)+x,(at.getZ()>>4)+z);ship.setPos(at.getX(),64,at.getZ());captain.teleportTo(at.getX(),65,at.getZ());
            ghost=PirateShip.spawnSpecial(captain.level(),at.offset(30,0,0),1);helper.assertTrue(ghost!=null&&ghost.encounterKind()==1,"Ghost ship spawns using bounded native patrol placement");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ghost.saveWithoutId(output);var restored=new PirateShip(Ships.SHIP,captain.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));helper.assertTrue(restored.encounterKind()==1,"Ghost type survives save");restored.discard();
            for(int n=0;n<20&&!ghost.isRemoved();n++)ghost.cannonDamage(ship,false);
            helper.assertTrue(ghost.isRemoved()&&(state.seals&32)!=0&&!state.ghostVoyage&&state.repeatContracts[1]==1,"Ghost win records Courage without satisfying outlaw hunt");
            var enemy=PirateShip.spawnSpecial(captain.level(),at.offset(-30,0,0),2);helper.assertTrue(enemy!=null&&!enemy.hostileTo(ship),"Hunters leave low-infamy captains alone");
            state.infamy=30;helper.assertTrue(enemy.hostileTo(ship),"Hunters pursue infamous captains");state.infamy=0;
            helper.assertTrue(enemy.cannonDamage(ship,false)&&enemy.hostileTo(ship),"Hunters retaliate when a low-infamy captain attacks them");
            for(int n=0;n<20&&!ship.isRemoved();n++)ship.cannonDamage(enemy,false);enemy.discard();
            helper.assertTrue(ship.isRemoved()&&SeaLedger.get(captain.level()).lost(captain.getUUID())==15,"Sinking creates rescue claims for all four named officers");
            captain.stopRiding();captain.teleportTo(2,65,4);captain.getInventory().clearContent();captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,8));
            helper.assertTrue(SeaServices.execute(captain,7)==SeaServices.Reply.DONE&&state.officerContracts[0]==3,"Sunk officer can be paid for and rehired without repeating commission");
        }finally{if(ghost!=null)clear(ghost);clear(ship);remove(captain);}helper.succeed();
    }
    private static BlockPos unloadingFixture(GameTestHelper h,ServerPlayer captain,PirateShip ship,BlockPos pos) {
        var level=captain.level();
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.getChunk((pos.getX()>>4)+x,(pos.getZ()>>4)+z);
        var ledger=SeaLedger.get(level);ledger.fort(new SeaLedger.Fort(pos,captain.getUUID(),"Unload port",0,0));ledger.homePort(new SeaLedger.HomePort(captain.getUUID(),pos,1));
        captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,32));h.assertTrue(ship.recruit(captain,PirateShip.WORKER),"Hire original shipworker");
        for(int x=4;x<=6;x++)for(int z=-2;z<=2;z++){level.setBlock(pos.offset(x,0,z),Blocks.OAK_PLANKS.defaultBlockState(),2);for(int y=1;y<=3;y++)level.setBlock(pos.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        var chest=pos.offset(5,1,0);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);
        h.assertTrue(ship.unloading.select(captain,chest),"Captain explicitly selects nearby single chest");
        return chest;
    }
    private static String unloadingDebug(PirateShip ship) {
        var worker=(net.minecraft.world.entity.Mob)ship.crewMembers().getFirst();int missing=0;var at=worker.blockPosition();
        for(int x=at.getX()-64;x<=at.getX()+64;x+=16)for(int z=at.getZ()-64;z<=at.getZ()+64;z+=16)if(!ship.level().hasChunkAt(new BlockPos(x,at.getY(),z)))missing++;
        String saved="";try{for(var name:new String[]{"deck","fetching"}){var field=DockedUnloading.class.getDeclaredField(name);field.setAccessible(true);saved+=" "+name+"="+field.get(ship.unloading);}}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        var path=worker.getNavigation().createPath(ship.unloading.target().west(),0,24);
        return DockedUnloading.STATUS[ship.unloading.status()]+" worker="+worker.position()+" ship="+ship.position()+" target="+ship.unloading.target()+" ground="+worker.onGround()+" missingChunks="+missing+" range="+worker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)+" path="+(path==null?"null":path.canReach()+"/"+path.getTarget())+saved;
    }
    private static void unloadStep(PirateShip ship) {
        ship.crewMembers().getFirst().setOnGround(true);ship.tickCount+=20;ship.unloading.tick();
        var worker=(net.minecraft.world.entity.Mob)ship.crewMembers().getFirst();var path=worker.getNavigation().getPath();
        // Arrival fixture: use the endpoint of a real reachable native path; motion has its own test below.
        if(path!=null&&path.canReach())worker.setPos(Vec3.atBottomCenterOf(path.getTarget()));
    }
    private static void clearUnloading(PirateShip ship,BlockPos pos) {
        for(var crew:ship.crewMembers())crew.discard();
        for(int x=4;x<=6;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=3;y++)ship.level().setBlock(pos.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        clear(ship);
    }
    @GameTest public void unloadingConservesComponentsAndStopsAtFullChest(GameTestHelper h) {
        var pos=location(h);var p=player(h,pos.above());var ship=build(h,p,pos);
        try {
            var target=unloadingFixture(h,p,ship,pos);var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)p.level().getBlockEntity(target);
            var cargo=new ItemStack(Items.DIAMOND,20);cargo.set(DataComponents.CUSTOM_NAME,Component.literal("Marked cargo"));ship.cargo.setItem(0,cargo);
            for(int i=0;i<27;i++)chest.setItem(i,new ItemStack(Items.STONE,64));chest.setItem(0,cargo.copyWithCount(62));
            h.assertTrue(ship.unloading.toggle(p),"Start job");
            for(int i=0;i<12&&cargo.getCount()==20;i++)unloadStep(ship);
            h.assertTrue(cargo.getCount()==18&&chest.getItem(0).getCount()==64&&ItemStack.isSameItemSameComponents(cargo,chest.getItem(0)),"Transfer only the two fitting items, with exact components: "+unloadingDebug(ship));
            for(int i=0;i<12;i++)unloadStep(ship);
            h.assertTrue(cargo.getCount()==18&&ship.unloading.status()==5,"Full chest pauses without consuming cargo");
            chest.setItem(1,ItemStack.EMPTY);for(int i=0;i<10&&cargo.getCount()==18;i++)unloadStep(ship);
            h.assertTrue(cargo.getCount()==10&&chest.getItem(1).getCount()==8,"Free space resumes a bounded eight-item delivery");
            ship.unloading.pause();int count=cargo.getCount();unloadStep(ship);h.assertTrue(cargo.getCount()==count,"Explicit pause stops deliveries");
        } finally {clearUnloading(ship,pos);remove(p);}h.succeed();
    }
    @GameTest public void unloadingOwnershipBlockedChestAndMissingWorkerAreSafe(GameTestHelper h) {
        var pos=location(h);var p=player(h,pos.above());var other=player(h,pos.above());var ship=build(h,p,pos);
        try {
            var target=unloadingFixture(h,p,ship,pos);ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,20));
            h.assertTrue(!ship.unloading.select(other,target)&&!ship.unloading.toggle(other)&&!ship.unloading.mark(other),"Other players cannot redirect or start the job");
            h.assertTrue(!ship.unloading.select(p,pos.offset(25,1,0))&&ship.unloading.target().equals(target),"Rejected target preserves the selected chest");
            h.assertTrue(ship.unloading.toggle(p),"Start job");p.level().setBlock(target.above(),Blocks.STONE.defaultBlockState(),2);
            unloadStep(ship);h.assertTrue(ship.unloading.status()==9&&ship.cargo.getItem(0).getCount()==20,"Blocked chest cannot receive cargo");
            p.level().setBlock(target.above(),Blocks.AIR.defaultBlockState(),2);p.setPos(pos.getX()+100,pos.getY()+1,pos.getZ());
            unloadStep(ship);h.assertTrue(ship.unloading.status()==7&&ship.cargo.getItem(0).getCount()==20,"Absent captain pauses job");
            p.setPos(Vec3.atBottomCenterOf(pos.above()));ship.unloading.pause();ship.crewMembers().getFirst().discard();
            h.assertTrue(!ship.unloading.toggle(p)&&ship.unloading.status()==8,"Missing worker is not respawned");
        } finally {clearUnloading(ship,pos);remove(p);remove(other);}h.succeed();
    }
    @GameTest public void unloadingReloadRequiresResumeAndLaunchRecallsOriginalWorker(GameTestHelper h) {
        var pos=location(h);var p=player(h,pos.above());var ship=build(h,p,pos);
        try {
            var target=unloadingFixture(h,p,ship,pos);ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,20));var worker=ship.crewMembers().getFirst();var id=worker.getUUID();
            h.assertTrue(ship.unloading.toggle(p),"Start job");unloadStep(ship);
            h.assertTrue(!ship.toggleCrewRoaming(p)&&!ship.toggleExpedition(p),"Shore orders cannot override active unloading");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());ship.saveWithoutId(saved);
            var restored=new PirateShip(Ships.SHIP,p.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
            h.assertTrue(target.equals(restored.unloading.target())&&!restored.unloading.active()&&restored.unloading.status()==1&&restored.cargo.getItem(0).getCount()==20,"Reload retains destination/cargo and pauses work");restored.discard();
            var error=ship.launch(p);h.assertTrue(error==null&&!ship.unloading.active()&&worker.getVehicle()==ship&&worker.getUUID().equals(id),"Launch pauses and recalls original worker: "+error);
        } finally {clearUnloading(ship,pos);remove(p);}h.succeed();
    }
    @GameTest(maxTicks=500) public void unloadingWorkerWalksNativeRouteToChest(GameTestHelper h) {
        var pos=location(h);var p=player(h,pos.above());var ship=build(h,p,pos);var target=unloadingFixture(h,p,ship,pos);
        ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,8));h.assertTrue(ship.unloading.toggle(p),"Start real movement job");
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)p.level().setChunkForced((pos.getX()>>4)+x,(pos.getZ()>>4)+z,true);
        h.runAfterDelay(440,()->{
            try {
                var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)p.level().getBlockEntity(target);
                h.assertTrue(chest.countItem(Items.DIAMOND)==8&&ship.cargo.getItem(0).isEmpty(),"Native worker movement completes delivery: "+unloadingDebug(ship));
                h.succeed();
            } finally {for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)p.level().setChunkForced((pos.getX()>>4)+x,(pos.getZ()>>4)+z,false);clearUnloading(ship,pos);remove(p);}
        });
    }
    @GameTest public void unloadingBlockedRouteReplacementAndLockedSlotsPreserveCargo(GameTestHelper h) {
        var pos=location(h);var p=player(h,pos.above());var ship=build(h,p,pos);
        try {
            var target=unloadingFixture(h,p,ship,pos);ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,8));ship.cargo.setItem(53,new ItemStack(Items.EMERALD,7));
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)p.level().setBlock(target.relative(direction),Blocks.STONE.defaultBlockState(),2);
            h.assertTrue(ship.unloading.toggle(p),"Start blocked-route order");for(int i=0;i<10;i++)unloadStep(ship);
            h.assertTrue(ship.unloading.status()==6&&ship.cargo.getItem(0).getCount()==8,"No accessible chest approach means no remote transfer");
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)p.level().setBlock(target.relative(direction),Blocks.AIR.defaultBlockState(),2);
            p.level().setBlock(target,Blocks.AIR.defaultBlockState(),2);p.level().setBlock(target,Blocks.CHEST.defaultBlockState(),2);unloadStep(ship);
            h.assertTrue(ship.unloading.status()==9&&ship.cargo.getItem(0).getCount()==8,"Replacing destination does not silently rebind a running order");
            h.assertTrue(ship.unloading.toggle(p)&&ship.unloading.toggle(p),"Explicit pause/resume rebinds replacement");
            for(int i=0;i<15;i++)unloadStep(ship);
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)p.level().getBlockEntity(target);
            h.assertTrue(chest.countItem(Items.DIAMOND)==8&&ship.cargo.getItem(53).getCount()==7&&ship.unloading.status()==10,"Only unlocked cargo slots unload; inaccessible cargo is preserved");
        } finally {clearUnloading(ship,pos);remove(p);}h.succeed();
    }
}
