package darkspawn.black.journal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Personal discoveries only; recipes, hearts and kills retain their existing owners. */
public final class JournalProgress {
    private final Set<String> discovered = new LinkedHashSet<>();
    public boolean knows(String id) { return discovered.contains(id); }
    public boolean discover(String id) {
        return JournalCatalog.entries().containsKey(id) && discovered.add(id);
    }
    public List<String> saved() { return List.copyOf(discovered); }
    public void restore(List<String> ids) { discovered.clear(); ids.stream().limit(512).forEach(this::discover); }
}
