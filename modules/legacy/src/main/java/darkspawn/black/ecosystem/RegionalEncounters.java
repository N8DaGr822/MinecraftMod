package darkspawn.black.ecosystem;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.Level;

/** Bounded regional encounters share participation and cleanup, with separate saved claims. */
public final class RegionalEncounters {
    public static final RegionalEncounter BLOOM = new RegionalEncounter("fungalbloom", "Fungal Bloom", "surface Overworld Mushroom Fields", Level.OVERWORLD,
        Biomes.MUSHROOM_FIELDS, RegionalKind.SPORELING, true, Items.RED_MUSHROOM);
    public static final RegionalEncounter SHADOW = new RegionalEncounter("shadowincursion", "Shadow Incursion", "Overworld Deep Dark", Level.OVERWORLD,
        Biomes.DEEP_DARK, RegionalKind.SHADOW_DRONE, false, Items.SCULK);
    public static final RegionalEncounter SOUL = new RegionalEncounter("soulstorm", "Soul Storm", "Nether Soul Sand Valley", Level.NETHER,
        Biomes.SOUL_SAND_VALLEY, RegionalKind.LOST_SOUL, false, Items.SOUL_TORCH);
    public static final RegionalEncounter RIFT = new RegionalEncounter("endrift", "End Rift", "End islands", Level.END,
        Biomes.END_HIGHLANDS, RegionalKind.VOIDLING, false, Items.CHORUS_FRUIT);
    private static final List<RegionalEncounter> EVENTS = List.of(BLOOM, SHADOW, SOUL, RIFT);
    private RegionalEncounters() { }
    private static RegionalEncounter event(Entity mob) { return EVENTS.stream().filter(e -> e.combatant(mob)).findFirst().orElse(null); }
    public static boolean combatant(Entity mob) { return event(mob) != null; }
    public static boolean canTarget(Entity mob, LivingEntity target) { var event=event(mob); return event != null && event.canTarget(mob,target); }
    public static void tickMob(RegionalMob mob) { var event=event(mob); if(event != null) event.tickMob(mob); }
    public static void initialize() { EVENTS.forEach(RegionalEncounter::initialize); }
}
