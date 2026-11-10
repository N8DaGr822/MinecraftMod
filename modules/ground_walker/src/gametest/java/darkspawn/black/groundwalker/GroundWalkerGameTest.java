package darkspawn.black.groundwalker;

import darkspawn.black.mountkit.MountCompanions;
import darkspawn.black.mountkit.MountMenu;
import darkspawn.black.mountkit.MountRegistry;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
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

public final class GroundWalkerGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "walker-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
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
	private static GroundWalker walker(GameTestHelper helper) {
		var walker = new GroundWalker(GroundWalkerMod.GROUND_WALKER, helper.getLevel());
		var origin = helper.absolutePos(new BlockPos(2, 3, 2));
		for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
			helper.getLevel().setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
			for (int y = 0; y < 10; y++) helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		walker.setPos(Vec3.atBottomCenterOf(origin));
		walker.setOnGround(true);
		helper.getLevel().addFreshEntity(walker);
		return walker;
	}
	private static void mount(GroundWalker walker, ServerPlayer player) {
        // Movement fixtures begin owned; placement and repair tests exercise crafted-machine interactions.
        var ownership = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, walker.level().registryAccess());
        walker.saveWithoutId(ownership);
        ownership.store("MountOwner", net.minecraft.core.UUIDUtil.CODEC, player.getUUID());
        walker.load(TagValueInput.create(ProblemReporter.DISCARDING, walker.level().registryAccess(), ownership.buildResult()));
		player.teleportTo(walker.getX(), walker.getY(), walker.getZ());
		walker.mobInteract(player, InteractionHand.MAIN_HAND);
		walker.positionRider(player);
	}

    @GameTest(skyAccess=true)
    public void ownerOnlyGroundControlsRepairAndMachineBoundaries(GameTestHelper h) {
        var mech=walker(h); var owner=player(h); var stranger=player(h);
        try {
            mount(mech,owner);
            h.assertTrue(owner.getVehicle()==mech && !stranger.startRiding(mech),"Only one owner pilot");
            h.assertTrue(!mech.acceptControls(stranger,GroundWalker.FORWARD) && mech.acceptControls(owner,GroundWalker.FORWARD),"Only pilot input accepted");
            h.assertTrue(!mech.canFly() && !mech.combat.supported(),"No flight or weapons");
            h.assertTrue(mech.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT)==1,"One-block step height");
            owner.stopRiding(); owner.teleportTo(mech.getX()+3,mech.getY(),mech.getZ());
            h.assertTrue(!mech.acceptControls(owner,GroundWalker.FORWARD),"Dismounted input rejected");
            mech.care.setFollowing(true);
            h.assertTrue(!mech.care.following && !mech.care.requestBreeding(owner),"No follow or breeding");
            var menu=new MountMenu(1,owner.getInventory(),mech);
            h.assertTrue(!menu.stillValid(owner) && !menu.clickMenuButton(owner,MountMenu.FOLLOW),"No animal cargo/care menu");
            mech.setHealth(79); owner.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3));
            mech.mobInteract(owner,InteractionHand.MAIN_HAND);
            h.assertTrue(mech.getHealth()==80 && owner.getMainHandItem().getCount()==2,"Iron repair clamps health and consumes one");
            mech.setHealth(70); mech.mobInteract(owner,InteractionHand.MAIN_HAND);
            h.assertTrue(mech.getHealth()==70 && owner.getMainHandItem().getCount()==2,"Repair cooldown prevents repeat consumption");
            stranger.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
            mech.mobInteract(stranger,InteractionHand.MAIN_HAND);
            h.assertTrue(mech.getHealth()==70 && !stranger.getMainHandItem().isEmpty(),"Stranger cannot repair or claim");
        } finally {cleanup(owner);cleanup(stranger);mech.discard();}
        h.succeed();
    }
    @GameTest(skyAccess=true)
    public void recipePlacementAndHeadroomConserveItem(GameTestHelper h) {
        var mech=walker(h); var pos=mech.blockPosition(); mech.discard(); var owner=player(h);
        try {
            var I=net.minecraft.world.item.Items.IRON_INGOT; var P=net.minecraft.world.item.Items.PISTON;
            var R=net.minecraft.world.item.Items.REDSTONE; var B=net.minecraft.world.item.Items.IRON_BLOCK;
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(I,P,I,R,B,R,I,net.minecraft.world.item.Items.AIR,I).stream().map(net.minecraft.world.item.ItemStack::new).toList());
            var stack=h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input);
            h.assertTrue(stack.is(GroundWalkerItem.ITEM),"Native recipe produces the walker");
            owner.teleportTo(pos.getX()+3,pos.getY(),pos.getZ());
            var context=new net.minecraft.world.item.context.UseOnContext(h.getLevel(),owner,InteractionHand.MAIN_HAND,stack,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos.below()),net.minecraft.core.Direction.UP,pos.below(),false));
            h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            h.assertTrue(GroundWalkerItem.ITEM.useOn(context)==net.minecraft.world.InteractionResult.FAIL && stack.getCount()==1,"Blocked placement preserves item");
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            h.assertTrue(GroundWalkerItem.ITEM.useOn(context).consumesAction() && stack.isEmpty(),"Safe placement consumes one item");
            var placed=h.getLevel().getEntitiesOfClass(GroundWalker.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).getFirst();
            try {
                h.assertTrue(placed.isOwnedBy(owner),"Placement binds owner");
                h.getLevel().setBlockAndUpdate(pos.above(3),Blocks.STONE.defaultBlockState());
                placed.mobInteract(owner,InteractionHand.MAIN_HAND);
                h.assertTrue(!owner.isPassenger(),"Low rider ceiling prevents boarding");
            } finally {placed.discard();}
        } finally {cleanup(owner);}
        h.succeed();
    }
    @GameTest(skyAccess=true)
    public void steppingWallsAndSafeDismountUseNativeCollision(GameTestHelper h) {
        var mech=walker(h); var owner=player(h);
        try {
            mount(mech,owner); var pos=mech.blockPosition(); mech.setSpeed(.18F);
            for(int x=-3;x<=3;x++) for(int z=2;z<=4;z++) h.getLevel().setBlockAndUpdate(pos.offset(x,0,z),Blocks.STONE.defaultBlockState());
            for(int x=-3;x<=3;x++) for(int y=1;y<7;y++) h.getLevel().setBlockAndUpdate(pos.offset(x,y,4),Blocks.STONE.defaultBlockState());
            for(int i=0;i<28;i++){mech.travel(new Vec3(0,0,1));mech.positionRider(owner);}
            h.assertTrue(mech.getY()>=pos.getY()+.99 && mech.getZ()>pos.getZ()+2,"Walker actually climbs a one-block step");
            for(int i=0;i<20;i++){mech.travel(new Vec3(0,0,1));mech.positionRider(owner);}
            h.assertTrue(mech.getBoundingBox().maxZ<=pos.getZ()+4.001 && !mech.flying(),"Tall wall blocks ground travel");
            var exit=mech.getDismountLocationForPassenger(owner);
            h.assertTrue(h.getLevel().noCollision(owner,owner.getDimensions(owner.getPose()).makeBoundingBox(exit)),"Dismount has player clearance");
        } finally {cleanup(owner);mech.discard();}
        h.succeed();
    }
    @GameTest(skyAccess=true)
    public void saveLoadClearsControlsAndKeepsRepairCooldown(GameTestHelper h) {
        var mech=walker(h);var owner=player(h);GroundWalker restored=null;
        try {
            mount(mech,owner);mech.acceptControls(owner,GroundWalker.FORWARD);
            mech.setHealth(60);owner.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,2));mech.mobInteract(owner,InteractionHand.MAIN_HAND);
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());mech.saveWithoutId(saved);
            restored=new GroundWalker(GroundWalkerMod.GROUND_WALKER,h.getLevel());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved.buildResult()));
            h.assertTrue(restored.isOwnedBy(owner) && restored.getRiddenInput(owner,Vec3.ZERO).equals(Vec3.ZERO) && !restored.flying(),"Native save/load retains owner and clears pilot input");
            h.assertTrue(!restored.canFly() && !restored.care.following,"Reload cannot enable flight or following");
            restored.mobInteract(owner,InteractionHand.MAIN_HAND);
            h.assertTrue(restored.getHealth()==64 && owner.getMainHandItem().getCount()==1,"Repair cooldown survives save/load");
        } finally {cleanup(owner);mech.discard();if(restored!=null)restored.discard();}
        h.succeed();
    }

    @GameTest(skyAccess=true)
    public void realTicksExpirePilotInputWithoutEnablingFlight(GameTestHelper h) {
        var mech=walker(h);var owner=player(h);mount(mech,owner);
        mech.acceptControls(owner,GroundWalker.FORWARD);
        h.runAfterDelay(16,()->{
            try {
                h.assertTrue(mech.getRiddenInput(owner,Vec3.ZERO).equals(Vec3.ZERO),"Stale controls expire after ten ticks");
                h.assertTrue(!mech.flying() && !mech.isNoGravity(),"Ground machine never hovers");
            } finally {cleanup(owner);mech.discard();}
            h.succeed();
        });
    }
}
