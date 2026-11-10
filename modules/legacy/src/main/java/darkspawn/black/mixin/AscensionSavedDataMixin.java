package darkspawn.black.mixin;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import darkspawn.black.oneblock.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SavedDataStorage.class)
public abstract class AscensionSavedDataMixin {
    @Shadow @Final private HolderLookup.Provider registries;
    @Shadow protected abstract Path getDataFile(Identifier id);

    @Inject(method="readSavedData",at=@At("HEAD"),cancellable=true)
    private <T extends SavedData> void darkspawn$preserveAscensionSave(SavedDataType<T> type,CallbackInfoReturnable<T> ci){
        if(!type.equals(AscensionLedger.TYPE)&&!type.equals(AscensionBridges.TYPE)&&!type.equals(AscensionPhases.TYPE))return;
        var file=getDataFile(type.id());if(Files.notExists(file))return;
        // Save Recovery: Reject partial decoding before vanilla can replace unreadable ownership with a fresh ledger.
        // These custom schemas migrate in their codecs; they have no vanilla data-fixer type.
        try(var input=new BufferedInputStream(Files.newInputStream(file))){
            input.mark(2);int first=input.read(),second=input.read();input.reset();
            var tag=first==31&&second==139?NbtIo.readCompressed(input,NbtAccounter.unlimitedHeap()):NbtIo.read(new DataInputStream(input));
            ci.setReturnValue(type.codec().parse(registries.createSerializationContext(NbtOps.INSTANCE),tag.get("data")).getOrThrow());
        }catch(Exception failure){
            throw new IllegalStateException("Ascension save could not be read completely: "+file+". Original file preserved. Stop the server and restore a complete world backup; do not delete this file to reset it.",failure);
        }
    }
}
