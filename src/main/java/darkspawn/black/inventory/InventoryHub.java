package darkspawn.black.inventory;

import java.util.*;
import darkspawn.black.mount.*;
import darkspawn.black.storage.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;

/** Nearby access revalidates physical reach and native menus on every action; snapshots grant no authority. */
public final class InventoryHub {
    private record Reply(long until,HubSnapshotPayload snapshot) { }
    private static final WeakHashMap<ServerPlayer,Reply> NEXT=new WeakHashMap<>();
    private InventoryHub() { }
    public static void initialize(){
        PayloadTypeRegistry.serverboundPlay().register(HubRequestPayload.TYPE,HubRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HubSnapshotPayload.TYPE,HubSnapshotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HubRequestPayload.TYPE,(request,context)->request(context.player(),request));
    }
    public static StorageBlockEntity accessible(ServerPlayer p,BlockPos pos){
        if(!p.level().hasChunkAt(pos)||p.distanceToSqr(Vec3.atCenterOf(pos))>25||!p.level().mayInteract(p,pos))return null;
        var hit=p.level().clip(new ClipContext(p.getEyePosition(),Vec3.atCenterOf(pos),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(pos))return null;
        return p.level().getBlockEntity(pos) instanceof StorageBlockEntity chest&&chest.stillValid(p)?chest:null;
    }
    public static void request(ServerPlayer p,HubRequestPayload r){
        if(!p.isAlive()||p.isSpectator()||p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty())return;
        if(r.stateId()!=p.inventoryMenu.getStateId()){p.inventoryMenu.broadcastFullState();return;}
        if(r.action()==1||r.action()==2){var chest=accessible(p,BlockPos.of(r.target()));if(chest==null)return;
            if(r.action()==1){p.openMenu(chest);return;}
            // Deposit Matching: Only normal storage with existing destinations; skip favorite slots and all equipment.
            if(!chest.isDump())return;
            for(int slot:InventoryExpansion.sortableSlots(p.getInventory())){
                if(Accessories.state(p).favorite(slot))continue;var stack=p.getInventory().getItem(slot);if(stack.isEmpty())continue;
                boolean match=false;for(int i=0;i<chest.getContainerSize();i++)if(net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack,chest.getItem(i))){match=true;break;}
                if(match)stack.shrink(chest.insert(stack,false));
            }
            p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
        }else if(r.action()==3){
            var entity=p.level().getEntity((int)r.target());
            if(entity instanceof PrototypeMount mount&&mount.isOwnedBy(p)&&mount.care.accessible(p)&&!mount.isVehicle()&&p.hasLineOfSight(mount))p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,player)->new MountMenu(id,inventory,mount),mount.getDisplayName()));
            return;
        }else if(r.action()!=0)return;
        long now=p.level().getGameTime();var last=NEXT.get(p);if(last!=null&&now<last.until()){ServerPlayNetworking.send(p,last.snapshot());return;}
        var entries=new ArrayList<HubSnapshotPayload.Entry>();
        for(var mount:p.level().getEntitiesOfClass(PrototypeMount.class,p.getBoundingBox().inflate(32),m->m.isOwnedBy(p))){if(entries.size()>=64)break;entries.add(new HubSnapshotPayload.Entry(Integer.toString(mount.getId()),"companions",mount.getName().getString(),"Deployed nearby. Health "+Math.round(mount.getHealth())+"/"+Math.round(mount.getMaxHealth())+". Distance "+Math.round(p.distanceTo(mount))+" blocks. Care requires normal reach and clear sight.",true));}
        for(var wolf:p.level().getEntitiesOfClass(darkspawn.black.ecosystem.TaigaWolf.class,p.getBoundingBox().inflate(32),w->p.getUUID().equals(w.companionOwner()))){if(entries.size()>=80)break;entries.add(new HubSnapshotPayload.Entry(Integer.toString(wolf.getId()),"companions",wolf.getName().getString(),"Dire wolf companion. Health "+Math.round(wolf.getHealth())+"/"+Math.round(wolf.getMaxHealth())+". Distance "+Math.round(p.distanceTo(wolf))+" blocks. Empty-hand interaction toggles Follow/Rest.",false));}
        boolean island=darkspawn.black.oneblock.OneBlock.LEVEL.equals(p.level().dimension());
        for(var entry:MountRegistry.get(p.level()).entries().values()){if(entries.size()>=96)break;if(entry.owner().equals(p.getUUID())&&entry.island()==island)entries.add(new HubSnapshotPayload.Entry("stored","companions",entry.type(),entry.recovery()?"Awaiting veterinary recovery. Use your veterinary kit.":"Stored in a companion capsule. Use its owned capsule to release; the hub never duplicates or remotely summons it.",false));}
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-4,-3,-4),p.blockPosition().offset(4,3,4))){var chest=accessible(p,pos);if(chest==null)continue;if(entries.size()>=128)break;
            int used=0;for(int i=0;i<chest.getContainerSize();i++)if(!chest.getItem(i).isEmpty())used++;
            entries.add(new HubSnapshotPayload.Entry(Long.toString(pos.asLong()),"storage",chest.getDisplayName().getString(),pos.toShortString()+" | "+(chest.isBank()?chest.variants()+"/"+chest.capacity()+" variants":used+"/"+chest.getContainerSize()+" slots")+". Open to inspect filters, routing and refilter controls. Deposit matching is available for Dump Chests.",true));
        }
        var snapshot=new HubSnapshotPayload(List.copyOf(entries));NEXT.put(p,new Reply(now+10,snapshot));ServerPlayNetworking.send(p,snapshot);
    }
}
