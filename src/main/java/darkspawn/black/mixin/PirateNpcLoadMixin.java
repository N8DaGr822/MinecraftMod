package darkspawn.black.mixin;

import com.mojang.serialization.Codec;
import darkspawn.black.pirate.PirateDiplomacy;
import darkspawn.black.pirate.PirateProgression;
import darkspawn.black.pirate.PirateShip;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class PirateNpcLoadMixin {
    // Peaceful Reload: Native monster factories reject creation before saved NPC tags are loaded.
    @Inject(method="create(Lnet/minecraft/world/level/storage/ValueInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Ljava/util/Optional;",at=@At("HEAD"),cancellable=true)
    private static void darkspawn$loadFriendlyNpc(ValueInput input,Level level,EntitySpawnRequest request,CallbackInfoReturnable<Optional<Entity>> callback) {
        if(request.ignoreChecks()||level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL)return;
        String id=input.getStringOr("id","");
        if(!id.equals("minecraft:piglin")&&!id.equals("minecraft:pillager")&&!id.equals("minecraft:husk"))return;
        var tags=input.read("Tags",Codec.STRING.listOf()).orElse(List.of());
        boolean crew=tags.contains(PirateShip.CREW_TAG)&&tags.stream().anyMatch(tag->tag.startsWith(PirateShip.CREW_OWNER));
        boolean contact=tags.contains(PirateDiplomacy.CONTACT_TAG)&&java.util.Arrays.stream(PirateProgression.Faction.values()).anyMatch(faction->tags.contains(PirateDiplomacy.FACTION_TAG+faction.id));
        if(crew||contact)callback.setReturnValue(EntityType.create(input,level,new EntitySpawnRequest(request.reason(),true)));
    }
}
