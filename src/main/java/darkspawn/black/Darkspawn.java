package darkspawn.black;

import darkspawn.black.inventory.InventorySorter;
import darkspawn.black.inventory.InventoryExpansion;
import darkspawn.black.inventory.InventoryUpgradeItem;
import darkspawn.black.inventory.SortInventoryPayload;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.BossItems;
import darkspawn.black.boss.BossEmpowerment;
import darkspawn.black.health.BossHearts;
import darkspawn.black.cooking.Cooking;
import darkspawn.black.ecosystem.ForestEcosystem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Darkspawn implements ModInitializer {
	public static final String MOD_ID = "darkspawn";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		darkspawn.black.audio.DarkspawnSounds.initialize();
		InventoryUpgradeItem.register();
		InventoryExpansion.initialize();
		darkspawn.black.boss.BossEffects.initialize();
		BossItems.register();
		BossEntities.register();
		darkspawn.black.boss.Endborn.initializeSpawning();
		BossEmpowerment.initialize();
		BossHearts.initialize();
		Cooking.initialize();
		darkspawn.black.cooking.Agriculture.initialize();
		darkspawn.black.cooking.Cuisine.initialize();
		darkspawn.black.cooking.Culinary.initialize();
		darkspawn.black.cooking.VillageKitchen.initialize();
		darkspawn.black.boss.EquipmentPowers.initialize();
		ForestEcosystem.initialize();
		darkspawn.black.ecosystem.TaigaEcosystem.initialize();
		darkspawn.black.ecosystem.EcosystemEffects.initialize();
		darkspawn.black.ecosystem.RegionalEcosystems.initialize();
		darkspawn.black.ecosystem.RegionalAtmosphere.initialize();
		darkspawn.black.worldgen.BossLandmarkStructure.initialize();
		PayloadTypeRegistry.serverboundPlay().register(SortInventoryPayload.TYPE, SortInventoryPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SortInventoryPayload.TYPE,
				(payload, context) -> InventorySorter.sort(context.player(), payload.stateId()));

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
