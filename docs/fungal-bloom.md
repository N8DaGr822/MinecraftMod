# Fungal Bloom

Optional solo/cooperative Sporeling encounters in surface Overworld Mushroom Fields, after the world dragon victory. Requires Survival, non-Peaceful difficulty and enabled regional events. An occasional habitat message offers the encounter; nothing spawns until you opt in.

- `/fungalbloom solo`: three marked Sporelings.
- `/fungalbloom form`, friends `/fungalbloom join <leader>`, then leader `/fungalbloom start`: two to four opted-in players, six Sporelings.
- `/fungalbloom status`, `/fungalbloom leave`, `/fungalbloom claim`: instructions, cancellation and reward collection.

Lobbies last 60 seconds; fights last five minutes. Stay alive within 48 blocks. Any participant death, disconnect, departure or opt-out cancels the active group. Starts require loaded, clear, dry mycelium/grass/dirt around a 12-block ring. Invalid placement spends no cooldown. Successful starts consume the configured regional-event cooldown (20 minutes by default). At most four blooms exist with centers at least 128 blocks apart.

Victory saves four red mushrooms per participant. Claim once in Overworld Survival, outside island possession profiles, with a free inventory slot and closed containers. A full inventory preserves the saved reward. Pending rewards must be claimed before another bloom. Rewards and cooldowns survive reload; active fights do not resume. Orphan event mobs are discarded on load.

Event Sporelings use their existing model and native melee, but cannot damage or accept ordinary damage from bystanders, recruit natural fungal wildlife, grant normal loot/XP, grow terrain or spawn further mobs. Their death particles remain cosmetic. No new boss, heart reward or summon recipe is added. Existing regional timed boons remain separate.

Fresh camp guidebooks explain the commands. Travel to actual Mushroom Fields; the camp does not alter its biome. Live difficulty, real multiplayer/reconnect and guide visibility remain acceptance checks.

Validation: six targeted server checks passed for participation, finite spawns, rewards, terrain/progression rejection, interruption and claim safety. AI isolation, ledger persistence, camp and shared regional-mob regressions also pass; see [regional verification](regional-encounters.md). No JAR packaged.

The shared lifecycle and the other three approved regional encounters are documented in [regional encounters](regional-encounters.md).
