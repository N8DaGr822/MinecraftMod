package darkspawn.black.boss;

import com.mojang.serialization.Codec;
import darkspawn.black.Darkspawn;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;

public final class BossEmpowerment {
	public static final String ROOTBOUND = "rootbound";
	public static final String PREDATORS_RUSH = "predators_rush";
	public static final DataComponentType<String> POWER = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			Darkspawn.id("boss_power"), DataComponentType.<String>builder().persistent(Codec.STRING)
					.networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	private static final Map<Player, Map<String, Long>> COOLDOWNS = new WeakHashMap<>();

	private BossEmpowerment() {
	}

	public static ItemStack apply(ItemStack original) {
		return apply(original, ROOTBOUND);
	}

	public static ItemStack apply(ItemStack original, String power) {
		// Smithing Preservation: Keep enchantments, names, damage, and every unrelated component.
		ItemStack result = original.copyWithCount(1);
		result.set(POWER, power);
		return result;
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Darkspawn.id("boss_empowerment"), BossEmpowermentRecipe.SERIALIZER);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0 && source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player player) {
				String power = player.getMainHandItem().get(POWER);
				if (ROOTBOUND.equals(power)) {
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
				} else if (PREDATORS_RUSH.equals(power)) {
					// Predator's Rush: Refresh a short Speed II effect; repeated hits never stack its strength.
					player.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 1));
				} else if (power != null) { applyBiomePower(player, target, power); }
			}
		});
	}

	public static boolean isKnownPower(String power) {
		if (ROOTBOUND.equals(power) || PREDATORS_RUSH.equals(power)) { return true; }
		for (BossProfile profile : BossProfile.values()) { if (profile.power.equals(power)) { return true; } }
		return false;
	}

	private static boolean ready(Player player, String power, int delay) {
		long now = player.level().getGameTime();
		var cooldowns = COOLDOWNS.computeIfAbsent(player, ignored -> new HashMap<>());
		if (now < cooldowns.getOrDefault(power, Long.MIN_VALUE)) { return false; }
		cooldowns.put(power, now + delay);
		return true;
	}

	private static void applyBiomePower(Player player, LivingEntity target, String power) {
		switch (power) {
			case "undying_fury" -> player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0));
			case "armor_rend" -> target.addEffect(new MobEffectInstance(BossEffects.FRACTURED_ARMOR, 100, 0));
			case "stormchain" -> {
				if (target.level() instanceof ServerLevel level && ready(player, power, 40)) {
					// Chain Damage: Use magic attribution so secondary hits cannot recursively trigger weapon powers.
					level.getEntitiesOfClass(Monster.class, target.getBoundingBox().inflate(6),
							enemy -> enemy != target && enemy.isAlive() && player.hasLineOfSight(enemy)).stream().limit(3).forEach(enemy -> {
						enemy.hurtServer(level, player.damageSources().indirectMagic(player, player), 4);
						level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, enemy.getX(), enemy.getY() + 1,
								enemy.getZ(), 16, 0.5, 1, 0.5, 0.1);
					});
				}
			}
			case "venomfang" -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
			case "hexweaver" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
			case "stoneguard" -> player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
			case "winterbite" -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3));
			case "tidecaller" -> {
				player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 160, 0));
				player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 160, 0));
			}
			case "broodmark" -> {
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
				target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
			}
			case "shadowbane" -> {
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
			}
			case "mycelial_mending" -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
			case "netherflame" -> {
				target.igniteForSeconds(4);
				player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));
			}
			case "soul_siphon" -> { if (ready(player, power, 80)) { player.heal(2); BossEffects.returnHeart(player); } }
			case "voidstep" -> {
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 2));
				player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
			}
			default -> { }
		}
	}
}
