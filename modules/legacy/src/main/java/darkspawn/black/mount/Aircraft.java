package darkspawn.black.mount;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One pilot and at most nine ordinary blocks; existing server mount controls drive flight. */
public final class Aircraft extends PrototypeMount {
    private static final EntityDataAccessor<String> BUILD = SynchedEntityData.defineId(Aircraft.class, EntityDataSerializers.STRING);
    private List<AirframeBlock> blocks = List.of();
    private String renderedBuild = "";
    public Aircraft(EntityType<? extends Aircraft> type, Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() { return PrototypeMount.attributes().add(Attributes.MAX_HEALTH,40).add(Attributes.STEP_HEIGHT,0); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(BUILD, ""); }
    @Override public net.minecraft.world.item.Item tamingFood() { return Items.IRON_INGOT; }
    @Override protected String mountingMessage() { return "message.darkspawn.aircraft_mounted"; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }
    @Override public boolean canTeleport(Level from, Level to) { return MagicCarpet.allowedLevel(to) && super.canTeleport(from,to); }
    @Override public net.minecraft.world.entity.Entity teleport(net.minecraft.world.level.portal.TeleportTransition transition) {
        return MagicCarpet.allowedLevel(transition.newLevel()) ? super.teleport(transition) : null;
    }
    public List<AirframeBlock> blocks() {
        if (level().isClientSide() && !renderedBuild.equals(entityData.get(BUILD))) {
            renderedBuild=entityData.get(BUILD); var decoded=new ArrayList<AirframeBlock>();
            for (String entry:renderedBuild.split(";")) {
                var parts=entry.split(","); if (parts.length!=3 || decoded.size()==9) continue;
                try { decoded.add(new AirframeBlock(new BlockPos(Integer.parseInt(parts[0]),0,Integer.parseInt(parts[1])),Block.stateById(Integer.parseInt(parts[2])))); }
                catch (NumberFormatException ignored) { }
            }
            blocks=List.copyOf(decoded);
        }
        return blocks;
    }
    void setBuild(List<AirframeBlock> build) {
        blocks=List.copyOf(build); var encoded=new StringBuilder();
        for (var block:blocks) encoded.append(block.pos().getX()).append(',').append(block.pos().getZ()).append(',').append(Block.getId(block.state())).append(';');
        entityData.set(BUILD,encoded.toString());
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) { super.addAdditionalSaveData(output); output.store("Airframe",AirframeBlock.CODEC.listOf(),blocks); }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        var saved=input.read("Airframe",AirframeBlock.CODEC.listOf()).orElse(List.of());
        setBuild(AircraftCoreItem.validBuild(saved) ? saved : List.of());
        care.setFollowing(false);
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Aircraft Editing: Sneak-use restores the captured blocks instead of exposing animal cargo/care.
        if (player.isSecondaryUseActive()) {
            if (player instanceof ServerPlayer server && player.getItemInHand(hand).isEmpty()) {
                boolean ok=disassemble(server);
                server.sendSystemMessage(net.minecraft.network.chat.Component.translatable(ok ? "message.darkspawn.aircraft_edit" : "message.darkspawn.aircraft_edit_blocked"));
            }
            return InteractionResult.SUCCESS;
        }
        if (blocks.isEmpty()) return InteractionResult.FAIL;
        return super.mobInteract(player,hand);
    }
    boolean disassemble(ServerPlayer player) {
        if (!isOwnedBy(player) || !care.accessible(player) || !care.available() || !onGround() || !player.mayBuild()
            || getDeltaMovement().horizontalDistanceSqr()>.01 || !AircraftCoreItem.validBuild(blocks)) return false;
        var level=player.level(); var origin=blockPosition();
        // Editing Transaction: Check every destination before changing any block or returning the core.
        for (var block:blocks) {
            var pos=origin.offset(block.pos());
            if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player,pos) || !level.getBlockState(pos).isAir()
                || !level.getEntities(this,new net.minecraft.world.phys.AABB(pos)).isEmpty()) return false;
        }
        var placed=new ArrayList<BlockPos>();
        for (var block:blocks) {
            var pos=origin.offset(block.pos());
            if (!level.setBlock(pos,block.state(),3)) {
                for (var undo:placed) level.setBlock(undo,Blocks.AIR.defaultBlockState(),3);
                return false;
            }
            placed.add(pos);
        }
        setBuild(List.of()); discard();
        var core=new ItemStack(AircraftCoreItem.ITEM);
        if (!player.getInventory().add(core)) player.drop(core,false,net.minecraft.util.Prediction.SERVER_ONLY);
        return true;
    }
}
