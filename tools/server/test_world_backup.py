import tempfile
import unittest
import subprocess
import sys
from pathlib import Path
import world_backup


class WorldBackupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="darkspawn-backup-test-")
        self.root = Path(self.temp.name)
        self.world = self.root / "source"
        self.world.mkdir()
        (self.world / "level.dat").write_bytes(b"native-world-placeholder")
        (self.world / "session.lock").write_bytes(b"lock")
        (self.world / "data").mkdir()
        (self.world / "data" / "claims.dat").write_bytes(bytes(range(256)))

    def tearDown(self):
        self.temp.cleanup()

    def test_round_trip_and_lock_exclusion(self):
        snapshot = self.root / "snapshot"
        expected = world_backup.snapshot_world(self.world, snapshot)
        actual = world_backup.restore_world(snapshot, self.root / "restored")
        self.assertEqual(expected, actual)
        self.assertEqual(expected, world_backup.inventory(self.root / "restored"))
        self.assertFalse((self.root / "restored" / "session.lock").exists())

    def test_existing_destination_is_never_overwritten(self):
        snapshot = self.root / "snapshot"
        world_backup.snapshot_world(self.world, snapshot)
        with self.assertRaises(ValueError):
            world_backup.restore_world(snapshot, self.world)
        self.assertEqual((self.world / "level.dat").read_bytes(), b"native-world-placeholder")
        with self.assertRaises(ValueError):
            world_backup.snapshot_world(self.world, snapshot)

    def test_corruption_blocks_restore_before_destination_creation(self):
        snapshot = self.root / "snapshot"
        world_backup.snapshot_world(self.world, snapshot)
        (snapshot / "world" / "data" / "claims.dat").write_bytes(b"damaged")
        with self.assertRaises(ValueError):
            world_backup.restore_world(snapshot, self.root / "restored")
        self.assertFalse((self.root / "restored").exists())

    def test_nested_snapshot_and_incomplete_snapshot_are_rejected(self):
        with self.assertRaises(ValueError):
            world_backup.snapshot_world(self.world, self.world / "backup")
        with self.assertRaises(OSError):
            world_backup.restore_world(self.world, self.root / "restored")

    def test_unlisted_file_blocks_restore(self):
        snapshot = self.root / "snapshot"
        world_backup.snapshot_world(self.world, snapshot)
        (snapshot / "world" / "unexpected.dat").write_bytes(b"unlisted")
        with self.assertRaises(ValueError):
            world_backup.verify(snapshot)

    def test_locked_world_is_rejected_before_copy(self):
        with world_backup.stopped_world(self.world):
            result = subprocess.run([sys.executable, str(Path(world_backup.__file__)), "snapshot", str(self.world), str(self.root / "snapshot")], capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / "snapshot").exists())


if __name__ == "__main__":
    unittest.main()
