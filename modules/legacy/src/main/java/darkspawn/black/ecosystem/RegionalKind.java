package darkspawn.black.ecosystem;

import darkspawn.black.boss.BossProfile;
import java.util.Locale;
import net.minecraft.world.entity.MobCategory;

/** Regional roster: identities are saved as registry IDs, never enum ordinals. */
public enum RegionalKind {
	ROTTED_ZOMBIE(BossProfile.MUTANT_ZOMBIE, Form.HUMANOID, Move.GROUND, Behavior.MELEE, Temper.HOSTILE, 36, .19, 5, 4, .7F, 2F, 8),
	BRUTE_ZOMBIE(BossProfile.MUTANT_ZOMBIE, Form.HUMANOID, Move.GROUND, Behavior.SMASH, Temper.HOSTILE, 48, .2, 7, 4, 1F, 2.4F, 4),
	LEAPING_ZOMBIE(BossProfile.MUTANT_ZOMBIE, Form.HUMANOID, Move.GROUND, Behavior.POUNCE, Temper.HOSTILE, 20, .32, 4, 0, .7F, 2F, 2),
	MUTANT_HUSKLING(BossProfile.MUTANT_ZOMBIE, Form.HUMANOID, Move.GROUND, Behavior.BOULDER, Temper.HOSTILE, 50, .22, 7, 4, 1.1F, 2.5F, 3),
	FAILED_MUTANT(BossProfile.MUTANT_ZOMBIE, Form.HUMANOID, Move.GROUND, Behavior.SMASH, Temper.HOSTILE, 100, .23, 9, 6, 1.5F, 3F, 1),
	BONE_RAPTOR(BossProfile.FOSSIL_TYRANT, Form.BIPED, Move.GROUND, Behavior.POUNCE, Temper.HOSTILE, 24, .33, 5, 2, .9F, 1.4F, 7),
	FOSSIL_SCORPION(BossProfile.FOSSIL_TYRANT, Form.ARTHROPOD, Move.GROUND, Behavior.POISON, Temper.HOSTILE, 30, .23, 5, 6, 1.3F, .7F, 5),
	BONE_VULTURE(BossProfile.FOSSIL_TYRANT, Form.BIRD, Move.AIR, Behavior.SCAVENGE, Temper.HOSTILE, 20, .3, 4, 0, 1.2F, .8F, 4),
	FOSSILIZED_HUSK(BossProfile.FOSSIL_TYRANT, Form.HUMANOID, Move.GROUND, Behavior.MELEE, Temper.HOSTILE, 40, .19, 6, 8, .8F, 2F, 5),
	TYRANT_SKULL(BossProfile.FOSSIL_TYRANT, Form.SKULL, Move.STILL, Behavior.FOSSIL, Temper.PASSIVE, 20, 0, 0, 0, 2.5F, 1.5F, 1),
	STORM_FINCH(BossProfile.THUNDER_BIRD, Form.BIRD, Move.AIR, Behavior.STATIC, Temper.PASSIVE, 8, .34, 0, 0, .45F, .5F, 8),
	SHOCKTALON(BossProfile.THUNDER_BIRD, Form.BIRD, Move.AIR, Behavior.DIVE, Temper.HOSTILE, 26, .32, 5, 0, 1.4F, .9F, 5),
	STORMSTRIDER(BossProfile.THUNDER_BIRD, Form.QUADRUPED, Move.GROUND, Behavior.STATIC, Temper.NEUTRAL, 32, .4, 5, 2, 1.1F, 1.7F, 5),
	THUNDER_ROC(BossProfile.THUNDER_BIRD, Form.BIRD, Move.AIR, Behavior.LIGHTNING, Temper.HOSTILE, 90, .3, 7, 4, 2.2F, 1.8F, 1),
	TREE_VIPER(BossProfile.TITAN_BOA, Form.SERPENT, Move.CLIMB, Behavior.POISON, Temper.HOSTILE, 16, .27, 3, 0, .65F, .4F, 8),
	CONSTRICTOR(BossProfile.TITAN_BOA, Form.SERPENT, Move.GROUND, Behavior.CONSTRICT, Temper.HOSTILE, 32, .25, 5, 2, 1.2F, .6F, 5),
	JUNGLE_STALKER(BossProfile.TITAN_BOA, Form.QUADRUPED, Move.GROUND, Behavior.AMBUSH, Temper.HOSTILE, 30, .32, 6, 2, 1F, 1.1F, 4),
	BROOD_SERPENT(BossProfile.TITAN_BOA, Form.SERPENT, Move.GROUND, Behavior.CONSTRICT, Temper.HOSTILE, 100, .25, 8, 6, 2.4F, 1F, 1),
	BOGLING(BossProfile.BABA_YAGA, Form.HUMANOID, Move.GROUND, Behavior.THIEF, Temper.HOSTILE, 16, .28, 2, 0, .6F, .8F, 7),
	HEXED_FROG(BossProfile.BABA_YAGA, Form.QUADRUPED, Move.GROUND, Behavior.HEX, Temper.HOSTILE, 14, .27, 2, 0, .7F, .5F, 6),
	SWAMP_WISP(BossProfile.BABA_YAGA, Form.ORB, Move.AIR, Behavior.GUIDE, Temper.PASSIVE, 10, .25, 0, 0, .5F, .5F, 4),
	HEXBOUND(BossProfile.BABA_YAGA, Form.HUMANOID, Move.GROUND, Behavior.POTION, Temper.HOSTILE, 28, .22, 4, 2, .7F, 2F, 4),
	BABAS_FAMILIAR(BossProfile.BABA_YAGA, Form.QUADRUPED, Move.GROUND, Behavior.GUIDE, Temper.PASSIVE, 30, .28, 0, 0, .7F, .8F, 1),
	STONEBACK_GOAT(BossProfile.MOUNTAIN_TITAN, Form.QUADRUPED, Move.GROUND, Behavior.CHARGE, Temper.NEUTRAL, 36, .28, 6, 10, 1F, 1.3F, 7),
	CRAG_CRAWLER(BossProfile.MOUNTAIN_TITAN, Form.QUADRUPED, Move.GROUND, Behavior.AMBUSH, Temper.HOSTILE, 26, .26, 5, 8, 1.1F, .6F, 6),
	STONEBORN(BossProfile.MOUNTAIN_TITAN, Form.HUMANOID, Move.GROUND, Behavior.BOULDER, Temper.HOSTILE, 48, .17, 6, 12, 1F, 2.2F, 4),
	TITAN_SPAWN(BossProfile.MOUNTAIN_TITAN, Form.HUMANOID, Move.GROUND, Behavior.BOULDER, Temper.HOSTILE, 120, .18, 9, 14, 2F, 4.5F, 1),
	FROSTLING(BossProfile.ICE_WYRM, Form.HUMANOID, Move.GROUND, Behavior.FROST, Temper.HOSTILE, 16, .24, 3, 2, .6F, .8F, 7),
	FROZEN_HUSK(BossProfile.ICE_WYRM, Form.HUMANOID, Move.GROUND, Behavior.THAW, Temper.HOSTILE, 40, .16, 6, 12, .8F, 2F, 5),
	ICEFANG(BossProfile.ICE_WYRM, Form.QUADRUPED, Move.GROUND, Behavior.FROST, Temper.HOSTILE, 26, .3, 5, 2, .9F, 1F, 6),
	YOUNG_WYRM(BossProfile.ICE_WYRM, Form.SERPENT, Move.GROUND, Behavior.ICE_BREATH, Temper.HOSTILE, 90, .27, 7, 8, 2.5F, .9F, 1),
	ABYSSAL_FISH(BossProfile.KRAKEN, Form.FISH, Move.WATER, Behavior.MELEE, Temper.HOSTILE, 20, .3, 4, 0, .9F, .6F, 7),
	GIANT_CRAB(BossProfile.KRAKEN, Form.ARTHROPOD, Move.WATER, Behavior.SMASH, Temper.NEUTRAL, 40, .2, 6, 14, 1.7F, .8F, 5),
	SIREN(BossProfile.KRAKEN, Form.HUMANOID, Move.WATER, Behavior.SIREN, Temper.HOSTILE, 32, .26, 4, 2, .8F, 1.6F, 3),
	LEVIATHAN_SPAWN(BossProfile.KRAKEN, Form.SERPENT, Move.WATER, Behavior.SEA_GRAB, Temper.HOSTILE, 110, .28, 8, 6, 2.6F, 1.2F, 1),
	CAVE_SKITTERER(BossProfile.CAVE_CRAWLER, Form.ARTHROPOD, Move.CLIMB, Behavior.MELEE, Temper.HOSTILE, 10, .32, 2, 0, .55F, .3F, 8),
	WEB_SPITTER(BossProfile.CAVE_CRAWLER, Form.ARTHROPOD, Move.CLIMB, Behavior.WEB, Temper.HOSTILE, 24, .24, 3, 2, 1F, .6F, 5),
	BROOD_CARRIER(BossProfile.CAVE_CRAWLER, Form.ARTHROPOD, Move.CLIMB, Behavior.BROOD, Temper.HOSTILE, 38, .2, 5, 6, 1.5F, .9F, 3),
	TUNNEL_WIDOW(BossProfile.CAVE_CRAWLER, Form.ARTHROPOD, Move.CLIMB, Behavior.WIDOW, Temper.HOSTILE, 90, .28, 7, 8, 2F, 1F, 1),
	SHADOW_DRONE(BossProfile.SHADOW_CREEPER_QUEEN, Form.CREEPER, Move.GROUND, Behavior.DARK_BURST, Temper.HOSTILE, 24, .24, 4, 2, .7F, 1.6F, 8),
	SHADOW_STALKER(BossProfile.SHADOW_CREEPER_QUEEN, Form.CREEPER, Move.GROUND, Behavior.STALK, Temper.HOSTILE, 28, .34, 6, 2, .7F, 1.8F, 5),
	SHADOW_SPITTER(BossProfile.SHADOW_CREEPER_QUEEN, Form.CREEPER, Move.GROUND, Behavior.ACID, Temper.HOSTILE, 28, .22, 4, 4, .8F, 1.7F, 5),
	SHADOW_GUARDIAN(BossProfile.SHADOW_CREEPER_QUEEN, Form.CREEPER, Move.GROUND, Behavior.SHOCKWAVE, Temper.HOSTILE, 60, .18, 7, 14, 1.2F, 2.2F, 3),
	SHADOW_LARVA(BossProfile.SHADOW_CREEPER_QUEEN, Form.ARTHROPOD, Move.GROUND, Behavior.LARVA, Temper.HOSTILE, 8, .26, 2, 0, .5F, .3F, 3),
	SHADOW_EGG(BossProfile.SHADOW_CREEPER_QUEEN, Form.ORB, Move.STILL, Behavior.EGG, Temper.NEUTRAL, 10, 0, 0, 4, .7F, .9F, 3),
	SHADOW_PRAETORIAN(BossProfile.SHADOW_CREEPER_QUEEN, Form.CREEPER, Move.GROUND, Behavior.SHADOW_CLEAVE, Temper.HOSTILE, 140, .24, 10, 12, 1.8F, 3.2F, 1),
	SPORELING(BossProfile.MYCELIAL_SOVEREIGN, Form.MUSHROOM, Move.GROUND, Behavior.HIVE, Temper.NEUTRAL, 12, .23, 2, 0, .6F, .7F, 8),
	MYCELIUM_CRAWLER(BossProfile.MYCELIAL_SOVEREIGN, Form.ARTHROPOD, Move.GROUND, Behavior.AMBUSH, Temper.HOSTILE, 26, .25, 5, 4, 1.2F, .5F, 5),
	INFECTED_MOOSHROOM(BossProfile.MYCELIAL_SOVEREIGN, Form.QUADRUPED, Move.GROUND, Behavior.SPORE, Temper.HOSTILE, 40, .23, 5, 6, 1.1F, 1.6F, 4),
	SPOREWALKER(BossProfile.MYCELIAL_SOVEREIGN, Form.HUMANOID, Move.GROUND, Behavior.SPORE, Temper.HOSTILE, 30, .2, 5, 2, .8F, 2F, 5),
	MYCELIAL_GUARDIAN(BossProfile.MYCELIAL_SOVEREIGN, Form.MUSHROOM, Move.GROUND, Behavior.MYCELIUM, Temper.NEUTRAL, 110, .18, 8, 10, 2F, 3.5F, 1),
	CRIMSON_RAVAGER(BossProfile.NETHERBORN, Form.QUADRUPED, Move.GROUND, Behavior.CHARGE, Temper.HOSTILE, 50, .27, 7, 8, 1.4F, 1.5F, 6),
	FUNGAL_IMP(BossProfile.NETHERBORN, Form.HUMANOID, Move.GROUND, Behavior.BUFF, Temper.HOSTILE, 18, .3, 3, 0, .6F, .9F, 6),
	BLOODROOT(BossProfile.NETHERBORN, Form.PLANT, Move.STILL, Behavior.VINES, Temper.HOSTILE, 32, 0, 4, 6, 1F, 1.4F, 4),
	WARPED_STALKER(BossProfile.NETHERBORN, Form.HUMANOID, Move.GROUND, Behavior.STARE, Temper.NEUTRAL, 40, .28, 6, 4, .8F, 2.8F, 5),
	WARPED_WISP(BossProfile.NETHERBORN, Form.ORB, Move.AIR, Behavior.BLINK_SPORE, Temper.HOSTILE, 22, .25, 3, 0, .7F, .7F, 5),
	RIFTLING(BossProfile.NETHERBORN, Form.ORB, Move.AIR, Behavior.RIFT, Temper.NEUTRAL, 14, .3, 2, 0, .5F, .5F, 5),
	ASH_GHOUL(BossProfile.NETHERBORN, Form.HUMANOID, Move.GROUND, Behavior.FIRE, Temper.HOSTILE, 28, .24, 4, 4, .8F, 1.6F, 6),
	MAGMA_BRUTE(BossProfile.NETHERBORN, Form.HUMANOID, Move.GROUND, Behavior.MAGMA, Temper.HOSTILE, 60, .18, 7, 12, 1.4F, 2.8F, 4),
	INFERNAL_HOGLIN(BossProfile.NETHERBORN, Form.QUADRUPED, Move.GROUND, Behavior.CHARGE, Temper.HOSTILE, 44, .27, 6, 6, 1.4F, 1.4F, 5),
	CRIMSON_SPAWN(BossProfile.NETHERBORN, Form.QUADRUPED, Move.GROUND, Behavior.CHARGE, Temper.HOSTILE, 120, .25, 9, 10, 2F, 2F, 1),
	WARPED_SPAWN(BossProfile.NETHERBORN, Form.HUMANOID, Move.GROUND, Behavior.BLINK_SPORE, Temper.HOSTILE, 110, .28, 8, 8, 1.4F, 3F, 1),
	INFERNAL_SPAWN(BossProfile.NETHERBORN, Form.HUMANOID, Move.GROUND, Behavior.MAGMA, Temper.HOSTILE, 130, .2, 9, 12, 1.8F, 3F, 1),
	LOST_SOUL(BossProfile.SOULBOUND_COLOSSUS, Form.ORB, Move.AIR, Behavior.SOUL_TOUCH, Temper.HOSTILE, 12, .27, 2, 0, .6F, .8F, 7),
	BONEWALKER(BossProfile.SOULBOUND_COLOSSUS, Form.HUMANOID, Move.GROUND, Behavior.REBUILD, Temper.HOSTILE, 28, .23, 5, 4, .9F, 2F, 6),
	SOULFLAME_SKULL(BossProfile.SOULBOUND_COLOSSUS, Form.SKULL, Move.AIR, Behavior.SOUL_BOLT, Temper.HOSTILE, 22, .24, 4, 2, .8F, .8F, 5),
	TORMENTED(BossProfile.SOULBOUND_COLOSSUS, Form.HUMANOID, Move.STILL, Behavior.VINES, Temper.HOSTILE, 28, 0, 3, 2, .8F, 1F, 5),
	SOUL_KEEPER(BossProfile.SOULBOUND_COLOSSUS, Form.HUMANOID, Move.GROUND, Behavior.SOUL_KEEPER, Temper.HOSTILE, 110, .22, 7, 8, 1.3F, 2.8F, 1),
	VOIDLING(BossProfile.VOID_EYE, Form.ORB, Move.AIR, Behavior.BLINK, Temper.HOSTILE, 12, .3, 3, 0, .5F, .5F, 7),
	END_GRAZER(BossProfile.VOID_EYE, Form.QUADRUPED, Move.GROUND, Behavior.GRAZE, Temper.PASSIVE, 26, .25, 0, 2, 1.2F, 1.5F, 6),
	SHARDLING(BossProfile.VOID_EYE, Form.ORB, Move.AIR, Behavior.DODGE, Temper.NEUTRAL, 28, .25, 4, 8, .9F, 1F, 5),
	VOID_RAY(BossProfile.VOID_EYE, Form.RAY, Move.AIR, Behavior.MELEE, Temper.NEUTRAL, 40, .28, 5, 2, 2.5F, .6F, 3),
	WATCHER(BossProfile.VOID_EYE, Form.EYE, Move.AIR, Behavior.OBSERVE, Temper.HOSTILE, 26, .26, 3, 2, .9F, .9F, 4),
	VOID_SENTINEL(BossProfile.VOID_EYE, Form.EYE, Move.AIR, Behavior.SENTINEL, Temper.HOSTILE, 110, .27, 7, 8, 1.6F, 1.6F, 1);

