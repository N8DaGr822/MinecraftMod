package darkspawn.black.groundwalker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroundWalkerModelTest {
    @BeforeAll static void initialize() {
        Thread.currentThread().setContextClassLoader(GroundWalkerModelTest.class.getClassLoader());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        GroundWalkerMod.initialize();
    }
    @Test void mechanicalLegsAlternateAndResetForTheNextEntity() throws Exception {
        var modelClass=Class.forName("darkspawn.black.groundwalker.client.GroundWalkerModel");
        var stateClass=Class.forName("darkspawn.black.mountkit.client.GriffinRenderState");
        var layer=modelClass.getMethod("createBodyLayer").invoke(null);
        var root=layer.getClass().getMethod("bakeRoot").invoke(layer);
        var model=modelClass.getConstructors()[0].newInstance(root);
        var state=stateClass.getConstructor().newInstance();
        var left=root.getClass().getMethod("getChild",String.class).invoke(root,"left_leg");
        var right=root.getClass().getMethod("getChild",String.class).invoke(root,"right_leg");
        var angle=left.getClass().getField("xRot");
        stateClass.getField("walkAnimationSpeed").setFloat(state,1F);
        modelClass.getMethod("setupAnim",stateClass).invoke(model,state);
        assertTrue(angle.getFloat(left)>0);
        assertEquals(-angle.getFloat(left),angle.getFloat(right),.0001F);
        stateClass.getField("walkAnimationSpeed").setFloat(state,0F);
        modelClass.getMethod("setupAnim",stateClass).invoke(model,state);
        assertEquals(0F,angle.getFloat(left),.0001F);
        assertEquals(0F,angle.getFloat(right),.0001F);
    }
}
