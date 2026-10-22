package darkspawn.black.oneblock;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Island Automation: Loaded work only, reserved tool slot, and complete output plans before any resource mutation. */
public final class AscensionAutomation {
    public static final int COLLECT_LIMIT=8,MINE_INTERVAL=100;
    private AscensionAutomation(){ }
    public static BlockPos output(AscensionLedger.Team team){return team.center().offset(1,1,0);}
    static boolean loadedArea(ServerLevel level,AscensionLedger.Team team){
        var core=team.center();return level.hasChunkAt(core.offset(-6,0,-6))&&level.hasChunkAt(core.offset(-6,0,6))
            &&level.hasChunkAt(core.offset(6,0,-6))&&level.hasChunkAt(core.offset(6,0,6));
    }
    static boolean present(ServerLevel level,AscensionLedger.Team team){return level.players().stream().anyMatch(p->p.isAlive()&&!p.isSpectator()&&!p.preventsBlockDrops()
        &&team.members().contains(p.getUUID())&&(team.phases()!=null&&team.phases().features().relay?team.contains(p.blockPosition()):p.distanceToSqr(Vec3.atCenterOf(team.center()))<=16*16));}
    static ChestBlockEntity chest(ServerLevel level,AscensionLedger.Team team){
        var pos=output(team);if(!level.hasChunkAt(pos))return null;var state=level.getBlockState(pos);
        if(!state.is(Blocks.CHEST)||state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE)return null;
        return level.getBlockEntity(pos) instanceof ChestBlockEntity chest?chest:null;
    }
    private static int message(ServerPlayer p,String text){AscensionGuide.feedback(p,text);p.sendSystemMessage(Component.literal(text));return 1;}
    private static AscensionLedger.Team owner(ServerPlayer p){
        if(!Ascension.island(p.level())||!OneBlock.safe(p)||p.preventsBlockDrops()||OneBlock.state(p).pendingTicks>0)return null;
        var team=AscensionLedger.get(p.level()).member(p.getUUID());return team!=null&&team.phases()!=null&&team.owner().equals(p.getUUID())&&team.contains(p.blockPosition())?team:null;
    }
    public static int buy(ServerPlayer p,boolean miner){
        var team=owner(p);if(team==null)return message(p,"Only the MVP team's owner can install upgrades while safely on their island.");
        var f=team.phases().features();
        if(miner?f.miner:f.collector)return message(p,"That team upgrade is already installed.");
        if(team.phases().phase().equals("genesis")||miner&&(!f.collector||!team.phases().encountered("cave_guardian")))return message(p,miner?"Auto-miner requires the collector and Cave Guardian victory.":"Complete Genesis before installing the collector.");
        if(chest(p.level(),team)==null)return message(p,"Place a single ordinary chest at "+output(team).toShortString()+" on the platform east of the core.");
        int cost=miner?32:16;var held=p.getMainHandItem();if(!held.is(Items.IRON_INGOT)||held.getCount()<cost)return message(p,"Hold "+cost+" iron ingots to install this permanent team upgrade.");
        held.shrink(cost);if(miner)f.miner=true;else f.collector=true;f.enabled=true;f.mineAfter=p.level().getGameTime()+MINE_INTERVAL;
        AscensionLedger.get(p.level()).setDirty();p.inventoryMenu.broadcastChanges();return message(p,"Installed "+(miner?"auto-miner":"collector")+". Chest slot 1 is reserved for a plain iron pickaxe; output uses the other 26 slots. /ascension automation shows status.");
    }
    public static int toggle(ServerPlayer p,boolean enabled){
        var team=owner(p);if(team==null||!team.phases().features().collector)return message(p,"Only the owner can toggle installed upgrades from their island.");
        var f=team.phases().features();f.enabled=enabled;f.mineAfter=p.level().getGameTime()+MINE_INTERVAL;AscensionLedger.get(p.level()).setDirty();return message(p,enabled?"Island automation enabled; only nearby active teammates keep it running.":"Island automation paused.");
    }
    public static int info(ServerPlayer p){
        var team=AscensionLedger.get(p.level()).member(p.getUUID());if(team==null||team.phases()==null)return message(p,"Automation upgrades are available to MVP teams.");
        return message(p,status(p.level().getServer().getLevel(Ascension.LEVEL),team));
    }
    public static int buy(ServerPlayer p,String upgrade){
        if(upgrade.equals("collector")||upgrade.equals("miner"))return buy(p,upgrade.equals("miner"));
        if(!upgrade.equals("relay")&&!upgrade.equals("drive"))return 0;
        var team=owner(p);if(team==null)return message(p,"Only the owner can install upgrades from their island.");
        var f=team.phases().features();boolean relay=upgrade.equals("relay");
        if(relay?f.relay:f.drive)return message(p,"That upgrade is already installed.");
        if(relay?!f.collector||!team.phases().finished("forgeworks"):!f.miner||!team.phases().finished("stormbound"))
            return message(p,relay?"Relay requires a collector and completed Forgeworks.":"Drive requires an auto-miner and completed Stormbound.");
        var held=p.getMainHandItem();var item=relay?Items.COPPER_INGOT:Items.REDSTONE;int cost=32;
        if(!held.is(item)||held.getCount()<cost)return message(p,"Hold 32 "+(relay?"copper ingots":"redstone dust")+" to install this upgrade.");
        held.shrink(cost);if(relay)f.relay=true;else f.drive=true;
        AscensionLedger.get(p.level()).setDirty();p.inventoryMenu.broadcastChanges();
        return message(p,relay?"Relay installed: any survival teammate on this island can attend loaded automation.":"Drive installed: mining interval reduced from five to three seconds.");
    }
    public static String status(ServerLevel level,AscensionLedger.Team team){
        if(team.phases()==null)return "Legacy island: chapter automation is unavailable.";
        var f=team.phases().features();var chest=chest(level,team);
        String reason=!f.collector?"Collector not installed":!f.enabled?"Paused by owner":!present(level,team)?"No survival teammate "+(f.relay?"on the island":"within 16 blocks"):
            !loadedArea(level,team)?"Nearby chunks unloaded":chest==null?"Single ordinary output chest missing":"Collector ready";
        String miner=!f.miner?"Not installed":!reason.equals("Collector ready")?reason:!tool(chest.getItem(0))?"Plain iron pickaxe missing, enchanted or broken":
            !hasOutputSpace(chest)?"Output full: needs an empty slot":team.generator().state()!=AscensionGenerator.State.READY?"Generator "+team.generator().state():
            OneBlock.RESOURCES[team.progress().resource].defaultBlockState().requiresCorrectToolForDrops()&&!chest.getItem(0).isCorrectToolForDrops(OneBlock.RESOURCES[team.progress().resource].defaultBlockState())?"Harvest this resource manually with the correct tool":
            level.getGameTime()<f.mineAfter?"Waiting for mining cooldown":"Ready";
        return "Collector: "+reason+"\nMiner: "+miner+"\nOutput chest: "+output(team).toShortString()
            +"\nSlot 1: plain iron pickaxe. Slots 2-27: output.\nRelay: "+(f.relay?"installed (island attendance)":"not installed (16-block attendance)")
            +"\nDrive: "+(f.drive?"installed (3 seconds)":"not installed (5 seconds)")+". No offline work or forced chunks.";
    }
    static boolean hasOutputSpace(ChestBlockEntity chest){for(int i=1;i<chest.getContainerSize();i++)if(chest.getItem(i).isEmpty())return true;return false;}
    // Output Planning: Preserve components and stack limits; failure leaves every chest slot and source stack untouched.
    static List<ItemStack> plan(ChestBlockEntity chest,List<ItemStack> incoming){
        var result=new ArrayList<ItemStack>();for(int i=1;i<chest.getContainerSize();i++)result.add(chest.getItem(i).copy());
        for(var source:incoming){var remaining=source.copy();
            for(var stack:result)if(ItemStack.isSameItemSameComponents(stack,remaining)){
                int moved=Math.min(remaining.getCount(),Math.max(0,Math.min(chest.getMaxStackSize(),stack.getMaxStackSize())-stack.getCount()));stack.grow(moved);remaining.shrink(moved);
            }
            for(int i=0;i<result.size()&&!remaining.isEmpty();i++)if(result.get(i).isEmpty()){
                int moved=Math.min(remaining.getCount(),Math.min(chest.getMaxStackSize(),remaining.getMaxStackSize()));result.set(i,remaining.copyWithCount(moved));remaining.shrink(moved);
            }
            if(!remaining.isEmpty())return null;
        }
        return result;
    }
    static void apply(ChestBlockEntity chest,List<ItemStack> plan){for(int i=0;i<plan.size();i++)chest.setItem(i+1,plan.get(i));chest.setChanged();}
    static boolean tool(ItemStack tool){return tool.is(Items.IRON_PICKAXE)&&tool.isDamageableItem()&&tool.getDamageValue()<tool.getMaxDamage()
        &&tool.getOrDefault(DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).isEmpty();}
    public static void tick(ServerLevel level,AscensionLedger.Team team){
        if(team.archived()||team.phases()==null||!team.progress().initialized||!team.phases().features().enabled||!present(level,team)||!loadedArea(level,team))return;
        var chest=chest(level,team);if(chest==null)return;var f=team.phases().features();
        if(f.collector){
            var entities=new ArrayList<ItemEntity>();level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),new AABB(team.center()).inflate(3),e->true,entities,COLLECT_LIMIT);
            for(var item:entities){if(!item.isAlive()||item.hasPickUpDelay())continue;var output=plan(chest,List.of(item.getItem()));if(output==null)continue;apply(chest,output);item.discard();}
        }
        if(f.miner&&level.getGameTime()>=f.mineAfter){
            f.mineAfter=level.getGameTime()+(f.drive?60:MINE_INTERVAL);AscensionLedger.get(level).setDirty();AscensionGenerator.automine(level,team,chest);
        }
    }
}
