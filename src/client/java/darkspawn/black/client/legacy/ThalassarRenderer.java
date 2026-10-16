package darkspawn.black.client.legacy;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import darkspawn.black.Darkspawn;
import darkspawn.black.mount.Mounts;
import darkspawn.black.mount.Thalassar;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class ThalassarRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<Thalassar,R> {
    public ThalassarRenderer(EntityRendererProvider.Context context) {
        super(context,new DefaultedEntityGeoModel<Thalassar>(Mounts.THALASSAR).withAltTexture(Darkspawn.id("thalassar")));shadowRadius=.65F;
    }
    @Override public void addRenderData(Thalassar entity,Void context,R state,float partialTicks) {
        state.addGeckolibData(Thalassar.SWIMMING,entity.isInWater());
    }
}
