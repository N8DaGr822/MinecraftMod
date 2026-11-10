package darkspawn.black.worldgen;

import darkspawn.black.Darkspawn;
import darkspawn.black.journal.JournalCatalog;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** Explicit bounded searches for existing native boss landmarks, without revealing Journal knowledge. */
public final class LandmarkCompass extends Item {
    public static final List<JournalCatalog.Entry> SITES = JournalCatalog.entries().values().stream()
        .filter(entry -> "landmarks".equals(entry.section()) && entry.boss() != null && !entry.boss().isEmpty()).toList();
    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("landmark_compass"), new LandmarkCompass());
    private static final WeakHashMap<ServerPlayer, Long> SEARCH_AFTER = new WeakHashMap<>();
    private LandmarkCompass() {
        super(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("landmark_compass"))).stacksTo(1)
            .component(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false))
            .component(DataComponents.LORE, new ItemLore(List.of(Component.literal("Sneak-use: select landmark. Use: search nearby regions."),
                Component.literal("Finds ritual sites, not bosses. Normal summon rules still apply.")))));
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ITEM));
    }
    public static JournalCatalog.Entry selected(ItemStack stack) {
        String site = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("DarkspawnLandmark", "");
        return SITES.stream().filter(entry -> entry.site().equals(site)).findFirst().orElse(SITES.getFirst());
    }
    public static void cycle(ItemStack stack) {
        var next = SITES.get((SITES.indexOf(selected(stack)) + 1) % SITES.size());
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("DarkspawnLandmark", next.site());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Landmark Compass: " + next.title()));
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
    }
    public static ResourceKey<Level> dimension(JournalCatalog.Entry site) {
        return switch (site.boss()) {
            case "netherborn", "soulbound_colossus" -> Level.NETHER;
            case "void_eye" -> Level.END;
            default -> Level.OVERWORLD;
        };
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && player.isAlive() && !player.isSpectator()) {
            var stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown()) { cycle(stack); player.sendSystemMessage(Component.literal("Selected " + selected(stack).title() + ". Use in " + dimension(selected(stack)).identifier() + " to search.")); }
            else search(server, stack);
        }
        return InteractionResult.SUCCESS;
    }
    public static boolean search(ServerPlayer player, ItemStack stack) {
        if (!stack.is(ITEM) || !player.isAlive() || player.isSpectator()) return false;
        var site = selected(stack); var level = player.level();
        if (!level.dimension().equals(dimension(site))) {
            player.sendSystemMessage(Component.literal(site.title() + " belongs in " + dimension(site).identifier() + ". Existing chart retained.")); return false;
        }
        long now = level.getServer().overworld().getGameTime();
        if (now < SEARCH_AFTER.getOrDefault(player, Long.MIN_VALUE)) {
            player.sendSystemMessage(Component.literal("Wait ten seconds between landmark searches.")); return false;
        }
        SEARCH_AFTER.put(player, now + 200);
        var pos = locate(player, site.site());
        if (pos == null) {
            player.sendSystemMessage(Component.literal("No " + site.title() + " found in nearby candidate regions. Travel farther and retry; any existing chart is retained.")); return false;
        }
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos)), false));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Landmark Compass: " + site.title()));
        player.sendSystemMessage(Component.literal(site.title() + ": " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ". " + site.hint()));
        return true;
    }
    private static BlockPos locate(ServerPlayer player, String site) {
        var level = player.level();
        if (!level.getServer().getWorldGenSettings().options().generateStructures()) return null;
        var holder = player.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, Darkspawn.id(site))).orElse(null);
        if (holder == null) return null;
        var state = level.getChunkSource().getGeneratorState(); var candidates = new ArrayList<BlockPos>();
        for (var placement : state.getPlacementsForStructure(holder)) {
            if (!(placement instanceof RandomSpreadStructurePlacement spread)) continue;
            int rx = Math.floorDiv(player.blockPosition().getX() >> 4, spread.spacing()), rz = Math.floorDiv(player.blockPosition().getZ() >> 4, spread.spacing());
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                var chunk = spread.getPotentialStructureChunk(state.getLevelSeed(), (rx + x) * spread.spacing(), (rz + z) * spread.spacing());
                var center = new BlockPos(chunk.getMiddleBlockX(), player.blockPosition().getY(), chunk.getMiddleBlockZ());
                if (level.getWorldBorder().isWithinBounds(center)) candidates.add(center);
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())));
        // Bounded Search: Reuse the Logbound candidate approach; never place templates or consume shared references.
        for (var center : candidates.stream().distinct().limit(25).toList()) {
            var chunk = level.getChunk(center.getX() >> 4, center.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS);
            var start = chunk.getAllStarts().get(holder.value());
            if (start != null && start.isValid()) {
                var target = start.getBoundingBox().getCenter();
                if (level.getWorldBorder().isWithinBounds(target)) return target;
            }
        }
        return null;
    }
}
