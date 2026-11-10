package darkspawn.black.client;

import darkspawn.black.oneblock.AscensionGuardianEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class AscensionGuardianRenderer extends MobRenderer<AscensionGuardianEntity,AscensionGuardianRenderState,AscensionGuardianModel> {
    public AscensionGuardianRenderer(EntityRendererProvider.Context context){super(context,new AscensionGuardianModel(AscensionGuardianModel.createBodyLayer().bakeRoot()),.7F);}
    @Override public AscensionGuardianRenderState createRenderState(){return new AscensionGuardianRenderState();}
    public static Identifier texture(int chapter){return Identifier.withDefaultNamespace("textures/block/"+switch(chapter){
        case 4->"oxidized_copper";case 5->"netherrack";case 6->"packed_ice";case 7->"prismarine_bricks";
        case 8->"amethyst_block";case 9->"purpur_block";case 10->"polished_deepslate";case 11->"quartz_block_side";default->"stone";
    }+".png");}
    @Override public Identifier getTextureLocation(AscensionGuardianRenderState state){return texture(state.chapter);}
    @Override public void extractRenderState(AscensionGuardianEntity entity,AscensionGuardianRenderState state,float partialTick){
        super.extractRenderState(entity,state,partialTick);state.chapter=entity.chapter();state.warning=entity.warning();
    }
    @Override protected int getModelTint(AscensionGuardianRenderState state){return state.warning>0?0xFFFFDDAA:0xFFFFFFFF;}
}
