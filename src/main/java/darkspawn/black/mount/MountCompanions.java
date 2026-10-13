package darkspawn.black.mount;

import darkspawn.black.Darkspawn;
import darkspawn.black.oneblock.OneBlock;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class MountCompanions {
    public static final MenuType<MountMenu> MENU = Registry.register(BuiltInRegistries.MENU, Darkspawn.id("mount_care"),
        new MenuType<>(MountMenu::new, FeatureFlags.VANILLA_SET));
    public static final Item CAPSULE = register("companion_capsule", false);
    public static final Item VET_KIT = register("veterinary_kit", true);
    private MountCompanions() { }
    private static Item register(String name, boolean kit) {
        var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).stacksTo(1).component(DataComponents.LORE,
            new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                Component.translatable("tooltip.darkspawn." + name + ".use"),
                Component.translatable("tooltip.darkspawn." + name + ".detail"))))) {
            @Override public boolean canFitInsideContainerItems() { return kit; }
            @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
                if (!(entity instanceof PrototypeMount mount)) return InteractionResult.PASS;
                if (player instanceof ServerPlayer server) {
                    boolean ok = kit ? heal(server, mount, stack) : capture(server, mount, stack);
                    message(server, ok ? kit ? "mount_healed" : "mount_saved" : "mount_action_failed");
                }
                return InteractionResult.SUCCESS;
            }
            @Override public InteractionResult useOn(UseOnContext context) {
                if (kit) return super.useOn(context);
                if (context.getPlayer() instanceof ServerPlayer player) {
                    boolean ok = release(player, context.getItemInHand(), context.getClickedPos().relative(context.getClickedFace()));
                    message(player, ok ? "mount_released" : "mount_release_failed");
                }
                return InteractionResult.SUCCESS;
            }
            @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
                if (player instanceof ServerPlayer server && kit) reclaim(server);
                return InteractionResult.SUCCESS;
            }
        });
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> { e.accept(CAPSULE); e.accept(VET_KIT); });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> reclaim(handler.player));
    }
    static void message(Player player, String key) { player.sendOverlayMessage(Component.translatable("message.darkspawn." + key)); }
    static boolean consume(Player player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(item)) { stack.consume(1, player); player.getInventory().setChanged(); return true; }
        }
        return false;
    }
    public static boolean cargoAllowed(ItemStack stack) {
        return !stack.is(CAPSULE) && !stack.is(ItemTags.SHULKER_BOXES) && stack.getItem().canFitInsideContainerItems()
            && !stack.has(darkspawn.black.storage.Storage.BANK_CONTENTS) && !stack.has(DataComponents.CONTAINER) && !stack.has(DataComponents.BUNDLE_CONTENTS);
    }
    public static String token(ItemStack stack) {
        return stack.is(CAPSULE) ? stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("MountToken", "") : "";
    }
    private static void token(ItemStack stack, String id, Component name, boolean recovery) {
        var tag = new CompoundTag(); tag.putString("MountToken", id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(recovery ? "item.darkspawn.mount_recovery_token" : "item.darkspawn.mount_stored", name));
    }
    private static String store(PrototypeMount mount, boolean recovery) {
        var level = (ServerLevel)mount.level();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        mount.saveWithoutId(output);
        return MountRegistry.get(level).store(new MountRegistry.Entry(mount.getOwnerReference().getUUID(), mount.getUUID(),
            BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType()).toString(), output.buildResult(), recovery, OneBlock.island(level)));
    }
    private static void closeMenus(PrototypeMount mount) {
        for (var player : ((ServerLevel) mount.level()).players())
            if (player.containerMenu instanceof MountMenu menu && menu.mount == mount) player.closeContainer();
    }
    public static boolean capture(ServerPlayer player, PrototypeMount mount, ItemStack capsule) {
        if (!capsule.is(CAPSULE) || !token(capsule).isEmpty() || !mount.isOwnedBy(player) || !mount.care.accessible(player)
                || !mount.care.available() || mount.isLeashed()) return false;
        closeMenus(mount);
        String id = store(mount, false);
        token(capsule, id, mount.getName(), false);
        mount.discard(); player.getInventory().setChanged();
        return true;
    }
    public static void defeated(PrototypeMount mount) {
        var level = (ServerLevel)mount.level();
        mount.ejectPassengers(); closeMenus(mount);
        mount.care.cancelOffer(); mount.care.setFollowing(false);
        mount.setHealth(Math.max(1, mount.getMaxHealth() / 4)); mount.clearFire(); mount.removeAllEffects();
        String id = store(mount, true);
        var owner = level.getServer().getPlayerList().getPlayer(mount.getOwnerReference().getUUID());
        if (owner != null && OneBlock.island(owner.level()) == OneBlock.island(level)) giveToken(owner, id, mount.getName(), true);
        mount.discard();
    }
    private static void giveToken(ServerPlayer player, String id, Component name, boolean recovery) {
        var stack = new ItemStack(CAPSULE); token(stack, id, name, recovery);
        if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }
    public static void reclaim(ServerPlayer player) {
        // Lost Tickets: Reissuing a recovery token is safe because its server record can be redeemed only once.
        for (var saved : MountRegistry.get(player.level()).entries().entrySet()) {
            var entry = saved.getValue();
            if (!entry.recovery() || !entry.owner().equals(player.getUUID()) || entry.island() != OneBlock.island(player.level())) continue;
            boolean hasToken = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (token(player.getInventory().getItem(i)).equals(saved.getKey())) hasToken = true;
            if (!hasToken) { giveToken(player, saved.getKey(), Component.translatable("entity." + entry.type().replace(':', '.')), true); return; }
        }
    }
    public static boolean safePlacement(ServerLevel level, PrototypeMount mount) {
        var bounds = mount.getBoundingBox(); var pos = mount.blockPosition();
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(bounds) || bounds.minY < level.getMinY()
                || bounds.maxY > level.getMaxY() || !level.noCollision(mount)) return false;
        boolean water = false, submerged = true;
        for (var cursor : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX - .001, bounds.maxY - .001, bounds.maxZ - .001))) {
            if (!level.hasChunkAt(cursor)) return false;
            var fluid = level.getFluidState(cursor);
            if (fluid.is(net.minecraft.tags.FluidTags.LAVA)) return false;
            var block = level.getBlockState(cursor);
            if (block.is(net.minecraft.tags.BlockTags.FIRE) || block.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
                    || block.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) || block.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE)) return false;
            water |= fluid.is(net.minecraft.tags.FluidTags.WATER);
            submerged &= fluid.is(net.minecraft.tags.FluidTags.WATER);
        }
        if (mount instanceof Seahorse) return submerged;
        if (water) return mount.canSwim();
        var floor = level.getBlockState(pos.below());
        if (floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || floor.is(net.minecraft.world.level.block.Blocks.CACTUS)) return false;
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }
    public static boolean release(ServerPlayer player, ItemStack capsule, BlockPos pos) {
        var level = player.level(); var registry = MountRegistry.get(level); String id = token(capsule);
        var entry = registry.get(id);
        if (entry == null || !entry.owner().equals(player.getUUID()) || entry.island() != OneBlock.island(level)
                || !player.isAlive() || player.isSpectator() || !player.mayBuild() || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64
                || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)) return false;
        for (var world : level.getServer().getAllLevels()) if (world.getEntity(entry.mountId()) != null) return false;
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entry.type()));
        if (type == null || !(type.create(level, EntitySpawnReason.COMMAND) instanceof PrototypeMount mount)) return false;
        mount.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entry.entity().copy()));
        mount.setPos(Vec3.atBottomCenterOf(pos)); mount.setDeltaMovement(Vec3.ZERO); mount.resetFallDistance();
        mount.clearFire(); mount.care.setFollowing(false);
        if (!safePlacement(level, mount)) { mount.discard(); return false; }
        if (entry.recovery()) {
            boolean hasKit = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) hasKit |= player.getInventory().getItem(i).is(VET_KIT);
            if (!hasKit) { mount.discard(); return false; }
            mount.setHealth(mount.getMaxHealth()); mount.removeAllEffects();
        }
        if (!level.addFreshEntity(mount)) { mount.discard(); return false; }
        registry.remove(id);
        if (entry.recovery()) consume(player, VET_KIT);
        capsule.remove(DataComponents.CUSTOM_DATA); capsule.remove(DataComponents.CUSTOM_NAME);
        player.getInventory().setChanged();
        return true;
    }
    private static boolean heal(ServerPlayer player, PrototypeMount mount, ItemStack kit) {
        if (!mount.isOwnedBy(player) || !mount.care.accessible(player) || mount.getHealth() >= mount.getMaxHealth()) return false;
        mount.heal(mount.getMaxHealth()); kit.consume(1, player); return true;
    }
}
