from __future__ import annotations
import math, struct, subprocess, tempfile, wave, zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PNG = ROOT / "src/main/resources/assets/rickportalgun/textures"
ITEM = PNG / "item"
MISC = PNG / "misc"
SND = ROOT / "src/main/resources/assets/rickportalgun/sounds"
for p in (ITEM, MISC, SND):
    p.mkdir(parents=True, exist_ok=True)

def write_png(path, w, h, pixel):
    rows = []
    for y in range(h):
        row = bytearray([0])
        for x in range(w):
            row.extend(pixel(x, y, w, h))
        rows.append(row)
    def chunk(k, d):
        return struct.pack(">I", len(d)) + k + d + struct.pack(">I", zlib.crc32(k + d) & 0xffffffff)
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
    data += chunk(b"IEND", b"")
    path.write_bytes(data)

def portal(x, y, w, h):
    nx = (x + .5) / w - .5
    ny = (y + .5) / h - .5
    d = math.sqrt((nx/.46)**2 + (ny/.63)**2)
    inside = max(0.0, 1.0 - d/1.02)
    ring = max(0.0, 1.0 - abs(d-.80)/.22)
    a = int(255 * max(.25*inside, ring))
    wave_ = .5 + .5*math.sin(20*math.atan2(ny,nx) + 10*d + 8*nx - 7*ny)
    r = int(8 + 46*(1-wave_))
    g = int(125 + 120*wave_)
    b = int(55 + 145*(.5 + .5*math.sin(wave_*7 + nx*9)))
    return bytes((r,g,b,a))

def body(x,y,w,h):
    v = 118 + int(28*y/max(1,h-1)) + int(8*math.sin(x/w*math.pi*5))
    return bytes((v+5,v+5,v+2,255))

def dark(x,y,w,h):
    v = 42 + int(16*(1-y/max(1,h-1)))
    return bytes((v,v+2,v+4,255))

def glow(x,y,w,h):
    p = .5 + .5*math.sin((x+y*1.7)/w*16)
    return bytes((int(30+35*p), int(215+40*p), int(75+85*p), 255))

def display(x,y,w,h):
    out = [95,22,30,255]
    glyphs = {"C":["111","100","100","100","111"],
              "-":["000","111","000","000","000"],
              "1":["010","110","010","010","111"],
              "3":["110","001","010","001","110"],
              "7":["111","001","010","010","010"]}
    scale = 2
    cursor = 1
    for ch in "C-137":
        for gy,row in enumerate(glyphs[ch]):
            for gx,bit in enumerate(row):
                px = cursor + gx*scale
                py = 5 + gy*scale
                if bit == "1" and px <= x < px+scale and py <= y < py+scale:
                    out = [255,220,80,255]
        cursor += 7
    return bytes(out)

write_png(MISC/"portal_green.png", 96, 128, portal)
write_png(ITEM/"portal_gun_body.png", 32, 32, body)
write_png(ITEM/"portal_gun_dark.png", 32, 32, dark)
write_png(ITEM/"portal_gun_glow.png", 32, 32, glow)
write_png(ITEM/"portal_gun_display.png", 32, 32, display)

def sound(path, mode):
    rate = 48000
    length = {"create":1.15,"remove":.78,"draw":.52}[mode]
    n = int(rate*length)
    frames = bytearray()
    for i in range(n):
        t = i/rate
        if mode == "create":
            f = 160 + 980*min(1,t/.75)
            env = min(1,t*28)*max(0,1-t/length)**1.8
            s = .60*math.sin(2*math.pi*f*t) + .20*math.sin(2*math.pi*f*2.02*t)
        elif mode == "remove":
            f = 1050 - 760*min(1,t/.55)
            env = min(1,t*50)*max(0,1-t/length)**2.2
            s = .60*math.sin(2*math.pi*f*t) + .20*math.sin(2*math.pi*f*.47*t)
        else:
            f = 180 + 540*t/length
            env = min(1,t*90)*max(0,1-t/length)**3
            click = math.sin(2*math.pi*1300*t) if t < .085 else 0
            s = .45*click + .48*math.sin(2*math.pi*f*t)
        s = max(-1,min(1,(s + .02*math.sin(i*13.13+41.71))*env*.65))
        frames += struct.pack("<h", int(s*32767))
    with tempfile.NamedTemporaryFile(suffix=".wav",delete=False) as tmp:
        wav = Path(tmp.name)
    with wave.open(str(wav),"wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(rate)
        wf.writeframes(frames)
    subprocess.run(["ffmpeg","-y","-loglevel","error","-i",str(wav),"-c:a","libvorbis","-q:a","5",str(path)],check=True)
    wav.unlink(missing_ok=True)

sound(SND/"portal_create.ogg","create")
sound(SND/"portal_remove.ogg","remove")
sound(SND/"gun_draw.ogg","draw")
print("assets generated")
