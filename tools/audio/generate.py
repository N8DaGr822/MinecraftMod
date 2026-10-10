"""Original deterministic synthesis; no third-party recordings or film samples.

Requires numpy and soundfile. Run from the repository root. Local encoder packages
can be installed under build/audio-python without changing the system Python.
"""
import sys, json, hashlib
from pathlib import Path
sys.path.insert(0, str(Path('build/audio-python').resolve()))
import numpy as np
import soundfile as sf

ROOT = Path('src/main/resources/assets/darkspawn')
OUT = ROOT / 'sounds'
ROSTER = json.loads(Path('tools/audio/roster.json').read_text())
FAMILIES = sorted({e['region'] for e in ROSTER})
THEMES = {
    'nature': ['ancient_tree_spirit', 'titan_boa', 'mycelial_sovereign'],
    'mutated': ['mutant_wolf', 'mutant_zombie', 'fossil_tyrant'],
    'arcane': ['thunder_bird', 'baba_yaga', 'mountain_titan', 'ice_wyrm', 'kraken', 'cave_crawler'],
    'hive': ['shadow_creeper_queen'],
    'nether': ['netherborn', 'soulbound_colossus'],
    'void': ['void_eye', 'endborn'],
}
events, subtitles, report = {}, {}, []

def rng_for(key):
    return np.random.default_rng(int.from_bytes(hashlib.sha256(key.encode()).digest()[:8], 'little'))

