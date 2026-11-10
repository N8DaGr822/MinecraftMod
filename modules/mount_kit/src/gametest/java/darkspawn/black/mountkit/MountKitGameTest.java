package darkspawn.black.mountkit;

import darkspawn.black.core.DarkspawnCore;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MountKitGameTest {
	@GameTest
	public void sharedItemsMenuAndRecipesLoadAlone(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(DarkspawnCore.id("companion_capsule")) == MountCompanions.CAPSULE, "Capsule is registered");
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(DarkspawnCore.id("veterinary_kit")) == MountCompanions.VET_KIT, "Veterinary kit is registered");
		helper.assertTrue(BuiltInRegistries.MENU.getValue(DarkspawnCore.id("mount_care")) == MountCompanions.MENU, "Care menu is registered");
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : new String[] { "companion_capsule", "veterinary_kit" })
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, DarkspawnCore.id(id))).isPresent(), "Recipe loads: " + id);
		helper.succeed();
	}

	@GameTest
	public void cargoRulesWorkWithoutStorage(GameTestHelper helper) {
		helper.assertTrue(MountCompanions.cargoAllowed(new ItemStack(Items.WHEAT)), "Plain items are cargo");
		helper.assertTrue(MountCompanions.cargoAllowed(new ItemStack(MountCompanions.VET_KIT)), "Kits travel as cargo");
		helper.assertFalse(MountCompanions.cargoAllowed(new ItemStack(MountCompanions.CAPSULE)), "Capsules never nest");
		helper.assertFalse(MountCompanions.cargoAllowed(new ItemStack(Items.SHULKER_BOX)), "Shulker boxes are refused");
		helper.assertFalse(MountCompanions.cargoAllowed(new ItemStack(Items.BUNDLE)), "Bundles are refused");
		helper.assertTrue(MountCompanions.token(new ItemStack(MountCompanions.CAPSULE)).isEmpty(), "An empty capsule has no token");
		helper.succeed();
	}

	@GameTest
	public void registryRecordsAreServerOwnedAndRoundTrip(GameTestHelper helper) {
		var level = helper.getLevel();
		var registry = MountRegistry.get(level);
		helper.assertTrue(registry == MountRegistry.get(level.getServer().overworld()), "Without boundaries, companions are stored with the overworld");
		helper.assertFalse(darkspawn.black.core.hook.TravelBoundary.sharedIsland(level), "No shared island without a boundary mod");
		var entry = new MountRegistry.Entry(UUID.randomUUID(), UUID.randomUUID(), "darkspawn:griffin", new CompoundTag(), true, false);
		String token = registry.store(entry);
		helper.assertTrue(registry.get(token).equals(entry), "Stored record is found by token");
		var encoded = MountRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		var decoded = MountRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		helper.assertTrue(entry.equals(decoded.get(token)), "Record survives a save");
		registry.remove(token);
		helper.assertTrue(registry.get(token) == null, "A token redeems once");
		helper.succeed();
	}
}
