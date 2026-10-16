package darkspawn.black.mixin;

import darkspawn.black.pirate.Pirate;
import darkspawn.black.pirate.PirateIslandBiomes;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class PirateIslandBiomeMixin {
	@Inject(method="applyBiomeDecoration",at=@At("TAIL"))
	private void darkspawn$islandBiomes(WorldGenLevel level,ChunkAccess chunk,StructureManager structures,CallbackInfo ci) {
		if(level.getLevel().dimension().equals(Pirate.LEVEL))PirateIslandBiomes.apply(chunk,structures);
	}
}
