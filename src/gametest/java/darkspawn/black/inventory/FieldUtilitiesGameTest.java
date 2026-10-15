package darkspawn.black.inventory;
import com.mojang.authlib.GameProfile;
import darkspawn.black.cooking.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

public final class FieldUtilitiesGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "loadout-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		Vec3 pos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 2)));
		player.teleportTo(pos.x, pos.y, pos.z);
		// Mock Connection: Acknowledge loading so vanilla permits actual damage in interruption tests.
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		player.setOnGround(true);
		return player;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }

    @GameTest public void accessorySlotsSaveAndOneBlockProfilesStaySeparate(GameTestHelper h){
        var p=player(h);try{
            var named=new ItemStack(Accessories.ITEMS.getFirst());named.set(DataComponents.CUSTOM_NAME,Component.literal("Captain's glasses"));Accessories.state(p).slots.setItem(0,named);
            Accessories.state(p).slots.setItem(1,new ItemStack(Accessories.ITEMS.getFirst()));
            h.assertTrue(Accessories.mask(p)==1,"Duplicate accessories never stack");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());Accessories.state(p).write(out);var restored=new AccessoryState();restored.read(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),out.buildResult()));
            h.assertTrue(ItemStack.matches(restored.slots.getItem(0),named),"Named equipped item survives serialization");
            var profile=darkspawn.black.oneblock.OneBlockItems.capture(p);darkspawn.black.oneblock.OneBlockItems.EMPTY.apply(p);h.assertTrue(Accessories.mask(p)==0,"Empty island profile cannot retain expedition accessories");profile.apply(p);h.assertTrue(ItemStack.matches(Accessories.state(p).slots.getItem(0),named),"Return restores exact accessory components");
            var slot=p.inventoryMenu.getSlot(Accessories.FIRST_MENU_SLOT);h.assertTrue(!slot.mayPlace(new ItemStack(Items.DIAMOND))&&slot.getMaxStackSize()==1,"Dedicated slots accept accessories only");
            InventoryExpansion.setTier(p.getInventory(),1);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.inventoryMenu.quickMoveStack(p,Accessories.FIRST_MENU_SLOT);
            h.assertTrue(Accessories.state(p).slots.getItem(0).isEmpty()&&ItemStack.matches(p.getInventory().getItem(43),named),"Shift-removing accessories uses unlocked extra storage when main inventory is full");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void glassesTogglePreservesPotionAndFavoritesProtectSort(GameTestHelper h){
        var p=player(h);try{
            var night=net.minecraft.world.effect.MobEffects.NIGHT_VISION;p.addEffect(new net.minecraft.world.effect.MobEffectInstance(night,1200,0));Accessories.state(p).slots.setItem(0,new ItemStack(Accessories.ITEMS.getFirst()));
            Accessories.action(p,new AccessoryActionPayload(-1,p.inventoryMenu.getStateId()));h.assertTrue(!Accessories.vision(p)&&p.getEffect(night).getDuration()==1200,"Switching off preserves the existing potion");
            p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,3));p.getInventory().setItem(10,new ItemStack(Items.APPLE,4));Accessories.action(p,new AccessoryActionPayload(9,p.inventoryMenu.getStateId()));InventorySorter.sort(p,p.inventoryMenu.getStateId());
            h.assertTrue(p.getInventory().getItem(9).is(Items.DIAMOND)&&p.getInventory().countItem(Items.APPLE)==4,"Favorite remains in place and other items are conserved");
            boolean before=Accessories.state(p).glasses;Accessories.action(p,new AccessoryActionPayload(-1,p.inventoryMenu.getStateId()+1));h.assertTrue(Accessories.state(p).glasses==before,"Stale requests cannot toggle equipment state");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void accessoryDeathDropsOnceAndCopyDoesNotDuplicate(GameTestHelper h){
        var p=player(h);var next=player(h);try{
            Accessories.state(p).slots.setItem(0,new ItemStack(Accessories.ITEMS.get(2)));p.die(p.damageSources().generic());ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(p,next,false);
            h.assertTrue(Accessories.mask(p)==0&&Accessories.mask(next)==0,"Normal death clears equipment in both old and respawned player");
        }finally{remove(p);remove(next);}h.succeed();
    }
    @GameTest public void fishBucketsPreserveSpeciesAndFishingKeepsSpecialLoot(GameTestHelper h){
        for(int i=0;i<6;i++){
            var fish=RegionalFishing.TYPES.get(i).create(h.getLevel(),EntitySpawnReason.COMMAND);fish.setFromBucket(true);fish.setCustomName(Component.literal("Fish "+i));
            h.assertTrue(fish.getBucketItemStack().is(RegionalFishing.BUCKETS.get(i)),"Each swimming species has its own bucket");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());fish.saveWithoutId(out);var copy=RegionalFishing.TYPES.get(i).create(h.getLevel(),EntitySpawnReason.LOAD);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));h.assertTrue(copy.fromBucket()&&copy.getName().getString().equals("Fish "+i),"Bucket persistence and custom name survive");
        }
        var book=new ItemStack(Items.ENCHANTED_BOOK);var named=new ItemStack(Items.COD);named.set(DataComponents.CUSTOM_NAME,Component.literal("Other mod's fish"));var result=RegionalFishing.catches(h.getLevel(),h.absolutePos(BlockPos.ZERO),List.of(book,named));h.assertTrue(ItemStack.matches(result.get(0),book)&&ItemStack.matches(result.get(1),named),"Treasure and customized catches remain untouched");h.succeed();
    }
    @GameTest public void animalHarvestCostsCooldownsAndNextEggPersist(GameTestHelper h){
        var p=player(h);try{
            var goat=EntityTypes.GOAT.create(h.getLevel(),EntitySpawnReason.COMMAND);goat.setPos(p.position());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
            h.assertTrue(AnimalUtility.interact(p,goat,InteractionHand.MAIN_HAND),"Adult goat supplies specialty milk");h.assertTrue(!AnimalUtility.interact(p,goat,InteractionHand.MAIN_HAND)&&p.getMainHandItem().getCount()==1&&p.getInventory().countItem(AnimalUtility.GOAT_MILK)==1,"Cooldown prevents repeat milk and charges nothing");
            var chicken=EntityTypes.CHICKEN.create(h.getLevel(),EntitySpawnReason.COMMAND);chicken.setPos(p.position());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.PUMPKIN_SEEDS,2));h.assertTrue(AnimalUtility.interact(p,chicken,InteractionHand.MAIN_HAND),"Feed queues the next egg");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());chicken.saveWithoutId(out);var restored=EntityTypes.CHICKEN.create(h.getLevel(),EntitySpawnReason.LOAD);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));h.assertTrue(AnimalUtility.state(restored).egg==1,"Feed choice survives native entity reload");
            restored.eggTime=1;restored.aiStep();h.assertTrue(AnimalUtility.state(restored).egg==0&&restored.eggTime>=6000,"One specialty egg consumes the pending choice and restarts the natural laying timer");
            goat.setBaby(true);AnimalUtility.state(goat).readyAt=0;h.assertTrue(!AnimalUtility.interact(p,goat,InteractionHand.MAIN_HAND),"Baby animals do not supply specialty harvests");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void hubRejectsRemoteStorageAndConservesFavoriteDeposits(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(2,2,2));try{
            h.getLevel().setBlockAndUpdate(pos,darkspawn.black.storage.Storage.DUMP.defaultBlockState());var chest=(darkspawn.black.storage.StorageBlockEntity)h.getLevel().getBlockEntity(pos);chest.setItem(0,new ItemStack(Items.APPLE,1));p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()-2);
            h.assertTrue(InventoryHub.accessible(p,pos)!=null,"Visible nearby native storage is accessible");p.getInventory().setItem(9,new ItemStack(Items.APPLE,5));p.getInventory().setItem(10,new ItemStack(Items.APPLE,7));Accessories.state(p).favorites=1;
            InventoryHub.request(p,new HubRequestPayload(2,pos.asLong(),p.inventoryMenu.getStateId()));h.assertTrue(p.getInventory().getItem(9).getCount()==5&&p.getInventory().getItem(10).isEmpty()&&chest.getItem(0).getCount()==8,"Matching deposit preserves favorites and moves exact remaining count");
            p.teleportTo(pos.getX()+30,pos.getY(),pos.getZ());h.assertTrue(InventoryHub.accessible(p,pos)==null,"Remote access rejected");
        }finally{h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());remove(p);}h.succeed();
    }
    @GameTest public void magnetHonorsOwnershipDelayAndFullInventory(GameTestHelper h){
        var p=player(h);var level=h.getLevel();var drops=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        try{
            Accessories.state(p).slots.setItem(0,new ItemStack(Accessories.ITEMS.get(3)));
            for(int i=0;i<3;i++){var drop=new net.minecraft.world.entity.item.ItemEntity(level,p.getX()+1,p.getY()+.3,p.getZ(),new ItemStack(Items.DIAMOND));drop.setNoGravity(true);drops.add(drop);level.addFreshEntity(drop);}
            drops.get(0).setNoPickUpDelay();drops.get(1).setPickUpDelay(40);drops.get(2).setNoPickUpDelay();drops.get(2).setTarget(UUID.randomUUID());
            Accessories.tick(p);h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==1&&!drops.get(1).isRemoved()&&!drops.get(2).isRemoved(),"Magnet respects pickup delay and another owner's item");
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));drops.get(1).setNoPickUpDelay();Accessories.tick(p);h.assertTrue(!drops.get(1).isRemoved(),"Full inventory leaves the drop in the world");
            Accessories.state(p).slots.setItem(0,new ItemStack(Accessories.ITEMS.get(2)));Accessories.tick(p);var fall=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER);h.assertTrue(fall.hasModifier(darkspawn.black.Darkspawn.id("fall_talisman")),"Equipped talisman contributes its modifier");Accessories.state(p).slots.clearContent();Accessories.tick(p);h.assertTrue(!fall.hasModifier(darkspawn.black.Darkspawn.id("fall_talisman")),"Unequipping removes only the accessory modifier");
        }finally{drops.forEach(net.minecraft.world.entity.Entity::discard);remove(p);}h.succeed();
    }
    @GameTest public void pigHuntAndMooshroomHarvestAreFinite(GameTestHelper h){
        var p=player(h);var level=h.getLevel();var patch=h.absolutePos(new BlockPos(2,1,2));
        try{
            level.setBlockAndUpdate(patch.below(),Blocks.PODZOL.defaultBlockState());var pig=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);pig.setPos(Vec3.atCenterOf(patch));var state=AnimalUtility.state(pig);state.forage=patch;state.forageUntil=level.getGameTime()+400;pig.tickCount=20;
            var before=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,pig.getBoundingBox().inflate(3),e->e.getItem().is(AnimalUtility.TRUFFLE)).size();AnimalUtility.tick(pig);AnimalUtility.tick(pig);
            h.assertTrue(state.forage==null&&level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,pig.getBoundingBox().inflate(3),e->e.getItem().is(AnimalUtility.TRUFFLE)).size()==before+1,"Reachable patch awards exactly one truffle");h.assertTrue(level.getBlockState(patch.below()).is(Blocks.PODZOL),"Foraging preserves terrain");
            var cow=EntityTypes.MOOSHROOM.create(level,EntitySpawnReason.COMMAND);cow.setPos(p.position());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOWL,2));h.assertTrue(AnimalUtility.interact(p,cow,InteractionHand.MAIN_HAND)&&!AnimalUtility.interact(p,cow,InteractionHand.MAIN_HAND),"Mooshroom specialty harvest is cooldown limited");h.assertTrue(p.getInventory().countItem(AnimalUtility.FUNGAL_CREAM)==1&&p.getMainHandItem().getCount()==1,"Cream costs one bowl once");
        }finally{level.setBlockAndUpdate(patch.below(),Blocks.AIR.defaultBlockState());remove(p);}h.succeed();
    }

}
