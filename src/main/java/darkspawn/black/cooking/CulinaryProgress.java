package darkspawn.black.cooking;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Saved discoveries are permanent; dietary variety counts the last eight eating events. */
public final class CulinaryProgress {
	private final List<String> recent = new ArrayList<>();
	private final Set<String> meals = new LinkedHashSet<>();
	private final Set<String> recipes = new LinkedHashSet<>();
	public void eat(String food, boolean meal) {
		recent.add(food);
		while (recent.size() > 8) recent.removeFirst();
		if (meal) meals.add(food);
	}
	public int variety() { return (int) recent.stream().distinct().count(); }
	public float saturationBonus() { return variety() >= 5 ? 0.10F : variety() >= 3 ? 0.05F : 0; }
	public boolean unlock(String recipe) { return recipes.add(recipe); }
	public boolean knows(String recipe) { return recipes.contains(recipe); }
	public List<String> recent() { return List.copyOf(recent); }
	public List<String> meals() { return List.copyOf(meals); }
	public List<String> recipes() { return List.copyOf(recipes); }
	public void restore(List<String> foods, List<String> eaten, List<String> known) {
		recent.clear(); meals.clear(); recipes.clear();
		foods.stream().skip(Math.max(0, foods.size() - 8)).forEach(recent::add);
		meals.addAll(eaten); recipes.addAll(known);
	}
}
