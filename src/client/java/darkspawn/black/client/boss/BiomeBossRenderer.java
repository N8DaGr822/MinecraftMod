package darkspawn.black.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import darkspawn.black.boss.BiomeBoss;
import darkspawn.black.boss.BossMinion;
import darkspawn.black.boss.BossProfile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public final class BiomeBossRenderer extends MobRenderer<Mob, BiomeBossRenderState, BiomeBossModel> {
	private final BossProfile profile;
	private final Identifier texture;

	public BiomeBossRenderer(EntityRendererProvider.Context context, BossProfile profile) {
		super(context, new BiomeBossModel(BiomeBossModel.createBodyLayer(profile).bakeRoot()), profile.width / 3);
		this.profile = profile;
		texture = Identifier.withDefaultNamespace("textures/block/" + switch (profile) {
			case MUTANT_ZOMBIE -> "moss_block";
			case FOSSIL_TYRANT, SOULBOUND_COLOSSUS -> "bone_block_side";
			case THUNDER_BIRD -> "yellow_terracotta";
			case TITAN_BOA -> "green_terracotta";
			case BABA_YAGA -> "spruce_planks";
			case MOUNTAIN_TITAN -> "deepslate_tiles";
			case ICE_WYRM -> "blue_ice";
			case KRAKEN -> "dark_prismarine";
			case CAVE_CRAWLER -> "black_wool";
			case SHADOW_CREEPER_QUEEN -> "sculk";
			case MYCELIAL_SOVEREIGN -> "red_mushroom_block";
			case NETHERBORN -> "netherrack";
			case VOID_EYE -> "obsidian";
		} + ".png");
		addLayer(new BossAccentLayer(this, profile));
	}
	@Override
	public BiomeBossRenderState createRenderState() { return new BiomeBossRenderState(); }
	@Override
	public Identifier getTextureLocation(BiomeBossRenderState state) { return texture; }
	@Override
	public void extractRenderState(Mob entity, BiomeBossRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.profile = profile;
		state.minion = entity instanceof BossMinion;
		if (entity instanceof BiomeBoss boss) {
			state.phase = boss.phase(); state.windup = boss.windup(); state.recovery = boss.recovery();
			state.variant = boss.variant(); state.awakening = boss.awakening();
		} else if (entity instanceof BossMinion minion) { state.role = minion.role().ordinal(); state.windup = minion.windup(); }
	}
	@Override
	protected void scale(BiomeBossRenderState state, PoseStack pose) {
		float size = profile.height * (state.minion && profile != BossProfile.KRAKEN ? 0.18F : 1);
		float rise = state.awakening > 0 ? 0.25F + 0.75F * (100 - state.awakening) / 100 : 1;
		pose.scale(size, size * rise, size);
	}
	@Override
	protected int getModelTint(BiomeBossRenderState state) {
		if (state.minion && state.role == BossMinion.Role.HEALER.ordinal()) { return 0xFF77FF88; }
		if (state.minion && state.role == BossMinion.Role.SOUL_CAGE.ordinal()) { return 0xFF77FFFF; }
		if (state.recovery > 0) { return 0xFFFFFFAA; }
		if (state.phase == 3) { return 0xFFFFA6A6; }
		if (profile == BossProfile.NETHERBORN && state.variant == 1) { return 0xFF65DDCC; }
		return 0xFFFFFFFF;
	}
	@Override
	protected float getShadowRadius(BiomeBossRenderState state) { return state.minion ? 0.6F : super.getShadowRadius(state); }
}
