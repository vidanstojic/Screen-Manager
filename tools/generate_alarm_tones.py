# Generise ugradjene zvuke alarma (app/src/main/res/raw/alarm_*.ogg).
# Zvuci su sintetizovani ovde (numpy + ffmpeg/libvorbis), bez tudjih snimaka i licenci.
# Pokretanje iz korena repoa: python3 tools/generate_alarm_tones.py
import numpy as np, wave, subprocess, os
SR=44100
def env(n, a=0.005, d=1.5):
    t=np.arange(n)/SR
    e=np.minimum(t/a,1)*np.exp(-t/d*3)
    return e
def bell(f, dur, decay=1.2, partials=((1,1),(2,0.4),(3,0.2),(4.2,0.08))):
    n=int(dur*SR); t=np.arange(n)/SR
    s=sum(a*np.sin(2*np.pi*f*m*t)*np.exp(-t*m/decay) for m,a in partials)
    return s*np.minimum(t/0.004,1)
def place(buf, sig, at):
    i=int(at*SR); j=min(len(buf), i+len(sig)); buf[i:j]+=sig[:j-i]
def norm(x, peak=0.85):
    return x/np.max(np.abs(x))*peak
def save(name, x):
    x=norm(x)
    fade=int(0.02*SR); x[-fade:]*=np.linspace(1,0,fade)
    w=wave.open(f'{name}.wav','wb'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((x*32767).astype(np.int16).tobytes()); w.close()
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',f'{name}.wav','-c:a','libvorbis','-q:a','4',f'app/src/main/res/raw/alarm_{name}.ogg'],check=True)
rng=np.random.default_rng(7)
# 1 sunrise: C major arpeggio rising, two octaves, soft
L=4.0; b=np.zeros(int(L*SR))
notes=[523.25,659.25,783.99,1046.5,783.99,1046.5,1318.5]
for i,f in enumerate(notes): place(b, bell(f,2.2,1.4)*0.6, i*0.38)
save('sunrise', b)
# 2 classic: beep beep beep beep - pause
L=2.0; b=np.zeros(int(L*SR))
for i in range(4):
    n=int(0.09*SR); t=np.arange(n)/SR
    s=np.sign(np.sin(2*np.pi*2000*t))*0.3+np.sin(2*np.pi*2000*t)*0.7
    s*=np.minimum(np.minimum(t/0.003,1),(0.09-t)/0.003)
    place(b,s,i*0.18)
save('classic', b)
# 3 chimes: random pentatonic bells
L=5.0; b=np.zeros(int(L*SR))
penta=[880,987.77,1108.73,1318.51,1479.98,1760,1975.5]
for k in range(14):
    f=rng.choice(penta); place(b, bell(f*1.0,3.0,1.8,((1,1),(2.76,0.35),(5.4,0.12)))*rng.uniform(0.35,0.7), k*0.33+rng.uniform(0,0.1))
save('chimes', b)
# 4 marimba: warm mallet melody
L=3.2; b=np.zeros(int(L*SR))
mel=[392,440,523.25,587.33,523.25,440,523.25,659.25]
for i,f in enumerate(mel):
    place(b, bell(f,1.0,0.45,((1,1),(4,0.25),(9.2,0.05)))*0.8, i*0.2 + (0.6 if i>=4 else 0))
save('marimba', b)
# 5 birds: chirps
L=4.5; b=np.zeros(int(L*SR))
for k in range(16):
    d=rng.uniform(0.06,0.14); n=int(d*SR); t=np.arange(n)/SR
    f0=rng.uniform(2800,4200); f1=f0*rng.uniform(0.6,1.4)
    ph=2*np.pi*np.cumsum(np.linspace(f0,f1,n))/SR
    s=np.sin(ph+0.8*np.sin(2*np.pi*rng.uniform(30,60)*t))*np.sin(np.pi*t/d)**2
    group=k//4
    place(b,s*rng.uniform(0.4,0.9), group*1.05+ (k%4)*0.16 + rng.uniform(0,0.04))
save('birds', b)
# 6 pulse: accelerating pulses
L=2.4; b=np.zeros(int(L*SR)); t0=0; gap=0.32
while t0<2.1:
    n=int(0.12*SR); t=np.arange(n)/SR
    s=(np.sin(2*np.pi*660*t)+0.5*np.sin(2*np.pi*990*t))*np.minimum(t/0.005,1)*np.exp(-t*18)
    place(b,s,t0); t0+=gap; gap=max(0.12,gap*0.82)
save('pulse', b)
