"""Inspect actual Java cuboids and yaw-safe horizontal bounds; flat colors are not texture QA."""
import math
import re
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
source = (ROOT / 'src/client/java/darkspawn/black/client/mount/GroundWalkerModel.java').read_text()
parts = []
for statement in re.findall(r'root\.addOrReplaceChild\(.*?;', source, re.S):
    for side in (-1, 1) if 'side' in statement else (0,):
        def number(text):
            # Only the authored scalar multiplication in PartPose is supported; never execute source.
            factors = text.strip().replace('side', str(side)).split('*')
            return math.prod(float(factor) for factor in factors)
        pose = [number(n) for n in re.search(r'PartPose\.offset\(([^)]+)\)', statement)[1].split(',')]
        boxes = [[float(n) for n in match.split(',')] for match in re.findall(r'\.addBox\(([^)]+)\)', statement)]
        parts.append((pose, boxes, side if 'left_leg' in statement else 0))

assert sum(len(boxes) for _, boxes, _ in parts) == 11, 'Unexpected Java geometry; update the preview parser.'
image = Image.new('RGB', (1000, 650), '#19272f')
draw = ImageDraw.Draw(image)
draw.text((25, 20), 'GROUND WALKER / Java model geometry', fill='#edf0f3')
draw.text((25, 40), 'Flat-color inspection: idle and walking. Native iron texture and live camera remain client checks.', fill='#b6c6cd')
for frame, stride in enumerate((0, .25)):
    faces = []
    def project(p):
        x, y, z = p
        return (250 + frame * 500 + (x + z) * 8, 275 + (x - z) * 4 + y * 9)
    for pose, boxes, side in parts:
        angle = stride * side
        def transform(point):
            x, y, z = point
            result = (x + pose[0], y * math.cos(angle) - z * math.sin(angle) + pose[1],
                      y * math.sin(angle) + z * math.cos(angle) + pose[2])
            assert math.hypot(result[0], result[2]) <= 1.8 * 16 / 2, 'Limb exceeds collision footprint at some yaw'
            return result
        for x, y, z, w, h, d in boxes:
            assert w > 0 and h > 0 and d > 0
            X, Y, Z = x + w, y + h, z + d
            for points, shade in (
                ([(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)], 1.08),
                ([(X,y,z),(X,Y,z),(X,Y,Z),(X,y,Z)], .82),
                ([(x,y,z),(x,Y,z),(X,Y,z),(X,y,z)], .95),
            ):
                points = [transform(p) for p in points]
                depth = sum(a - c - b * .8 for a, b, c in points) / 4
                color = tuple(min(255, int(c * shade)) for c in (178, 188, 198))
                faces.append((depth, points, color))
    for _, points, color in sorted(faces, key=lambda face: face[0]):
        draw.polygon([project(p) for p in points], fill=color, outline='#535e68')
    draw.text((190 + frame * 500, 600), 'IDLE' if frame == 0 else 'WALK EXTREME', fill='#edf0f3')
output = ROOT / 'build/ground-walker-preview.png'
image.save(output)
print(f'PASS: 11 Java cuboids; idle/extreme yaw-safe horizontal bounds. Preview: {output}')
