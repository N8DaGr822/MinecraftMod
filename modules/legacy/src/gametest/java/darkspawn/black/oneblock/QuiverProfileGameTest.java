package darkspawn.black.oneblock;

import darkspawn.black.quiver.*;

import com.mojang.authlib.GameProfile;
import darkspawn.black.oneblock.OneBlockItems;
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

/** Container contents survive the OneBlock/Ascension profile codec (cross-mod integration). */
public final class QuiverProfileGameTest {
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
    @GameTest public void profilesAndDeathPreserveQuiver(GameTestHelper h){
        var p=player(h);
        try{
            var q=quiver(tipped(6));p.getInventory().setItem(35,q);
            var bow=new ItemStack(Items.BOW);p.getInventory().setItem(0,bow);h.assertTrue(p.getProjectile(bow).is(Items.TIPPED_ARROW),"Main inventory quiver can feed bow");
            var profile=OneBlockItems.capture(p);var ops=p.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var saved=OneBlockItems.CODEC.encodeStart(ops,profile).getOrThrow();OneBlockItems.EMPTY.apply(p);
            h.assertTrue(p.getProjectile(new ItemStack(Items.BOW)).isEmpty(),"Empty dimension profile has no access to outside quiver");
            OneBlockItems.CODEC.parse(ops,saved).getOrThrow().apply(p);
            h.assertTrue(ItemStack.isSameItemSameComponents(p.getProjectile(p.getMainHandItem()),tipped(1)),"Profile restore retains arrow effects");
            var area=p.getBoundingBox().inflate(8);p.die(p.damageSources().generic());
            var drops=p.level().getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(QuiverItem.ITEM));
            h.assertTrue(drops.size()==1&&count(drops.getFirst().getItem())==6&&p.getInventory().countItem(QuiverItem.ITEM)==0,"Death drops one filled quiver");drops.forEach(ItemEntity::discard);
        }finally{remove(p);}h.succeed();
    }
}