	public enum Form { HUMANOID, QUADRUPED, BIPED, ARTHROPOD, SERPENT, BIRD, FISH, CREEPER, MUSHROOM, ORB, SKULL, PLANT, RAY, EYE }
	public enum Move { GROUND, CLIMB, AIR, WATER, STILL }
	public enum Temper { HOSTILE, NEUTRAL, PASSIVE }
	public enum Behavior { MELEE, SMASH, POUNCE, BOULDER, POISON, SCAVENGE, FOSSIL, STATIC, DIVE, LIGHTNING,
		CONSTRICT, AMBUSH, THIEF, HEX, GUIDE, POTION, CHARGE, FROST, THAW, ICE_BREATH, SIREN, SEA_GRAB,
		WEB, BROOD, WIDOW, DARK_BURST, STALK, ACID, SHOCKWAVE, LARVA, EGG, SHADOW_CLEAVE, HIVE, SPORE,
		MYCELIUM, BUFF, VINES, STARE, BLINK_SPORE, RIFT, FIRE, MAGMA, SOUL_TOUCH, REBUILD, SOUL_BOLT,
		SOUL_KEEPER, BLINK, GRAZE, DODGE, OBSERVE, SENTINEL }

	public final BossProfile region;
	public final Form form;
	public final Move movement;
	public final Behavior behavior;
	public final Temper temper;
	public final double health, speed, damage, armor;
	public final float width, height;
	public final int weight;

