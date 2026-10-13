"""Deterministic prototype griffin atlas; Python standard library only."""
from pathlib import Path
import struct
import zlib

SIZE = 128


def color(x, y):
    if 112 <= x and 40 <= y < 48:
        return (24, 29, 31)
    if 80 <= x < 112 and 40 <= y < 68:
        base = (216, 160, 51)
    elif y < 40:
        base = (154, 103, 53) if x < 80 or y < 20 else (94, 62, 36)
    elif y < 70:
        base = (235, 225, 198)
    elif y < 96:
        base = (184, 149, 96)
    else:
        base = (227, 213, 176) if y < 107 else (98, 73, 48)
    shade = ((x * 17 + y * 31) % 7 - 3) * 2
    if y >= 70 and x % 5 == 0:
        shade -= 12
    return tuple(max(0, min(255, c + shade)) for c in base)


def chunk(kind, payload):
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))


rows = b"".join(b"\0" + bytes(c for x in range(SIZE) for c in color(x, y)) for y in range(SIZE))
png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0))
png += chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b"")
target = Path(__file__).resolve().parents[2] / "src/main/resources/assets/darkspawn/textures/entity/griffin.png"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(png)
print(target)
