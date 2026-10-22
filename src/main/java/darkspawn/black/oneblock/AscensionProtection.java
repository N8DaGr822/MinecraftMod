package darkspawn.black.oneblock;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/** Claim Permissions: Claims and recorded bridge blocks retain their team owner. */
public final class AscensionProtection {
    private AscensionProtection() { }
    public static boolean canEdit(Player player,BlockPos pos) {
        if(!Ascension.island(player.level()))return true;
        if(!(player.level() instanceof ServerLevel level))return true;
        var team=AscensionLedger.get(level).member(player.getUUID());
        if(team==null)return false;
        if(team.contains(pos))return true;
        var entry=AscensionBridges.get(level).at(pos);
        return entry!=null&&entry.owner().equals(team.id());
    }
    public static boolean canPlace(Player player,BlockPos pos) {
        if(!Ascension.island(player.level())||!(player.level() instanceof ServerLevel level))return true;
        var teams=AscensionLedger.get(level);var team=teams.member(player.getUUID());if(team==null)return false;
        if(team.contains(pos))return true;
        var bridges=AscensionBridges.get(level);if(!bridges.compatible()||teams.at(pos)!=null)return false;
        var entry=bridges.at(pos);
        return AscensionBridges.corridor(teams,team,pos)!=null&&(entry==null?level.isEmptyBlock(pos):entry.owner().equals(team.id()));
    }
    public static boolean canPlace(Player player,BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
        if(!canPlace(player,pos))return false;
        if(!Ascension.island(player.level())||!(player.level() instanceof ServerLevel level)||AscensionLedger.get(level).at(pos)!=null)return true;
        // Bridge Materials: No containers, gravity blocks, explosives, fluids or machines in the neutral corridor.
        var block=state.getBlock();
        return !state.hasBlockEntity()&&state.getFluidState().isEmpty()&&!(block instanceof net.minecraft.world.level.block.WeatheringCopper)&&(state.is(net.minecraft.tags.BlockTags.PLANKS)||state.is(net.minecraft.tags.BlockTags.LOGS)
            ||block instanceof net.minecraft.world.level.block.StairBlock||block instanceof net.minecraft.world.level.block.SlabBlock
            ||block instanceof net.minecraft.world.level.block.FenceBlock||block instanceof net.minecraft.world.level.block.WallBlock
            ||state.is(net.minecraft.world.level.block.Blocks.STONE)||state.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)
            ||state.is(net.minecraft.world.level.block.Blocks.STONE_BRICKS)||state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE)
            ||state.is(net.minecraft.world.level.block.Blocks.COBBLED_DEEPSLATE)
            ||state.is(net.minecraft.world.level.block.Blocks.GLASS));
    }
    public static boolean canFluid(Player player,BlockPos pos) {
        if(!Ascension.island(player.level())||!(player.level() instanceof ServerLevel level))return true;
        var team=AscensionLedger.get(level).member(player.getUUID());return team!=null&&team.contains(pos);
    }
    public static boolean sameClaim(net.minecraft.world.level.Level level,BlockPos source,BlockPos target) {
        if(!Ascension.island(level)||!(level instanceof ServerLevel server))return true;
        var team=AscensionLedger.get(server).at(source);
        return team!=null&&!team.archived()&&team.contains(target);
    }
    public static boolean canOperate(net.minecraft.world.level.Level level,BlockPos source,BlockPos target) {
        if(!sameClaim(level,source,target))return false;
        if(!Ascension.island(level)||!(level instanceof ServerLevel server))return true;
        var team=AscensionLedger.get(server).at(source);
        // Offline Automation: Supported native machines run only with an online member present on this claim.
        return server.players().stream().anyMatch(p->team.members().contains(p.getUUID())&&team.contains(p.blockPosition()));
    }
    public static void placed(Player player,BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
        if(!Ascension.island(player.level())||!(player.level() instanceof ServerLevel level))return;
        var teams=AscensionLedger.get(level);if(teams.at(pos)!=null)return;
        var team=teams.member(player.getUUID());var corridor=AscensionBridges.corridor(teams,team,pos);
        if(corridor!=null)AscensionBridges.get(level).record(pos,team.id(),corridor,net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }
    public static void initialize() {
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->canEdit(player,entity.blockPosition())?InteractionResult.PASS:InteractionResult.FAIL);
        AttackEntityCallback.EVENT.register((player,level,hand,entity,hit)->canEdit(player,entity.blockPosition())?InteractionResult.PASS:InteractionResult.FAIL);
        // Ranged Ownership: A projectile retains its player's claim permissions at impact.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->!(source.getEntity() instanceof Player player)||canEdit(player,entity.blockPosition()));
    }
}
