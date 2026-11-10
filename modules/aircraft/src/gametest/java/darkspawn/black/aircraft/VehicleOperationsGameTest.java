package darkspawn.black.aircraft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Explicit isolated-world drill; these tests intentionally preserve a restart sentinel. */
public final class VehicleOperationsGameTest {
    private static final BlockPos SENTINEL=new BlockPos(1024,100,1024);
    private static final UUID ID=UUID.fromString("ea6a3ba5-bdb7-4282-aeb3-6fc27319b372");
    private static final UUID OWNER=UUID.fromString("e0952071-f5f2-41a3-aad7-c7692ab74041");
    private static List<AirframeBlock> build() {
        return List.of(BlockPos.ZERO,new BlockPos(1,0,0),new BlockPos(-1,0,0),new BlockPos(0,0,1),new BlockPos(0,0,-1))
            .stream().map(pos -> new AirframeBlock(pos,Blocks.OAK_PLANKS.defaultBlockState())).toList();
    }
    @GameTest(maxTicks=220,skyAccess=true)
    public void seedIsolatedRecoveryAndMeasureIdleFleet(GameTestHelper h) {
        if (!"seed".equals(System.getProperty("darkspawn.operationsDrill"))) { h.succeed(); return; }
        var level=h.getLevel(); level.getChunkAt(SENTINEL);
        // Recovery Sentinel: This fixed chunk and entity are intentionally retained for the restore-only check.
        for (int x=-3;x<=3;x++) for (int z=-3;z<=3;z++) {
            level.setBlockAndUpdate(SENTINEL.offset(x,0,z),Blocks.IRON_BLOCK.defaultBlockState());
            for (int y=1;y<5;y++) level.setBlockAndUpdate(SENTINEL.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var sentinel=new Aircraft(AircraftMod.AIRCRAFT,level); sentinel.setUUID(ID); sentinel.assignOwner(OWNER);
        sentinel.setBuild(build()); sentinel.setPos(Vec3.atBottomCenterOf(SENTINEL.above())); sentinel.setHealth(23);
        h.assertTrue(level.addFreshEntity(sentinel),"Fresh isolated world accepts sentinel");
        var fleet=new ArrayList<Aircraft>();
        var origin=h.absolutePos(new BlockPos(2,3,2));
        for (int x=-1;x<=3;x++) for (int z=-1;z<=3;z++) level.setChunkForced((origin.getX()>>4)+x,(origin.getZ()>>4)+z,true);
        level.getChunkSource().tick(() -> true,false);
        for(int tasks=0;tasks<10000 && level.getChunkSource().pollTask();tasks++) { }
        for (int i=0;i<20;i++) {
            var pos=origin.offset(i%5*6,0,i/5*6);
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
                level.setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
                for(int y=0;y<4;y++) level.setBlockAndUpdate(pos.offset(x,y,z),Blocks.AIR.defaultBlockState());
            }
            var craft=new Aircraft(AircraftMod.AIRCRAFT,level); craft.setPos(Vec3.atBottomCenterOf(pos)); craft.setBuild(build());
            craft.assignOwner(OWNER); level.addFreshEntity(craft); fleet.add(craft);
        }
        var samples=new ArrayList<Long>(); long[] start={0}; int[] ticks={0}; boolean[] active={true};
        ServerTickEvents.START_SERVER_TICK.register(server -> { if(active[0]) start[0]=System.nanoTime(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if(active[0] && ++ticks[0]>40 && start[0]!=0) samples.add(System.nanoTime()-start[0]);
        });
        h.runAfterDelay(160,() -> {
            active[0]=false;
            boolean ticked=fleet.stream().allMatch(craft -> craft.tickCount>=100);
            fleet.forEach(Aircraft::discard);
            for (int x=-1;x<=3;x++) for (int z=-1;z<=3;z++) level.setChunkForced((origin.getX()>>4)+x,(origin.getZ()>>4)+z,false);
            h.assertTrue(ticked,"All 20 aircraft actually tick during the sample");
            h.assertTrue(samples.size()>=100,"At least 100 measured server ticks after warmup");
            samples.sort(Long::compare);
            double mean=samples.stream().mapToLong(Long::longValue).average().orElse(0)/1e6;
            System.out.printf(java.util.Locale.ROOT,"DARKSPAWN_IDLE_FLEET ticks=%d aircraft=20 real_clients=0 mean_ms=%.3f p95_ms=%.3f max_ms=%.3f%n",
                samples.size(),mean,samples.get((int)Math.ceil(samples.size()*.95)-1)/1e6,samples.getLast()/1e6);
            h.succeed();
        });
    }
    @GameTest(maxTicks=100,skyAccess=true)
    public void restoredWorldRetainsAircraftAndTerrain(GameTestHelper h) {
        if (!"restore".equals(System.getProperty("darkspawn.operationsDrill"))) { h.succeed(); return; }
        var level=h.getLevel(); level.getChunkAt(SENTINEL);
        level.setChunkForced(SENTINEL.getX()>>4,SENTINEL.getZ()>>4,true);
        level.getChunkSource().tick(() -> true,false);
        for(int tasks=0;tasks<10000 && level.getChunkSource().pollTask();tasks++) { }
        h.runAfterDelay(30,() -> {
            level.setChunkForced(SENTINEL.getX()>>4,SENTINEL.getZ()>>4,false);
            h.assertTrue(level.getBlockState(SENTINEL).is(Blocks.IRON_BLOCK),"Restored native terrain sentinel exists");
            h.assertTrue(level.getEntity(ID) instanceof Aircraft,"Restored native entity chunk contains aircraft");
            var craft=(Aircraft)level.getEntity(ID);
            h.assertTrue(craft.getOwnerReference()!=null && craft.getOwnerReference().getUUID().equals(OWNER),"Owner survives actual process restart");
            h.assertTrue(craft.blocks().equals(build()) && craft.getHealth()==23,"Exact airframe and health survive restoration");
            h.succeed();
        });
    }
}
