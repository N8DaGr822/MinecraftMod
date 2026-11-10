package darkspawn.black.starter;

import darkspawn.black.Darkspawn;
import darkspawn.black.bonfire.Bonfires;
import darkspawn.black.cooking.*;
import darkspawn.black.armorstyles.ArmorStyles;
import darkspawn.black.legendaryarmor.LegendaryArmor;
import darkspawn.black.expeditiongear.ExpeditionArmor;
import darkspawn.black.boss.BossItems;
import darkspawn.black.boss.BossProfile;
import darkspawn.black.forsaken.ForsakenContent;
import darkspawn.black.mount.MountCompanions;
import darkspawn.black.pirate.*;
import darkspawn.black.worldgen.LandmarkCompass;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Opt-in, repeatable Creative/command testing supplies. Does not change progression. */
public final class TestingCamp extends Item {
	public static final int RADIUS = 10;
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("testing_camp"),
		new TestingCamp(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("testing_camp"))).stacksTo(1).component(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
			Component.translatable("tooltip.darkspawn.testing_camp.area"), Component.translatable("tooltip.darkspawn.testing_camp.use"))))));
	public record Supply(String name, List<ItemStack> items) { }
	private TestingCamp(Properties properties) { super(properties); }
	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(ITEM));
	}
	@Override public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || !player.mayBuild()) return InteractionResult.FAIL;
		if (context.getClickedFace() != Direction.UP) {
			if (context.getLevel() instanceof ServerLevel) player.sendOverlayMessage(Component.translatable("tooltip.darkspawn.testing_camp.use"));
			return InteractionResult.FAIL;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		String error = place(level, context.getClickedPos(), player, context.getItemInHand());
		if (error != null) {
			player.sendSystemMessage(Component.literal(error));
			return InteractionResult.FAIL;
		}
		if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
		player.sendSystemMessage(Component.literal("Testing camp ready! Open the labeled barrels. Cookbook + ingredients test cooking; scrolls unlock recipes. Bonfire starts OFF. Use /summon darkspawn:griffin for flight tests."));
		return InteractionResult.SUCCESS;
	}

	private static void add(List<ItemStack> list, Item item, int count) {
		list.add(new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize())));
	}
	private static List<ItemStack> stacks(int count, Item... items) {
		var result = new ArrayList<ItemStack>();
		for (Item item : items) add(result, item, count);
		return result;
	}
	public static List<Supply> supplies(ServerLevel level) {
		var result = new ArrayList<Supply>();
		result.add(new Supply("Bonfire upgrades", List.of(new ItemStack(Items.IRON_INGOT, 8),
			new ItemStack(Items.GOLD_INGOT, 4), new ItemStack(Items.DIAMOND, 2))));
		var guide = new ItemStack(Items.WRITTEN_BOOK);
		var guidePages = List.of(
			"TESTING CAMP\n\nTake the dragon kit, cookbook and scrolls. Switch to Survival to test normal mechanics. Follow the signs clockwise around the camp. Place another camp in clear ground to restock.",
			"COOKING\n\nUse scrolls to learn recipes. Open the cookbook, then combine the listed ingredients at the Cooking Station. Pantry barrels contain all cooking ingredients; meal barrels provide ready-made samples.",
			"FARMING\n\nAll nine crops are planted. Use seeds and bone meal from the Farming barrel. The central lamp lights the plot at night. Take a bed from Base supplies and place it in the Overworld to set your spawn.",
			"ARMOR\n\nCompare complete iron/diamond styles, expedition gear and legendary sets. Combat tools include a spear, axe, trident, mace and crossbow for testing each weapon bonus.",
			"WORKSHOP\n\nFurnace, smoker and blast furnace have coal. The brewing stand has blaze powder; fill bottles at the water cauldron. Use XP bottles and lapis at the enchanting table. Keep the bookshelf gaps clear.",
			"BASE TESTS\n\nBonfire starts OFF. Look at it for level, range and upgrade help. Empty-hand use toggles; sneak-use upgrades. The barrel beside it has 8 iron, 4 gold and 2 diamonds. Carry these in Survival to test payment. Creative upgrades are free.",
			"SHELTER\n\nThe northern stone workshop blocks rain. Warm up beside its guarded campfire; the bonfire ward itself provides no heat. Cold biomes may still need expedition armor. Keep the ward OFF for ordinary hostile tests.",
			"BONFIRE LEVELS\n\nLevels 1-4 cover 12x12x12, 24x24x12, 32x32x20 and 50x50x30 blocks. Blue flame: activating. Orange: active. No fuel needed. The ward excludes ordinary hostiles, not bosses or incoming projectiles.",
			"GRIFFIN FLIGHT\n\n/summon darkspawn:griffin\n\nRide empty-handed. Default keys: Space rises, X descends, Shift dismounts. Rebind Descend / Land in Controls. Test flight in open space away from the camp.",
			"TRAVEL\n\nOneBlock is in the inventory menu. Quarantine Gates still require dragon completion and a valid frame. The camp does not change progression, time, weather or gamerules.",
			"PIRATE SEAS\n\nPlace a Sea Gate in the Overworld. Take ship parts, timber, ammunition, emeralds and a Logbound Compass. Test launch/dock, cargo, crew and restart with two players. Promotions are earned through harbor contacts; see your Journal.",
			"PARTY CHARTS\n\nLeader: sneak-use Logbound Compass on a nearby player. Invitee: sneak-use yours on the leader within 60 seconds. Six players maximum. Sneak-use in air away from shore services to leave/disband. Rewards and ship access stay personal.",
			"LANDMARKS\n\nSneak-use Landmark Compass to choose one of sixteen boss sites. Use in its dimension to search nearby regions. Retry after traveling if no site is found. Normal rituals and dragon gates still apply. Supplied hearts change progression only when consumed.",
			"FORSAKEN\n\nTake Quarantine armor, Pulse Blade, Bio Scanner, beacon and cores. Test contamination, scanner modes, beacon fuel and recovery in the dimension. Test combat away from the camp.",
			"TAVERN DRINKS\n\nBeer: Haste I for 2 minutes, one emerald. Rum: Resistance I for 1 minute, two emeralds. Buy through Harbor services. Samples are in Pirate supplies. Drinking returns a bowl/bottle; repeating refreshes duration, not strength.",
			"NEW ISLANDS\n\nChart unexplored seas with the Logbound Compass: Redwater Landing, Pearlwater Atoll and Cinderjaw Anchorage. Old generated islands stay unchanged. Pearlwater has no authored sentries; Cinderjaw hides rum and ammunition.",
			"FACTION SKIRMISHES\n\nAt Redwater, Survival visitors trigger four fighters. Watch or help one side. Neutral players are spared. Supporting the winner: +10 standing, +3 renown. Attacking both sides forfeits thanks. Leaving ends the one-time battle; use another landing to retry.",
			"HOME PORTS\n\nCapture Saltwatch Fort, then designate it through Sea Services. Drydock upgrade unlocks ship refits and hiring; tavern upgrade adds supply purchases. Owner only. Moving port resets upgrades; losing the fort removes access.",
            "SHIP CLASSES / CREW\n\nRefit tab: Clipper trades hull for speed; Ironclad trades speed for hull. Care tab: wages and provisions improve morale. Unpaid crew work slower but never desert. Shore leave lets crew walk on clear docked decks; load all sailors before recall/launch.",
            "DISCOVERY CHARTS\n\nLocate a landmark with its compass. Hold that compass in your other hand and use a Blank Discovery Chart. Share or copy the resulting map normally. Charting does not reveal Journal entries or bypass summon rituals.",
            "ARCHAEOLOGY\n\nBrush the two suspicious gravel samples southwest of this camp. Fresh regional landmarks have two supported deposits and themed cache finds. Breaking a deposit loses its find. Brush, charts and Awakening Seals are in barrel 18.",
            "AWAKENED REMATCHES\n\nAfter a normal victory or consumed heart, hold an Awakening Seal OFFHAND during the usual ritual. Every fourth attack becomes three warned strikes: avoid marked circles and ring bands. Win a trophy and laurel; no extra hearts. All ritual requirements still apply.",
            "WOLF HUNT\n\nAfter world dragon victory, visit an open taiga in Survival. /wolfhunt solo: 3 wolves. For 2–4 players: /wolfhunt form, friends /wolfhunt join <leader>, then /wolfhunt start: 6 wolves. Defeat all in 5 minutes; stay alive within 48 blocks. Win: /wolfhunt claim for 4 emeralds each.",
            "REGIONAL EVENTS\n\nAfter dragon completion, eligible habitats occasionally grant a 30-second boon with a local sound/particle cue. Initial wait: 20 minutes. Cooldowns survive reconnect. Server properties control event timing, ambience and naval encounter caps.",
            "MOUNT ROSTER\n\nUse /summon darkspawn:<id> in open space. griffin, woolly_rhino, flying_squirrel, poptart_cat, dragon_mount, winged_unicorn, riding_golem, sasquatch, cerberus, leopard, giant_scorpion, crocodile, seahorse, giant_mole. Seahorse needs water.",
			"INGREDIENT POUCH\n\nFind one in barrel 09. Brewing reagents only, with one bundle of capacity. Left-click to insert; right-click with an empty cursor to remove. Use in hand drops contents. No nested bags, bottles or finished potions. Ingredients must be removed before brewing.",
            "QUIVER\n\nFind a quiver and bow in barrel 10; arrow samples in 11. One bundle capacity, all arrow types. Bows use loose arrows first, then the selected quiver stack. Scroll to select. Infinity saves ordinary arrows only. Crossbows need loose ammunition.",
            "EXPLORER'S PACK\n\nFind a pack and exploration tools in barrel 09. Hold and use the pack to open six slots for maps, compasses, clocks, spyglasses and brushes. Move tools back to your inventory to use them. The pack stays locked while open. No nested storage.",
            "REGIONAL FISHING\n\nSix fish have swimming entities and matching buckets. Release in water. Fish rivers, oceans, jungles, cold water, deep oceans below Y40 and caves below Y0. Native treasure/junk rolls stay unchanged. Cook catches or use the Cooking Station.",
            "ANIMAL UTILITY\n\nSneak-use: carrot on an adult forest pig starts a truffle hunt; pumpkin seeds or wild herbs on a chicken changes its next egg; bottle on goat gives milk; bowl on mooshroom gives cream. Specialty harvest cooldown: five minutes. Ordinary interactions remain.",
            "FACTION VESSELS\n\nCare tab adds Raider, Battleship and Ghostship. Matching membership, rank 3, 60 standing; 64 iron + 16 emeralds. Existing size limits remain. Ammo order selects Chain or Piercing; ammunition must be in the hold.",
            "DOCKED UNLOADING\n\nShip Jobs tab: mark a single chest within 16 blocks, then start unloading with a hired shipworker. Build a dry walking route. All hold cargo can move, including ammunition and repair planks. Stay nearby. Pause/recall before sailing; resume manually after reload.",
            "PATROL BOARDING\n\nDamage an ordinary NPC patrol to 25% hull, stop firing, approach within 24 blocks. Sail > Board patrol or /boarding form. Friends: /boarding join <leader>. Leader: /boarding start. Start moves everyone aboard. Defeat sentries, collect shared cargo, leave the wreck within 60 seconds.",
            "SHADOW INCURSION\n\nAfter the dragon: Survival, Overworld Deep Dark. /shadowincursion solo or form, join <leader>, start. Three drones solo, six cooperative. Dodge warned bursts; no event block destruction. Claim 4 sculk in the Overworld. Normal sculk hazards still apply.",
            "SOUL STORM / END RIFT\n\nAfter the dragon: /soulstorm in Nether Soul Sand Valleys; /endrift on End islands. Solo or form, join <leader>, start. Three foes solo, six cooperative; five minutes within 48 blocks. Return to Overworld to claim 4 soul torches or chorus fruit. Rift particles are not a portal.",
            "GROUND WALKER\n\nBarrel 22 has a mech. Place on clear solid ground in the Overworld, Nether or End. Empty-hand use pilots: WASD, Jump, Sneak to exit. Climbs one-block steps. Iron repairs 4 health every 2 seconds. Land and use a companion capsule to store; veterinary kit restores defeat tokens. No cargo, flight or weapons.",
            "AIRCRAFT PROTOTYPE\n\nBarrel 22 has a core and 9 planks. Build a flat plus sign (optional corners) in a clear 5x5 area on solid ground. Use the core on its center from beside it. Empty-hand use pilots: WASD, Jump up, X down. Land, exit and sneak-use to restore blocks for editing. Capsule recovery works. No cargo or passengers.",
            "MAGIC CARPET\n\nCraft PPP / AMA / PPP: P=purple carpet, A=amethyst shard, M=phantom membrane. Place on solid ground in a normal dimension to bind ownership. Empty-hand use rides; sneak-use opens care. WASD, Jump up, X down, Sneak dismount. Capsule storage and recovery use existing mount rules.",
            "FUNGAL BLOOM\n\nAfter the dragon, visit surface Mushroom Fields in Survival. /fungalbloom solo starts 3 Sporelings. Or form, join <leader>, start for 2-4 players and 6 Sporelings. Five minutes, stay within 48 blocks. Any departure ends it. Claim 4 red mushrooms with /fungalbloom claim. No terrain changes.",
            "FOSSIL RECONSTRUCTION\n\nBarrel 18: four fossil fragments + bone block craft an Ancient Tyrant Fossil. Craft BFB / SSS: B=bone block, F=fossil, S=smooth stone slab. Place the display; break to move it. Fragments come from Fossil Altar brushing; natural Tyrant Skulls provide replacement fossils after the dragon gate.",
            "THEMATIC ENCHANTMENTS\n\nBarrel 22 has rank II samples. Hunter: bows gain +1/+2 damage against ordinary listed hostiles, never bosses. Soulward: worn chest armor adds 1/2 native protection points against undead. Both stack with native enchantments within normal limits.",
            "FOOD ENCHANTMENTS\n\nButcher: melee livestock kills may add one raw meat. Forager: hoe harvesting mature wheat, carrots, potatoes or beetroot may add one produce. Chance: 12.5%/25%. Survival only; no babies, Silk Touch or automated harvest. Enchanting, loot and trades offer all four.",
            "KRAKEN CLUES\n\nBarrel 20: note + compass + ink sac + prismarine shard craft a tide chart. Chart + nautilus shell + dragon breath craft the existing Kraken Sigil. Use notes/charts to read. Notes also come from wreck brushing and open-water Overworld ocean fishing. Original summons and gates remain.",
            "SHORE EXPEDITIONS\n\nCare > Shore expedition: assigned crew follow within 40 blocks of your docked ship. Nearby mate grants Resistance, worker Haste, lookout night vision. Recall everyone before launch. No extra crew slots or moving-deck walking.",
            "CULINARY BREWING\n\nAwkward potion + wild herbs, jungle pepper, goat milk, woodland truffle, marsh rice or frost garlic. Redstone extends; glowstone strengthens the first four. Gunpowder and dragon breath make splash/lingering bottles. Meals stay independent.",
            "WORLD AWAKENING\n\nBoss victories unlock the whole world. Sneak-use a brush on logs, mushrooms or an exposed lightning rod during thunder for renewable rewards, five-minute shared harvest cooldown. Plant Heartwood Seedlings in clear space after the Tree Spirit victory.",
            "DIRE WOLVES\n\nAfter the world's Mutant Wolf victory, offer four cooked beef in your main hand to a normal Dire Wolf. Empty-hand use toggles Follow/Rest. Named ownership survives reload; companions do not teleport or force chunks. Fossil/shadow natural spawn attempts halve after their bosses.",
            "NEXUS GAUNTLET\n\nUse a Nexus Key in the Overworld. Survival captain: world defeated all 16 bosses, 15 consumed hearts, one Nether Star. Up to five friends within 12 blocks opt in by holding a key. Creative captains may rehearse without changing shared boss progression.",
            "NEXUS ROOMS\n\nArrive in the sheltered entry hall. Gather everyone through its north doorway, then /nexusbegin. /nexusstatus and the action bar show your objective, enemies and time. Five waves then the Warden; 20 minutes includes preparation.",
            "NEXUS EXIT / RELICS\n\nUse key or /nexusleave to retreat; /nexusrecover retrieves equipment or rejoins your saved run. Surviving entrants earn one core after the Warden. Test supplies include all four relics. Keep arrival pads and the Overworld entrance clear. Use a disposable test world for rehearsals.",
            "MOUNT CARE\n\nTaming foods, empty companion capsules and veterinary kits are supplied. Test trust, riding, Follow/Stay, capture/release and recovery. Keep named cargo and compare before/after reload; use a separate testing world.",
			"NOAH'S LEGACY\n\nAdmin gift, supplied separately:\n/give @s darkspawn:legacy_of_noah\n\nOpening it binds the relics to you and gives a controls guide. Give it directly to the intended tester. Test Thalassar in open water.",
			"ONEBLOCK ASCENSION\n\nOpen inventory > Ascension for teams, objectives, rewards and automation. Twelve phases reach Frostbound, Tidal Reach, Crystal Depths, Endreach, Astral Forge and Ascension Summit. Separate from legacy OneBlock. Return restores outside possessions; camp supplies do not travel with you.");
		guide.set(DataComponents.WRITTEN_BOOK_CONTENT, new net.minecraft.world.item.component.WrittenBookContent(
			net.minecraft.server.network.Filterable.passThrough("Testing Camp Guide"), "Darkspawn", 0,
			guidePages.stream().map(text -> net.minecraft.server.network.Filterable.passThrough((Component) Component.literal(text))).toList(), true));
		result.add(new Supply("00 START HERE - testing guide", List.of(guide)));
		result.add(new Supply("01 Dragon combat kit", DragonStarterChest.createContents(level)));
		// Use the loaded recipes, including data packs, so the pantry cannot drift from cooking recipes.
		var ingredients = new TreeMap<String, Item>();
		level.getServer().getRecipeManager().getRecipes().forEach(holder -> {
			if (holder.value() instanceof CookingRecipe recipe) recipe.ingredients().forEach(ingredient ->
				ingredient.items().forEach(item -> ingredients.put(BuiltInRegistries.ITEM.getKey(item.value()).toString(), item.value())));
		});
		var pantry = new ArrayList<ItemStack>();
		ingredients.values().forEach(item -> add(pantry, item, 64));
		result.add(new Supply("02 Cooking ingredients", pantry));
		var meals = stacks(8, Cooking.HERB_STEAK, Cooking.HONEY_PORK, Cooking.BEEF_WELLINGTON, Cooking.FISH_CHOWDER,
			Cooking.CHICKEN_CORDON_BLEU, Cooking.WOODLAND_STEW);
		Cuisine.DISHES.values().forEach(dish -> add(meals, dish.item(), 8));
		result.add(new Supply("03 Meals and feasts", meals));
		var scrolls = stacks(1, Cuisine.COOKBOOK);
		Cuisine.SCROLLS.values().forEach(item -> add(scrolls, item, 4));
		result.add(new Supply("04 Cookbook and recipe scrolls", scrolls));
		var crops = stacks(64, Items.BONE_MEAL, Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS);
		darkspawn.black.farming.Agriculture.CROPS.forEach(crop -> { add(crops, crop.seeds(), 64); add(crops, crop.ingredient(), 64); });
		add(crops, Items.DIAMOND_HOE, 1);
		result.add(new Supply("05 Farming", crops));
		var expedition = new ArrayList<ItemStack>();
		List.of(ExpeditionArmor.MOUNTAINEERING, ExpeditionArmor.SPELUNKING, ExpeditionArmor.DIVER,
			ExpeditionArmor.DESERT_WANDERER, ExpeditionArmor.ASHWALKER).forEach(set -> set.values().forEach(item -> add(expedition, item, 1)));
		result.add(new Supply("06 Climate and expedition armor", expedition));
		var legendary = new ArrayList<ItemStack>();
		LegendaryArmor.SETS.values().forEach(set -> set.values().forEach(item -> add(legendary, item, 1)));
		LegendaryArmor.CORES.values().forEach(item -> add(legendary, item, 16));
		result.add(new Supply("07 Legendary armor and cores", legendary));

		result.add(new Supply("08 Crafting and fuel", stacks(64, Items.COAL, Items.IRON_INGOT, Items.COPPER_INGOT, Items.GOLD_INGOT,
			Items.DIAMOND, Items.NETHERITE_INGOT, Items.LEATHER, Items.PAPER, Items.OAK_LOG, Items.OAK_PLANKS, Items.STICK,
			Items.COBBLESTONE, Items.STRING, Items.FLINT, Items.LAPIS_LAZULI, Items.EXPERIENCE_BOTTLE, Items.BOOK,
			Items.BLAZE_POWDER, Items.NETHER_WART, Items.GLASS_BOTTLE, Items.REDSTONE, Items.GLOWSTONE_DUST)));
		var utilities = stacks(16, Bonfires.ITEM, Items.STONE_BRICKS, Items.WATER_BUCKET, Items.CAMPFIRE, Items.TORCH,
			Items.CHEST, Items.OBSIDIAN);
		for (String id : List.of("copper_block", "white_bed", "anvil", "smithing_table")) add(utilities, BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id)), 8);
		for (String id : List.of("dump_chest", "sorting_chest", "sorting_tube", "bank_chest", "quarantine_gate", "containment_key")) add(utilities, BuiltInRegistries.ITEM.getValue(Darkspawn.id(id)), 16);
		add(utilities, darkspawn.black.ingredientpouch.IngredientPouchItem.ITEM, 1);
		add(utilities, darkspawn.black.explorerpack.ExplorerPackItem.ITEM, 1);
		for (var item : List.of(Items.MAP, Items.COMPASS, Items.CLOCK, Items.SPYGLASS, Items.BRUSH)) add(utilities, item, 1);
		result.add(new Supply("09 Base, storage and travel", utilities));
		result.add(new Supply("10 Combat tools", stacks(1, Items.DIAMOND_AXE, Items.DIAMOND_SPEAR, Items.TRIDENT,
			Items.MACE, Items.CROSSBOW, Items.DIAMOND_SHOVEL, Items.FISHING_ROD, Items.BOW, darkspawn.black.quiver.QuiverItem.ITEM)));
        var practice=stacks(16, Items.TARGET, Items.ARMOR_STAND, Items.ARROW, Items.SPECTRAL_ARROW,
			Items.ZOMBIE_SPAWN_EGG, Items.SKELETON_SPAWN_EGG, Items.CREEPER_SPAWN_EGG);
        var tipped=new ItemStack(Items.TIPPED_ARROW,16);
        tipped.set(DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.SLOWNESS));
        practice.add(tipped);result.add(new Supply("11 Target practice",practice));
		var pirate = stacks(64, Pirate.GATE_ITEM, Ships.SHIPYARD_ITEM, Ships.HELM_ITEM, Ships.CANNON_ITEM,
			Ships.SHOT, Ships.FIRE_SHOT, Items.EMERALD, Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.IRON_INGOT);
		add(pirate, LogboundCompass.ITEM, 1);
		add(pirate, PirateDrinks.BEER, 16); add(pirate, PirateDrinks.RUM, 16);
		for (int i = 0; i < 12; i++) add(pirate, Items.OAK_PLANKS, 64);
		result.add(new Supply("12 Pirate ships and voyages", pirate));
		var forsaken = stacks(64, ForsakenContent.ALLOY, ForsakenContent.BIOFIBER, ForsakenContent.CORE,
			ForsakenContent.DATA, ForsakenContent.MODULE, ForsakenContent.BEACON_ITEM);
		add(forsaken, ForsakenContent.BLADE, 1); add(forsaken, ForsakenContent.SCANNER, 1);
		ForsakenContent.ARMOR.values().forEach(item -> add(forsaken, item, 1));
		result.add(new Supply("13 Forsaken equipment", forsaken));
		var summons = stacks(1, LandmarkCompass.ITEM, BossItems.ANCIENT_HEARTWOOD, BossItems.MOONLIT_FANG);
		BossItems.SUMMONS.values().forEach(item -> add(summons, item, 1));
		result.add(new Supply("14 Boss landmarks and summons", summons));
		var hearts = new ArrayList<ItemStack>();
		BossItems.HEARTS.values().forEach(item -> add(hearts, item, 1));
		result.add(new Supply("15 Hearts - consume to unlock", hearts));
		var rituals = new LinkedHashSet<Item>();
		for (var profile : BossProfile.values()) { rituals.add(profile.altar.asItem()); rituals.add(profile.outerMaterial); rituals.add(profile.innerMaterial); }
		var ritualSupplies = new ArrayList<ItemStack>();
		rituals.forEach(item -> add(ritualSupplies, item, 64));
		result.add(new Supply("16 Boss ritual materials", ritualSupplies));
		result.add(new Supply("17 Mount food and care", stacks(64, MountCompanions.CAPSULE, MountCompanions.VET_KIT,
			Items.RABBIT, Items.WHEAT, Items.SWEET_BERRIES, Items.COOKIE, Items.COOKED_BEEF, Items.GOLDEN_APPLE,
			Items.IRON_INGOT, Items.APPLE, Items.BONE, Items.CHICKEN, Items.KELP, Items.SPIDER_EYE, Items.COD, Items.BEETROOT)));
        result.add(new Supply("18 Charts archaeology rematches",stacks(16,darkspawn.black.worldgen.DiscoveryCharts.ITEM,
            darkspawn.black.boss.AwakenedRematches.SEAL,Items.BRUSH,Items.ECHO_SHARD,Items.AMETHYST_SHARD,Items.DIAMOND,Items.BREAD,
            darkspawn.black.worldgen.ExplorationFinds.FOSSIL_FRAGMENT,Items.BONE_BLOCK,Items.SMOOTH_STONE_SLAB)));
        var fish=new ArrayList<ItemStack>();darkspawn.black.fishing.RegionalFishing.RAW.forEach(item->add(fish,item,16));darkspawn.black.fishing.RegionalFishing.COOKED.forEach(item->add(fish,item,16));darkspawn.black.fishing.RegionalFishing.BUCKETS.forEach(item->add(fish,item,1));add(fish,Items.FISHING_ROD,1);
        fish.addAll(stacks(4,darkspawn.black.worldgen.ExplorationFinds.OCEAN_NOTE,Items.COMPASS,Items.INK_SAC,Items.PRISMARINE_SHARD,Items.NAUTILUS_SHELL,Items.DRAGON_BREATH));result.add(new Supply("20 Regional fishing",fish));
        result.add(new Supply("21 Animal husbandry",stacks(16,Items.PIG_SPAWN_EGG,Items.CHICKEN_SPAWN_EGG,Items.GOAT_SPAWN_EGG,Items.MOOSHROOM_SPAWN_EGG,Items.CARROT,Items.PUMPKIN_SEEDS,Items.GLASS_BOTTLE,Items.BOWL,darkspawn.black.farming.Agriculture.WILD_HERBS,darkspawn.black.cooking.AnimalUtility.TRUFFLE,darkspawn.black.cooking.AnimalUtility.GOAT_CHEESE,darkspawn.black.cooking.AnimalUtility.GOAT_MILK,darkspawn.black.cooking.AnimalUtility.FUNGAL_CREAM,darkspawn.black.cooking.AnimalUtility.RICH_EGG,darkspawn.black.cooking.AnimalUtility.HERBED_EGG)));
        // Enchantment Trials: Ready-enchanted equipment shares the ammunition barrel's spare slots.
        var enchantmentTrials=stacks(64,darkspawn.black.pirate.Ships.CHAIN_SHOT,darkspawn.black.pirate.Ships.PIERCING_SHOT);
        enchantmentTrials.add(new ItemStack(darkspawn.black.mount.MagicCarpetItem.ITEM));
        enchantmentTrials.add(new ItemStack(darkspawn.black.mount.AircraftCoreItem.ITEM));
        enchantmentTrials.add(new ItemStack(darkspawn.black.mount.GroundWalkerItem.ITEM));
        enchantmentTrials.add(new ItemStack(Items.OAK_PLANKS,9));
        var trialItems=List.of(Items.BOW,Items.IRON_SWORD,Items.IRON_HOE,Items.IRON_CHESTPLATE);
        var trialEnchantments=List.of(darkspawn.black.enchantments.ThematicEnchantments.HUNTER,darkspawn.black.enchantments.ThematicEnchantments.BUTCHER,darkspawn.black.enchantments.ThematicEnchantments.FORAGER,darkspawn.black.enchantments.ThematicEnchantments.SOULWARD);
        for(int i=0;i<trialItems.size();i++){var stack=new ItemStack(trialItems.get(i));stack.enchant(level.registryAccess().getOrThrow(trialEnchantments.get(i)),2);enchantmentTrials.add(stack);}
        result.add(new Supply("22 Faction ammunition",enchantmentTrials));
        var brews=new ArrayList<ItemStack>();darkspawn.black.cooking.CulinaryBrewing.POTIONS.values().forEach(potion->brews.add(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,potion)));brews.add(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.AWKWARD));add(brews,Items.REDSTONE,32);add(brews,Items.GLOWSTONE_DUST,32);add(brews,Items.GUNPOWDER,32);add(brews,Items.DRAGON_BREATH,16);result.add(new Supply("23 Culinary brewing",brews));
        result.add(new Supply("24 World awakening",stacks(16,darkspawn.black.ecosystem.HeartwoodGrove.SEEDLING_ITEM,darkspawn.black.ecosystem.HeartwoodGrove.LOG_ITEM,Items.BONE_MEAL,Items.COOKED_BEEF,Items.BRUSH,Items.RED_MUSHROOM,Items.LIGHTNING_ROD.weathering().unaffected())));
        result.add(new Supply("25 Nexus and legendary relics",stacks(1,darkspawn.black.endgame.Nexus.KEY,darkspawn.black.endgame.LegendaryRelics.CORE,darkspawn.black.endgame.LegendaryRelics.TITANBREAKER,darkspawn.black.endgame.LegendaryRelics.VOIDBOW,darkspawn.black.endgame.LegendaryRelics.CARAPACE,darkspawn.black.endgame.LegendaryRelics.LANTERN)));
		for (String style : ArmorStyles.STYLES) {
			var armor = new ArrayList<ItemStack>();
			for (Item item : new Item[] {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
				Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}) armor.add(ArmorStyles.apply(new ItemStack(item), style));
			result.add(new Supply("Style: " + style.replace('_', ' '), armor));
		}
		return result;
	}

	public static List<BlockPos> barrelPositions(BlockPos center) {
		var positions = new ArrayList<BlockPos>();
		positions.add(center.offset(-6, 1, -1));
		for (int x = -9; x <= 9; x += 2) positions.add(center.offset(x, 1, -9));
		for (int z = -7; z <= 9; z += 2) positions.add(center.offset(9, 1, z));
		for (int x = 7; x >= -9; x -= 2) positions.add(center.offset(x, 1, 9));
		for (int z = 7; z >= -7; z -= 2) positions.add(center.offset(-9, 1, z));
		// Camp Capacity: Fill the existing perimeter gaps after the original 36 positions; keep the same safe footprint.
		for (int x = -8; x <= 8; x += 2) positions.add(center.offset(x, 1, -9));
		for (int z = -8; z <= 8; z += 2) positions.add(center.offset(9, 1, z));
		for (int x = 8; x >= -8; x -= 2) positions.add(center.offset(x, 1, 9));
		for (int z = 8; z >= -8; z -= 2) positions.add(center.offset(-9, 1, z));
		return positions;
	}

	/** Returns a readable refusal before changing any block. Null means placement succeeded. */
	public static String place(ServerLevel level, BlockPos center, Player player, ItemStack kit) {
		var pages = new ArrayList<Supply>();
		for (Supply supply : supplies(level)) for (int i = 0; i < supply.items().size(); i += 27)
			pages.add(new Supply(supply.name() + (supply.items().size() > 27 ? " / " + (i / 27 + 1) : ""),
				supply.items().subList(i, Math.min(i + 27, supply.items().size()))));
		var barrels = barrelPositions(center);
		if (pages.size() > barrels.size()) return "Too many testing supplies for this camp (data-pack recipes may add more ingredients).";
		var plan = new LinkedHashMap<BlockPos, BlockState>();
		for (int x = -RADIUS; x <= RADIUS; x++) for (int z = -RADIUS; z <= RADIUS; z++) for (int y = 0; y <= 4; y++) {
			var pos = center.offset(x, y, z);
			if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos))
				return "Camp needs a loaded 21 x 21 area inside the world border, with four blocks of headroom.";
			if (player != null && (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, kit)))
				return "You cannot build throughout this camp area.";
			var state = level.getBlockState(pos);
			if (y == 0 && state.isFaceSturdy(level, pos, Direction.UP) && state.getFluidState().isEmpty() && level.getBlockEntity(pos) == null) continue;
			if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || level.getBlockEntity(pos) != null)
				return "Camp blocked at " + pos.toShortString() + ". Choose flat, open ground: 21 x 21 with four blocks of headroom.";
			if (y == 0) plan.put(pos, Blocks.STONE_BRICKS.defaultBlockState());
		}
		// Camp Shelter: Fit a nonflammable workshop inside the existing checked headroom and footprint.
		for (int x = -8; x <= 8; x++) for (int z = -8; z <= 0; z++) {
			plan.put(center.offset(x, 4, z), Blocks.STONE_BRICKS.defaultBlockState());
			if (x == -8 || x == 8 || z == -8) for (int y = 1; y <= 3; y++)
				plan.put(center.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState());
		}
		for (int x : new int[] {-6, 0, 6}) plan.put(center.offset(x, 4, -4), Blocks.SEA_LANTERN.defaultBlockState());
		// Local Heat: A vanilla campfire warms players independently of the initially disabled ward.
		plan.put(center.offset(-7, 1, -4), Blocks.CAMPFIRE.defaultBlockState());
		plan.put(center.offset(-7, 2, -4), Blocks.IRON_BARS.defaultBlockState());
		for (var offset : List.of(new BlockPos(-6, 1, -4), new BlockPos(-7, 1, -5), new BlockPos(-7, 1, -3)))
			plan.put(center.offset(offset), Blocks.IRON_BARS.defaultBlockState());
		Block[] stations = {Blocks.CRAFTING_TABLE, Cooking.STATION, Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE,
			Blocks.STONECUTTER, Blocks.SMITHING_TABLE, Blocks.ANVIL, Blocks.GRINDSTONE, Blocks.BREWING_STAND};
		for (int i = 0; i < stations.length; i++) {
			var station = stations[i].defaultBlockState();
			var facing = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
			if (station.hasProperty(facing)) station = station.setValue(facing, Direction.SOUTH);
			plan.put(center.offset(-5 + i, 1, -6), station);
		}
		plan.put(center.offset(6, 1, -6), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		// Accepted replaceable plants in these gaps must not block bookshelf power.
		for (int x = 4; x <= 6; x++) for (int z = -4; z <= -2; z++) for (int y = 1; y <= 2; y++)
			plan.put(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
		plan.put(center.offset(5, 1, -3), Blocks.ENCHANTING_TABLE.defaultBlockState());
		for (int x = 3; x <= 7; x++) for (int z = -5; z <= -1; z++)
			if (x == 3 || x == 7 || z == -5 || z == -1) if (!(x == 5 && z == -1)) plan.put(center.offset(x, 1, z), Blocks.BOOKSHELF.defaultBlockState());
		for (int x = -5; x <= -3; x++) for (int z = -3; z <= -1; z++) plan.put(center.offset(x, 1, z),
			(x == -4 && z == -2 ? BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_block")) : Blocks.STONE_BRICKS).defaultBlockState());
		plan.put(center.offset(-4, 2, -2), Bonfires.CORE.defaultBlockState());
		for (int x = 2; x <= 6; x++) for (int z = 2; z <= 6; z++) {
			plan.put(center.offset(x, 1, z), (x == 4 && z == 4 ? Blocks.WATER.defaultBlockState() : Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7)));
			if (!(x == 4 && z == 4)) plan.put(center.offset(x, 2, z), darkspawn.black.farming.Agriculture.CROPS.get(((x - 2) * 5 + z - 2) % darkspawn.black.farming.Agriculture.CROPS.size()).block().getStateForAge(0));
		}
		plan.put(center.offset(4, 2, 4), Blocks.SEA_LANTERN.defaultBlockState());
		for(int x:new int[]{-5,-3})plan.put(center.offset(x,1,4),Blocks.SUSPICIOUS_GRAVEL.defaultBlockState());
		for (int x : new int[] {-7, 7}) for (int z : new int[] {-7, 7}) plan.put(center.offset(x, 1, z), Blocks.SEA_LANTERN.defaultBlockState());
		for (int i = 0; i < pages.size(); i++) {
			var pos = barrels.get(i);
			plan.put(pos, Blocks.BARREL.defaultBlockState());
			int rotation = pos.getZ() == center.getZ() - 9 ? 0 : pos.getZ() == center.getZ() + 9 ? 8 : pos.getX() < center.getX() ? 12 : 4;
			plan.put(pos.above(), Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
		}
		for (var entry : plan.entrySet()) if (!entry.getValue().getCollisionShape(level, entry.getKey()).isEmpty()
			&& !level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(entry.getKey()), entity -> entity.isAlive() && !entity.isSpectator()).isEmpty())
			return "A player or entity is in the camp's building area. Move them clear and try again.";
		plan.forEach(level::setBlockAndUpdate);
        for(int x:new int[]{-5,-3})if(level.getBlockEntity(center.offset(x,1,4)) instanceof net.minecraft.world.level.block.entity.BrushableBlockEntity deposit)
            deposit.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,Darkspawn.id("archaeology/"+(x==-5?"farm_excavation":"deep_dark_hive"))),level.getRandom().nextLong());
		for (int x = -3; x <= -1; x++) {
			var furnace = (net.minecraft.world.Container) level.getBlockEntity(center.offset(x, 1, -6));
			furnace.setItem(1, new ItemStack(Items.COAL, 32));
			furnace.setChanged();
		}
		var brewing = (net.minecraft.world.Container) level.getBlockEntity(center.offset(4, 1, -6));
		brewing.setItem(4, new ItemStack(Items.BLAZE_POWDER, 32));
		brewing.setChanged();
		for (int i = 0; i < pages.size(); i++) {
			var barrel = (BarrelBlockEntity) level.getBlockEntity(barrels.get(i));
			var label = new ItemStack(Items.BARREL);
			label.set(DataComponents.CUSTOM_NAME, Component.literal(pages.get(i).name()));
			barrel.applyComponentsFromItemStack(label);
			for (int slot = 0; slot < pages.get(i).items().size(); slot++) barrel.setItem(slot, pages.get(i).items().get(slot).copy());
			barrel.setChanged();
			var sign = (SignBlockEntity) level.getBlockEntity(barrels.get(i).above());
			var lines = new ArrayList<Component>();
			String line = "";
			for (String word : pages.get(i).name().split(" ")) {
				if (!line.isEmpty() && line.length() + word.length() + 1 > 15) { lines.add(Component.literal(line)); line = ""; }
				line += (line.isEmpty() ? "" : " ") + word;
			}
			lines.add(Component.literal(line));
			while (lines.size() < 4) lines.add(Component.empty());
			var text = new SignText(lines.subList(0, 4), lines.subList(0, 4), DyeColor.BLACK, false);
			for (var side : SignTextSlot.values()) sign.setText(text, side);
			sign.setWaxed(true);
		}
		return null;
	}
}
