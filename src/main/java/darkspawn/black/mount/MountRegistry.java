package darkspawn.black.mount;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** A token is only a claim ticket. The server owns the sole stored mount snapshot. */
public final class MountRegistry extends SavedData {
    public record Entry(UUID owner, UUID mountId, String type, CompoundTag entity, boolean recovery, boolean island) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner), UUIDUtil.CODEC.fieldOf("mount").forGetter(Entry::mountId),
            Codec.STRING.fieldOf("type").forGetter(Entry::type), CompoundTag.CODEC.fieldOf("entity").forGetter(Entry::entity),
            Codec.BOOL.fieldOf("recovery").forGetter(Entry::recovery), Codec.BOOL.fieldOf("island").forGetter(Entry::island)
        ).apply(i, Entry::new));
    }
    public static final Codec<MountRegistry> CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC).xmap(MountRegistry::new, r -> r.entries);
    public static final SavedDataType<MountRegistry> TYPE = new SavedDataType<>(Darkspawn.id("mount_registry"), () -> new MountRegistry(Map.of()), CODEC, null);
    private final Map<String, Entry> entries;
    private MountRegistry(Map<String, Entry> entries) { this.entries = new HashMap<>(entries); }
    public static MountRegistry get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE); }
    public String store(Entry entry) {
        String token = UUID.randomUUID().toString(); entries.put(token, entry); setDirty(); return token;
    }
    public Entry get(String token) { return entries.get(token); }
    public void remove(String token) { entries.remove(token); setDirty(); }
    public Map<String, Entry> entries() { return Map.copyOf(entries); }
}
