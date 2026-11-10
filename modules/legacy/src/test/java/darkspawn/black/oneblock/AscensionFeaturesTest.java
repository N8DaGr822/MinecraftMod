package darkspawn.black.oneblock;

import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

class AscensionFeaturesTest {
    @BeforeAll static void bootstrap(){darkspawn.black.TestBootstrap.initialize();}
    @Test void oldPhaseSavesReceiveIndependentDisabledFeatures(){
        var old=AscensionPhaseProgress.CODEC.encodeStart(JsonOps.INSTANCE,new AscensionPhaseProgress()).getOrThrow().getAsJsonObject();old.remove("features");
        var first=AscensionPhaseProgress.CODEC.parse(JsonOps.INSTANCE,old).getOrThrow();var second=AscensionPhaseProgress.CODEC.parse(JsonOps.INSTANCE,old).getOrThrow();
        first.features().collector=true;first.features().enabled=true;
        assertTrue(first.compatible());assertTrue(second.compatible());assertFalse(second.features().collector);assertFalse(second.features().enabled);
    }
    @Test void invalidFeatureSchemasAndDependentFlagsFailCompatibility(){
        var f=new AscensionFeatures();assertTrue(f.compatible());f.miner=true;assertFalse(f.compatible());f.collector=true;assertTrue(f.compatible());
        f.mineAfter=-1;assertFalse(f.compatible());f.mineAfter=0;f.guardian=java.util.UUID.randomUUID();assertFalse(f.compatible());f.expires=100;assertTrue(f.compatible());
        var saved=AscensionFeatures.CODEC.encodeStart(JsonOps.INSTANCE,f).getOrThrow().getAsJsonObject();saved.addProperty("schema",2);
        assertFalse(AscensionFeatures.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow().compatible());f.clearGuardian();assertTrue(f.compatible());assertEquals(0,f.expires);
    }
    @Test void outputPlanIsAtomicPreservesInputsAndNeverUsesToolSlot(){
        var chest=new ChestBlockEntity(BlockPos.ZERO,Blocks.CHEST.defaultBlockState());chest.setItem(0,new ItemStack(Items.IRON_PICKAXE));
        for(int i=1;i<27;i++)chest.setItem(i,new ItemStack(Items.DIRT,64));chest.setItem(1,new ItemStack(Items.DIRT,63));
        var dirt=new ItemStack(Items.DIRT,2);assertNull(AscensionAutomation.plan(chest,List.of(dirt)));assertEquals(63,chest.getItem(1).getCount());assertEquals(2,dirt.getCount());
        chest.setItem(2,ItemStack.EMPTY);var plan=AscensionAutomation.plan(chest,List.of(dirt));assertNotNull(plan);assertEquals(64,plan.get(0).getCount());assertEquals(1,plan.get(1).getCount());
        AscensionAutomation.apply(chest,plan);assertTrue(chest.getItem(0).is(Items.IRON_PICKAXE));assertEquals(2,dirt.getCount());
    }
}
