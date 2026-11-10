package darkspawn.black.boss;

import darkspawn.black.Darkspawn;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum BossProfile {
	MUTANT_ZOMBIE(8, 14, 1000, Blocks.HAY_BLOCK, Items.ROTTEN_FLESH, Items.IRON_INGOT, "undying_fury"),
	FOSSIL_TYRANT(12, 14, 1200, Blocks.BONE_BLOCK, Items.BONE, Items.DIAMOND, "armor_rend"),
	THUNDER_BIRD(12, 8, 1100, Blocks.COPPER_BLOCK.weathering().unaffected(), Items.FEATHER, Items.LIGHTNING_ROD.weathering().unaffected(), "stormchain"),
	TITAN_BOA(14, 7, 1100, Blocks.MOSS_BLOCK, Items.VINE, Items.EMERALD, "venomfang"),
	BABA_YAGA(10, 15, 1200, Blocks.CAULDRON, Items.FERMENTED_SPIDER_EYE, Items.GHAST_TEAR, "hexweaver"),
	MOUNTAIN_TITAN(12, 20, 1500, Blocks.CHISELED_STONE_BRICKS, Items.IRON_INGOT, Items.AMETHYST_SHARD, "stoneguard"),
	ICE_WYRM(14, 10, 1300, Blocks.BLUE_ICE, Items.SNOWBALL, Items.PRISMARINE_CRYSTALS, "winterbite"),
	KRAKEN(12, 9, 1500, Blocks.PRISMARINE, Items.INK_SAC, Items.NAUTILUS_SHELL, "tidecaller"),
	CAVE_CRAWLER(10, 5, 1100, Blocks.COBWEB, Items.STRING, Items.SPIDER_EYE, "broodmark"),
	SHADOW_CREEPER_QUEEN(12, 18, 2400, Blocks.SCULK, Items.ECHO_SHARD, Items.NETHER_STAR, "shadowbane"),
	MYCELIAL_SOVEREIGN(14, 20, 1400, Blocks.MYCELIUM, Items.RED_MUSHROOM, Items.BROWN_MUSHROOM, "mycelial_mending"),
	NETHERBORN(12, 14, 1500, Blocks.MAGMA_BLOCK, Items.BLAZE_ROD, Items.CRYING_OBSIDIAN, "netherflame"),
	SOULBOUND_COLOSSUS(12, 22, 1700, Blocks.SOUL_SAND, Items.SOUL_LANTERN, Items.WITHER_SKELETON_SKULL, "soul_siphon"),
	VOID_EYE(10, 10, 1800, Blocks.OBSIDIAN, Items.ENDER_EYE, Items.END_CRYSTAL, "voidstep");

	public final float width;
	public final float height;
	public final double health;
	public final Block altar;
	public final Item outerMaterial;
	public final Item innerMaterial;
	public final String power;

	BossProfile(float width, float height, double health, Block altar, Item outerMaterial, Item innerMaterial, String power) {
		this.width = width;
		this.height = height;
		this.health = health;
		this.altar = altar;
		this.outerMaterial = outerMaterial;
		this.innerMaterial = innerMaterial;
		this.power = power;
	}

	public BossKind kind() { return BossKind.valueOf(name()); }
	public String id() { return kind().id(); }
	public TagKey<Biome> biomes() { return TagKey.create(Registries.BIOME, Darkspawn.id(id() + "_biomes")); }
	public boolean flying() { return this == THUNDER_BIRD || this == VOID_EYE; }
	public boolean underground() { return this == CAVE_CRAWLER || this == SHADOW_CREEPER_QUEEN; }
	public boolean fireproof() { return this == NETHERBORN || this == SOULBOUND_COLOSSUS; }

	public ResourceKey<Level> dimension() {
		return fireproof() ? Level.NETHER : this == VOID_EYE ? Level.END : Level.OVERWORLD;
	}

	public BossEvent.BossBarColor color() {
		return switch (this) {
			case THUNDER_BIRD, FOSSIL_TYRANT -> BossEvent.BossBarColor.YELLOW;
			case ICE_WYRM, KRAKEN -> BossEvent.BossBarColor.BLUE;
			case SHADOW_CREEPER_QUEEN, VOID_EYE, BABA_YAGA -> BossEvent.BossBarColor.PURPLE;
			case TITAN_BOA, MYCELIAL_SOVEREIGN, MUTANT_ZOMBIE -> BossEvent.BossBarColor.GREEN;
			case MOUNTAIN_TITAN, SOULBOUND_COLOSSUS -> BossEvent.BossBarColor.WHITE;
			default -> BossEvent.BossBarColor.RED;
		};
	}

	public List<BossAttack> attacks(int phase, int variant) {
		// Encounter Identity: Each phase changes the available mechanics, not just damage numbers.
		return switch (this) {
			case MUTANT_ZOMBIE -> phase == 1 ? List.of(BossAttack.SLAM, BossAttack.BOULDERS)
					: phase == 2 ? List.of(BossAttack.SLAM, BossAttack.BOULDERS, BossAttack.BROOD)
					: List.of(BossAttack.CHARGE, BossAttack.BOULDERS, BossAttack.SLAM);
			case FOSSIL_TYRANT -> phase == 1 ? List.of(BossAttack.BITE, BossAttack.SWEEP, BossAttack.BOULDERS)
					: phase == 2 ? List.of(BossAttack.CHARGE, BossAttack.SWEEP, BossAttack.BOULDERS)
					: List.of(BossAttack.CHARGE, BossAttack.BITE, BossAttack.ERUPTION, BossAttack.BOULDERS);
			case THUNDER_BIRD -> phase == 1 ? List.of(BossAttack.LIGHTNING, BossAttack.VOLLEY)
					: phase == 2 ? List.of(BossAttack.LIGHTNING, BossAttack.WIND, BossAttack.VOLLEY)
					: List.of(BossAttack.STORM, BossAttack.WIND, BossAttack.VOLLEY);
			case TITAN_BOA -> phase == 1 ? List.of(BossAttack.BITE, BossAttack.VENOM)
					: phase == 2 ? List.of(BossAttack.CONSTRICT, BossAttack.VENOM, BossAttack.BROOD)
					: List.of(BossAttack.CHARGE, BossAttack.CONSTRICT, BossAttack.VENOM);
			case BABA_YAGA -> phase == 1 ? List.of(BossAttack.HEX, BossAttack.SLAM)
					: phase == 2 ? List.of(BossAttack.HEX, BossAttack.MIASMA, BossAttack.BROOD)
					: List.of(BossAttack.BLINK, BossAttack.HEX, BossAttack.MIASMA);
			case MOUNTAIN_TITAN -> phase == 1 ? List.of(BossAttack.SLAM, BossAttack.BOULDERS)
					: phase == 2 ? List.of(BossAttack.ERUPTION, BossAttack.BOULDERS, BossAttack.SWEEP)
					: List.of(BossAttack.FAULTLINE, BossAttack.BOULDERS, BossAttack.ERUPTION);
			case ICE_WYRM -> phase == 1 ? List.of(BossAttack.FROST, BossAttack.BITE)
					: phase == 2 ? List.of(BossAttack.BURROW, BossAttack.FROST, BossAttack.BLIZZARD)
					: List.of(BossAttack.BURROW, BossAttack.BLIZZARD, BossAttack.FROST);
			case KRAKEN -> phase == 1 ? List.of(BossAttack.WAVE, BossAttack.INK)
					: phase == 2 ? List.of(BossAttack.WHIRLPOOL, BossAttack.WAVE, BossAttack.INK)
					: List.of(BossAttack.WHIRLPOOL, BossAttack.WATER_JET, BossAttack.WAVE);
			case CAVE_CRAWLER -> phase == 1 ? List.of(BossAttack.WEB, BossAttack.BITE)
					: phase == 2 ? List.of(BossAttack.WEB, BossAttack.BROOD, BossAttack.POUNCE)
					: List.of(BossAttack.POUNCE, BossAttack.VENOM, BossAttack.WEB);
			case SHADOW_CREEPER_QUEEN -> phase == 1 ? List.of(BossAttack.ACID, BossAttack.SWEEP, BossAttack.SHADOW)
					: phase == 2 ? List.of(BossAttack.ACID, BossAttack.BROOD, BossAttack.SHADOW)
					: List.of(BossAttack.SHADOW_STORM, BossAttack.ACID, BossAttack.CHARGE, BossAttack.SHADOW);
			case MYCELIAL_SOVEREIGN -> phase == 1 ? List.of(BossAttack.SPORES, BossAttack.ROOTS)
					: phase == 2 ? List.of(BossAttack.SPORES, BossAttack.ROOTS, BossAttack.FUNGAL_GROWTH)
					: List.of(BossAttack.FUNGAL_GROWTH, BossAttack.SPORES, BossAttack.ERUPTION);
			case NETHERBORN -> variant == 1 ? (phase == 1 ? List.of(BossAttack.BLINK, BossAttack.FLAMES)
					: List.of(BossAttack.BLINK, BossAttack.CONSTRICT, BossAttack.FLAMES))
					: phase == 1 ? List.of(BossAttack.CHARGE, BossAttack.FLAMES)
					: phase == 2 ? List.of(BossAttack.CHARGE, BossAttack.MAGMA, BossAttack.BROOD)
					: List.of(BossAttack.MAGMA, BossAttack.CHARGE, BossAttack.FLAMES);
			case SOULBOUND_COLOSSUS -> phase == 1 ? List.of(BossAttack.SOUL_DRAIN, BossAttack.VOLLEY)
					: phase == 2 ? List.of(BossAttack.SOUL_DRAIN, BossAttack.SOUL_CAGES, BossAttack.BEAM)
					: List.of(BossAttack.SOUL_DRAIN, BossAttack.BEAM, BossAttack.SOUL_STORM);
			case VOID_EYE -> phase == 1 ? List.of(BossAttack.BEAM, BossAttack.VOLLEY)
					: phase == 2 ? List.of(BossAttack.BEAM, BossAttack.PORTAL, BossAttack.BROOD)
					: List.of(BossAttack.PORTAL, BossAttack.VOID_RIFTS, BossAttack.BEAM);
		};
	}
}
