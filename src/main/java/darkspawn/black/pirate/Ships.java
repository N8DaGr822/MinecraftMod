package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import java.util.Comparator;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;

public final class Ships {
	public static final Block SHIPYARD = block("shipyard"), HELM = block("ship_helm"), CANNON = block("naval_cannon");
	public static final Item SHIPYARD_ITEM = blockItem("shipyard", SHIPYARD), HELM_ITEM = blockItem("ship_helm", HELM), CANNON_ITEM = blockItem("naval_cannon", CANNON);
	public static final Item SHOT = item("solid_shot"), FIRE_SHOT = item("incendiary_shot");
	public static final EntityType<PirateShip> SHIP = Registry.register(BuiltInRegistries.ENTITY_TYPE, Darkspawn.id("pirate_ship"),
		EntityType.Builder.<PirateShip>of(PirateShip::new, MobCategory.MISC).sized(7,7).clientTrackingRange(16).updateInterval(1).noLootTable()
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id("pirate_ship"))));
	public static final EntityType<Cannonball> BALL = Registry.register(BuiltInRegistries.ENTITY_TYPE, Darkspawn.id("cannonball"),
		EntityType.Builder.<Cannonball>of(Cannonball::new, MobCategory.MISC).sized(.35F,.35F).clientTrackingRange(12).updateInterval(1).noLootTable()
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id("cannonball"))));
	public static final MenuType<ShipMenu> MENU = Registry.register(BuiltInRegistries.MENU, Darkspawn.id("ship"), new MenuType<>(ShipMenu::new, FeatureFlags.VANILLA_SET));
	private Ships() { }
	private static Block block(String name) {
		return Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id(name), new ShipStationBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK,Darkspawn.id(name))).strength(3).sound(SoundType.WOOD)));
	}
	private static Item blockItem(String name, Block block) { return Registry.register(BuiltInRegistries.ITEM,Darkspawn.id(name),new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Darkspawn.id(name))).useBlockDescriptionPrefix())); }
	private static Item item(String name) { return Registry.register(BuiltInRegistries.ITEM,Darkspawn.id(name),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Darkspawn.id(name))))); }
	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> { entries.accept(SHIPYARD_ITEM); entries.accept(HELM_ITEM); entries.accept(CANNON_ITEM); entries.accept(SHOT); entries.accept(FIRE_SHOT); });
		PayloadTypeRegistry.serverboundPlay().register(ShipControlsPayload.TYPE,ShipControlsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShipIdentityPayload.TYPE,ShipIdentityPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShipIdentityPayload.TYPE,(payload,context)->{
            if(context.player().containerMenu instanceof ShipMenu menu&&menu.containerId==payload.menuId()&&menu.getStateId()==payload.revision())menu.rename(context.player(),payload.name());
        });
		ServerPlayNetworking.registerGlobalReceiver(ShipControlsPayload.TYPE,(payload,context) -> {
			if (context.player().getVehicle() instanceof PirateShip ship && ship.getId() == payload.entityId()) ship.controls(context.player(),payload.buttons());
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 1200 != 0) return;
			var level = server.getLevel(Pirate.LEVEL); if (level == null) return;
            spawnPatrols(level);
		});
	}
    public static void spawnPatrols(net.minecraft.server.level.ServerLevel level) {
            if(level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS))return;
            int count=0;for(var entity:level.getAllEntities())if(entity instanceof PirateShip vessel&&vessel.enemy()&&vessel.isAlive())count++;
            int attempts=0;
            for (var player : level.players()) {
                if(count>=8||attempts>=2)break;
                if (!player.isAlive()||player.isSpectator()||player.isCreative()||!(player.getVehicle() instanceof PirateShip ship) || !ship.ownedBy(player)||ship.docked() || ship.enemy() || ship.protectedHarbor()) continue;
                if (!level.getEntitiesOfClass(PirateShip.class,ship.getBoundingBox().inflate(192),e->e.enemy()&&e.isAlive()).isEmpty()) continue;
                // Loaded Patrol Budget: At most two attempts per minute and eight loaded NPC vessels.
                attempts++;var pos=ship.blockPosition().offset(96,0,64);
                int choice=level.getRandom().nextInt(4);
                if(PirateShip.spawnPatrol(level,pos,choice==0?null:PirateProgression.Faction.values()[choice-1])!=null)count++;
            }
    }

	public static PirateShip nearby(ServerPlayer player, BlockPos station) {
		return player.level().getEntitiesOfClass(PirateShip.class,new AABB(station).inflate(48),ship -> ship.ownedBy(player))
			.stream().min(Comparator.comparingDouble(ship -> ship.distanceToSqr(station.getX()+.5,station.getY(),station.getZ()+.5))).orElse(null);
	}
	public static void open(ServerPlayer player, BlockPos station) {
		if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(station)) > 36 || !player.isAlive()) return;
		player.openMenu(new SimpleMenuProvider((id,inventory,viewer) -> new ShipMenu(id,inventory,station,nearby(player,station)),Component.literal("Ship & Crew")));
	}
	public static void message(net.minecraft.world.entity.player.Player player, String text) { player.sendOverlayMessage(Component.literal(text)); }
	public static boolean pay(ServerPlayer player, Item item, int count, boolean consume) {
		if (player.getAbilities().instabuild) return true;
		int total = 0; for (int i=0;i<player.getInventory().getContainerSize();i++) if (player.getInventory().getItem(i).is(item)) total += player.getInventory().getItem(i).getCount();
		if (total < count) return false;
		if (consume) for (int i=0;i<player.getInventory().getContainerSize() && count>0;i++) { var stack=player.getInventory().getItem(i); if(stack.is(item)){int take=Math.min(count,stack.getCount());stack.shrink(take);count-=take;} }
		return true;
	}
}
