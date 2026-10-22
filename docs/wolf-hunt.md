# Wolf Hunt

Implemented in source: optional solo and cooperative encounters using the existing Dire Wolf model. Live balance, boss-bar readability and two-client acceptance remain pending.

After the world's Ender Dragon victory, enter an open surface Overworld taiga in Survival on non-Peaceful difficulty. Regional events must be enabled. An occasional message explains the hunt; it never spawns wolves automatically. Use `/wolfhunt` or `/wolfhunt status` for instructions, cooldown or pending reward.

| Mode | Commands | Encounter |
| --- | --- | --- |
| Solo | `/wolfhunt solo` | Three marked wolves |
| Cooperative | Leader: `/wolfhunt form`; friends within 32 blocks: `/wolfhunt join <leader>`; leader: `/wolfhunt start` | Two to four opted-in players, six marked wolves |

Cooperative lobbies last 60 seconds. Hunts last five minutes. Everyone must stay alive, in Survival, in the same dimension and within 48 blocks of the starting position. The boss bar shows defeated wolves and time remaining; the action bar shows distance from the start. Any participant's death, disconnect, departure or `/wolfhunt leave` ends the entire active hunt. A waiting member may leave a lobby without cancelling it; the leader leaving closes it.

Starts require enough clear, dry, loaded natural ground around the 12-block spawn ring. Invalid placement charges no cooldown. Successful starts consume each participant's personal cooldown, including failed or abandoned hunts: `regionalEventSeconds` in server configuration (20 minutes by default). At most four lobbies/hunts may exist, with centers at least 128 blocks apart. No terrain is rebuilt.

Defeat all marked wolves to save **four emeralds per participant**. Use `/wolfhunt claim` in Overworld Survival with one empty inventory slot, closed containers and an empty cursor. Full inventories preserve the claim. Claims cannot enter OneBlock or Ascension possession profiles. Pending rewards block a new hunt until claimed; each claim can be spent once.

Hunt wolves target and accept ordinary damage only from participants. They cannot be tamed and yield no normal loot or XP. Administrator removal, missing wolves or disabled events cancel the hunt without a reward. Active encounters do not resume after server restart; tagged orphan wolves are discarded when loaded. Earned claims and cooldowns are saved separately. Existing regional timed boons remain unchanged.

## Acceptance

Run `WolfHuntLedgerTest`, `wolf_hunt_game_test*`, ordinary `taiga_game_test*` and the starter-camp tests when changing their guide.

October 20 source validation: common/client compilation, two ledger unit tests, six targeted Wolf Hunt server tests, nine existing taiga wolf regressions and three starter-camp tests passed. These cover explicit participation, solo/cooperative rewards, party limits, bystanders, grass/podzol placement, failed starts, disconnect/orphan cleanup, boundary/death/opt-out cancellation, full-inventory/profile claim protection and camp guide coverage. No JAR was packaged and no release version changed.

In live play, check solo and two-to-four-player difficulty, warning visibility at different UI scales, snow-covered clearing placement, bystander safety, boundary/death/disconnect cancellation and reward claims after reconnect/restart. Confirm saves and backups preserve earned claims. These checks complement the automated tests; they have not yet been completed.
