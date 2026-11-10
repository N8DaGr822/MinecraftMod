package darkspawn.black.mount;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class AircraftGameTest {
    private static ServerPlayer player(GameTestHelper helper) {
        var level=helper.getLevel();
        var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"aircraft-test"),false);
        var player=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
        var connection=new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        return player;
    }
    private static void cleanup(ServerPlayer player) {
        player.stopRiding(); player.level().getServer().getPlayerList().remove(player); player.discard();
    }
    private static List<AirframeBlock> build() {
        return List.of(BlockPos.ZERO,new BlockPos(1,0,0),new BlockPos(-1,0,0),new BlockPos(0,0,1),new BlockPos(0,0,-1))
            .stream().map(pos -> new AirframeBlock(pos,Blocks.OAK_PLANKS.defaultBlockState())).toList();
    }
    private static BlockPos ground(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,3,2));
        for (int x=-5;x<=5;x++) for (int z=-5;z<=5;z++) {
            h.getLevel().setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for (int y=0;y<12;y++) h.getLevel().setBlockAndUpdate(pos.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        return pos;
    }
    private static Aircraft craft(GameTestHelper h, BlockPos pos) {
        var craft=new Aircraft(Mounts.AIRCRAFT,h.getLevel()); craft.setBuild(build());
        craft.setPos(Vec3.atBottomCenterOf(pos)); craft.setOnGround(true); h.getLevel().addFreshEntity(craft); return craft;
    }
    @GameTest(skyAccess=true)
    public void assemblyEditingAndCapsulesConserveBuildAndOwnership(GameTestHelper h) {
        var owner=player(h); var pos=ground(h); Aircraft craft=null; String token="";
        try {
            owner.teleportTo(pos.getX()+4,pos.getY(),pos.getZ());
            for (var block:build()) h.getLevel().setBlockAndUpdate(pos.offset(block.pos()),block.state());
            var I=net.minecraft.world.item.Items.IRON_INGOT; var P=net.minecraft.world.item.Items.PHANTOM_MEMBRANE;
            var R=net.minecraft.world.item.Items.REDSTONE; var A=net.minecraft.world.item.Items.AMETHYST_SHARD;
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,List.of(I,P,I,R,A,R,I,P,I).stream().map(ItemStack::new).toList());
            var stack=h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input);
            h.assertTrue(stack.is(AircraftCoreItem.ITEM) && stack.getCount()==1,"Native recipe produces one aircraft core");
            var context=new net.minecraft.world.item.context.UseOnContext(h.getLevel(),owner,InteractionHand.MAIN_HAND,stack,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
            h.assertTrue(AircraftCoreItem.ITEM.useOn(context).consumesAction() && stack.isEmpty(),"Assembly consumes one core");
            craft=h.getLevel().getEntitiesOfClass(Aircraft.class,new net.minecraft.world.phys.AABB(pos).inflate(3)).getFirst();
            h.assertTrue(craft.isOwnedBy(owner) && new java.util.HashSet<>(craft.blocks()).equals(new java.util.HashSet<>(build())),"Exact build and owner captured");
            for (var block:build()) h.assertTrue(h.getLevel().getBlockState(pos.offset(block.pos())).isAir(),"Captured blocks removed once");
            h.assertTrue(!craft.care.requestBreeding(owner),"Aircraft cannot breed");
            var menu=new MountMenu(1,owner.getInventory(),craft);
            h.assertTrue(!menu.stillValid(owner) && !menu.clickMenuButton(owner,MountMenu.FOLLOW),"Generic care rejects aircraft even if constructed directly");
            var capsule=new ItemStack(MountCompanions.CAPSULE); var id=craft.getUUID();
            h.assertTrue(MountCompanions.capture(owner,craft,capsule),"Aircraft stores safely"); token=MountCompanions.token(capsule); var copy=capsule.copy();
            h.assertTrue(MountCompanions.release(owner,capsule,pos),"Aircraft releases on safe ground");
            craft=(Aircraft)h.getLevel().getEntity(id); craft.setOnGround(true);
            h.assertTrue(craft.isOwnedBy(owner) && new java.util.HashSet<>(craft.blocks()).equals(new java.util.HashSet<>(build())),"Capsule preserves exact build and owner");
            h.assertTrue(!MountCompanions.release(owner,copy,pos),"Copied capsule cannot duplicate aircraft");
            craft.setYRot(75);
            h.assertTrue(craft.disassemble(owner),"Landed aircraft restores original orientation");
            for (var block:build()) h.assertTrue(h.getLevel().getBlockState(pos.offset(block.pos())).equals(block.state()),"Original block restored");
            h.assertTrue(owner.getInventory().countItem(AircraftCoreItem.ITEM)==1,"Exactly one core returns");
            h.assertTrue(!craft.disassemble(owner),"Repeated edit cannot return another core");
        } finally { if(!token.isEmpty()) MountRegistry.get(h.getLevel()).remove(token); if(craft!=null) craft.discard(); cleanup(owner); }
        h.succeed();
    }
    @GameTest(skyAccess=true)
    public void blockedAssemblyAndEditingPreserveWorld(GameTestHelper h) {
        var owner=player(h); var stranger=player(h); var pos=ground(h); Aircraft craft=null;
        try {
            owner.teleportTo(pos.getX()+4,pos.getY(),pos.getZ()); stranger.teleportTo(pos.getX()+4,pos.getY(),pos.getZ()+2);
            for (var block:build()) h.getLevel().setBlockAndUpdate(pos.offset(block.pos()),block.state());
            h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
            h.assertTrue(!AircraftCoreItem.assemble(owner,pos),"Container airframe rejected");
            h.assertTrue(h.getLevel().getBlockState(pos.east()).is(Blocks.OAK_PLANKS),"Rejected assembly preserves blocks");
            h.getLevel().setBlockAndUpdate(pos,Blocks.OAK_PLANKS.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos.offset(2,0,0),Blocks.STONE.defaultBlockState());
            h.assertTrue(!AircraftCoreItem.assemble(owner,pos),"Bounding-box obstruction rejected");
            h.getLevel().setBlockAndUpdate(pos.offset(2,0,0),Blocks.AIR.defaultBlockState());
            h.assertTrue(AircraftCoreItem.assemble(owner,pos),"Valid frame assembles");
            craft=h.getLevel().getEntitiesOfClass(Aircraft.class,new net.minecraft.world.phys.AABB(pos).inflate(3)).getFirst();
            h.assertTrue(!craft.disassemble(stranger),"Stranger cannot edit");
            craft.setOnGround(false); h.assertTrue(!craft.disassemble(owner),"Airborne edit rejected"); craft.setOnGround(true);
            h.getLevel().setBlockAndUpdate(pos.east(),Blocks.STONE.defaultBlockState());
            h.assertTrue(!craft.disassemble(owner),"Blocked edit rejected");
            h.assertTrue(h.getLevel().getBlockState(pos).isAir() && new java.util.HashSet<>(craft.blocks()).equals(new java.util.HashSet<>(build())),"Blocked edit preserves build");
            h.assertTrue(owner.getInventory().countItem(AircraftCoreItem.ITEM)==0,"Failed edit returns no core");
        } finally { if(craft!=null) craft.discard(); cleanup(owner); cleanup(stranger); }
        h.succeed();
    }
    @GameTest(skyAccess=true)
    public void flightAuthorityCollisionLandingAndInputExpiry(GameTestHelper h) {
        var owner=player(h); var stranger=player(h); var pos=ground(h); var craft=craft(h,pos);
        try {
            craft.assignOwner(owner.getUUID()); owner.teleportTo(craft.getX(),craft.getY(),craft.getZ());
            craft.mobInteract(owner,InteractionHand.MAIN_HAND); craft.positionRider(owner);
            h.assertTrue(owner.getVehicle()==craft && !stranger.startRiding(craft),"Only owner and one pilot");
            h.assertTrue(!craft.acceptControls(stranger,Aircraft.ASCEND),"Stranger controls rejected");
            for(int i=0;i<12;i++) { craft.travel(new Vec3(0,1,0)); craft.positionRider(owner); }
            h.assertTrue(craft.flying() && craft.getY()>pos.getY()+2,"Aircraft takes off");
            for(int i=0;i<20;i++) { craft.travel(Vec3.ZERO); craft.positionRider(owner); }
            h.assertTrue(Math.abs(craft.getDeltaMovement().y)<.001,"Released controls hover");
            for(int x=-3;x<=3;x++) for(int y=0;y<10;y++) h.getLevel().setBlockAndUpdate(pos.offset(x,y,3),Blocks.STONE.defaultBlockState());
            craft.setYRot(45);
            // Flight Collision: Counter-rotate input so the 45-degree airframe approaches the wall directly.
            for(int i=0;i<20;i++) { craft.travel(new Vec3(.70710678,0,.70710678)); craft.positionRider(owner); }
            h.assertTrue(craft.getBoundingBox().maxZ<=pos.getZ()+3.001,"Rotated airframe stays behind wall");
            for(int i=0;i<40;i++) { craft.travel(new Vec3(0,-1,0)); craft.positionRider(owner); }
            h.assertTrue(craft.onGround() && !craft.flying() && craft.getHealth()==40,"Manual landing preserves health");
            h.assertTrue(craft.getRiddenInput(owner,Vec3.ZERO).y<=0,"Missing controls never continue ascent");
        } finally { cleanup(owner); cleanup(stranger); craft.discard(); }
        h.succeed();
    }
}
