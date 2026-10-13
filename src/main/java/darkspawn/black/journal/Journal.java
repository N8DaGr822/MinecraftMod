package darkspawn.black.journal;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossKind;
import darkspawn.black.cooking.CookingRecipe;
import darkspawn.black.cooking.Cuisine;
import darkspawn.black.cooking.Culinary;
import darkspawn.black.forsaken.Forsaken;
import darkspawn.black.health.BossHearts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Journal {
    private record Reply(long tick, JournalSnapshotPayload snapshot) { }
    private static final WeakHashMap<ServerPlayer, Reply> REQUESTS = new WeakHashMap<>();
    private Journal() { }
    public static JournalProgress progress(Player player) { return ((JournalPlayer)player).darkspawn$journal(); }
    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(JournalRequestPayload.TYPE, JournalRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(JournalSnapshotPayload.TYPE, JournalSnapshotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(JournalRequestPayload.TYPE, (payload, context) -> {
            var player = context.player(); long now = player.level().getServer().overworld().getGameTime();
            var last = REQUESTS.get(player);
            // Reopening within the throttle window must still receive a reply, never an endless loading screen.
            if (last == null || now < last.tick() || now-last.tick() >= 20) { last = new Reply(now, snapshot(player)); REQUESTS.put(player,last); }
            ServerPlayNetworking.send(player, last.snapshot());
        });
        ServerPlayerEvents.COPY_FROM.register((old, next, alive) -> progress(next).restore(progress(old).saved()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                // Discovery Budget: Stagger players; inspect nearby entities and loaded structure references once per two seconds.
                if (Math.floorMod(server.getTickCount() + player.getId(), 40) == 0) observe(player);
            }
        });
    }
    public static void observe(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        var level = player.level(); var progress = progress(player);
        level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24), entity -> entity.isAlive()
            && player.distanceToSqr(entity) <= 24*24
            && JournalCatalog.entries().containsKey("entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath())).stream().sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(128)
            .forEach(entity -> {
                var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (id.getNamespace().equals(Darkspawn.MOD_ID) && player.hasLineOfSight(entity)) progress.discover("entity/" + id.getPath());
            });
        var chunk = level.getChunkSource().getChunkNow(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
        if (chunk == null) return;
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        // Loaded Starts Only: References may point outside simulation range; never load those chunks for a journal entry.
        chunk.getAllReferences().forEach((structure, references) -> {
            var id = structures.getKey(structure);
            if (id == null || !id.getNamespace().equals(Darkspawn.MOD_ID) || !JournalCatalog.entries().containsKey("landmark/"+id.getPath())) return;
            for (long reference : references) {
                var at = net.minecraft.world.level.ChunkPos.unpack(reference);
                var startChunk = level.getChunkSource().getChunkNow(at.x(), at.z()); if (startChunk == null) continue;
                var start = startChunk.getAllStarts().get(structure);
                if (start != null && start.isValid() && start.getBoundingBox().isInside(player.blockPosition())) progress.discover("landmark/"+id.getPath());
            }
        });
    }
    public static JournalSnapshotPayload snapshot(ServerPlayer player) {
        var result = new ArrayList<JournalSnapshotPayload.Entry>();
        var progress = progress(player); var hearts = BossHearts.progress(player); boolean researched = Forsaken.state(player).researched;
        for (var entry : JournalCatalog.entries().values()) {
            boolean known = progress.knows(entry.id()) || entry.section().equals("overview") || entry.id().startsWith("forsaken/") && researched;
            int kills = 0; boolean consumed = false;
            if (!entry.entity().isEmpty()) kills = player.getStats().getValue(Stats.ENTITY_KILLED, BuiltInRegistries.ENTITY_TYPE.getValue(Darkspawn.id(entry.entity())));
            if (!entry.boss().isEmpty() && entry.section().equals("bosses")) {
                consumed = hearts.hasConsumed(BossKind.valueOf(entry.boss().toUpperCase(java.util.Locale.ROOT)));
                boolean landmark = JournalCatalog.entries().values().stream().anyMatch(site -> site.boss().equals(entry.boss()) && progress.knows(site.id()));
                known |= consumed || kills > 0 || landmark;
            } else known |= kills > 0;
            String body = entry.hint() + (known ? "\n\n" + entry.body() : entry.id().startsWith("forsaken/") ? "\n\nUse a research archive to unlock this expedition entry." : "\n\nUndiscovered. Observe this creature or visit this landmark to learn more.");
            if (entry.section().equals("bosses")) body += "\n\nCredited kills: " + kills + " | Heart consumed: " + (consumed ? "yes" : "no");
            if (entry.id().equals("guide/starting")) body += "\n\nDiscoveries: " + progress.saved().size() + " | Hearts: " + hearts.savedBosses().size() + "/15 | Distinct meals: " + Culinary.progress(player).meals().size() + " | Forsaken research: " + (researched ? "learned" : "not learned");
            result.add(new JournalSnapshotPayload.Entry(entry.id(),entry.section(),known ? entry.title() : "Undiscovered " + (entry.section().equals("landmarks") ? "landmark" : entry.section().equals("forsaken") ? "entry" : entry.section().equals("bosses") ? "boss" : entry.section().equals("mounts") ? "mount" : "creature"),body.strip(),known));
        }
        player.level().getServer().getRecipeManager().getRecipes().stream().filter(holder -> holder.value() instanceof CookingRecipe)
            .sorted(Comparator.comparing(holder -> holder.id().identifier().toString())).limit(300).forEach(holder -> {
                var recipe = (CookingRecipe)holder.value(); String name = holder.id().identifier().getPath(); var dish = Cuisine.DISHES.get(name);
                boolean known = Cuisine.canCook(player,name);
                var text = new StringBuilder(known ? "Cooking Station ingredients (one of each):\n" : "Find a recipe scroll: " + (dish == null ? "Explore for clues" : dish.discovery()));
                if (known) for (var ingredient : recipe.ingredients()) {
                    var alternatives = ingredient.items().limit(12).map(item -> JournalCatalog.itemName(new ItemStack(item))).toList();
                    text.append("\n- ").append(String.join(" or ",alternatives));
                }
                text.append("\n\nYour existing cookbook discoveries apply here. Meal buffs replace each other; potion effects remain independent.");
                result.add(new JournalSnapshotPayload.Entry("recipe/"+holder.id().identifier(),"cooking",JournalCatalog.itemName(recipe.resultStack()),text.toString(),known));
            });
        return new JournalSnapshotPayload(java.util.List.copyOf(result));
    }
}
