package darkspawn.black.boss;

import darkspawn.black.Darkspawn;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Items;

public final class EquipmentPowers {
	public static final Set<String> POWERS = Set.of("shield_rootbound", "shield_stoneguard", "bow_stormshot", "bow_voidmark",
		"armor_winter", "armor_tide", "armor_shadow", "shield_frostguard", "bow_venom", "armor_highland");
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private EquipmentPowers() {}
	public static boolean wears(LivingEntity entity, String power) {
		for (EquipmentSlot slot : ARMOR) if (power.equals(entity.getItemBySlot(slot).get(BossEmpowerment.POWER))) return true;
		return false;
	}
	public static void shieldBlock(LivingEntity defender, LivingEntity attacker, float blocked) {
		if (blocked <= 0 || !defender.getUseItem().is(Items.SHIELD)) return;
		if ("shield_rootbound".equals(defender.getUseItem().get(BossEmpowerment.POWER)))
			attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
		if ("shield_frostguard".equals(defender.getUseItem().get(BossEmpowerment.POWER)))
			attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
	}
	public static void arrowHit(LivingEntity target, DamageSource source, float damage, boolean blocked) {
		if (blocked || damage <= 0 || !(source.getDirectEntity() instanceof AbstractArrow arrow)
			|| !(arrow.getOwner() instanceof Player player) || !(target.level() instanceof ServerLevel level)) return;
		// The arrow retains its firing weapon; switching held items cannot change a shot's power.
		var weapon = arrow.getWeaponItem();
		if (weapon == null) return;
		String power = weapon.get(BossEmpowerment.POWER);
		if ("bow_stormshot".equals(power)) {
			// No lightning entity: this cannot ignite terrain or recursively trigger arrow effects.
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 16, 0.4, 0.5, 0.4, 0.05);
			level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, target.getBoundingBox().inflate(4),
				e -> e != target && e.isAlive() && !player.isAlliedTo(e) && target.hasLineOfSight(e))
				.stream().limit(1).forEach(e -> e.hurtServer(level, player.damageSources().indirectMagic(arrow, player), 3));
		} else if ("bow_voidmark".equals(power)) {
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
		} else if ("bow_venom".equals(power)) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
		}
	}
	public static void update(Player player) {
		// A single transient modifier per power, irrespective of the number of matching armor pieces.
		set(player, Attributes.WATER_MOVEMENT_EFFICIENCY, "armor_tide", wears(player, "armor_tide") ? 0.35 : 0);
		set(player, Attributes.FALL_DAMAGE_MULTIPLIER, "armor_highland", wears(player, "armor_highland") ? -0.25 : 0);
		set(player, Attributes.KNOCKBACK_RESISTANCE, "shield_stoneguard",
			player.isUsingItem() && player.getUseItem().is(Items.SHIELD) && "shield_stoneguard".equals(player.getUseItem().get(BossEmpowerment.POWER)) ? 0.5 : 0);
		if (wears(player, "armor_winter")) player.setTicksFrozen(0);
		if (wears(player, "armor_shadow")) player.removeEffect(MobEffects.DARKNESS);
	}
	private static void set(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> type, String id, double amount) {
		var attribute = player.getAttribute(type);
		if (attribute == null) return;
		var key = Darkspawn.id(id);
		var current = attribute.getModifier(key);
		if (current != null && current.amount() == amount) return;
		attribute.removeModifier(key);
		if (amount != 0) attribute.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_VALUE));
	}
	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> arrowHit(entity, source, taken, blocked));
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(EquipmentPowers::update));
	}
}