	RegionalKind(BossProfile region, Form form, Move movement, Behavior behavior, Temper temper,
			double health, double speed, double damage, double armor, float width, float height, int weight) {
		this.region = region; this.form = form; this.movement = movement; this.behavior = behavior; this.temper = temper;
		this.health = health; this.speed = speed; this.damage = damage; this.armor = armor;
		this.width = width; this.height = height; this.weight = weight;
	}

	public String id() { return name().toLowerCase(Locale.ROOT); }
	public boolean herald() { return weight == 1; }
	public MobCategory category() { return temper == Temper.HOSTILE ? MobCategory.MONSTER : MobCategory.CREATURE; }
	public boolean ranged() {
		return switch (behavior) {
			case BOULDER, LIGHTNING, POTION, ICE_BREATH, WEB, ACID, SPORE, BLINK_SPORE, SOUL_BOLT, SOUL_KEEPER, OBSERVE, SENTINEL -> true;
			case FROST -> this == FROSTLING;
			default -> false;
		};
	}
	public String rewardId() {
		if (!herald()) { return null; }
		return switch (this) {
			case FAILED_MUTANT -> "mutated_flesh";
			case TYRANT_SKULL -> "ancient_tyrant_fossil";
			case THUNDER_ROC -> "charged_feather";
			case BROOD_SERPENT -> "titan_scale";
			case BABAS_FAMILIAR -> "witch_token";
			case TITAN_SPAWN -> "titan_stone";
			case YOUNG_WYRM -> "wyrm_scale";
			case LEVIATHAN_SPAWN -> "abyssal_scale";
			case TUNNEL_WIDOW -> "brood_fang";
			case SHADOW_PRAETORIAN -> "shadow_membrane";
			case MYCELIAL_GUARDIAN -> "mycelial_heart_fragment";
			case CRIMSON_SPAWN -> "crimson_organ";
			case WARPED_SPAWN -> "warped_organ";
			case INFERNAL_SPAWN -> "infernal_organ";
			case SOUL_KEEPER -> "captured_soul";
			case VOID_SENTINEL -> "watcher_fragment";
			default -> throw new IllegalStateException("Missing herald reward: " + this);
		};
	}
}
