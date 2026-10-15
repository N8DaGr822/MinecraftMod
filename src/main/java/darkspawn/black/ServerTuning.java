package darkspawn.black;

import java.util.Properties;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

/** Bounded server settings; invalid values fall back without changing saves or gamerules. Restart to apply. */
public final class ServerTuning {
    public static int skirmishCap=4,patrolCap=8,patrolAttempts=2,ambientTicks=1200,eventTicks=24000;
    public static boolean regionalEvents=true;
    private ServerTuning() { }
    private static int integer(Properties p,String key,int fallback,int min,int max) {
        try{return Math.clamp(Integer.parseInt(p.getProperty(key,Integer.toString(fallback))),min,max);}catch(NumberFormatException invalid){Darkspawn.LOGGER.warn("Invalid {}: using {}",key,fallback);return fallback;}
    }
    public static void initialize() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("darkspawn-server.properties");var p=new Properties();
        try {
            if(Files.exists(path)){try(var input=Files.newInputStream(path)){p.load(input);}}
            else {p.setProperty("skirmishCap","4");p.setProperty("patrolCap","8");p.setProperty("patrolAttemptsPerMinute","2");p.setProperty("ambientSeconds","60");p.setProperty("regionalEventSeconds","1200");p.setProperty("regionalEvents","true");Files.createDirectories(path.getParent());try(var output=Files.newOutputStream(path)){p.store(output,"Darkspawn server budgets. Restart to apply. Defaults suit a small cooperative server.");}}
        }catch(java.io.IOException failure){Darkspawn.LOGGER.warn("Could not read/write server tuning; using defaults",failure);}
        skirmishCap=integer(p,"skirmishCap",4,0,8);patrolCap=integer(p,"patrolCap",8,0,16);patrolAttempts=integer(p,"patrolAttemptsPerMinute",2,0,4);
        ambientTicks=((integer(p,"ambientSeconds",60,10,600)+9)/10)*200;eventTicks=integer(p,"regionalEventSeconds",1200,300,7200)*20;
        regionalEvents=Boolean.parseBoolean(p.getProperty("regionalEvents","true"));
    }
}
