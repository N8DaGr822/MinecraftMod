package darkspawn.black.client.boss;

import darkspawn.black.boss.BossMinion;
import darkspawn.black.boss.BossProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class BiomeBossModel extends EntityModel<BiomeBossRenderState> {
	private final Map<String, ModelPart> animated = new LinkedHashMap<>();
	private final ModelPart egg;
	private final ModelPart appendage;

	public BiomeBossModel(ModelPart root) {
		super(root);
		egg = root.getChild("egg");
		appendage = root.getChild("appendage");
		for (String prefix : new String[] {"leg_", "arm_", "wing_", "segment_", "shield_", "tentacle_"}) {
			for (int i = 0; i < 12; i++) {
				if (root.hasChild(prefix + i)) { animated.put(prefix + i, root.getChild(prefix + i)); }
			}
		}
	}

	public static LayerDefinition createBodyLayer(BossProfile profile) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float h = profile.height;
		float w = profile.width;
		float u = 16 / h;
		switch (profile) {
			case MUTANT_ZOMBIE, MOUNTAIN_TITAN, SOULBOUND_COLOSSUS -> {
				box(root, "body", u, 0, h * 0.35F, 0, w * 0.62F, h * 0.4F, w * 0.4F);
				box(root, "head", u, 0, h * 0.75F, 0, w * 0.48F, h * 0.25F, w * 0.48F);
				limb(root, "leg_0", u, -w * 0.2F, h * 0.35F, 0, w * 0.24F, h * 0.35F, w * 0.3F);
				limb(root, "leg_1", u, w * 0.2F, h * 0.35F, 0, w * 0.24F, h * 0.35F, w * 0.3F);
				limb(root, "arm_0", u, -w * 0.42F, h * 0.73F, 0, w * 0.2F, h * 0.45F, w * 0.25F);
				limb(root, "arm_1", u, w * 0.42F, h * 0.73F, 0, w * 0.2F, h * 0.45F, w * 0.25F);
				if (profile == BossProfile.MOUNTAIN_TITAN) {
					box(root, "crag_left", u, -w * 0.35F, h * 0.7F, 0, 3, 3, 3);
					box(root, "crag_right", u, w * 0.35F, h * 0.7F, 0, 3, 3, 3);
				} else if (profile == BossProfile.SOULBOUND_COLOSSUS) {
					for (int i = 0; i < 5; i++) { box(root, "rib_" + i, u, 0, h * 0.38F + i * 1.5F, -w * 0.23F, w * 0.7F, 0.5F, 0.6F); }
				}
			}
			case FOSSIL_TYRANT -> {
				box(root, "body", u, 0, 5, 1, 6, 6, 7);
				box(root, "neck", u, 0, 8, -2, 4, 4, 4);
				box(root, "head", u, 0, 11, -4, 5, 3, 5);
				box(root, "jaw", u, 0, 9.7F, -4, 4, 0.7F, 5);
				for (int i = 0; i < 5; i++) { box(root, "tooth_" + i, u, (i - 2) * 0.8F, 10.4F, -6.3F, 0.4F, 0.6F, 0.4F); }
				limb(root, "leg_0", u, -2.5F, 6, 1, 2, 6, 3);
				limb(root, "leg_1", u, 2.5F, 6, 1, 2, 6, 3);
				limb(root, "arm_0", u, -3, 9, -2, 0.8F, 2.5F, 1);
				limb(root, "arm_1", u, 3, 9, -2, 0.8F, 2.5F, 1);
				for (int i = 0; i < 3; i++) { box(root, "segment_" + i, u, 0, 4 + i * 0.3F, 4.5F + i, 3 - i * 0.7F, 2, 2); }
			}
			case THUNDER_BIRD -> {
				box(root, "body", u, 0, 2, 0, 4, 4, 5);
				box(root, "head", u, 0, 5, -2, 3, 3, 3);
				box(root, "beak", u, 0, 5.8F, -4, 1.2F, 1.2F, 2);
				box(root, "wing_0", u, -3.5F, 4.5F, 0, 5, 0.8F, 5);
				box(root, "wing_1", u, 3.5F, 4.5F, 0, 5, 0.8F, 5);
				for (int i = 0; i < 3; i++) { box(root, "tail_" + i, u, i - 1, 2, 3.8F, 0.7F, 0.5F, 3); }
				limb(root, "leg_0", u, -1, 2, 0, 0.6F, 2, 1.5F);
				limb(root, "leg_1", u, 1, 2, 0, 0.6F, 2, 1.5F);
			}
			case TITAN_BOA, ICE_WYRM -> {
				for (int i = 0; i < 10; i++) {
					double a = i * Math.PI / 5;
					box(root, "segment_" + i, u, (float) Math.cos(a) * 4.5F, 0.5F + i * 0.12F, (float) Math.sin(a) * 4.5F, 3, 3, 3);
				}
				box(root, "neck", u, 0, 2.5F, -3, 3, h - 5, 3);
				box(root, "head", u, 0, h - 3, -4, 4, 3, 5);
				if (profile == BossProfile.ICE_WYRM) {
					for (int i = 0; i < 5; i++) { box(root, "ice_spine_" + i, u, i - 2, h - 1, -3, 0.4F, 1, 1.5F); }
				}
			}
			case BABA_YAGA -> {
				limb(root, "leg_0", u, -2.5F, 6, 0, 1.2F, 6, 1.2F);
				limb(root, "leg_1", u, 2.5F, 6, 0, 1.2F, 6, 1.2F);
				for (int i = 0; i < 6; i++) { box(root, "claw_" + i, u, (i < 3 ? -2.5F : 2.5F) + (i % 3 - 1) * 0.7F, 0, -1, 0.4F, 0.5F, 3); }
				box(root, "hut", u, 0, 6, 0, 8, 5, 8);
				for (int i = 0; i < 4; i++) { box(root, "roof_" + i, u, 0, 11 + i * 0.75F, 0, 10 - i * 2, 0.75F, 10 - i * 2); }
				box(root, "chimney", u, 2, 11, 2, 1.5F, 4, 1.5F);
				box(root, "witch_head", u, 0, 8, -4.5F, 1.5F, 2, 1.5F);
			}
			case KRAKEN -> {
				box(root, "mantle", u, 0, 1, 0, 8, 8, 8);
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4;
					box(root, "tentacle_" + i, u, (float) Math.cos(a) * 4.5F, 0, (float) Math.sin(a) * 4.5F, 2, 2, 2);
				}
			}
			case CAVE_CRAWLER -> {
				box(root, "abdomen", u, 0, 1, 2, 5, 4, 5);
				box(root, "head", u, 0, 1.5F, -2, 4, 2.5F, 3);
				for (int i = 0; i < 8; i++) {
					limb(root, "leg_" + i, u, (i % 2 == 0 ? -1 : 1) * 3.7F, 3, (i / 2 - 1.5F) * 1.8F, 0.7F, 3, 1);
					box(root, "knee_" + i, u, (i % 2 == 0 ? -1 : 1) * 2.7F, 2.5F, (i / 2 - 1.5F) * 1.8F, 3.5F, 0.6F, 0.8F);
				}
			}
			case SHADOW_CREEPER_QUEEN -> {
				box(root, "carapace", u, 0, 5, 0, 6, 8, 6);
				box(root, "head", u, 0, 12, -1.5F, 5, 4, 5);
				box(root, "crown", u, 0, 15, 0, 9, 3, 3);
				for (int i = 0; i < 6; i++) { limb(root, "leg_" + i, u, (i % 2 == 0 ? -1 : 1) * 4, 5, (i / 2 - 1) * 3, 1.4F, 5, 2); }
				limb(root, "arm_0", u, -3.7F, 12, -2, 1, 6, 1.5F);
				limb(root, "arm_1", u, 3.7F, 12, -2, 1, 6, 1.5F);
				box(root, "segment_0", u, 0, 3, 4.5F, 2, 2, 4);
			}
			case MYCELIAL_SOVEREIGN -> {
				box(root, "stem", u, 0, 4, 0, 5, 12, 5);
				box(root, "cap", u, 0, 15, 0, 14, 3, 14);
				box(root, "cap_top", u, 0, 18, 0, 10, 2, 10);
				for (int i = 0; i < 4; i++) { limb(root, "leg_" + i, u, (i % 2 == 0 ? -1 : 1) * 2.5F, 6, (i < 2 ? -1 : 1) * 2.5F, 2, 6, 2); }
				box(root, "arm_0", u, -4, 10, 0, 4, 1.5F, 2);
				box(root, "arm_1", u, 4, 10, 0, 4, 1.5F, 2);
			}
			case NETHERBORN -> {
				box(root, "body", u, 0, 4, 0, 8, 7, 9);
				box(root, "head", u, 0, 7, -3, 6, 5, 5);
				box(root, "snout", u, 0, 7, -5, 4, 2, 3);
				box(root, "tusk_left", u, -3, 7, -5, 1, 7, 1);
				box(root, "tusk_right", u, 3, 7, -5, 1, 7, 1);
				for (int i = 0; i < 4; i++) { limb(root, "leg_" + i, u, (i % 2 == 0 ? -1 : 1) * 3, 5, i < 2 ? -3 : 3, 2, 5, 2); }
				for (int i = 0; i < 4; i++) { box(root, "shield_" + i, u, (i % 2 == 0 ? -1 : 1) * 4.5F, 6, i < 2 ? -3 : 3, 0.8F, 5, 0.8F); }
			}
			case VOID_EYE -> {
				box(root, "eye", u, 0, 2, 0, 7, 7, 7);
				for (int i = 0; i < 4; i++) {
					double a = i * Math.PI / 2;
					box(root, "shield_" + i, u, (float) Math.cos(a) * 5, 3, (float) Math.sin(a) * 5, 1.5F, 5, 1.5F);
				}
			}
		}
		box(root, "egg", u, 0, 0, 0, w * 0.7F, h * 0.8F, w * 0.7F);
		box(root, "appendage", u, 0, 0, 0, 3, 12, 3);
		return LayerDefinition.create(mesh, 64, 64);
	}

	static void box(PartDefinition root, String name, float unit, float x, float y, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0)
				.addBox(-width * unit / 2, -height * unit, -depth * unit / 2, width * unit, height * unit, depth * unit),
				PartPose.offset(x * unit, 24 - y * unit, z * unit));
	}
	private static void limb(PartDefinition root, String name, float unit, float x, float y, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0)
				.addBox(-width * unit / 2, 0, -depth * unit / 2, width * unit, height * unit, depth * unit),
				PartPose.offset(x * unit, 24 - y * unit, z * unit));
	}
	@Override
	public void setupAnim(BiomeBossRenderState state) {
		super.setupAnim(state);
		boolean asEgg = state.minion && (state.role == BossMinion.Role.EGG.ordinal() || state.role == BossMinion.Role.SOUL_CAGE.ordinal());
		boolean tentacle = state.minion && state.role == BossMinion.Role.TENTACLE.ordinal();
		root().getAllParts().forEach(part -> part.visible = true);
		if (asEgg || tentacle) { root().getAllParts().stream().filter(part -> part != root()).forEach(part -> part.visible = false); }
		egg.visible = asEgg;
		appendage.visible = tentacle;
		if (tentacle) { appendage.zRot = (float) Math.sin(state.ageInTicks * 0.045) * (state.windup > 0 ? 0.35F : 0.1F); }
		int index = 0;
		for (var entry : animated.entrySet()) {
			String name = entry.getKey();
			ModelPart part = entry.getValue();
			float wave = (float) Math.sin(state.walkAnimationPos * 0.6 + index++ * Math.PI) * state.walkAnimationSpeed;
			if (state.profile == BossProfile.MYCELIAL_SOVEREIGN && state.awakening > 50 && (name.startsWith("leg_") || name.startsWith("arm_"))) { part.visible = false; }
			if (name.startsWith("leg_")) { part.xRot = wave * 0.45F; }
			if (name.startsWith("arm_")) { part.xRot = state.windup > 0 ? -1.1F : wave * 0.3F; }
			if (name.startsWith("wing_")) { part.zRot = (name.endsWith("0") ? -1 : 1) * (0.2F + (float) Math.sin(state.ageInTicks * 0.14) * 0.3F); }
			if (name.startsWith("segment_") || name.startsWith("tentacle_")) { part.yRot = (float) Math.sin(state.ageInTicks * 0.05 + index) * 0.16F; }
			if (name.startsWith("shield_")) {
				int shard = Integer.parseInt(name.substring(7));
				float angle = state.ageInTicks * 0.035F + shard * (float) Math.PI / 2;
				float radius = (state.recovery > 0 ? 7 : 5) * 16 / state.profile.height;
				part.x = (float) Math.cos(angle) * radius;
				part.z = (float) Math.sin(angle) * radius;
				part.yRot = -angle;
				if (state.profile == BossProfile.VOID_EYE && state.phase == 3 && shard % 2 == 1) { part.visible = false; }
			}
		}
	}
}