def envelope(n, sr, attack=.05, release=.18):
    e = np.ones(n)
    a, b = min(n//2, int(sr*attack)), min(n//2, int(sr*release))
    e[:a] = np.sin(np.linspace(0, np.pi/2, a))**2
    e[-b:] = np.cos(np.linspace(0, np.pi/2, b))**2
    return e

def save(name, data, sr, peak):
    data = np.asarray(data, dtype=np.float64)
    data -= data.mean(axis=0)
    data *= peak / max(float(np.max(np.abs(data))), 1e-6)
    data *= envelope(len(data), sr, .015, .035).reshape((-1,) + (1,)*(data.ndim-1))
    assert np.isfinite(data).all()
    path = OUT / (name+'.ogg'); path.parent.mkdir(parents=True, exist_ok=True)
    # Bounded writes avoid the Windows libsndfile conversion stack limit on long stereo files.
    with sf.SoundFile(path, 'w', samplerate=sr, channels=1 if data.ndim==1 else data.shape[1], format='OGG', subtype='VORBIS') as stream:
        for start in range(0,len(data),8192): stream.write(data[start:start+8192])
    with sf.SoundFile(path) as stream:
        rate=stream.samplerate
        decoded=np.concatenate(list(stream.blocks(blocksize=8192)))
    assert rate == sr and len(decoded) == len(data)
    assert float(np.max(np.abs(decoded))) < 1, name+' clips after encoding'
    report.append({'name':name,'seconds':len(data)/sr,'channels':1 if data.ndim==1 else data.shape[1],
                   'peak':round(float(np.max(np.abs(decoded))),4),'rms':round(float(np.sqrt(np.mean(decoded**2))),4)})

for family in FAMILIES:
    index = FAMILIES.index(family)
    base = [72, 91, 57, 118, 63, 104, 82, 48, 77, 66, 96, 54, 88, 110, 61, 69, 98][index % 17]
    for kind in ['idle','hurt','death','warning','phase','ambience']:
        for variant in range(2):
            key=f'{family}/{kind}_{variant}'; rng=rng_for(key); sr=22050
            duration={'idle':1.25,'hurt':.36,'death':2.1,'warning':.85,'phase':2.6,'ambience':3.5}[kind]
            t=np.arange(int(sr*duration))/sr; p=t/duration
            noise=rng.normal(0,1,len(t)); low=np.convolve(noise,np.ones(35)/35,mode='same')
            high=noise-np.convolve(noise,np.ones(9)/9,mode='same')
            contour=1+.14*np.sin(2*np.pi*p)+( .6*(1-p) if kind=='death' else .25*p if kind=='warning' else 0)
            frequency=base*(1+variant*.07)*contour
            phase=2*np.pi*np.cumsum(frequency)/sr
            growl=sum(np.sin(phase*k+(.4 if k%2 else 0))/k**1.6 for k in range(1,9))
            rasp=low*(.4+.6*np.sin(phase*.5)**2)
            if family=='shadow_creeper_queen':
                data=.15*growl+.32*high*(.35+.65*np.sin(2*np.pi*13*t)**2)+.65*rasp
                for at in [.12,.24,.46]:
                    click=np.exp(-np.maximum(0,t-at)*95)*(t>=at)
                    data+=.22*click*np.sin(2*np.pi*1900*t)
            elif family in ['ancient_tree_spirit','mountain_titan','fossil_tyrant']:
                data=.6*growl+.8*rasp+high*.05
            elif family in ['thunder_bird','ice_wyrm','void_eye','endborn']:
                data=.3*growl+.24*np.sin(phase*3.03+.8*np.sin(phase*.6))+.32*rasp
            elif family=='kraken':
                data=.4*growl+.3*low*np.sin(2*np.pi*8*t)+.15*np.sin(phase*1.47)
            else:
                data=.45*growl+.6*rasp+.025*high
            if kind=='hurt': data*=np.exp(-p*3)
            if kind=='death': data*=(1-p)**.8
            if kind=='warning': data*=.45+.55*p
            if kind=='phase': data*=.65+.35*np.sin(2*np.pi*p*3)**2
            if kind=='ambience': data=np.convolve(data,np.ones(7)/7,mode='same')*.6
            data*=envelope(len(data),sr,.04 if kind=='hurt' else .12,.24)
            save('creatures/'+key,data,sr,.55 if kind=='idle' else .68)

for e in ROSTER:
    label=e['id'].replace('_',' ').capitalize()
    # Shared regional recordings, with size/caste-dependent pitch and independent variation.
    pitch=float(np.clip((2.2/max(.4,e['height']))**.16,.65,1.35))
    for kind in ['idle','hurt','death','warning','phase']:
        key=f"entity.{e['id']}.{kind}"; sub='subtitles.darkspawn.'+key
        action={'idle':'stirs','hurt':'is hurt','death':'dies','warning':'prepares an attack','phase':'grows furious'}[kind]
        subtitles[sub]=label+' '+action
        events[key]={'subtitle':sub,'sounds':[{'name':f"darkspawn:creatures/{e['region']}/{kind}_{i}",'pitch':pitch,
                     'attenuation_distance':40 if e['category']=='boss' else 16} for i in range(2)]}
for family in FAMILIES:
    key='ambient.'+family;sub='subtitles.darkspawn.'+key
    subtitles[sub]=family.replace('_',' ').capitalize()+' echoes in the distance'
    events[key]={'subtitle':sub,'sounds':[{'name':f'darkspawn:creatures/{family}/ambience_{i}'} for i in range(2)]}

for theme_index,(theme,bosses) in enumerate(THEMES.items()):
    sr=32000; duration=30; n=sr*duration; t=np.arange(n)/sr
    root=[55,49,65.406,46.249,41.203,58.270][theme_index]
    for intensity in [1,2,3]:
        rng=rng_for(theme+str(intensity)); mono=np.zeros(n)
        # Eight bars at 64 BPM. A held low fifth supports a sparse minor-mode motif.
        for ratio,amp in [(1,.11),(1.5,.045),(2,.025)]:
            freq=round(root*ratio*duration)/duration
            mono+=amp*np.sin(2*np.pi*freq*t+.18*np.sin(2*np.pi*t/duration*2))*(.75+.25*np.sin(2*np.pi*t/duration)**2)
        notes=[0,7,8,3,0,1,7,-2] if theme!='hive' else [0,1,7,0,8,1,0,-1]
        for bar,step in enumerate(notes):
            f=root*2**((step+24)/12);start=int(bar*3.75*sr);length=min(int(3.5*sr),n-start);u=np.arange(length)/sr
            tone=np.sin(2*np.pi*f*u+.8*np.exp(-u)*np.sin(2*np.pi*f*2.01*u))
            tone*=envelope(length,sr,.09,.8)*np.exp(-u*.8)*.13
            mono[start:start+length]+=tone
            if intensity>=2:
                for beat in [1,2,3]:
                    at=int((bar*3.75+beat*.9375)*sr);length=min(int(.5*sr),n-at);u=np.arange(length)/sr
                    mono[at:at+length]+=.075*np.sin(2*np.pi*(f/2)*u)*np.exp(-u*8)*envelope(length,sr,.012,.08)
        # Muted heartbeat-like percussion leaves the telegraph frequency range clear.
        spacing=.9375 if intensity<3 else .46875
        for beat in np.arange(0,duration,spacing):
            start=int(beat*sr);length=min(int(.4*sr),n-start);u=np.arange(length)/sr
            kick=np.sin(2*np.pi*(47*u+2*(1-np.exp(-u*25))))*np.exp(-u*13)
            mono[start:start+length]+=kick*(.025+.025*intensity)*envelope(length,sr,.008,.04)
        air=rng.normal(0,1,n);air=np.convolve(air,np.ones(90)/90,mode='same')
        mono+=air*.025*(1+.5*np.sin(2*np.pi*t/3.75))
        mono*=envelope(n,sr,.3,.4)
        stereo=np.column_stack([mono,mono*.91+np.roll(mono,int(sr*.019))*.09])
        save(f'music/{theme}_{intensity}',stereo,sr,.42)
    for boss in bosses:
        if not any(e['id']==boss and e['category']=='boss' for e in ROSTER): continue
        for intensity in [1,2,3]:
            events[f'music.{boss}.{intensity}']={'sounds':[{'name':f'darkspawn:music/{theme}_{intensity}','stream':True}]}

ROOT.mkdir(parents=True,exist_ok=True)
(ROOT/'sounds.json').write_text(json.dumps(events,indent=2)+'\n')
langpath=ROOT/'lang/en_us.json';lang=json.loads(langpath.read_text());lang.update(subtitles)
langpath.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
Path('tools/audio/analysis.json').write_text(json.dumps(report,indent=2)+'\n')
print(f'Generated {len(report)} original Ogg files, {len(events)} events, {len(subtitles)} subtitles.')
