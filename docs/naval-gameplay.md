# Ships, cannons and crew



First naval implementation, October 14. The user selected progressively larger editable platforms, seated sailing with ordinary walking/editing when docked, player-ship combat, and four initial crew roles.



## Build and customize



The harbor gains a **Shipyard** at **0, 65, 24** when next visited. Existing player blocks at that position are preserved; if occupied, craft and place a Shipyard yourself. Open it with an empty hand. Ship construction is available in Pirate Seas and places the platform **20 blocks south** of the station, at Y64. The entire construction area must already be loaded, above water and clear of blocks and other ships.



| Platform | Unlock | Construction cost | Build above deck | Block cap | Cannons |

| --- | --- | --- | --- | --- | --- |

| Small, 7 × 15 | Available immediately | 105 oak planks + one Ship Helm | 6 blocks | 512 including platform | 4 |

| Large, 11 x 23 | Five NPC victories OR four guided discoveries + two claimed contracts | Expand a docked small ship with 148 oak planks | 8 blocks | 1,024 including platform | 8 |



| Expanded, 15 × 31 | Twelve NPC victories OR eight guided discoveries + four claimed contracts | Expand the 11×23 ship with 212 oak planks | 10 blocks | 1,536 including platform | 12 |



Costs and milestone thresholds are initial tuning choices. Rank persists through saves and death. Player-vessel kills do not grant rank. Expansion preserves the existing platform and adds the surrounding planks; it requires the larger volume to be clear. Hull health is 200/400/600. See [captain milestones and crew offers](shattered-seas/player-progression.md#implemented-captain-milestones) for independent paths and earned hiring discounts.



The starter is an editable plank platform with a central helm. Build your own hull appearance, rails, cabin and sail. Keep the complete plank floor, central helm, and two-block headroom over crew/passenger seats. Permitted blocks are planks, logs/wood, wool, stairs, slabs, fences, glass, ladders, lanterns, helm and cannons. Ordinary block entities, waterlogged blocks, machinery, chests and other unsupported blocks reject the entire launch. The dedicated **27-slot ship hold**, upgradeable to 36 and 54 slots, carries items and preserves their components. See [ship identity, cargo upgrades and three named officers](shattered-seas/ship-identity-seals-and-officers.md) for the Refit/Crew controls.



Build within the platform outline and height limit. Place cannons to either side of the centerline: negative local X is port, positive X is starboard. Cannons align outward when launched. The original platform orientation faces south; docking restores the ship along the nearest cardinal direction. The shipyard is shore equipment and must stay outside the captured build volume.



## Sail and dock



- **W/S:** forward/reverse; **A/D:** turn. Small ships accelerate and turn faster.

- **Z/C:** fire port/starboard broadside.

- **G:** cycle nearby NPC and player-vessel targets.

- **U:** cargo and crew orders while seated.

- **R:** dock after coming to a stop, in unobstructed water.

- **Shift:** leave your seat. There is no walkable moving deck; leaving at sea puts you in the water. The vessel slows when captain input stops.



The owner controls sailing and ship management. Up to four players may travel seated; other players can board a stationary sailing vessel but cannot steer it. Launch captures the permitted world blocks and seats the captain/players aboard. Docking first validates the entire destination volume, then restores editable blocks and dismounts players. A blocked destination changes nothing. Ships stop at land, blocks, other ships, unloaded water or the world border. The conservative rectangular collision footprint needs extra clearance around tight channels and turns.



Ship cargo, ownership, layout, hull, burning and cannon reload survive saves. Crew use native mixed-species passenger persistence and saved captain UUID loyalty. Saved motion inputs and target orders do not resume automatically. Sailing vessels render their captured block models; block entities and moving-world lighting are not simulated.



## Recruit crew



Earned Captain status reduces ordinary hires by two emeralds; a Trusted faction commendation reduces its matching role by four instead. Membership reduces its matching earned offer by two more emeralds. Captain and faction discounts do not stack; current prices appear in ship-menu tooltips.



Recruit while docked within the harbor's 96-block protected radius. Open the ship through the Shipyard or helm. Crew are visible, named station workers assigned to the vessel. Ordinary hires are Villagers; earned faction offers hire a Piglin first mate, Pillager cannoneer and sun-safe Husk shipworker. Hired crew retain their captain through resignation and reloads, including Peaceful difficulty. They are not free-roaming or off-ship companions. See [diplomacy and crew rules](shattered-seas/factions.md#membership-and-local-diplomacy).



| Role | Price | Limit | Job |

| --- | --- | --- | --- |

| First mate | 12 emeralds | 1 | Coordinates automatic broadsides against your selected target when ordered; assists aiming within the broadside arc. Defaults to hold fire. |

| Shipworker | 12 emeralds | 1 | Every four seconds, consumes one stocked oak plank to restore five hull points and reduce an active fire. |

| Cannoneer | 8 emeralds | One per allowed cannon | Each gunner operates one gun per broadside. Without gunners the captain can operate one gun. |

| Lookout / crow's nest | 12 emeralds | 1 | Extends ship contact selection from 80 to 192 blocks and reports ship/island contacts and distance in the HUD. |



The lookout inspects loaded water/shore only. It does not alter graphics render distance or force chunks to load. Crew stay at assigned seats while sailing and docked. Crew are protected from ordinary direct damage while assigned; sinking loses the hired crew. There are no wages, food upkeep, experience or rescue systems yet.



## Cannons and encounters



Put **Solid Shot** or **Incendiary Shot** into the ship hold. Incendiary is selected first when both are stocked. Each fired gun consumes one round; a broadside shares a three-second reload. Solid shot deals 20 hull damage. Incendiary deals eight immediately and ignites a six-second hull fire that deals two damage per second. A shipworker can reduce the fire using repair materials.



NPC encounters now include white-sailed outlaws and red/blue/cyan faction patrols. Faction ships use the captain's standing and saved provocation to choose targets; names and colors identify them. Spawning checks once per minute, makes at most two attempts and caps loaded NPC vessels at eight. Clear loaded ocean, harbor distance and existing-patrol separation are required. Unengaged patrols leave after two minutes and all expire after ten minutes without rewarding cleanup. See [patrol rules and repeatable hunts](shattered-seas/repeat-voyages-and-garrick.md#faction-naval-patrols). Naval victories still count toward captain milestones.



**Player-owned ships are valid naval targets outside the harbor when the world's PvP rule is enabled.** Cannons do not directly damage players, creatures or terrain. Hull damage is shared by the vessel; individual component destruction is not simulated. Fires are vessel state, not spreading world fire.



The harbor's 96-block radius prevents firing and incoming vessel damage. Destroyed ships drop their hold contents and a small amount of plank salvage once, then remove the vessel. Cargo drops use ordinary item-entity pickup/despawn rules; rescue it promptly. Blocks used in the custom build are not fully refunded. Docked hulls outside harbor can also be destroyed. Make sure shared-server players know these combat rules.



## Crafting



All naval crafting recipes become visible after collecting an iron ingot.



```text

Shipyard          Ship Helm        Naval Cannon

I P I              . S .           I I I

P C P              S I S           . . I

P P P              . P .           P P P



I = iron ingot, P = any planks, S = stick, C = crafting table



Solid Shot: four iron nuggets in a 2×2 square -> four rounds.

Incendiary Shot: solid shot + gunpowder side by side -> one round.

```



## Verification and remaining playtests



October 14 source verification: common/client compilation, all nine naval GameTests, all seven pirate travel GameTests and three Journal unit tests passed. Logs: `build/ships-final-checks.log` and `build/naval-regressions.log`. These are targeted checks, not unfiltered fullCheck or live multiplayer acceptance. Future systems are tracked in [The Shattered Seas plan](shattered-seas/project-overview.md).



Rebuild assets with `node tools/assets/naval-assets.cjs`. Compile with `gradlew.bat classes clientClasses --max-workers=1`; run the naval server checks with `gradlew.bat runGameTest -PgameTestFilter=darkspawn-gametest:ships_game_test* --max-workers=1`.



Live multiplayer steering, visual alignment, docking convenience, contact readability, patrol pacing, PvP balance and progression costs still require playtesting. This is a first playable naval system; it does not include freely walkable moving decks, copper/iron/diamond hull families, advanced ammunition, wages or roaming crew companions. Garrick has a personal rescue and can join as a named stationed cannoneer. No JAR packaging or release-version change is included.
