package darkspawn.black.oneblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Island Features: Keep upgrades and the active encounter binding in the same save as phase completion. */
public final class AscensionFeatures {
    public static final Codec<AscensionFeatures> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.INT.fieldOf("schema").forGetter(f->f.schema),Codec.BOOL.fieldOf("collector").forGetter(f->f.collector),
        Codec.BOOL.fieldOf("miner").forGetter(f->f.miner),Codec.BOOL.fieldOf("enabled").forGetter(f->f.enabled),
        Codec.LONG.fieldOf("mineAfter").forGetter(f->f.mineAfter),UUIDUtil.CODEC.optionalFieldOf("guardian").forGetter(f->Optional.ofNullable(f.guardian)),
        Codec.LONG.fieldOf("expires").forGetter(f->f.expires),
        Codec.BOOL.optionalFieldOf("relay",false).forGetter(f->f.relay),Codec.BOOL.optionalFieldOf("drive",false).forGetter(f->f.drive)
    ).apply(i,(schema,collector,miner,enabled,mineAfter,guardian,expires,relay,drive)->{
        var f=new AscensionFeatures(schema,collector,miner,enabled,mineAfter,guardian.orElse(null),expires);f.relay=relay;f.drive=drive;return f;
    }));
    final int schema;
    boolean collector,miner,enabled,relay,drive;
    long mineAfter,expires;
    UUID guardian;
    public AscensionFeatures(){this(1,false,false,false,0,null,0);}
    private AscensionFeatures(int schema,boolean collector,boolean miner,boolean enabled,long mineAfter,UUID guardian,long expires){
        this.schema=schema;this.collector=collector;this.miner=miner;this.enabled=enabled;this.mineAfter=mineAfter;this.guardian=guardian;this.expires=expires;
    }
    boolean compatible(){return schema==1&&(!miner||collector)&&(!enabled||collector)&&(!relay||collector)&&(!drive||miner)&&mineAfter>=0&&(guardian==null?expires==0:expires>0);}
    void clearGuardian(){guardian=null;expires=0;}
}
