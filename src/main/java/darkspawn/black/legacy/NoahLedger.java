package darkspawn.black.legacy;

import com.mojang.serialization.Codec;
import darkspawn.black.Darkspawn;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Server-owned bonds and cooldowns: copied relics never duplicate Thalassar or reset powers. */
public final class NoahLedger extends SavedData {
    public static final Codec<NoahLedger> CODEC = Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).xmap(NoahLedger::new, r -> r.entries);
    public static final SavedDataType<NoahLedger> TYPE = new SavedDataType<>(Darkspawn.id("noah_legacy"), () -> new NoahLedger(Map.of()), CODEC, null);
    private final Map<String, CompoundTag> entries;
    private NoahLedger(Map<String, CompoundTag> entries) { this.entries = new HashMap<>(entries); }
    public static NoahLedger get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE); }
    public CompoundTag entry(UUID owner) {
        return entries.computeIfAbsent(owner.toString(), key -> { setDirty(); return new CompoundTag(); });
    }
    public static long time(ServerLevel level) { return level.getServer().overworld().getGameTime(); }
}
