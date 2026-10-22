"""Original Ascension motif with nine arrangements and three intensities; preserves other audio."""
import hashlib
import json
import sys
from pathlib import Path
sys.path.insert(0, str(Path("build/audio-python").resolve()))
import numpy as np
import soundfile as sf

ROOT = Path("src/main/resources/assets/darkspawn")
SR, SECONDS, BEAT = 32000, 24, .75
# One D-minor motif, with instrument, register, pulse and stereo color per Guardian.
VARIATIONS = {
    "cave": (0, .15, 1, .08), "storm": (12, 1.3, 4, .15),
    "ember": (0, .8, 3, .09), "frost": (12, .3, 1, .18),
    "tidal": (0, .45, 2, .20), "crystal": (24, 1.6, 1, .12),
    "end": (-12, 1.1, 3, .22), "astral": (0, 1.8, 2, .10),
    "summit": (12, .55, 4, .16),
}
MOTIF = [0, 7, 10, 5, 3, 2, 7, 0]

def envelope(count, attack=.03, release=.15):
    out = np.ones(count)
    a, b = min(count//2, int(SR*attack)), min(count//2, int(SR*release))
    out[:a] = np.sin(np.linspace(0, np.pi/2, a))**2
    out[-b:] = np.cos(np.linspace(0, np.pi/2, b))**2
    return out

def generate():
    report = []
    manifest_path = ROOT / "sounds.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    n = SR*SECONDS
    t = np.arange(n)/SR
    for name, (register, color, pulse, width) in VARIATIONS.items():
        for intensity in (1, 2, 3):
            mono = np.zeros(n)
            # Shared Harmony: Four slow changes under the same eight-bar rising/falling phrase.
            for bar, step in enumerate(MOTIF):
                at = int(bar*4*BEAT*SR)
                count = int(4*BEAT*SR)
                u = np.arange(count)/SR
                root = 73.41619198*2**([0, -2, 3, -5][bar//2]/12)
                pad = sum(np.sin(2*np.pi*root*ratio*u)/divisor for ratio, divisor in ((.5, 1), (.75, 2.5), (1, 4)))
                mono[at:at+count] += .085*pad*envelope(count, .22, .28)
                frequency = 146.83238396*2**((step+register)/12)
                voice = np.sin(2*np.pi*frequency*u + color*np.exp(-u*1.8)*np.sin(2*np.pi*frequency*2*u))
                voice += .16*np.sin(2*np.pi*frequency*3*u)*np.exp(-u*3)
                voice *= .8+.2*np.cos(2*np.pi*pulse*u/(4*BEAT))
                mono[at:at+count] += .12*voice*np.exp(-u*(.55 if name=="tidal" else .9))*envelope(count, .025, .35)
                if intensity >= 2:
                    for beat in (1, 2, 3):
                        start=at+int(beat*BEAT*SR); length=int(.45*SR); v=np.arange(length)/SR
                        tone=np.sin(2*np.pi*(frequency/2)*v + color*.2*np.sin(2*np.pi*frequency*v))
                        mono[start:start+length]+=.035*tone*np.exp(-v*7)*envelope(length,.012,.1)
                if name=="summit":
                    mono[at:at+count]+=.035*np.sin(2*np.pi*frequency*1.5*u)*np.exp(-u)*envelope(count,.05,.35)
            # Phase Density: Quiet low percussion leaves attack-warning frequencies readable.
            spacing=BEAT*(2 if intensity==1 else 1 if intensity==2 else .5)
            for index, beat in enumerate(np.arange(0,SECONDS,spacing)):
                start=int(beat*SR); length=min(int(.32*SR),n-start); v=np.arange(length)/SR
                kick=np.sin(2*np.pi*((42+pulse*3)*v+1.5*(1-np.exp(-v*25))))*np.exp(-v*15)
                mono[start:start+length]+=(.025+.012*intensity)*kick*envelope(length,.006,.04)
                if intensity==3 and index%2:
                    mono[start:start+length]+=.012*np.sin(2*np.pi*(180+pulse*40)*v)*np.exp(-v*30)*envelope(length,.004,.04)
            mono*=envelope(n,.06,.08)
            stereo=np.column_stack((mono,mono*(1-width)+np.roll(mono,int(SR*(.013+pulse*.002)))*width))
            stereo-=stereo.mean(axis=0)
            stereo*=.42/max(float(np.max(np.abs(stereo))),1e-6)
            stereo*=envelope(n,.015,.035)[:,None]
            assert np.isfinite(stereo).all()
            relative=f"music/ascension/{name}_{intensity}"
            path=ROOT/"sounds"/(relative+".ogg"); path.parent.mkdir(parents=True,exist_ok=True)
            with sf.SoundFile(path,"w",samplerate=SR,channels=2,format="OGG",subtype="VORBIS") as stream:
                for start in range(0,n,8192): stream.write(stereo[start:start+8192])
            with sf.SoundFile(path) as stream:
                assert stream.samplerate==SR and stream.channels==2
                decoded=np.concatenate(list(stream.blocks(blocksize=8192)))
            assert len(decoded)==n and np.isfinite(decoded).all()
            peak=float(np.max(np.abs(decoded)))
            assert peak<1 and float(np.max(np.abs(decoded[[0,-1]])))<.02
            manifest[f"music.ascension_{name}.{intensity}"]={"sounds":[{"name":"darkspawn:"+relative,"stream":True}]}
            report.append({"guardian":name,"intensity":intensity,"seconds":SECONDS,"channels":2,"peak":round(peak,5),
                           "rms":round(float(np.sqrt(np.mean(decoded**2))),5),"sha256":hashlib.sha256(path.read_bytes()).hexdigest()})
    assert len({entry["sha256"] for entry in report})==27
    manifest_path.write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    Path("tools/audio/ascension-analysis.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print("Verified 27 distinct original stereo Ogg scores: 9 Guardian variations x 3 intensities.")

if __name__=="__main__":
    generate()
