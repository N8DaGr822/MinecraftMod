package darkspawn.black.pirate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Shared guidance only: personal progression and ship permissions never transfer. */
public final class PartyNavigation extends SavedData {
    public record Link(UUID member, UUID leader) {
        static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("member").forGetter(Link::member),
            UUIDUtil.CODEC.fieldOf("leader").forGetter(Link::leader)).apply(i, Link::new));
    }
    private record Invitation(UUID leader, long expires) { }
    public static final Codec<PartyNavigation> CODEC = Link.CODEC.listOf().xmap(PartyNavigation::new,
        party -> party.leaders.entrySet().stream().map(e -> new Link(e.getKey(), e.getValue())).toList());
    public static final SavedDataType<PartyNavigation> TYPE = new SavedDataType<>(Darkspawn.id("navigation_parties"),
        () -> new PartyNavigation(List.of()), CODEC, null);
    private final Map<UUID, UUID> leaders = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();
    private PartyNavigation(List<Link> links) { for (var link : links) join(link.member(), link.leader()); }
    public static PartyNavigation get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public UUID leader(UUID member) { return leaders.get(member); }
    private boolean canJoin(UUID member, UUID leader) {
        return !member.equals(leader) && !leaders.containsKey(member) && !leaders.containsKey(leader)
            && !leaders.containsValue(member) && leaders.values().stream().filter(leader::equals).count() < 5;
    }
    private boolean join(UUID member, UUID leader) {
        if (!canJoin(member, leader)) return false;
        leaders.put(member, leader); setDirty(); return true;
    }
    public boolean leave(UUID player) {
        // World-owned membership lets a captain disband even while members are offline.
        boolean changed = leaders.remove(player) != null;
        changed |= leaders.values().removeIf(player::equals);
        invitations.entrySet().removeIf(e -> e.getKey().equals(player) || e.getValue().leader().equals(player));
        if (changed) setDirty();
        return changed;
    }
    public boolean invite(ServerPlayer leader, ServerPlayer member) {
        long now = leader.level().getServer().overworld().getGameTime();
        invitations.entrySet().removeIf(e -> e.getValue().expires() <= now);
        if (!nearby(leader, member) || !canJoin(member.getUUID(), leader.getUUID())) return false;
        if (Pirate.state(leader).logboundDestination.isEmpty()) Pirate.state(leader).logboundDestination = LogboundCompass.FIRST_COVE;
        invitations.put(member.getUUID(), new Invitation(leader.getUUID(), now + 1200));
        Ships.message(member, leader.getName().getString() + " invites you to follow their chart. Within 60 seconds, sneak-use your Logbound Compass on them to accept.");
        Ships.message(leader, "Navigation invitation sent. No progress or ship access is shared.");
        return true;
    }
    public boolean accept(ServerPlayer member, ServerPlayer leader) {
        var invitation = invitations.get(member.getUUID());
        long now = member.level().getServer().overworld().getGameTime();
        if (invitation == null || !invitation.leader().equals(leader.getUUID()) || invitation.expires() <= now
                || !nearby(member, leader) || !join(member.getUUID(), leader.getUUID())) return false;
        invitations.remove(member.getUUID());
        Ships.message(member, "Following " + leader.getName().getString() + "'s chart. Your personal route is preserved. Sneak-use the compass in air away from shore services to leave.");
        Ships.message(leader, member.getName().getString() + " joined your navigation party. Sneak-use in air away from shore services to disband.");
        return true;
    }
    private static boolean nearby(ServerPlayer first, ServerPlayer second) {
        return first != second && first.isAlive() && second.isAlive() && !first.isSpectator() && !second.isSpectator()
            && first.level() == second.level() && first.level().dimension().equals(Pirate.LEVEL)
            && first.distanceToSqr(second) <= 36 && first.hasLineOfSight(second);
    }
    public static ServerPlayer guide(ServerPlayer player) {
        var id = get(player.level()).leader(player.getUUID());
        if (id == null) return null;
        var leader = player.level().getServer().getPlayerList().getPlayer(id);
        return leader != null && leader.isAlive() && !leader.isSpectator() && leader.level() == player.level()
            && leader.level().dimension().equals(Pirate.LEVEL) ? leader : null;
    }
    public static void initialize() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                    || !player.getItemInHand(hand).is(LogboundCompass.ITEM) || !(entity instanceof net.minecraft.world.entity.player.Player)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer server && entity instanceof ServerPlayer other) {
                var party = get(server.level());
                if (!party.accept(server, other) && !party.invite(server, other))
                    Ships.message(server, "Stand within six blocks in Pirate Seas. Leave your current party first; parties hold six sailors including the leader.");
            }
            return InteractionResult.SUCCESS;
        });
    }
}
