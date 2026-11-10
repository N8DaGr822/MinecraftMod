package darkspawn.black.pirate;

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

/** Pirate crew and contacts register their bonfire ward exemption from the pirate side. */
public final class PirateBonfireWardGameTest {
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
}
