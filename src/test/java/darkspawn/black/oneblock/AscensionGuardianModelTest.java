package darkspawn.black.oneblock;

import darkspawn.black.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AscensionGuardianModelTest {
    @BeforeAll static void initialize(){TestBootstrap.initialize();}
    @Test void chapterSilhouettesAndWarningPoseResetBetweenEntities() throws Exception {
        var modelClass=Class.forName("darkspawn.black.client.AscensionGuardianModel");
        var stateClass=Class.forName("darkspawn.black.client.AscensionGuardianRenderState");
        var layer=modelClass.getMethod("createBodyLayer").invoke(null);
        var root=layer.getClass().getMethod("bakeRoot").invoke(layer);
        var model=modelClass.getConstructors()[0].newInstance(root);
        var state=stateClass.getConstructor().newInstance();
        int[] chapters={1,4,5,6,7,8,9,10,11};
        for(int chapter:chapters){
            stateClass.getField("chapter").setInt(state,chapter);
            stateClass.getField("warning").setInt(state,40);
            modelClass.getMethod("setupAnim",stateClass).invoke(model,state);
            for(int other:chapters){
                var crest=root.getClass().getMethod("getChild",String.class).invoke(root,"crest_"+other);
                assertEquals(chapter==other,crest.getClass().getField("visible").getBoolean(crest));
            }
            var arm=root.getClass().getMethod("getChild",String.class).invoke(root,"left_arm");
            assertEquals(-1.8F,arm.getClass().getField("xRot").getFloat(arm));
            stateClass.getField("warning").setInt(state,0);
            modelClass.getMethod("setupAnim",stateClass).invoke(model,state);
            assertEquals(0F,arm.getClass().getField("xRot").getFloat(arm),.0001F);
        }
    }
    @Test void everyChapterHasAllThreeRegisteredMusicIntensities(){
        var themes=new java.util.HashSet<String>();
        for(int chapter:new int[]{1,4,5,6,7,8,9,10,11}) {
            var theme=AscensionGuardianEntity.musicTheme(chapter);
            assertTrue(theme.startsWith("ascension_"),"Guardians use their original Ascension score catalog");
            assertTrue(themes.add(theme),"Each Guardian has its own arrangement");
        }
        for(int chapter:new int[]{1,4,5,6,7,8,9,10,11})for(int intensity=1;intensity<=3;intensity++)
            assertNotNull(darkspawn.black.audio.DarkspawnSounds.get("music."+AscensionGuardianEntity.musicTheme(chapter)+"."+intensity));
    }
}
