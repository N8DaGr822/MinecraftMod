package darkspawn.black.equipment;

import darkspawn.black.Darkspawn;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArmorStyleBonuses {
	private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
	private static final java.util.Map<java.util.UUID, Long> SHADOW_READY = new java.util.HashMap<>();
	private ArmorStyleBonuses() { }

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			server.getPlayerList().getPlayers().forEach(ArmorStyleBonuses::update);
			SHADOW_READY.values().removeIf(ready -> ready <= server.overworld().getGameTime());
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> SHADOW_READY.clear());
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && taken > 0 && !blocked) shadowHit(player);
		});
	}

	private static void shadowHit(ServerPlayer player) {
		if (!fullStyle(player).equals("shadowblade")) return;
		long now = player.level().getServer().overworld().getGameTime();
		if (SHADOW_READY.getOrDefault(player.getUUID(), Long.MIN_VALUE) > now) return;
		// Cooldown Ownership: Reserve before applying the effect; swapping gear, reconnecting, or respawning cannot reset it.
		SHADOW_READY.put(player.getUUID(), now + 300);
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 40, 0));
	}

	public static String fullStyle(LivingEntity entity) {
		if (!entity.isAlive() || entity.isSpectator()) return "";
		String style = null;
		for (EquipmentSlot slot : ARMOR) {
			var stack = entity.getItemBySlot(slot);
			var equippable = stack.get(DataComponents.EQUIPPABLE);
			if (!ArmorStyles.supports(stack) || equippable == null || equippable.slot() != slot) return "";
			String pieceStyle = ArmorStyles.style(stack);
			if (style != null && !style.equals(pieceStyle)) return "";
			style = pieceStyle;
		}
		return style;
	}

	public static void update(ServerPlayer player) {
		String style = fullStyle(player);
		// Modifier Ownership: Only replace this feature's transient modifiers; meals, potions and powers remain intact.
		set(player, Attributes.ARMOR_TOUGHNESS, "style_knight", style.equals("knight") ? 2 : 0, AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.ATTACK_DAMAGE, "style_war_chief", style.equals("war_chief") ? 1 : 0, AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.ATTACK_SPEED, "style_berserker", style.equals("berserker") ? .10 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		set(player, Attributes.ATTACK_DAMAGE, "style_brawler", 0, AttributeModifier.Operation.ADD_VALUE);
		boolean hadRoyalGuard = player.getAttribute(Attributes.MAX_HEALTH).getModifier(Darkspawn.id("style_royal_guard")) != null;
		set(player, Attributes.MAX_HEALTH, "style_royal_guard", 0, AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.KNOCKBACK_RESISTANCE, "style_juggernaut", style.equals("juggernaut") ? .15 : 0, AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.MOVEMENT_SPEED, "style_pathfinder", style.equals("pathfinder") ? .05 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		// Retired Bonuses: Remove only old feature modifiers and clamp health if Royal Guard was active.
		if (hadRoyalGuard && player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
	}

	private static void set(ServerPlayer player, Holder<Attribute> type, String id, double amount, AttributeModifier.Operation operation) {
		var attribute = player.getAttribute(type);
		if (attribute == null) return;
		var key = Darkspawn.id(id);
		var current = attribute.getModifier(key);
		if (current != null && current.amount() == amount && current.operation() == operation) return;
		attribute.removeModifier(key);
		if (amount != 0) attribute.addTransientModifier(new AttributeModifier(key, amount, operation));
	}

	public static float meleeDamage(DamageSource source, float damage) {
		if (damage <= 0 || !(source.getDirectEntity() instanceof ServerPlayer player)
				|| source.getEntity() != player || !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.SPEAR) || source.is(DamageTypes.MACE_SMASH))) return damage;
		String style = fullStyle(player);
		ItemStack weapon = source.getWeaponItem();
		if (weapon == null) return damage;
		if (style.equals("reaver") && weapon.is(Items.MACE) || style.equals("pirate") && weapon.is(Items.TRIDENT)) return damage * 1.10F;
		return style.equals("spartan") && weapon.is(ItemTags.SPEARS) || style.equals("viking") && weapon.is(ItemTags.AXES)
			|| style.equals("samurai") && weapon.is(ItemTags.SWORDS)
			? damage * 1.10F : damage;
	}

	public static float tridentDamage(DamageSource source, float damage) {
		// Thrown Weapons: Check the actual projectile and owner, not the item currently held after throwing.
		return damage > 0 && source.is(DamageTypes.TRIDENT)
			&& source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident
			&& source.getEntity() instanceof ServerPlayer player && fullStyle(player).equals("pirate")
			? damage * 1.10F : damage;
	}

	public static float rangedDamage(DamageSource source, float damage) {
		if (damage <= 0 || !(source.getEntity() instanceof ServerPlayer player)) return damage;
		String style = fullStyle(player);
		// Launch Identity: Arrows retain their firing weapon, so swapping a bow for a crossbow cannot change eligibility.
		if (source.is(DamageTypes.ARROW) && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) {
			ItemStack weapon = arrow.getWeaponItem();
			if (weapon != null && style.equals("ranger") && (weapon.is(Items.BOW) || weapon.is(Items.CROSSBOW))) return damage * 1.10F;
		}
		// Crossbow Rockets: Native crossbows launch angled rockets; hand-launched and elytra fireworks are excluded.
		if (style.equals("ranger") && source.is(DamageTypes.FIREWORKS)
				&& source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity rocket && rocket.isShotAtAngle()) return damage * 1.10F;
		return damage;
	}

	public static int armorWear(LivingEntity entity, ItemStack stack, EquipmentSlot slot, int amount) {
		if (amount <= 0 || !(entity instanceof ServerPlayer) || entity.getItemBySlot(slot) != stack
				|| !java.util.Arrays.asList(ARMOR).contains(slot) || !fullStyle(entity).equals("classic")) return amount;
		// Fractional Wear: Randomized rounding preserves the 20% saving even for one-point hits, before Unbreaking.
		double reduced = amount * .80;
		int whole = (int) reduced;
		return whole + (entity.getRandom().nextDouble() < reduced - whole ? 1 : 0);
	}

}
