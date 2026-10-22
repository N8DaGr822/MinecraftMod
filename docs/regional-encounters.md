# Regional encounters

October 21 source increment. These four opt-in events share the established Wolf Hunt participation lifecycle; Wolf Hunt's implementation and saved data remain unchanged.

| Event / command | Habitat | Finite enemies (solo / cooperative) | Reward per participant |
| --- | --- | --- | --- |
| Fungal Bloom `/fungalbloom` | Surface Overworld Mushroom Fields; mycelium, grass or dirt | 3 / 6 Sporelings | 4 red mushrooms |
| Shadow Incursion `/shadowincursion` | Overworld Deep Dark; sculk, deepslate or stone | 3 / 6 Shadow Drones | 4 sculk |
| Soul Storm `/soulstorm` | Nether Soul Sand Valley; soul sand or soul soil | 3 / 6 Lost Souls | 4 soul torches |
| End Rift `/endrift` | Main End island, highlands, midlands or barrens; end stone | 3 / 6 Voidlings | 4 chorus fruit |

All require the world's dragon victory, Survival, enabled regional events and non-Peaceful difficulty. Habitat offers never spawn enemies automatically. Each command supports `status`, `solo`, `form`, `join <leader>`, `start`, `leave` and `claim`. Cooperative lobbies accept two to four explicitly opted-in players within 32 blocks and expire after 60 seconds. Encounters last five minutes; everyone must stay alive in the same dimension within 48 blocks of the start. A boss bar shows kills/time, and the action bar shows distance.

Starts validate all clear, dry, loaded spawn sites on a 12-block ring before inserting mobs. No terrain is rebuilt. Invalid starts spend no cooldown. Successful starts consume each member's configured regional-event cooldown (20 minutes by default), even on abandonment. Each event allows at most four simultaneous lobbies/fights, at least 128 blocks apart. Cooldowns and rewards are separate by event and stored in the Overworld save.

Any death, disconnect, departure, timeout, disabled event setting, missing mob or administrator removal cancels the active encounter without a reward. Active fights do not resume on reload; tagged orphan creatures are removed on load. Earned claims persist. Claim once in Overworld Survival with a free slot, closed containers and an empty cursor, outside OneBlock/Ascension possession profiles. Full inventories preserve claims. Pending claims block new starts for that event.

## Encounter differences

Sporelings use their existing melee and presentation but do not recruit or buff nearby natural fungal creatures. Drones retain warned darkness bursts but do not self-discard after bursting during an event: defeat them normally. Event damage and debuffs exclude bystanders; event creatures accept ordinary damage only from participants. They grant no normal loot/XP. Natural creatures retain their existing behavior.

Lost Souls retain their existing flight and temporary soul-fracture effect. Voidlings retain their existing flight/blink behavior. End Rift shows temporary reverse-portal particles at the encounter center; there is no portal block, teleport destination or terrain edit. Soul Storm does not alter global weather. Shadow Incursion does not directly summon Wardens; ordinary Deep Dark sensors/shriekers and unrelated wildlife still operate.

Fresh camp books explain all four command routes. No new test supplies bypass their habitat or dragon gates. Rewards are renewable ordinary materials, not boss hearts or mandatory progression keys.

## Verification and live acceptance

Focused GameTests cover solo/cooperative membership, native attack restrictions, finite spawns, once-only claims, wrong progression/ground, full inventories and island-profile protection, disconnect/death/boundary/opt-out cleanup and orphan removal. Ledger unit tests cover saved cooldowns and claims. Shared regional-mob, taiga and camp checks are required for this increment. Verification passed: 25 event GameTests (6 Bloom, 7 Shadow, 6 Soul, 6 Rift), 21 shared regional-creature checks, 9 taiga regressions, 3 camp checks, and 2 ledger unit tests. Common/client compilation passed. Logs are `build/soul-storm-tests.log`, `build/shadow-incursion-tests.log` and `build/regional-final-*.log`. Native-dimension fixtures explicitly activate their chunks before querying entities and isolate the synthetic dragon gate per callback.

Live checks still required: solo and four-player balance, warning/UI readability, native Nether/End terrain, real two-client disconnect/reconnect, server restart with pending claims, and other-mod compatibility. Automated tests do not establish those outcomes. No JAR or version change is included.
