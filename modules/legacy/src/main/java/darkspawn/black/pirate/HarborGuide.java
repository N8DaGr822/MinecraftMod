package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class HarborGuide {
	public static final String BRIGGS_TAG = "darkspawn_briggs";
	public static final BlockPos BOARD_POS = new BlockPos(2,65,3);
	public static final MenuType<HarborMenu> MENU = Registry.register(BuiltInRegistries.MENU, Darkspawn.id("harbor_guide"), new MenuType<>(HarborMenu::new, FeatureFlags.VANILLA_SET));
	public static final Block BOARD = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id("harbor_contract_board"), new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Darkspawn.id("harbor_contract_board"))).strength(2)) {
		@Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
			if(player instanceof ServerPlayer server && inHarbor(player)) open(server,null,pos);
			return InteractionResult.SUCCESS;
		}
	});
	public static final Item BOARD_ITEM = Registry.register(BuiltInRegistries.ITEM,Darkspawn.id("harbor_contract_board"),new BlockItem(BOARD,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Darkspawn.id("harbor_contract_board"))).useBlockDescriptionPrefix()));
	private HarborGuide() { }
	public static void initialize() {
		UseEntityCallback.EVENT.register((player,level,hand,entity,hit) -> {
			if(!entity.entityTags().contains(BRIGGS_TAG)) return InteractionResult.PASS;
			if(player instanceof ServerPlayer server && hand==InteractionHand.MAIN_HAND && inHarbor(player) && player.distanceToSqr(entity)<=36) open(server,entity,null);
			return InteractionResult.SUCCESS;
		});
	}
	public static boolean inHarbor(Player player) {
		return player.isAlive() && !player.isSpectator() && player.level().dimension().equals(Pirate.LEVEL) && player.getX()*player.getX()+player.getZ()*player.getZ()<=96*96;
	}
	private static void open(ServerPlayer player,Entity guide,BlockPos board) {
		player.openMenu(new SimpleMenuProvider((id,inventory,viewer)->new HarborMenu(id,inventory,guide,board),Component.literal(guide==null?"Harbor Contracts":"Old Salt Briggs")));
	}
	public static boolean compass(ServerPlayer player) {
		if(!inHarbor(player))return false;
		var state=Pirate.state(player);
		for(int i=0;i<player.getInventory().getContainerSize();i++) if(player.getInventory().getItem(i).is(LogboundCompass.ITEM)) return false;
		if(player.getInventory().getFreeSlot()<0 || (state.logboundCompassIssued&&!Ships.pay(player,Items.COMPASS,1,false))) return false;
		var compass=new ItemStack(LogboundCompass.ITEM);
		if(!player.getInventory().add(compass)) return false;
		if(state.logboundCompassIssued) Ships.pay(player,Items.COMPASS,1,true);
		state.logboundCompassIssued=true;
		if(state.logboundDestination.isEmpty()) state.logboundDestination=LogboundCompass.FIRST_COVE;
		for(int i=0;i<player.getInventory().getContainerSize();i++) if(player.getInventory().getItem(i).is(LogboundCompass.ITEM)) LogboundCompass.ITEM.inventoryTick(player.getInventory().getItem(i),player.level(),player,null);
		return true;
	}
	public static void observeContract(ServerPlayer player) {
		var state=Pirate.state(player);
		if(state.harborContract!=1 || state.harborSurveyed || !player.isAlive() || player.isSpectator() || !player.level().dimension().equals(Pirate.LEVEL)) return;
		var pos=player.blockPosition();
		if(pos.getX()>=168 && pos.getX()<=216 && pos.getZ()>=-24 && pos.getZ()<=24 && pos.getY()>=60 && pos.getY()<=84) {
			state.harborSurveyed=true;player.sendSystemMessage(Component.literal("Cove survey complete. Return to Briggs or the harbor board for your reward."));
		}
	}
	public static boolean contract(ServerPlayer player,int action) {
		if(!inHarbor(player))return false;
		var state=Pirate.state(player);
		if(action==4 && state.harborContract==0) {state.harborContract=1;state.harborSurveyed=false;return true;}
		if(action==6 && state.harborContract==1) {state.harborContract=0;state.harborSurveyed=false;return true;}
		if(action!=5 || state.harborContract!=1 || !state.harborSurveyed || player.getInventory().getFreeSlot()<0) return false;
		// Contract Reward: Server-owned completion prevents repeated menu packets paying twice.
		state.harborContract=2;SeaLegends.renown(player,5);player.getInventory().add(new ItemStack(Items.EMERALD,4));return true;
	}
	public static final String[] CONTRACT_NAMES={"Cove survey","Wreck salvage","Shrine exploration","Beacon investigation","Corsair cargo","Fleet dispatches","Drowned offering","Cargo recovery","Outlaw patrol hunt","Shrine expedition","Garrick rescue","The First Binding","Beacon bearings","Supply ledger","Lost songbook"};
	public static final String[] CONTRACT_DETAILS={
		"Visit Castaway Cove after accepting, then return for four emeralds.",
		"Inspect the cargo chest aboard The Pale Gull. Return for six emeralds. Shared or already emptied caches count; no loot is consumed.",
		"Examine the relic chest at Tideglass Shrine. Return for eight emeralds. Beware the shrine sentries.",
		"Reach the lantern platform atop Lastlight Beacon. Return for six emeralds. The keeper's warning remains below.",
        "Recover the manifest by inspecting Rootbound Camp cargo. Return for +25 Corsair standing and an 8-emerald first mate offer.",
        "Recover dispatches from Saltwatch Fort cache, held by renegades. Return for +25 Fleet standing and a 4-emerald cannoneer offer.",
        "Bring four prismarine shards and inspect Tideglass Shrine cache to offer them. Return for +50 Drowned standing and an 8-emerald shipworker offer.",
        "Repeat voyage: inspect The Pale Gull cargo chest after accepting. Return for +10 Corsair standing and six emeralds. Ten-minute cooldown after claiming.",
        "Repeat voyage: sink one white-sailed Outlaw Raider with your ship after accepting. Faction patrols and player ships do not count. Reward: +10 Fleet standing, six emeralds; ten-minute cooldown.",
        "Repeat voyage: offer four prismarine shards at Tideglass Shrine cache after accepting. Return for +10 Drowned standing and six emeralds. Ten-minute cooldown.",
        "Personal rescue: visit Saltwatch Fort, inspect its dispatch chest for release papers, then speak to your captive Garrick. Return here to recruit him free aboard a nearby docked ship, or free him without hiring.",
        "Discover the Seven Seals symbol in Tideglass Shrine cache first. Accept, climb Lastlight Beacon, then return for eight emeralds and the next Journal clue. Optional; pays once.",
        "Mara Tidewise, navigator: accept, reach Lastlight Beacon lantern platform, and return. Then hire her free from your docked harbor ship's Refit / Crew page.",
        "Bram Ledger, quartermaster: accept, inspect Rootbound Camp cargo, then return with eight iron ingots and sixteen oak planks. Claim to unlock free harbor recruitment.",
        "Nell Saltvoice, shanty singer: accept, recover her songbook by inspecting The Pale Gull cargo cache, and return. Claim to unlock free harbor recruitment; recorded shanty playback is optional."};
	public static boolean islandContract(ServerPlayer player,int index,int action) {
		if(!inHarbor(player)||index<0||index>=3)return false;
		var state=Pirate.state(player);int bit=1<<index;
		if(action==4&&state.islandContracts[index]==0){state.islandContracts[index]=1;state.islandObjectives&=~bit;return true;}
		if(action==6&&state.islandContracts[index]==1){state.islandContracts[index]=0;state.islandObjectives&=~bit;return true;}
		if(action!=5||state.islandContracts[index]!=1||(state.islandObjectives&bit)==0||player.getInventory().getFreeSlot()<0)return false;
		state.islandContracts[index]=2;SeaLegends.renown(player,5);player.getInventory().add(new ItemStack(Items.EMERALD,index==1?8:6));return true;
	}
}
