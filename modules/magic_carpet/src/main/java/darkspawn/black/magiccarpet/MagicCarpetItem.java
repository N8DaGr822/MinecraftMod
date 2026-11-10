package darkspawn.black.magiccarpet;

import darkspawn.black.mountkit.MountCompanions;

import darkspawn.black.core.DarkspawnCore;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class MagicCarpetItem extends Item {
    public static Item ITEM;
    private MagicCarpetItem(Properties properties) { super(properties); }
    public static void initialize() {
        var key=ResourceKey.create(Registries.ITEM,DarkspawnCore.id("magic_carpet"));
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new MagicCarpetItem(new Properties().setId(key).stacksTo(1)
            .component(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(java.util.List.of(Component.translatable("tooltip.darkspawn.magic_carpet"))))));
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries -> entries.accept(ITEM));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        var level=player.level(); var pos=context.getClickedPos().above();
        if (context.getClickedFace()!=Direction.UP || !player.isAlive() || player.isSpectator() || !player.mayBuild()
            || player.distanceToSqr(Vec3.atCenterOf(pos))>64 || !level.mayInteract(player,pos)
            || !level.mayInteract(player,context.getClickedPos())
            || level.dimension()!=Level.OVERWORLD && level.dimension()!=Level.NETHER && level.dimension()!=Level.END) return InteractionResult.FAIL;
        var carpet=new MagicCarpet(MagicCarpetMod.MAGIC_CARPET,level);
        carpet.setPos(Vec3.atBottomCenterOf(pos)); carpet.setYRot(player.getYRot());
        // Placement Transaction: Bind only the newly created entity; consume after a safe insertion.
        if (!MountCompanions.safePlacement(level,carpet)) { carpet.discard(); player.sendSystemMessage(Component.translatable("message.darkspawn.magic_carpet_blocked")); return InteractionResult.FAIL; }
        carpet.assignOwner(player.getUUID());
        if (!level.addFreshEntity(carpet)) { carpet.discard(); return InteractionResult.FAIL; }
        if (!player.isCreative()) context.getItemInHand().shrink(1);
        player.sendSystemMessage(Component.translatable("message.darkspawn.magic_carpet_placed"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
