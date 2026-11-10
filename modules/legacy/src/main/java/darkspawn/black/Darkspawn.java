package darkspawn.black;

import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.BossItems;
import darkspawn.black.boss.BossEmpowerment;
import darkspawn.black.health.BossHearts;
import darkspawn.black.cooking.Cooking;
import darkspawn.black.ecosystem.ForestEcosystem;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;

public class Darkspawn implements ModInitializer {
	public static final String MOD_ID = darkspawn.black.core.DarkspawnCore.NAMESPACE;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = darkspawn.black.core.DarkspawnCore.LOGGER;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		darkspawn.black.oneblock.AscensionProtection.initialize();
		ServerTuning.initialize();
        darkspawn.black.audio.DarkspawnSounds.initialize();
		darkspawn.black.boss.BossEffects.initialize();
		BossItems.register();
        darkspawn.black.boss.AwakenedRematches.initialize();
		BossEntities.register();
        darkspawn.black.endgame.LegendaryRelics.initialize();
        darkspawn.black.endgame.Nexus.initialize();
		darkspawn.black.mount.Mounts.initialize();
		darkspawn.black.legacy.NoahLegacy.initialize();
		darkspawn.black.starter.DragonStarterChest.initialize();
		darkspawn.black.boss.Endborn.initializeSpawning();
		BossEmpowerment.initialize();
		BossHearts.initialize();
		Cooking.initialize();
		darkspawn.black.cooking.Cuisine.initialize();
        darkspawn.black.cooking.RegionalFishing.initialize();
        darkspawn.black.cooking.AnimalUtility.initialize();
        darkspawn.black.cooking.CulinaryBrewing.initialize();
		darkspawn.black.cooking.Culinary.initialize();
		darkspawn.black.cooking.VillageKitchen.initialize();
		darkspawn.black.boss.EquipmentPowers.initialize();
		ForestEcosystem.initialize();
        darkspawn.black.ecosystem.WorldAwakening.initialize();
        darkspawn.black.ecosystem.HeartwoodGrove.initialize();
        darkspawn.black.ecosystem.WorldVictoryCondition.initialize();
		darkspawn.black.oneblock.OneBlock.initialize();
		darkspawn.black.oneblock.Ascension.initialize();
		// Recovery Inventory: Restore OneBlock profiles before delivering companion tokens on join.
		darkspawn.black.mount.MountCompanions.initialize();
		darkspawn.black.forsaken.Forsaken.initialize();
		darkspawn.black.pirate.Pirate.initialize();
		darkspawn.black.starter.TestingCamp.initialize();
		darkspawn.black.ecosystem.TaigaEcosystem.initialize();
		darkspawn.black.ecosystem.WolfHunt.initialize();
		darkspawn.black.ecosystem.RegionalEncounters.initialize();
		darkspawn.black.ecosystem.EcosystemEffects.initialize();
		darkspawn.black.ecosystem.RegionalEcosystems.initialize();
		darkspawn.black.ecosystem.RegionalAtmosphere.initialize();
		darkspawn.black.worldgen.BossLandmarkStructure.initialize();
		darkspawn.black.worldgen.LandmarkCompass.initialize();
        darkspawn.black.worldgen.DiscoveryCharts.initialize();
        darkspawn.black.worldgen.ExplorationFinds.initialize();
		darkspawn.black.journal.Journal.initialize();

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return darkspawn.black.core.DarkspawnCore.id(path);
	}
}
