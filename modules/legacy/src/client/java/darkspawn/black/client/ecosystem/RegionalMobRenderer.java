package darkspawn.black.client.ecosystem;

import darkspawn.black.ecosystem.RegionalKind;
import darkspawn.black.ecosystem.RegionalMob;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class RegionalMobRenderer extends MobRenderer<RegionalMob, RegionalMobRenderState, RegionalMobModel> {
	private final Identifier texture;
	public RegionalMobRenderer(EntityRendererProvider.Context context, RegionalKind kind) {
		super(context, new RegionalMobModel(RegionalMobModel.createBodyLayer(kind).bakeRoot(), kind), kind.width * .4F);
		texture = texture(kind);
	}
	public static Identifier texture(RegionalKind kind) {
		String block = switch (kind.region) {
			case MUTANT_ZOMBIE -> "moss_block";
			case FOSSIL_TYRANT, SOULBOUND_COLOSSUS -> "bone_block_side";
			case THUNDER_BIRD -> "oxidized_copper";
			case TITAN_BOA -> "mossy_cobblestone";
			case BABA_YAGA -> "mangrove_log";
			case MOUNTAIN_TITAN -> "stone";
			case ICE_WYRM -> "blue_ice";
			case KRAKEN -> "dark_prismarine";
			case CAVE_CRAWLER -> "deepslate";
			case SHADOW_CREEPER_QUEEN -> "sculk";
			case MYCELIAL_SOVEREIGN -> "red_mushroom_block";
			case NETHERBORN -> kind.name().startsWith("WARPED") || kind == RegionalKind.RIFTLING ? "warped_wart_block"
					: kind.name().startsWith("CRIMSON") || kind == RegionalKind.BLOODROOT || kind == RegionalKind.FUNGAL_IMP ? "nether_wart_block" : "magma";
			case VOID_EYE -> kind == RegionalKind.VOID_SENTINEL ? "obsidian" : "end_stone";
		};
		return Identifier.withDefaultNamespace("textures/block/" + block + ".png");
	}
	@Override public RegionalMobRenderState createRenderState() { return new RegionalMobRenderState(); }
	@Override public Identifier getTextureLocation(RegionalMobRenderState state) { return texture; }
	@Override public void extractRenderState(RegionalMob entity, RegionalMobRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.warning = entity.warning(); state.active = entity.active(); state.hiding = entity.hiding();
		state.shieldAngle = entity.shieldAngle();
	}
	@Override protected int getModelTint(RegionalMobRenderState state) {
		return state.warning > 0 ? 0xFFFFD080 : state.active > 0 ? 0xFF90DDFF : state.hiding ? 0xFF707070 : 0xFFFFFFFF;
	}
}
