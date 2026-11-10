package darkspawn.black.ecosystem;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossKind;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.*;

/** Opt-in heartwood cultivation checks the entire volume before changing any block. */
public final class HeartwoodGrove extends SaplingBlock {
    public static final Block LOG=Registry.register(BuiltInRegistries.BLOCK,Darkspawn.id("heartwood_log"),new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).setId(ResourceKey.create(Registries.BLOCK,Darkspawn.id("heartwood_log")))));
    public static final Block SEEDLING=Registry.register(BuiltInRegistries.BLOCK,Darkspawn.id("heartwood_seedling"),new HeartwoodGrove());
    public static final Item LOG_ITEM=item("heartwood_log",LOG),SEEDLING_ITEM=item("heartwood_seedling",SEEDLING);
    private HeartwoodGrove(){super(TreeGrower.OAK,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).setId(ResourceKey.create(Registries.BLOCK,Darkspawn.id("heartwood_seedling"))));}
    private static Item item(String id,Block block){var item=new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Darkspawn.id(id))).useBlockDescriptionPrefix());item.registerBlocks(Item.BY_BLOCK,item);return Registry.register(BuiltInRegistries.ITEM,Darkspawn.id(id),item);}
    @Override public void advanceTree(ServerLevel level,BlockPos pos,BlockState state,RandomSource random){
        if(!WorldAwakening.get(level).defeated(BossKind.ANCIENT_TREE_SPIRIT))return;
        // No growth through structures, fluids, unloaded chunks or the world border.
        for(var target:BlockPos.betweenClosed(pos.offset(-2,0,-2),pos.offset(2,6,2)))
            if(!level.hasChunkAt(target)||!level.isInsideBuildHeight(target)||!level.getWorldBorder().isWithinBounds(target)||!target.equals(pos)&&!level.isEmptyBlock(target))return;
        for(int y=0;y<5;y++)level.setBlock(pos.above(y),LOG.defaultBlockState(),3);
        for(int y=3;y<=5;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if((x!=0||z!=0||y==5)&&Math.abs(x)+Math.abs(z)<4)
            level.setBlock(pos.offset(x,y,z),Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,Math.min(7,Math.abs(x)+Math.abs(z)+1)),3);
    }
    public static void initialize(){net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(e->{e.accept(SEEDLING_ITEM);e.accept(LOG_ITEM);});}
}
