package darkspawn.black.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public record BankContents(List<Entry> stored, List<Entry> trash, List<ItemStack> filters, int tier) {
	public static final int MAX_COUNT = 1_000_000;
	public static final BankContents EMPTY = new BankContents(List.of(), List.of(), List.of(), 0);
	public static final Codec<BankContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Entry.CODEC.listOf(0, 2048).fieldOf("stored").forGetter(BankContents::stored),
		Entry.CODEC.listOf(0, 2048).fieldOf("trash").forGetter(BankContents::trash),
		ItemStack.OPTIONAL_CODEC.listOf(0, 3).fieldOf("filters").forGetter(BankContents::filters),
		Codec.intRange(0, 3).fieldOf("tier").forGetter(BankContents::tier)
	).apply(instance, BankContents::new));

	public BankContents {
		stored = List.copyOf(stored);
		trash = List.copyOf(trash);
		filters = filters.stream().map(ItemStack::copy).toList();
	}

	public record Entry(ItemStack item, int count) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.CODEC.fieldOf("item").forGetter(Entry::item),
			Codec.intRange(1, MAX_COUNT).fieldOf("count").forGetter(Entry::count)
		).apply(instance, Entry::new));

		public Entry { item = item.copyWithCount(1); }
	}
}
