package darkspawn.black.mount;

import darkspawn.black.creaturemounts.CreatureMounts;

import darkspawn.black.creaturemounts.Griffin;
import darkspawn.black.bonfire.BonfireBlock;
import darkspawn.black.bonfire.BonfireWards;
import darkspawn.black.bonfire.Bonfires;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Darkspawn mounts move freely through bonfire wards. */
public final class MountBonfireWardGameTest {
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
	@GameTest(skyAccess = true)
	public void friendlyMountsAndDeliberateBossesAreNotBlocked(GameTestHelper helper) {
		var pos = core(helper);
		var mount = new Griffin(CreatureMounts.GRIFFIN, helper.getLevel());
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
}
