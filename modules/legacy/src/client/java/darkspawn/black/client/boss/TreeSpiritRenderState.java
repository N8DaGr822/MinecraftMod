package darkspawn.black.client.boss;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class TreeSpiritRenderState extends LivingEntityRenderState {
	public boolean sapling;
	public int phase = 1;
	public int windup;
	public int recovery;
}
