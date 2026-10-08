package darkspawn.black.client.boss;

import darkspawn.black.boss.BossProfile;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class BiomeBossRenderState extends LivingEntityRenderState {
	public BossProfile profile;
	public int phase = 1;
	public int windup;
	public int recovery;
	public int variant;
	public int awakening;
	public boolean minion;
	public int role;
}
