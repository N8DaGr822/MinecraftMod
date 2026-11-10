package darkspawn.black.bonfire;

import darkspawn.black.mount.Griffin;
import darkspawn.black.mount.Mounts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class BonfireGameTest {
	@GameTest public void polishWardAllowsRecruitedCrewAndContacts(GameTestHelper helper) {
		var crew=EntityTypes.PILLAGER.create(helper.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		try {
			helper.assertTrue(BonfireWards.ordinaryHostile(crew),"Wild pillagers remain warded");
			crew.addTag(darkspawn.black.pirate.PirateShip.CREW_TAG);
			crew.addTag(darkspawn.black.pirate.PirateShip.CREW_OWNER+java.util.UUID.randomUUID());
			helper.assertTrue(!BonfireWards.ordinaryHostile(crew),"Recruited crew can enter their captain's ward");
			crew.removeTag(darkspawn.black.pirate.PirateShip.CREW_TAG);
			crew.addTag(darkspawn.black.pirate.PirateDiplomacy.CONTACT_TAG);
			crew.addTag(darkspawn.black.pirate.PirateDiplomacy.FACTION_TAG+darkspawn.black.pirate.PirateProgression.Faction.values()[0].id);
			helper.assertTrue(!BonfireWards.ordinaryHostile(crew),"Friendly faction contacts are not expelled");
		} finally {crew.discard();}helper.succeed();
	}
	private static BlockPos core(GameTestHelper helper) { return core(helper, 0); }
	private static BlockPos core(GameTestHelper helper, int rise) {
		// Keep the ward away from neighboring tests' living players and encounter mobs.
		BlockPos pos = helper.absolutePos(new BlockPos(2, 6 + rise, 1024));
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
			var block = x == 0 && z == 0 ? BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("copper_block")) : Blocks.STONE_BRICKS;
			helper.getLevel().setBlockAndUpdate(pos.offset(x, -1, z), block.defaultBlockState());
		}
		helper.getLevel().setBlockAndUpdate(pos, Bonfires.CORE.defaultBlockState().setValue(BonfireBlock.RANGE, 1));
		return pos;
	}
	private static void enable(GameTestHelper helper, BlockPos pos) {
		helper.getLevel().setBlockAndUpdate(pos, helper.getLevel().getBlockState(pos).setValue(BonfireBlock.PHASE, 1));
		BonfireWards.register(helper.getLevel(), pos);
	}
	private static void remove(GameTestHelper helper, BlockPos pos) { helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
	private static Creeper creeper(GameTestHelper helper, Vec3 pos) {
		var mob = new Creeper(EntityTypes.CREEPER, helper.getLevel()); mob.setPos(pos); return mob;
	}
	@GameTest(skyAccess = true)
	public void foundationActivationAndImmediateRemovalControlSpawnGate(GameTestHelper helper) {
		var pos = core(helper);
		var mob = creeper(helper, Vec3.atBottomCenterOf(pos.offset(3, 0, 0)));
		try {
			helper.assertTrue(!BonfireWards.blocksSpawn(helper.getLevel(), mob), "Inactive bonfire has no ward");
			enable(helper, pos);
			helper.assertTrue(!helper.getLevel().addFreshEntity(mob), "Actual server spawn path rejects an ordinary hostile");
			helper.getLevel().setBlockAndUpdate(pos.offset(1, -1, 1), Blocks.AIR.defaultBlockState());
			helper.assertTrue(!BonfireWards.blocksSpawn(helper.getLevel(), mob), "Breaking foundation disables protection immediately");
		} finally { mob.discard(); remove(helper, pos); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void friendlyMountsAndDeliberateBossesAreNotBlocked(GameTestHelper helper) {
		var pos = core(helper);
		var mount = new Griffin(Mounts.GRIFFIN, helper.getLevel());
		var boss = new WitherBoss(EntityTypes.WITHER, helper.getLevel());
		try {
			enable(helper, pos);
			mount.setPos(Vec3.atBottomCenterOf(pos.offset(3, 0, 0))); boss.setPos(mount.position());
			helper.assertTrue(helper.getLevel().addFreshEntity(mount), "Owned-mount type can enter normally");
			helper.assertTrue(!BonfireWards.blocksSpawn(helper.getLevel(), boss), "Boss summons remain explicit exceptions");
			helper.assertTrue(!BonfireWards.blocksMove(mount, new Vec3(10, 0, 0)), "Friendly movement stays free");
		} finally { mount.discard(); boss.discard(); remove(helper, pos); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void actualMovementRejectsWalkingKnockbackAndFalling(GameTestHelper helper) {
		var pos = core(helper);
		var mob = creeper(helper, Vec3.atBottomCenterOf(pos.offset(10, 0, 0)));
		try {
			helper.getLevel().addFreshEntity(mob); enable(helper, pos);
			Vec3 before = mob.position();
			mob.move(MoverType.SELF, new Vec3(-60, 0, 0));
			helper.assertTrue(mob.position().equals(before), "Fast crossing cannot tunnel through ward");
			mob.move(MoverType.PISTON, new Vec3(-10, 0, 0));
			helper.assertTrue(mob.position().equals(before), "External movement cannot push hostile inside");
			mob.setPos(Vec3.atBottomCenterOf(pos.above(30))); before = mob.position();
			mob.move(MoverType.SELF, new Vec3(0, -40, 0));
			helper.assertTrue(mob.position().equals(before), "Falling hostile cannot enter from above");
		} finally { mob.discard(); remove(helper, pos); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void activationRelocatesLivingHostileWithoutDamageOrLoot(GameTestHelper helper) {
		var pos = core(helper);
		var mob = creeper(helper, Vec3.atBottomCenterOf(pos.offset(2, 0, 0)));
		try {
			helper.getLevel().addFreshEntity(mob);
			// The first safe-search candidate lies east, beyond the ward and explosion margin.
			for (int x = 18; x <= 22; x++) for (int z = -2; z <= 2; z++) {
				helper.getLevel().setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
				for (int y = 0; y < 4; y++) helper.getLevel().setBlockAndUpdate(pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
			}
			enable(helper, pos);
			helper.assertTrue(!BonfireWards.guard(mob), "Safe loaded destination allows clearing");
			helper.assertTrue(!WardVolume.forTier(pos, 1).intersects(mob.getBoundingBox()), "Hostile moved outside");
			helper.assertTrue(mob.isAlive() && mob.getHealth() == mob.getMaxHealth(), "Clearing does not kill or damage");
		} finally { mob.discard(); remove(helper, pos); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void trappedIgnitedCreeperIsPausedBeforeItsFuseTicks(GameTestHelper helper) {
		// Quarantine Fixture: Neighboring tests must not supply a safe relocation platform.
		var pos = core(helper, 64);
		var mob = creeper(helper, Vec3.atBottomCenterOf(pos.above(7)));
		try {
			helper.getLevel().addFreshEntity(mob); mob.ignite(); enable(helper, pos);
			for (int tick = 0; tick < 40; tick++) helper.getLevel().tickNonPassenger(mob);
			helper.assertTrue(mob.isAlive() && mob.tickCount == 0, "Quarantined creeper does not tick or explode");
			helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BonfireBlock.PHASE) == 1, "No false active/safe status");
		} finally { mob.discard(); remove(helper, pos); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void lightsOnlyReplaceAirAndCleanUpWithoutDeletingBuildingBlocks(GameTestHelper helper) {
		var pos = core(helper);
		var light = pos.offset(-6, 4, -6);
		var occupied = pos.offset(2, 4, -6);
		try {
			helper.getLevel().setBlockAndUpdate(light, Blocks.AIR.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(occupied, Blocks.DIAMOND_BLOCK.defaultBlockState());
			enable(helper, pos);
			var entity = (BonfireBlockEntity) helper.getLevel().getBlockEntity(pos);
			for (int tick = 0; tick < 15; tick++) BonfireBlockEntity.tick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), entity);
			helper.assertTrue(helper.getLevel().getBlockState(light).is(Bonfires.LIGHT), "Lattice creates actual block light");
			helper.assertTrue(helper.getLevel().getBlockState(light).getLightEmission() == 15, "Light emits level 15");
			helper.assertTrue(helper.getLevel().getBlockState(occupied).is(Blocks.DIAMOND_BLOCK), "Lighting never overwrites structures");
			remove(helper, pos);
			((WardLightBlock) Bonfires.LIGHT).tick(Bonfires.LIGHT.defaultBlockState(), helper.getLevel(), light, RandomSource.create(1));
			helper.assertTrue(helper.getLevel().getBlockState(light).isAir() && !helper.getLevel().getBlockState(light).is(Bonfires.LIGHT), "Orphan light removed");
			helper.getLevel().setBlockAndUpdate(light, Blocks.GOLD_BLOCK.defaultBlockState());
			((WardLightBlock) Bonfires.LIGHT).tick(Bonfires.LIGHT.defaultBlockState(), helper.getLevel(), light, RandomSource.create(1));
			helper.assertTrue(helper.getLevel().getBlockState(light).is(Blocks.GOLD_BLOCK), "Stale cleanup cannot erase a replacement block");
			helper.assertTrue(helper.getLevel().getBlockState(occupied).is(Blocks.DIAMOND_BLOCK), "Cleanup does not erase player blocks");
		} finally { remove(helper, pos); helper.getLevel().setBlockAndUpdate(occupied, Blocks.AIR.defaultBlockState()); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void overlappingWardKeepsLightsAndProtectionAfterOneCoreBreaks(GameTestHelper helper) {
		var first = core(helper);
		var second = first.offset(4, 0, 0);
		try {
			for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
				helper.getLevel().setBlockAndUpdate(second.offset(x, -1, z), helper.getLevel().getBlockState(first.offset(x, -1, z)));
			}
			helper.getLevel().setBlockAndUpdate(second, Bonfires.CORE.defaultBlockState().setValue(BonfireBlock.RANGE, 1));
			enable(helper, first); enable(helper, second);
			helper.assertTrue(BonfireWards.foundation(helper.getLevel(), second), "Second foundation is valid");
			helper.assertTrue(BonfireWards.covers(helper.getLevel(), Vec3.atCenterOf(first)), "Both cores cover overlap initially");
			BlockPos light = first.above(4);
			helper.getLevel().setBlockAndUpdate(light, Bonfires.LIGHT.defaultBlockState());
			remove(helper, first);
			helper.assertTrue(BonfireWards.covers(helper.getLevel(), Vec3.atCenterOf(light)), "Remaining core covers light after removal: " + helper.getLevel().getBlockState(second));
			((WardLightBlock) Bonfires.LIGHT).tick(Bonfires.LIGHT.defaultBlockState(), helper.getLevel(), light, RandomSource.create(1));
			helper.assertTrue(helper.getLevel().getBlockState(light).is(Bonfires.LIGHT), "Another ward retains shared lighting");
			helper.assertTrue(BonfireWards.covers(helper.getLevel(), Vec3.atCenterOf(first)), "Remaining ward still covers overlap");
			BonfireWards.unload(helper.getLevel(), net.minecraft.world.level.ChunkPos.containing(second));
			helper.assertTrue(!BonfireWards.covers(helper.getLevel(), Vec3.atCenterOf(first)), "Unloaded core no longer protects");
			BonfireWards.register(helper.getLevel(), second);
			helper.assertTrue(BonfireWards.covers(helper.getLevel(), Vec3.atCenterOf(first)), "Chunk-load registration restores coverage");
		} finally { remove(helper, first); remove(helper, second); }
		helper.succeed();
	}
}
