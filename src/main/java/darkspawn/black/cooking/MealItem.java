package darkspawn.black.cooking;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MealItem extends Item {
	private final Holder<MobEffect> effect;
	private final int duration;

	public MealItem(Properties properties, Holder<MobEffect> effect, int duration) {
		super(properties);
		this.effect = effect;
		this.duration = duration;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (!level.isClientSide()) {
			MealEffects.apply(entity, effect, duration);
		}
		return result;
	}

	public Component effectDescription() {
		return Component.translatable("tooltip.darkspawn.meal_effect", effect.value().getDisplayName(),
				String.format(java.util.Locale.ROOT, "%d:%02d", duration / 1200, duration / 20 % 60));
	}
	public Component effectDetail() {
		return Component.translatable("tooltip.darkspawn."
			+ net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).getPath() + "_detail");
	}

	public static Component nutritionDescription(ItemStack stack) {
		var food = stack.get(DataComponents.FOOD);
		return food == null ? Component.empty() : Component.translatable("tooltip.darkspawn.meal_nutrition", food.nutrition(),
				String.format(java.util.Locale.ROOT, "%.1f", food.saturation()));
	}
}
