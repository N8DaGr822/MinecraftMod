package darkspawn.black.cooking;
import net.minecraft.core.BlockPos;
public final class AnimalUtilityState {
    public long readyAt;
    public int egg;
    public BlockPos forage;
    public long forageUntil;
    public interface Holder { AnimalUtilityState darkspawn$animalUtility(); }
}
