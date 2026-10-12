package darkspawn.black.equipment;

import com.mojang.serialization.Codec;
import darkspawn.black.Darkspawn;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArmorStyles {
	public static final List<String> STYLES = List.of("classic", "viking", "spartan", "war_chief", "knight", "berserker", "samurai", "juggernaut", "pathfinder", "reaver", "pirate", "conquistador", "ranger", "paladin", "shadowblade");
	public static final Codec<String> STYLE_CODEC = Codec.STRING.xmap(ArmorStyles::canonicalStyle, ArmorStyles::canonicalStyle).validate(value -> STYLES.contains(value)
		? com.mojang.serialization.DataResult.success(value)
		: com.mojang.serialization.DataResult.error(() -> "Unknown armor style: " + value));
	public static final DataComponentType<String> STYLE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		Darkspawn.id("armor_style"), DataComponentType.<String>builder().persistent(STYLE_CODEC)
			.networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
	private ArmorStyles() { }

	public static void initialize() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Darkspawn.id("armor_style"), ArmorStyleRecipe.SERIALIZER);
		ArmorStyleBonuses.initialize();
	}

	// Save Compatibility: Retired identities load as their replacement without losing the item's other components.
	private static String canonicalStyle(String style) {
		return switch (style) {
			case "arbalist" -> "ranger";
			case "royal_guard" -> "paladin";
			case "brawler" -> "berserker";
			default -> style;
		};
	}

	public static boolean supports(ItemStack stack) {
		return stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE) || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)
			|| stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE) || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS);
	}

	public static String style(ItemStack stack) {
		String stored = stack.get(STYLE);
		String style = stored == null ? null : canonicalStyle(stored);
		return style != null && STYLES.contains(style) ? style : "classic";
	}

	public static ItemStack apply(ItemStack original, String style) {
		if (!supports(original) || !STYLES.contains(style)) return ItemStack.EMPTY;
		// Cosmetic Ownership: Preserve the vanilla item and every unrelated component, including native trims.
		ItemStack result = original.copyWithCount(1);
		if (style.equals("classic")) result.remove(STYLE);
		else result.set(STYLE, style);
		return result;
	}
}
