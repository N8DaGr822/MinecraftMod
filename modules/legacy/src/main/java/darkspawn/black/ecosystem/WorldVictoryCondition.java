package darkspawn.black.ecosystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossKind;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.*;

public record WorldVictoryCondition(BossKind boss) implements LootItemCondition {
    public static final MapCodec<WorldVictoryCondition> CODEC=Codec.STRING.comapFlatMap(id->java.util.Arrays.stream(BossKind.values()).filter(k->k.id().equals(id)).findFirst().map(com.mojang.serialization.DataResult::success).orElseGet(()->com.mojang.serialization.DataResult.error(()->"Unknown boss family: "+id)),BossKind::id).fieldOf("boss").xmap(WorldVictoryCondition::new,WorldVictoryCondition::boss);
    public static final MapCodec<WorldVictoryCondition> TYPE=Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE,Darkspawn.id("world_victory"),CODEC);
    @Override public MapCodec<WorldVictoryCondition> codec(){return TYPE;}
    @Override public boolean test(LootContext context){return WorldAwakening.get(context.getLevel()).defeated(boss);}
    public static void initialize(){ }
}
