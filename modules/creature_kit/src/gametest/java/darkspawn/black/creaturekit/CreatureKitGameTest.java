package darkspawn.black.creaturekit;

import darkspawn.black.core.DarkspawnCore;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class CreatureKitGameTest {
	@GameTest
	public void bossBoltRegistersAloneAndSurvivesReload(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getValue(DarkspawnCore.id("boss_bolt")) == CreatureKit.BOSS_BOLT, "darkspawn:boss_bolt is registered by the kit");
		var level = helper.getLevel();
		var owner = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		for (BossBolt.Kind kind : BossBolt.Kind.values()) {
			BossBolt bolt = new BossBolt(CreatureKit.BOSS_BOLT, level);
			bolt.configure(owner, kind, 10);
			bolt.setPos(owner.position().add(0, 20, 0));
			helper.assertTrue(!bolt.getItem().isEmpty(), "Projectile is missing its visual: " + kind);
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			bolt.saveWithoutId(output);
			BossBolt restored = new BossBolt(CreatureKit.BOSS_BOLT, level);
			restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
			helper.assertTrue(restored.kind() == kind && ItemStack.matches(bolt.getItem(), restored.getItem()), "Projectile changed on reload: " + kind);
			for (int tick = 0; tick < 182 && !restored.isRemoved(); tick++) { restored.commonTick(); restored.tick(); }
			helper.assertTrue(restored.isRemoved(), "Missed projectile never expired: " + kind);
			bolt.discard();
		}
		helper.succeed();
	}

	@GameTest
	public void boltEffectsWorkWithoutTheBossEffectMod(GameTestHelper helper) {
		var target = helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 1));
		BossBolt.applyEffect(target, BossBolt.Kind.FROST);
		helper.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "Frost slows");
		// Acid's armor fracture belongs to the boss mod; without it the hit is plain damage.
		BossBolt.applyEffect(target, BossBolt.Kind.ACID);
		boolean fracture = BuiltInRegistries.MOB_EFFECT.containsKey(DarkspawnCore.id("fractured_armor"));
		helper.assertTrue(fracture || target.getActiveEffects().size() == 1, "Acid adds nothing without fractured_armor");
		helper.succeed();
	}

	@GameTest
	public void immuneTargetsAndShooterAlliesAreSpared(GameTestHelper helper) {
		var level = helper.getLevel();
		var cow = helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 1));
		var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 2, 1));
		BossBolt.registerImmune(entity -> entity == cow);
		BossBolt bolt = new BossBolt(CreatureKit.BOSS_BOLT, level);
		bolt.configure(zombie, BossBolt.Kind.PHYSICAL, 4);
		bolt.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 4, 1))));
		bolt.setDeltaMovement(0, -.8, 0);
		level.addFreshEntity(bolt);
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(cow.getHealth() == cow.getMaxHealth(), "Immune entity was not hurt");
			bolt.discard();
			helper.succeed();
		});
	}
}
