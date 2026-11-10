package darkspawn.black.noah.client;

import darkspawn.black.noah.NoahMod;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.noah.Thalassar;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class ThalassarRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<Thalassar,R> {
    public ThalassarRenderer(EntityRendererProvider.Context context) {
        super(context,new DefaultedEntityGeoModel<Thalassar>(NoahMod.THALASSAR).withAltTexture(DarkspawnCore.id("thalassar")));shadowRadius=.65F;
    }
    @Override public void addRenderData(Thalassar entity,Void context,R state,float partialTicks) {
        state.addGeckolibData(Thalassar.SWIMMING,entity.isInWater());
    }
}
