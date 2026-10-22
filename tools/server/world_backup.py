"""Stopped-world snapshots and verified restore to a NEW directory; never overwrite a world."""
import argparse
import contextlib
import hashlib
import json
import os
from pathlib import Path
import shutil


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def inventory(root):
    result = {}
    for path in sorted(root.rglob("*")):
        if path.is_symlink() or path.is_junction():
            raise ValueError(f"Links are not supported: {path}")
        if path.is_file() and path.relative_to(root).as_posix() != "session.lock":
            result[path.relative_to(root).as_posix()] = {"bytes": path.stat().st_size, "sha256": digest(path)}
    return result


@contextlib.contextmanager
def stopped_world(root):
    # Native World Lock: Hold the same file lock for the entire snapshot, preventing concurrent startup.
    with (root / "session.lock").open("r+b") as lock:
        if os.name == "nt":
            import msvcrt
            msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        else:
            import fcntl
            fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        try:
            yield
        finally:
            lock.seek(0)
            if os.name == "nt":
                msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.lockf(lock, fcntl.LOCK_UN)


def separate_new_destination(source, target):
    if target.exists() or target.is_symlink():
        raise ValueError(f"Destination must not exist: {target}")
    if target.is_relative_to(source) or source.is_relative_to(target):
        raise ValueError("Source and destination must be separate trees")


def copy_files(source, target, files):
    for name in files:
        path = Path(name)
        if path.is_absolute() or ".." in path.parts or "\\" in name or ":" in name:
            raise ValueError(f"Invalid manifest path: {name}")
        output = target / path
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source / path, output)


def verify(snapshot):
    manifest = json.loads((snapshot / "manifest.json").read_text(encoding="utf-8"))
    if manifest.get("format") != 1 or "level.dat" not in manifest.get("files", {}):
        raise ValueError("Not a completed world snapshot")
    if inventory(snapshot / "world") != manifest["files"]:
        raise ValueError("Snapshot contents do not match the SHA-256 manifest")
    return manifest["files"]


def snapshot_world(source, target):
    source, target = source.resolve(), target.resolve()
    separate_new_destination(source, target)
    if not (source / "level.dat").is_file():
        raise ValueError("Source must be a native world containing level.dat")
    with stopped_world(source):
        files = inventory(source)
        target.mkdir(parents=True)
        world = target / "world"
        world.mkdir()
        copy_files(source, world, files)
        if inventory(world) != files or inventory(source) != files:
            raise ValueError("World changed during snapshot or copy verification failed; snapshot remains incomplete")
        # Completion Marker: An interrupted copy has no manifest and cannot be restored.
        (target / "manifest.json").write_text(json.dumps({"format": 1, "files": files}, indent=2) + "\n", encoding="utf-8")
    return files


def restore_world(source, target):
    source, target = source.resolve(), target.resolve()
    separate_new_destination(source, target)
    files = verify(source)
    target.mkdir(parents=True)
    copy_files(source / "world", target, files)
    if inventory(target) != files:
        raise ValueError("Restored world does not match snapshot; do not start it")
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["snapshot", "verify", "restore"])
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path, nargs="?")
    args = parser.parse_args()
    if args.action != "verify" and args.destination is None:
        parser.error("snapshot and restore require a new destination")
    try:
        files = verify(args.source.resolve()) if args.action == "verify" else (
            snapshot_world(args.source, args.destination) if args.action == "snapshot" else restore_world(args.source, args.destination))
    except (OSError, ValueError) as error:
        parser.exit(1, f"FAILED: {error}\n")
    print(json.dumps({"action": args.action, "files": len(files), "bytes": sum(info["bytes"] for info in files.values()), "verified": True}))


if __name__ == "__main__":
    main()
