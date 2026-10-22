# Optional NPC patrol boarding

Implemented in source, October 20, 2026. No playable JAR is packaged by this increment. Live solo/cooperative balance, client movement and reconnect acceptance remain outstanding.

## Playing

1. In Pirate Seas Survival, damage an ordinary NPC patrol with your ship to **25% hull or less** without sinking it. It stops sailing and firing. Special ghost/bounty encounters and player ships are excluded. Normal faction penalties for attacking patrols still apply.
2. Stop firing and approach within **24 blocks**. Wait for incendiary fires to stop. Use **Sail > Board patrol** or `/boarding form`. The nearest eligible patrol last damaged by one of your owned ships is selected.
3. Start alone with `/boarding start`, or have up to three friends explicitly use `/boarding join <leader>`. The lobby lasts 60 seconds. Everyone must be alive in Survival, nearby, with menus closed. Joining explicitly accepts being moved onto the patrol when the leader starts.
4. Starting transfers the party onto clear deck positions. Defeat **three stationed archers solo**, or **four with two to four participants**, within five minutes. The boss bar shows kills, time and the 48-block boundary. `/boarding status` repeats the instructions; `/boarding leave` opts out.
5. Victory releases the **existing hold contents once**, with item components intact, onto the deck as shared loot. There is no per-player duplication, sentry equipment/XP, or extra wreck-plank reward. The party captain receives the existing naval victory credit. Return to your own ship before the captured wreck disappears **60 seconds later**. There is no automatic return teleport or ship capture.

## Limits and recovery

- At most four boarding encounters are retained per sea level. A player can belong to one unfinished party; joining after combat starts is refused.
- An unstarted lobby can be cancelled without spending the patrol. Starting persists the attempt before spawning sentries or transferring players. Death, disconnect, departure beyond 48 blocks, dimension/game-mode change, timeout, Peaceful difficulty, disappearing ship/defender or a failed spawn cancels the started encounter without reward. Cancellation removes the patrol; players remaining aboard fall into the sea and must return to their ship.
- Reserved patrols cannot fire, move or take further cannon damage. Their ordinary expiry is suspended for the bounded lobby/fight/salvage period. Unreserved disabled patrols retain ordinary expiry and can still be sunk normally.
- Reloaded attempted patrols without the matching live encounter are discarded without cargo or credit. Orphan sentries and their marked arrows are discarded on load. Boarding is deliberately not resumable after restart.
- Defenders accept participant attacks only; their arrows cannot damage uninvolved living entities and cannot be picked up. Outstanding sentry arrows are removed when the encounter ends.
- Sentries use the native skeleton appearance and bow shots. They remain stationed because native mob navigation does not understand the moving ships' virtual deck blocks. This is not autonomous moving-deck crew combat.
- No player-ship capture, player cargo theft, new summon item or changes to existing crew support roles.

## Verification and live acceptance

Six focused boarding GameTests pass (`build/boarding-verified.log`): exact solo cargo/credit, cooperative opt-in/damage isolation, reload recovery, lobby cancellation/disconnect, native arrows and orphan cleanup, and party-cap/blocked-entry/boundary handling. The 51-test ship run passed all 45 existing ship tests and five boarding checks; its arrow-cleanup failure was corrected and all six boarding tests then rerun successfully. The shared run also exposed a docked-worker retry freezing a mid-jump worker; keeping physics active until landing fixed that failure (`build/boarding-final-ships.log`). These are targeted checks, not full verification or a play build.

Final common/client compilation, the registered-resource coverage check and all three Testing Camp tests passed (`build/boarding-camp-resources.log`). No JAR or version change was produced.

Run `gradlew.bat classes clientClasses --max-workers=1`, then `gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ships_game_test_boarding*' --max-workers=1`. Shared ship damage/tick/menu changes also require the complete `ships_game_test*` group. Run `testing_camp_game_test*` after guide edits.

Check solo and two-to-four-client boarding from an actual sailing ship; confirm entry clearance, visible sentries, arrow cues, shields, shared cargo collection and a practical return route before salvage expiry. Exercise death, disconnection and restart with real clients. Automated correctness does not establish enjoyable combat or safe-looking client interpolation.
