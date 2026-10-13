package darkspawn.black.client.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.mount.DragonMount;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class DragonMountRenderer extends MobRenderer<DragonMount, GriffinRenderState, DragonMountModel> {
    public DragonMountRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonMountModel(DragonMountModel.createBodyLayer().bakeRoot()), .7F);
    }
    @Override public GriffinRenderState createRenderState() { return new GriffinRenderState(); }
    @Override public Identifier getTextureLocation(GriffinRenderState state) { return Darkspawn.id("textures/entity/dragon_mount.png"); }
    @Override public void extractRenderState(DragonMount entity, GriffinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick); state.flying = entity.flying();
    }
}
