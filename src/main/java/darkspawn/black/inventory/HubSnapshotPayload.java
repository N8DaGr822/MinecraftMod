package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HubSnapshotPayload(List<Entry> entries) implements CustomPacketPayload {
    public record Entry(String id, String section, String title, String body, boolean discovered) { }
    public static final Type<HubSnapshotPayload> TYPE = new Type<>(Darkspawn.id("hub_snapshot"));
    // Bounded Snapshot: No client supplied discovery state or arbitrary collection lengths.
    public static final StreamCodec<RegistryFriendlyByteBuf, HubSnapshotPayload> CODEC = new StreamCodec<>() {
        @Override public HubSnapshotPayload decode(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            if (count < 0 || count > 128) throw new IllegalArgumentException("Hub entry limit");
            var entries = new java.util.ArrayList<Entry>(count);
            for (int i=0; i<count; i++) entries.add(new Entry(buf.readUtf(128), buf.readUtf(32), buf.readUtf(256), buf.readUtf(2048), buf.readBoolean()));
            return new HubSnapshotPayload(List.copyOf(entries));
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, HubSnapshotPayload payload) {
            if (payload.entries.size() > 128) throw new IllegalArgumentException("Hub entry limit");
            buf.writeVarInt(payload.entries.size());
            for (var entry : payload.entries) {
                buf.writeUtf(entry.id,128); buf.writeUtf(entry.section,32); buf.writeUtf(entry.title,256); buf.writeUtf(entry.body,2048); buf.writeBoolean(entry.discovered);
            }
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
