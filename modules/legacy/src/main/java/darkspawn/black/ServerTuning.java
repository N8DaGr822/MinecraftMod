package darkspawn.black;

import java.util.Map;

/** Bounded server settings; invalid values fall back without changing saves or gamerules. Restart to apply. */
public final class ServerTuning {
    public static int skirmishCap=4,patrolCap=8,patrolAttempts=2,ambientTicks=1200,eventTicks=24000;
    public static boolean regionalEvents=true;
    private ServerTuning() { }
    public static void initialize() {
        var p=darkspawn.black.core.ServerTuning.load(Map.of("skirmishCap","4","patrolCap","8","patrolAttemptsPerMinute","2","ambientSeconds","60","regionalEventSeconds","1200","regionalEvents","true"));
        skirmishCap=integer(p,"skirmishCap",4,0,8);patrolCap=integer(p,"patrolCap",8,0,16);patrolAttempts=integer(p,"patrolAttemptsPerMinute",2,0,4);
        ambientTicks=((integer(p,"ambientSeconds",60,10,600)+9)/10)*200;eventTicks=integer(p,"regionalEventSeconds",1200,300,7200)*20;
        regionalEvents=darkspawn.black.core.ServerTuning.bool(p,"regionalEvents",true);
    }
    private static int integer(java.util.Properties p,String key,int fallback,int min,int max) { return darkspawn.black.core.ServerTuning.integer(p,key,fallback,min,max); }
}
