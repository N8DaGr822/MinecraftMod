package darkspawn.black.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import darkspawn.black.boss.BossMinion;
import darkspawn.black.boss.BossProfile;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

public final class BossAccentLayer extends RenderLayer<BiomeBossRenderState, BiomeBossModel> {
	private static final Identifier GLOW = Identifier.withDefaultNamespace("textures/block/sea_lantern.png");
	private final EntityModel<BiomeBossRenderState> accents;

	public BossAccentLayer(BiomeBossRenderer renderer, BossProfile profile) {
		super(renderer);
		MeshDefinition mesh = new MeshDefinition();
		float h = profile.height;
		float u = 16 / h;
		float y = h * 0.82F;
		float z = -profile.width * 0.25F;
		switch (profile) {
			case FOSSIL_TYRANT -> { y = 12; z = -6.55F; }
			case THUNDER_BIRD -> { y = 6.4F; z = -3.55F; }
			case TITAN_BOA, ICE_WYRM -> { y = h - 1.7F; z = -6.55F; }
			case BABA_YAGA -> { y = 9; z = -4.06F; }
			case KRAKEN -> { y = 5; z = -4.06F; }
			case CAVE_CRAWLER -> { y = 3; z = -3.56F; }
			case SHADOW_CREEPER_QUEEN -> { y = 14; z = -4.06F; }
			case MYCELIAL_SOVEREIGN -> { y = 12; z = -2.56F; }
			case NETHERBORN -> { y = 10.5F; z = -5.56F; }
			case VOID_EYE -> { y = 5; z = -3.56F; }
			default -> { }
		}
		if (profile == BossProfile.VOID_EYE) {
			// Void Eye: A luminous square iris surrounds the dark central pupil.
			BiomeBossModel.box(mesh.getRoot(), "iris_top", u, 0, 7, z, 5, 1, 0.1F);
			BiomeBossModel.box(mesh.getRoot(), "iris_bottom", u, 0, 3, z, 5, 1, 0.1F);
			BiomeBossModel.box(mesh.getRoot(), "iris_left", u, -2, 4, z, 1, 3, 0.1F);
			BiomeBossModel.box(mesh.getRoot(), "iris_right", u, 2, 4, z, 1, 3, 0.1F);
		} else {
			BiomeBossModel.box(mesh.getRoot(), "eye_left", u, -1, y, z, 1, 0.6F, 0.1F);
			BiomeBossModel.box(mesh.getRoot(), "eye_right", u, 1, y, z, 1, 0.6F, 0.1F);
		}
		if (profile == BossProfile.SOULBOUND_COLOSSUS) { BiomeBossModel.box(mesh.getRoot(), "soul_core", u, 0, 9, -2.5F, 3, 5, 0.2F); }
		accents = new EntityModel<>(LayerDefinition.create(mesh, 32, 32).bakeRoot()) { };
	}
	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, BiomeBossRenderState state, float yaw, float pitch) {
		if (state.minion && (state.role == BossMinion.Role.EGG.ordinal() || state.role == BossMinion.Role.SOUL_CAGE.ordinal()
				|| state.role == BossMinion.Role.TENTACLE.ordinal())) { return; }
		coloredCutoutModelCopyLayerRender(accents, GLOW, pose, collector, 15728880, state, state.phase == 3 ? 0xFFFF5555 : 0xFF99DDFF, 1);
	}
}
