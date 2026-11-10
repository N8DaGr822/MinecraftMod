package darkspawn.black.quiver;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;

public final class QuiverGameTest {
	private static ServerPlayer player(GameTestHelper h) {
		var level = h.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "quiver-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.teleportTo(h.absolutePos(net.minecraft.core.BlockPos.ZERO).getX(), h.absolutePos(net.minecraft.core.BlockPos.ZERO).getY() + 2,
			h.absolutePos(net.minecraft.core.BlockPos.ZERO).getZ());
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		return player;
	}

	private static void remove(ServerPlayer p) {
		p.level().getServer().getPlayerList().remove(p);
		p.discard();
	}

    private static ItemStack quiver(ItemStack... arrows){
        var stack=new ItemStack(QuiverItem.ITEM);stack.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(java.util.Arrays.stream(arrows).map(net.minecraft.world.item.ItemStackTemplate::fromNonEmptyStack).toList()));return stack;
    }
    private static int count(ItemStack stack){return stack.get(DataComponents.BUNDLE_CONTENTS).itemCopies().mapToInt(ItemStack::getCount).sum();}
    private static ItemStack tipped(int count){
        var stack=new ItemStack(Items.TIPPED_ARROW,count);
        stack.set(DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.POISON));
        stack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Test venom"));
        return stack;
    }
    private static java.util.List<net.minecraft.world.entity.projectile.arrow.AbstractArrow> arrows(ServerPlayer p){
        return p.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,p.getBoundingBox().inflate(12),a->a.getOwner()==p);
    }
    private static void shoot(GameTestHelper h,ServerPlayer p){
        var bow=p.getMainHandItem();bow.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(bow.getItem().releaseUsing(bow,p.level(),p,71980),"Charged native bow releases");p.stopUsingItem();
    }
    @GameTest public void allArrowTypesUseBundleCapacityAndRejectNesting(GameTestHelper h){
        var p=player(h);
        try{
            var q=quiver(new ItemStack(Items.ARROW,60));var item=(QuiverItem)q.getItem();var container=new SimpleContainer(1);var slot=new Slot(container,0,0,0);
            container.setItem(0,q);var tips=tipped(8);
            item.overrideOtherStackedOnMe(q,tips,slot,ClickAction.PRIMARY,p,SlotAccess.of(()->ItemStack.EMPTY,s->{}));
            h.assertTrue(count(q)==64&&tips.getCount()==4,"Tipped arrows share the 64-arrow capacity");
            for(var invalid:new net.minecraft.world.item.Item[]{Items.APPLE,Items.FIREWORK_ROCKET,Items.BUNDLE,Items.SHULKER_BOX,QuiverItem.ITEM}){
                var stack=new ItemStack(invalid);h.assertTrue(!QuiverItem.accepts(stack),"Invalid content rejected");
                item.overrideOtherStackedOnMe(q,stack,slot,ClickAction.PRIMARY,p,SlotAccess.of(()->ItemStack.EMPTY,s->{}));
                h.assertTrue(stack.getCount()==1&&count(q)==64,"Rejection consumes nothing");
            }
            var disguised=new ItemStack(Items.ARROW);disguised.set(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.EMPTY);
            h.assertTrue(!QuiverItem.accepts(disguised)&&QuiverItem.accepts(new ItemStack(Items.SPECTRAL_ARROW)),"Storage components cannot disguise nested inventory");
            h.assertTrue(!BundleContents.canItemBeInBundle(q)&&!new net.minecraft.world.inventory.ShulkerBoxSlot(container,0,0,0).mayPlace(q),"Menus reject nesting");
            var shulker=new net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity(net.minecraft.core.BlockPos.ZERO,net.minecraft.world.level.block.Blocks.SHULKER_BOX.defaultBlockState());
            h.assertTrue(!shulker.canPlaceItemThroughFace(0,q,net.minecraft.core.Direction.UP),"Hopper cannot nest quiver");
            var empty=quiver();container.setItem(0,new ItemStack(Items.SPECTRAL_ARROW,3));
            item.overrideStackedOnOther(empty,slot,ClickAction.PRIMARY,p);
            h.assertTrue(container.isEmpty()&&count(empty)==3,"Reverse insertion accepts spectral arrows");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void selectionLoosePriorityAndCrossbowIsolation(GameTestHelper h){
        var p=player(h);
        try{
            var bow=new ItemStack(Items.BOW);p.getInventory().setItem(0,bow);
            var q=quiver(new ItemStack(Items.ARROW,4),tipped(3));var selected=q.get(DataComponents.BUNDLE_CONTENTS).asMutable();selected.toggleSelectedItem(1);q.set(DataComponents.BUNDLE_CONTENTS,selected.toImmutable());p.getInventory().setItem(9,q);
            h.assertTrue(p.getProjectile(bow).is(Items.TIPPED_ARROW)&&count(q)==7,"Reading selected ammunition does not consume");
            p.getInventory().setItem(10,new ItemStack(Items.SPECTRAL_ARROW,2));
            h.assertTrue(p.getProjectile(bow)==p.getInventory().getItem(10),"Loose ammunition takes priority");
            p.getInventory().setItem(10,ItemStack.EMPTY);
            h.assertTrue(p.getProjectile(new ItemStack(Items.CROSSBOW)).isEmpty(),"Crossbows do not draw quiver ammunition");
            shoot(h,p);
            h.assertTrue(count(q)==6&&q.get(DataComponents.BUNDLE_CONTENTS).itemCopies().findFirst().orElseThrow().is(Items.TIPPED_ARROW),"Fired stack remains first after synchronization");
            var fired=arrows(p);h.assertTrue(fired.size()==1&&ItemStack.isSameItemSameComponents(fired.getFirst().getPickupItemStackOrigin(),tipped(1)),"Actual projectile preserves potion and name");fired.forEach(net.minecraft.world.entity.Entity::discard);
            var ops=p.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var loaded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,q).getOrThrow()).getOrThrow();p.getInventory().setItem(9,loaded);
            h.assertTrue(p.getProjectile(bow).is(Items.TIPPED_ARROW),"Arrow preference survives save through content ordering");
        }finally{arrows(p).forEach(net.minecraft.world.entity.Entity::discard);remove(p);}h.succeed();
    }
    @GameTest public void actualShotsConsumeOneAndPreserveArrowKinds(GameTestHelper h){
        var p=player(h);
        try{
            for(var ammo:new ItemStack[]{new ItemStack(Items.ARROW,3),new ItemStack(Items.SPECTRAL_ARROW,3),tipped(3)}){
                var bow=new ItemStack(Items.BOW);p.getInventory().setItem(0,bow);var q=quiver(ammo);p.getInventory().setItem(9,q);
                shoot(h,p);var fired=arrows(p);
                h.assertTrue(count(q)==2&&bow.getDamageValue()==1&&fired.size()==1,"One shot consumes one arrow and one bow durability");
                h.assertTrue(ItemStack.isSameItemSameComponents(ammo,fired.getFirst().getPickupItemStackOrigin()),"Projectile retains arrow kind and components");
                h.assertTrue(fired.getFirst().pickup==net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED,"Paid arrow remains recoverable");
                fired.forEach(net.minecraft.world.entity.Entity::discard);
            }
        }finally{arrows(p).forEach(net.minecraft.world.entity.Entity::discard);remove(p);}h.succeed();
    }
    @GameTest public void infinityAndCreativeFollowNativeCosts(GameTestHelper h){
        var p=player(h);
        try{
            var bow=new ItemStack(Items.BOW);bow.enchant(p.registryAccess().getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY),1);p.getInventory().setItem(0,bow);
            for(var ammo:new ItemStack[]{new ItemStack(Items.ARROW,3),new ItemStack(Items.SPECTRAL_ARROW,3),tipped(3)}){
                var q=quiver(ammo);p.getInventory().setItem(9,q);shoot(h,p);var fired=arrows(p);
                boolean free=ammo.is(Items.ARROW);
                h.assertTrue(count(q)==(free?3:2),"Infinity conserves only ordinary arrows");
                h.assertTrue(fired.size()==1&&fired.getFirst().pickup==(free?net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.CREATIVE_ONLY:net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED),"Infinity pickup rule preserved");
                fired.forEach(net.minecraft.world.entity.Entity::discard);
            }
            p.getAbilities().instabuild=true;var q=quiver(tipped(3));p.getInventory().setItem(9,q);shoot(h,p);
            h.assertTrue(count(q)==3&&arrows(p).getFirst().getPickupItemStackOrigin().is(Items.TIPPED_ARROW),"Creative uses selected arrow without consuming");
        }finally{arrows(p).forEach(net.minecraft.world.entity.Entity::discard);remove(p);}h.succeed();
    }
    @GameTest public void canceledDrawMissingQuiverAndFullInventoryAreSafe(GameTestHelper h){
        var p=player(h);
        try{
            var bow=new ItemStack(Items.BOW);var q=quiver(new ItemStack(Items.ARROW,3));p.getInventory().setItem(0,bow);p.getInventory().setItem(9,q);
            bow.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(!bow.getItem().releaseUsing(bow,p.level(),p,72000)&&count(q)==3&&arrows(p).isEmpty(),"Uncharged release is free");p.stopUsingItem();
            bow.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);p.getInventory().setItem(9,ItemStack.EMPTY);
            h.assertTrue(!bow.getItem().releaseUsing(bow,p.level(),p,71980)&&count(q)==3&&arrows(p).isEmpty(),"Removed quiver cannot provide phantom ammunition");p.stopUsingItem();
            for(int i=1;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.getInventory().setItem(9,q);shoot(h,p);
            h.assertTrue(count(q)==2&&arrows(p).size()==1,"Full inventory does not require spilling ammunition");
        }finally{arrows(p).forEach(net.minecraft.world.entity.Entity::discard);remove(p);}h.succeed();
    }}
