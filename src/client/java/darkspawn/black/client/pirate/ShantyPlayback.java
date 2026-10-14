package darkspawn.black.client.pirate;

import darkspawn.black.audio.DarkspawnSounds;
import darkspawn.black.pirate.Pirate;
import darkspawn.black.pirate.PirateShip;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** One nearby singer per listener, using the existing streamed encounter-audio pattern. */
public final class ShantyPlayback {
    private static final String[] TRACKS={"chase_the_sun","the_blackened_sail","brothers_on_the_water","haul_away_hearties"};
    private static PirateShip ship;
    private static Song song;
    private static Object world;
    private static int track,age,rest;
    private ShantyPlayback() { }
    public static void initialize(){ClientTickEvents.END_CLIENT_TICK.register(ShantyPlayback::tick);}
    private static boolean eligible(Minecraft client,PirateShip candidate) {
        return client.level!=null&&client.player!=null&&client.player.isAlive()
                &&client.level.dimension().equals(Pirate.LEVEL)&&candidate.level()==client.level
                &&candidate.isAlive()&&!candidate.isRemoved()&&candidate.shantyAudible()
                &&candidate.distanceToSqr(client.player)<=32*32;
    }
    private static void silence(Minecraft client) {
        if(song!=null)client.getSoundManager().stop(song);
        song=null;age=0;
    }
    private static void tick(Minecraft client) {
        if(world!=client.level){silence(client);ship=null;rest=0;track=0;world=client.level;}
        if(client.isPaused())return;
        if(ship!=null&&!eligible(client,ship)){silence(client);ship=null;rest=0;}
        if(ship==null&&client.level!=null&&client.player!=null) {
            double closest=32*32+1;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof PirateShip candidate&&eligible(client,candidate)) {
                double distance=candidate.distanceToSqr(client.player);
                if(distance<closest){ship=candidate;closest=distance;}
            }
        }
        if(ship==null)return;
        // Muting releases the stream and pauses the playlist instead of cycling discarded channels.
        if(client.options.getSoundSourceVolume(SoundSource.RECORDS)<=0||client.options.getSoundSourceVolume(SoundSource.MASTER)<=0){silence(client);return;}
        if(song!=null) {
            if(++age>40&&!client.getSoundManager().isActive(song)){silence(client);track=(track+1)%TRACKS.length;rest=400;}
            return;
        }
        if(rest>0){rest--;return;}
        song=new Song(ship,TRACKS[track]);age=0;client.getSoundManager().play(song);
    }
    private static final class Song extends AbstractTickableSoundInstance {
        private final PirateShip sourceShip;
        Song(PirateShip ship,String track) {
            super(DarkspawnSounds.get("shanty."+track),SoundSource.RECORDS,RandomSource.create());
            sourceShip=ship;volume=.65F;pitch=1;looping=false;relative=false;tick();
        }
        @Override public void tick() {
            if(!sourceShip.isAlive()||sourceShip.isRemoved()||!sourceShip.shantyAudible()){stop();return;}
            x=sourceShip.getX();y=sourceShip.getY()+2;z=sourceShip.getZ();
        }
    }
}
