"""Package AI artwork as small Minecraft PNGs (Pillow; no network access)."""
import argparse
from pathlib import Path
from PIL import Image


def prepare(source, destination, size, cutout=False):
    with Image.open(source) as image:
        if image.width != image.height:
            raise ValueError(f"Expected square texture, received {image.size}: {source}")
        # Asset Packaging: Area sampling retains small highlights; cutouts keep crisp pixel edges.
        texture = image.convert("RGBA").resize((size, size), Image.Resampling.BOX)
        if cutout:
            texture.putalpha(texture.getchannel("A").point(lambda value: 255 if value >= 128 else 0))
        destination.parent.mkdir(parents=True, exist_ok=True)
        texture.save(destination, optimize=True)
    print(f"{destination}: {size}x{size}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--size", type=int, default=32, choices=(32, 64, 128, 256))
    parser.add_argument("--cutout", action="store_true", help="Use binary alpha for inventory/crop sprites")
    args = parser.parse_args()
    prepare(args.source, args.destination, args.size, args.cutout)
