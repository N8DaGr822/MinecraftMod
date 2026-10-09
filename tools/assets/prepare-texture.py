"""Package AI artwork as small Minecraft PNGs (Pillow; no network access)."""
import argparse
from pathlib import Path
from PIL import Image


def prepare(source, destination, size):
    with Image.open(source) as image:
        if image.width != image.height:
            raise ValueError(f"Expected square texture, received {image.size}: {source}")
        # Asset Packaging: Area sampling retains small highlights and generated transparency.
        texture = image.convert("RGBA").resize((size, size), Image.Resampling.BOX)
        destination.parent.mkdir(parents=True, exist_ok=True)
        texture.save(destination, optimize=True)
    print(f"{destination}: {size}x{size}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--size", type=int, default=32, choices=(32, 64, 128, 256))
    args = parser.parse_args()
    prepare(args.source, args.destination, args.size)
