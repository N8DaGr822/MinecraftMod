package darkspawn.black.boss;

public enum BossKind {
	ANCIENT_TREE_SPIRIT("ancient_tree_spirit"),
	MUTANT_WOLF("mutant_wolf"),
	MUTANT_ZOMBIE("mutant_zombie"),
	FOSSIL_TYRANT("fossil_tyrant"),
	THUNDER_BIRD("thunder_bird"),
	TITAN_BOA("titan_boa"),
	BABA_YAGA("baba_yaga"),
	MOUNTAIN_TITAN("mountain_titan"),
	ICE_WYRM("ice_wyrm"),
	KRAKEN("kraken"),
	CAVE_CRAWLER("cave_crawler"),
	SHADOW_CREEPER_QUEEN("shadow_creeper_queen"),
	MYCELIAL_SOVEREIGN("mycelial_sovereign"),
	NETHERBORN("netherborn"),
	SOULBOUND_COLOSSUS("soulbound_colossus"),
	VOID_EYE("void_eye");

	private final String id;

	BossKind(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}
}
