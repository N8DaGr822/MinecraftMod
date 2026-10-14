# Codex Master Prompt: The Shattered Seas

## Project Overview

We are developing a custom Minecraft pirate dimension currently named **The Shattered Seas**.

The goal is to create an immersive, open-world pirate sandbox RPG with three competing factions, recruitable crews, naval exploration, faction reputation, treasure hunting, dynamic world encounters, and a hidden overarching story.

**Core design philosophy:** The player should create their own pirate adventure rather than follow a rigid, linear campaign.

Players can join an existing faction, become independent captains, build their own crews, explore islands, uncover legendary treasures, and influence the balance of power throughout the dimension.

The world must feel alive, interconnected, replayable, and consistent with Minecraft's gameplay identity.

This document describes the intended gameplay and world design. It is not a claim that these features are already implemented.

---

# 1. Technical Instructions for Codex

Before writing implementation code:

1. Inspect the existing repository and determine the Minecraft version, mod loader, programming language, dependencies, and project architecture.
2. Identify any existing implementations of the custom dimension, dimension gate, NPCs, factions, custom mobs, structures, quests, or world generation.
3. Preserve existing code, naming conventions, and architecture wherever practical.
4. Prefer small, modular changes rather than extensive rewrites.
5. Use existing libraries and systems when available instead of implementing redundant functionality.
6. Treat multiplayer compatibility, persistence, and server-authoritative gameplay as important design requirements.
7. Do not assume that a ship mod, NPC framework, quest system, or animation library is installed.
8. Separate gameplay design from implementation details so features can be developed incrementally.

**Important:** The Overworld-craftable dimension gate already exists or is being developed. Investigate its current implementation before proposing changes.

For the initial task, produce a technical assessment, implementation roadmap, and organized design documentation. Do not attempt to implement the entire dimension in one pass.

Clearly distinguish:

- Implemented features
- Confirmed requirements
- Proposed features
- Open design decisions
- Technical dependencies

Never claim a feature is working without verifying it.

---

# 2. Core Dimension Concept

## Name

The Shattered Seas

This is the current working name.

## Setting

The Shattered Seas is a vast ocean dimension featuring:

- Tropical pirate islands
- Neutral trading ports
- Piglin pirate settlements
- Illager naval fortresses
- Haunted islands
- Ship graveyards
- Underwater ruins
- Hidden coves
- Abandoned shipwrecks
- Ancient temples
- Treasure islands
- Dangerous open-ocean regions
- Legendary captain strongholds

Each region should support exploration, combat, resource gathering, treasure hunting, and environmental storytelling.

The world should feel like an archipelago populated by competing naval powers.

Players should have freedom to explore, build, trade, fight, and establish bases.

## Atmosphere

The overall atmosphere mixes classic pirate adventure with darker supernatural mysteries.

Everyday gameplay should involve treasure hunting, naval rivalry, and exploration.

The deeper players explore, the more they encounter curses, ghost ships, forgotten civilizations, and an ancient mystery surrounding the Seven Seals.

Avoid making the entire dimension uniformly dark or hostile.

---

# 3. Entering the Dimension

## Overworld Dimension Gate

Players craft a dimension gate in the Overworld.

Upon activation, the gate transports them into The Shattered Seas.

The dimension gate is the primary entry mechanism.

## First Arrival: Breaker's Rest

The player arrives at a fixed, hand-designed gateway on an island called **Breaker's Rest**.

Breaker's Rest is a neutral starting island and central trading hub.

The gateway should be positioned within an ancient stone structure, ruined fort, or shipwreck-like structure overlooking the harbor.

The arrival should be atmospheric but nonrestrictive.

Potential environmental details:

- Ocean waves
- Seagulls
- Distant ship bells
- Harbor activity
- Anchored pirate vessels
- Distant naval fortresses
- Fog-covered shipwrecks
- Visible faction banners
- A lighthouse overlooking the harbor

On first arrival, display the dimension title and an atmospheric subtitle.

Example:

**The Shattered Seas**

*Where fortunes are made, legends are buried, and the sea claims its own.*

Do not force combat, cutscenes, tutorials, or mandatory quests.

## Gateway Mechanics

- The gate must support travel back to the Overworld.
- Reentering the dimension returns players to the established gateway.
- First-time introduction events should not repeat unnecessarily.
- Player inventories should remain intact.
- Faction progression should persist across dimension transfers.
- The gate and arrival area should be protected from accidental destruction and hostile spawning through an appropriate configurable design.
- Multiplayer players should be able to share the starting area while maintaining independent progression.
- Respawn behavior must be explicitly designed to avoid trapping players or creating unsafe spawn loops.

## Environmental Storytelling

The gateway contains seven ancient symbols carved into its structure.

These symbols relate to the overarching Seven Seals mystery.

Players initially have no reason to understand their significance.

Later discoveries reveal that the portal may have been constructed by the same civilization connected to the ancient seals.

---

# 4. Breaker's Rest — Starting Hub

Breaker's Rest should act as a neutral sanctuary that introduces the dimension's systems naturally.

## Main Locations

### Ancient Gateway

The entry and exit point for the dimension.

### Harbor and Docks

Contains ship merchants, storage, fishing opportunities, ship repair facilities, and potential crew recruitment.

### The Rusty Anchor Tavern

A gathering place for pirates, merchants, faction contacts, travelers, and recruitable sailors.

Offers rumors, contracts, dialogue, and exploration clues.

### Contract Board

Provides optional activities such as:

- Bounties
- Cargo recovery
- Exploration contracts
- Hunting dangerous creatures
- Investigating shipwrecks
- Delivery missions

### The Old Lighthouse

A landmark overlooking the port.

Contains an optional mystery involving a missing lighthouse keeper, strange lights offshore, and an abandoned journal.

### Market District

Offers basic equipment, provisions, supplies, and trade opportunities.

## Neutral Territory Rules

Breaker's Rest should remain politically neutral.

Faction members may visit without automatically fighting each other.

Players should not be attacked by visiting Piglins or Pillagers simply because of normal vanilla hostility rules.

Faction-specific hostility and reputation should be integrated carefully with custom mob AI.

Certain story events may threaten the settlement, but the primary hub should remain dependable and accessible.

---

# 5. Introductory NPC: Old Salt Briggs

Old Salt Briggs is an experienced sailor living near the gateway.

He acts as an optional guide, not a mandatory tutorial NPC.

His personality should be sarcastic, weathered, and knowledgeable about pirate history.

## Initial Dialogue Options

**Where am I?**

Explains the dimension and surrounding islands.

**Who's in charge here?**

Introduces the three major factions.

**How do I get a ship?**

Introduces the optional first-ship quest.

**Any work around here?**

Directs players toward the tavern and contract board.

## Dynamic Dialogue

Briggs should recognize player progression.

An unknown traveler receives basic guidance.

A renowned captain receives more respectful dialogue.

A notorious pirate may be greeted with caution or warnings about bounty hunters.

---

# 6. Three Major Factions

The dimension contains three established factions.

Each should have:

- Distinct visual identity
- Unique mob types and variants
- Settlements and controlled territory
- Reputation system
- Faction-specific quests
- Faction ranks
- Rewards and equipment
- Named captains
- Unique combat mechanics
- Distinct motivations and lore

## Faction A: The Crimson Corsairs

**Primary species:** Piglins and Piglin Brutes

**Theme:** Treasure hunting, smuggling, raiding, gold, greed, and piracy.

**Visual identity:** Crimson and gold pirate uniforms, red sails, gold accessories, weathered pirate equipment.

**Primary territory:**

- Tropical pirate settlements
- Smuggler coves
- Gold-rich islands
- Hidden harbors
- Pirate forts

### Personality and Lore

The Crimson Corsairs believe wealth and strength determine who deserves to rule the seas.

They are chaotic, aggressive, greedy, and sometimes humorous, but intensely loyal to their own crews.

### Important Leader

**Captain Gildtusk — The Golden Tyrant**

A powerful Piglin pirate captain obsessed with collecting legendary treasure.

### Mob Variants

**Crimson Crewman**
- Base: Piglin
- Standard melee pirate
- Uses sword or cutlass

**Crimson Gunner**
- Base: Piglin
- Ranged sailor
- Uses crossbow

**Crimson Brute**
- Base: Piglin Brute
- Heavy melee specialist
- Increased toughness and damage

**Crimson First Mate**
- Elite Piglin officer
- Stronger than standard sailors
- May provide leadership bonuses to nearby crew

**Crimson Captain**
- Named or elite Piglin leader
- Acts as miniboss or major encounter
- Uses unique equipment and mechanics
- May drop special treasure

### Faction Ranks

Outsider → Swabbie → Corsair → First Mate → Captain → Dread Captain

### Gameplay Activities

- Gold smuggling
- Treasure hunting
- Cargo raids
- Attacking Iron Fleet patrols
- Recovering stolen treasure
- Hunting rival pirate captains

### Potential Rewards

- Golden cutlasses
- Rare treasure maps
- Discounted Piglin trades
- Unique pirate equipment
- Access to hidden ports
- Special pirate crew recruitment

### Proposed Special Ability

**Golden Tribute**

Spend gold to hire temporary Piglin reinforcements.

---

## Faction B: The Iron Fleet

**Primary species:** Pillagers and other Illagers

**Theme:** Naval authority, discipline, conquest, military power, and law enforcement.

**Visual identity:** Iron-gray, blue, and black naval uniforms; reinforced armor; organized military vessels and fortified harbors.

**Primary territory:**

- Naval fortresses
- Watchtowers
- Military harbors
- Shipyards
- Strategic trading routes

### Personality and Lore

The Iron Fleet claims its mission is to eliminate piracy and establish order.

However, its leadership seeks absolute control over the seas.

Not every sailor or officer is necessarily evil.

### Important Leader

**Grand Admiral Blackthorn**

A ruthless Pillager admiral who believes total naval control is necessary to bring peace.

### Mob Variants

**Iron Sailor**
- Base: Pillager
- Standard naval marine
- Uses crossbow

**Iron Sharpshooter**
- Base: Pillager
- Elite ranged attacker
- Often stationed in elevated positions or crow's nests

**Iron Enforcer**
- Base: Vindicator
- Heavily armed boarding specialist
- Strong melee combatant

**Iron First Mate**
- Elite Illager officer
- Commands nearby naval units
- May provide combat coordination bonuses

**Iron Captain**
- Elite Illager commander
- Major encounter or miniboss
- May summon reinforcements or coordinate attacks

### Faction Ranks

Civilian → Recruit → Marine → Lieutenant → Captain → Admiral

### Gameplay Activities

- Pirate bounties
- Defending forts
- Capturing wanted criminals
- Escorting cargo
- Suppressing pirate raids
- Recovering stolen supplies

### Potential Rewards

- Reinforced naval armor
- Powerful crossbows
- Military equipment
- Ship upgrades
- Access to naval shipyards
- Allied naval reinforcements

### Proposed Special Ability

**Call to Arms**

Request temporary assistance from Iron Fleet marines under appropriate conditions.

---

## Faction C: The Drowned

**Primary species:** Zombies, Skeletons, Drowned, and Wither Skeletons

**Theme:** Curses, supernatural sailors, shipwrecks, ancient history, and undead fleets.

**Visual identity:** Tattered pirate uniforms, spectral lighting, barnacles, seaweed, rusted weapons, dark sails, and ghost ships.

**Primary territory:**

- Haunted islands
- Ghost ships
- Ship graveyards
- Underwater ruins
- Cursed temples
- Fog-covered waters

### Personality and Lore

The Drowned are sailors who died during ancient naval disasters and were bound to the sea by a mysterious curse.

Many are mindless monsters.

However, the oldest undead captains retain their memories, intelligence, and individual motivations.

Some undead leaders possess vital knowledge about the history of the dimension.

### Important Leader

**Captain Hollow-Eye — The Undying**

An ancient Wither Skeleton captain who remembers the events surrounding the original seven captains.

### Mob Variants

**Drowned Deckhand**
- Base: Zombie
- Standard undead pirate
- Uses rusty melee weapons

**Drowned Marksman**
- Base: Skeleton
- Ranged undead pirate
- Uses bow

**Drowned Harpooner**
- Base: Drowned
- Aquatic combat specialist
- Uses trident

**Drowned First Mate**
- Base: Wither Skeleton
- Elite undead officer
- Uses cursed melee weapons

**Drowned Captain**
- Elite undead pirate
- Powerful named boss or miniboss
- Uses supernatural combat abilities
- May summon undead reinforcements

### Faction Ranks

Living Intruder → Marked → Oathbound → Deathsworn → Harbinger → Wraith Captain

### Special Faction Access

The Drowned begin hostile toward ordinary players.

Joining or cooperating with them requires discovering a supernatural ritual, earning a cursed captain's trust, or recovering a special artifact.

This faction should feel mysterious and difficult to access.

### Potential Rewards

- Cursed swords
- Special tridents
- Spectral equipment
- Underwater exploration benefits
- Unique undead companions
- Access to ghost ships and ancient lore

### Proposed Special Ability

**Dead Man's Call**

Temporarily summon undead sailors to assist the player.

---

# 7. Mob Hierarchy and Combat Behavior

Faction enemies should feel like functioning crews rather than unrelated mobs.

General hierarchy:

Crew → Specialist → First Mate → Captain

## Crew

Ordinary enemies providing basic melee or ranged combat.

## Specialists

Enemies with distinct combat functions, such as sharpshooting, heavy melee attacks, or aquatic combat.

## First Mates

Elite units who command groups.

Possible mechanics:

- Buff nearby crew members
- Coordinate attacks
- Call limited reinforcements
- Improve nearby mob morale or aggressiveness

Defeating a First Mate could weaken or disrupt the remaining crew.

## Captains

Powerful named enemies with:

- Unique equipment
- Higher health
- Special abilities
- Distinct combat phases when appropriate
- Unique rewards
- Associated ships or strongholds

Do not make every captain a generic health-inflated mob.

Combat encounters should use coordinated enemy composition and environmental advantages.

---

# 8. Player Progression

Player progression is divided into four interconnected systems.

## A. Renown

Measures the player's overall fame throughout the dimension.

Earned through:

- Exploration
- Discovering islands
- Completing contracts
- Defeating important captains
- Finding legendary treasure
- Completing major adventures

Renown unlocks advanced opportunities and causes NPCs to recognize the player's achievements.

Renown is not the same as moral reputation.

## B. Faction Standing

Each player has separate standing with each major faction.

Proposed numerical range: -100 to +100.

Suggested reputation states:

- -100 through -50: Hunted
- -49 through -1: Hostile
- 0 through +24: Neutral
- +25 through +59: Trusted
- +60 through +89: Honored
- +90 through +100: Legendary

These thresholds are initial balancing suggestions.

Reputation affects:

- Mob hostility
- NPC dialogue
- Available contracts
- Trading access
- Recruitment
- Military assistance
- Restricted areas

Formal faction rank promotions should require important quests or achievements, not reputation alone.

## C. Infamy

Measures criminal notoriety and piracy.

Infamy increases through actions such as:

- Raiding ships
- Attacking naval patrols
- Stealing valuable cargo
- Accumulating bounties
- Attacking important faction leaders

High infamy may attract bounty hunters or unlock intimidating dialogue and pirate opportunities.

Infamy should be tracked separately from overall renown and faction standing.

## D. Captaincy

Measures the player's progress in commanding a crew and managing a ship.

Includes:

- Owning vessels
- Recruiting sailors
- Assigning officers
- Improving equipment
- Upgrading ships
- Establishing a personal hideout
- Expanding crew capabilities

This progression remains available to independent players and faction members.

---

# 9. Player Allegiance

**Confirmed design decision: Players may join existing factions OR establish independent pirate crews.**

Neither choice should be permanently required.

## Joining a Faction

Players may earn the right to join a faction by developing sufficient standing and completing an initiation quest.

Faction membership unlocks:

- Promotion quests
- Exclusive equipment
- Faction-aligned contracts
- Access to restricted locations
- Specialized crew members
- Naval or pirate support
- Recognition by faction NPCs

Players should eventually be able to captain their own vessels within the faction.

## Independent Pirate Captain

Players can form their own pirate crews without swearing loyalty to any existing faction.

Independent captains can:

- Recruit sailors
- Build and own ships
- Establish hideouts
- Accept selected contracts from multiple factions
- Trade with friendly settlements
- Raid hostile factions
- Collect bounties
- Claim strategic locations
- Become an influential fourth power

## Switching Allegiance

Players should be able to leave factions.

Leaving may cause:

- Lost privileges
- Reputation penalties
- Reduced trading access
- Desertion bounties
- Special amnesty or reconciliation quests

However, players should generally retain personally owned ships, legitimate inventory, and independent crew members.

Avoid irreversible decisions that permanently ruin sandbox progression without a deliberate gameplay reason.

---

# 10. Independent Player Faction

A long-term progression goal is allowing players to form their own pirate organization.

Possible features:

- Custom pirate flag or banner
- Named pirate crew
- Personal ship
- Crew recruitment
- Home port
- Resource storage
- Territory control
- Diplomacy with established factions

As the player's organization grows, other factions begin recognizing it as a significant power.

For example:

- The Crimson Corsairs may propose an alliance.
- The Iron Fleet may issue a bounty.
- The Drowned may offer forbidden knowledge.

The player's organization becomes a potential fourth power, but it should not require implementing a full kingdom-management simulation.

Start with simple mechanics and expand incrementally.

---

# 11. Crew Recruitment and Management

Recruitable NPC sailors are a major component of the experience.

Players should be able to assemble a crew through exploration, contracts, rescue missions, payments, and reputation.

## Crew Roles

### First Mate

- Elite combat companion
- Leads sailors
- Provides combat bonuses
- May have unique dialogue

### Navigator

- Helps locate destinations
- Provides treasure clues
- Improves exploration capabilities
- May identify rare locations

### Gunner

- Ranged combat specialist
- Defends vessels
- Participates in boarding combat

### Quartermaster

- Manages cargo or supplies
- Supports repairs and resupply
- May provide access to specialized trade

### Standard Sailors

General crew members who defend ships, accompany players where supported, or occupy assigned stations.

## Recruitment

Crew members should have varied requirements.

Examples:

- Piglins may require gold.
- Former naval officers may require rescue quests.
- Undead sailors may require cursed artifacts.
- Mercenaries may require payment.
- Named characters may require personal quest completion.

## Crew Loyalty

Introduce a lightweight loyalty mechanic.

Possible factors:

- Completing personal quests
- Winning battles
- Providing rewards
- Abandoning companions
- Betraying crew interests

Avoid excessive micromanagement.

## Example Recruitable NPC

**Garrick the Deserter**

An Iron Fleet officer imprisoned after refusing an unethical order.

The player can:

- Rescue and recruit him
- Turn him in for a bounty
- Free him without recruiting him

If recruited, Garrick becomes a skilled ranged officer with a personal storyline.

## Mixed-Faction Crews

Allow players to recruit members from different species and backgrounds when trust and recruitment conditions are satisfied.

A mixed crew of Piglins, former Pillagers, and intelligent undead should be possible as an advanced achievement.

However, this must not automatically make all hostile mobs friendly.

Crew allegiance and faction allegiance should be modeled separately.

---

# 12. Ships and Naval Progression

Ships are central to the pirate fantasy.

## Proposed Ship Classes

### Sloop

- Entry-level vessel
- Small crew
- Basic storage
- Limited combat capability

### Brig

- Midgame vessel
- Larger crew
- More cargo
- Improved combat and equipment capacity

### Frigate

- Advanced vessel
- Strong defenses
- Specialized crew stations
- Suitable for major naval operations

### Legendary Galleon

- Endgame vessel
- Large crew capacity
- Unique upgrades
- Important prestige achievement

## Ship Ownership

Both faction captains and independent players can own or command ships.

Ships may have:

- Names
- Crew assignments
- Cargo storage
- Equipment
- Repairs
- Upgrades
- Faction or custom banners

## Critical Open Technical Decision

Determine whether the project will use:

1. A compatible moving-ship mod or framework
2. A custom ship movement implementation
3. Stationary ship structures with travel mechanics
4. A hybrid approach

Do not assume working naval movement or naval combat already exists.

This choice affects world generation, island distances, NPC pathfinding, boarding mechanics, and multiplayer synchronization.

Design the systems so gameplay progression does not unnecessarily depend on one particular ship implementation.

---

# 13. First Ship Quest: A Ship of Your Own

The introductory ship quest is optional.

The player discovers an abandoned, damaged sloop at Breaker's Rest.

Its former captain disappeared during a voyage into the fog.

Provide three possible acquisition routes.

## Route A: The Salvager

Collect materials and recover parts from nearby wrecks to repair the abandoned ship.

Emphasizes exploration and resource gathering.

## Route B: The Merchant

Earn currency through contracts and trading to purchase or repair a small vessel.

Emphasizes economy and quests.

## Route C: The Pirate

Locate a small outlaw crew at a nearby cove.

Defeat or otherwise overcome the crew and claim their vessel.

Emphasizes combat.

All routes unlock equivalent initial access to naval exploration.

Players should also be able to leave the starting island using ordinary Minecraft boats or other existing means of travel.

Do not forcibly gate the entire dimension behind this quest.

---

# 14. Pirate Hideouts and Bases

Players can establish personal hideouts in appropriate locations.

Potential structures and facilities:

- Private dock
- Crew quarters
- Storage rooms
- Treasure vault
- Repair station
- Trading area
- Defensive positions
- Contract board

Faction members may receive access to special faction harbors.

Independent captains can establish their own bases.

Minecraft's natural building systems should remain useful and relevant.

Avoid replacing the building experience entirely with menus.

---

# 15. Quests and Sandbox Activities

Quests must be optional wherever practical.

Players should discover activities through:

- NPC dialogue
- Taverns
- Bounty boards
- Treasure maps
- Journals
- Exploration
- Shipwrecks
- Faction reputation
- Environmental clues

## Quest Categories

### Bounty Hunting

Track down named pirates, fugitives, or dangerous creatures.

### Treasure Hunting

Follow maps and clues to discover buried loot, secret vaults, and lost islands.

### Faction Contracts

Support existing factions through military, economic, or exploration assignments.

### Cargo Missions

Deliver, recover, escort, or steal shipments.

### Shipwreck Exploration

Search wrecks for treasure, journals, and historical clues.

### Legendary Captain Hunts

Challenge powerful captains for rare equipment and lore.

### Haunted Expeditions

Explore ghost ships, cursed locations, and ancient ruins.

### Crew Quests

Complete personal missions to recruit, develop, or retain important companions.

## Example Quest: A Pirate's Debt

Faction: Crimson Corsairs

A Piglin captain requests the recovery of stolen gold from the Iron Fleet.

Potential outcomes:

- Return the gold to the Corsairs
- Sell it to another buyer
- Keep the treasure

Consequences should reflect the player's choices.

## Example Quest: The King's Law

Faction: Iron Fleet

An officer places a bounty on a Piglin First Mate.

The player discovers evidence suggesting the officer is secretly trafficking confiscated cargo.

The player decides whether to complete the bounty or expose the corruption.

## Example Quest: Dead Men Tell Tales

Faction connection: The Drowned

A ghost ship appears offshore.

Its captain's journal points to an island missing from modern maps.

The player can follow clues to an ancient vault and discover part of the overarching mystery.

---

# 16. Living World Events

The dimension should feel active even when players are not following quests.

Proposed events:

## Naval Skirmishes

Iron Fleet and Crimson Corsair vessels clash.

Players may:

- Assist one side
- Attack both
- Loot wreckage
- Ignore the battle

## Ghost Ship Sightings

Rare undead ships appear during foggy weather, storms, or at night.

They provide dangerous optional encounters.

## Treasure Rumors

NPCs provide hints about hidden treasure, lost ships, or undiscovered islands.

## Outpost Raids

Factions occasionally attack rival positions.

Players may defend, raid, or exploit the situation.

## Bounty Hunters

Notorious players may encounter specialized hunters.

## Legendary Encounters

Rare captains or sea creatures may appear under special conditions.

For initial implementation, prefer controlled encounters at predefined locations rather than simulating a fully persistent naval economy.

---

# 17. Territory Control

Certain strategic locations can change ownership.

Examples:

- Fortresses
- Trading ports
- Shipyards
- Pirate coves
- Watchtowers
- Resource-rich islands

Territory ownership influences:

- NPC spawns
- Guards
- Banners
- Merchants
- Available contracts
- Patrols
- Access permissions
- Defensive encounters

## Example: Skullwatch Fort

An important coastal fortress with several possible states.

**Iron Fleet Control**

- Pillager marines defend the walls.
- Military contracts are available.
- The prison contains wanted pirates.

**Crimson Corsair Control**

- Piglin pirates occupy the fort.
- Black-market traders appear.
- Smuggling contracts become available.

**Drowned Control**

- The fort becomes haunted.
- Undead sailors patrol.
- A supernatural vault becomes accessible.

**Player Control**

- Recruited crew members guard the area.
- A personal quartermaster becomes available.
- Defensive contracts or encounters become accessible.

The same underlying structure can be reused while changing NPCs, decorations, and gameplay interactions.

Treat ownership as persistent world state.

Multiplayer territory conflicts and ownership permissions will need explicit rules.

---

# 18. Main Story: The Seven Seals

The overarching narrative should remain a mystery that players discover organically.

**Important naming restriction:** Do not use the previously proposed name "Heart of the Deep."

The current working central mystery is **The Seven Seals**.

The Siren's Crown, Black Compass, Leviathan's Eye, and Dreadwake are potential names for separate artifacts, legendary vessels, or future lore elements. None is finalized as the central object.

## Ancient History

Centuries ago, seven legendary pirate captains sailed into the Blackwater Expanse.

They discovered an ancient power buried beneath the ocean.

Legends claim the power could command storms, raise the dead, or reshape the seas.

The captains returned changed.

Their alliance collapsed.

Their fleets fought one another.

A supernatural storm engulfed their ships.

This disaster became known as:

**The Night of the Broken Tide**

The event created much of the modern mythology surrounding the Shattered Seas.

## The Seven Seals

Seven ancient artifacts or seals are scattered throughout the dimension.

Most people believe collecting them will reveal great treasure or supernatural power.

However, their true purpose is more dangerous.

The seals were separated to contain an ancient entity.

Reassembling or misusing them may weaken the entity's prison and threaten the dimension.

The detailed mythology and mechanics of this entity remain open for development.

---

# 19. Each Faction's Interpretation of the Legend

## Crimson Corsairs

Believe the seven captains concealed an unimaginable fortune.

Captain Gildtusk wants the seals to locate the treasure.

Their clues are hidden in treasure maps, buried caches, and forgotten pirate journals.

## Iron Fleet

Believe the seals are ancient weapons.

Grand Admiral Blackthorn wants their power to establish absolute control.

Their information comes from restricted naval archives, confiscated artifacts, and military expeditions.

## The Drowned

Know the legends are incomplete.

Captain Hollow-Eye remembers the ancient disaster.

The Drowned possess lost journals, cursed memories, and clues pointing to the seals' real purpose.

---

# 20. Storytelling Approach

The Seven Seals are a long-term mystery, not a compulsory campaign.

Players should discover information in any order where technically feasible.

For example, a hidden island could be discovered through:

- A Crimson treasure map
- A stolen Iron Fleet navigation chart
- A Drowned captain's journal
- Direct exploration

Different paths should lead toward the same discoveries.

The mystery gradually becomes clearer through environmental clues and questlines.

Do not require every player to complete every faction's quests.

## Potential Additional Twist

One of the original seven captains may still be alive and secretly manipulating the modern factions.

This is a proposed narrative direction, not finalized canon.

## Potential Endings

### Pirate King

The player claims control of the ancient power.

### Liberator

The player releases and defeats the imprisoned entity.

### Eternal Guardian

The player restores the prison and sacrifices access to the seals' power.

These endings should produce appropriate world and faction reactions.

Endings must not permanently destroy the player's ability to continue playing in the sandbox.

Specific ending mechanics remain to be designed.

---

# 21. Legendary Captains

Important faction leaders should be more than ordinary boss mobs.

Each notable captain should have:

- Name and title
- Faction allegiance
- Distinct personality
- Unique equipment
- A ship or headquarters
- Specialized crew
- Relevant quests
- Rewards
- Connections to dimension lore

Initial important characters:

**Captain Gildtusk**
- Piglin
- Crimson Corsairs
- Treasure-obsessed pirate leader

**Grand Admiral Blackthorn**
- Pillager
- Iron Fleet
- Ambitious naval commander

**Captain Hollow-Eye**
- Wither Skeleton
- The Drowned
- Ancient undead captain with knowledge of the original disaster

Some legendary captains may be enemies.

Others may become allies or important quest contacts.

Avoid forcing every named character into a boss encounter.

---

# 22. Player Onboarding and First 30 Minutes

The opening experience should introduce the sandbox without restricting exploration.

## First 5 Minutes

- Player enters the dimension.
- Arrives at Breaker's Rest.
- Sees the harbor and surrounding points of interest.
- Can speak to Old Salt Briggs.
- Can immediately explore independently.

## Minutes 5–10

- Discover the tavern.
- Visit the harbor.
- Examine the contract board.
- Learn basic information about the factions.
- Discover available ship options.

## Minutes 10–20

The player may complete an introductory contract.

Examples:

- Recover stolen cargo
- Explore a small shipwreck
- Defeat nearby outlaws
- Deliver supplies

## Minutes 20–30

The player acquires a vessel or uses another travel method.

Several nearby destinations provide meaningful choices.

A scripted or event-driven encounter may introduce conflict between Crimson Corsairs and the Iron Fleet.

The player can intervene or ignore it.

This establishes a core sandbox principle:

**The player chooses how to participate in the world.**

These timings are design targets, not mandatory pacing restrictions.

---

# 23. Progression Milestones

Avoid relying on a single character level.

Use milestone-based progression supported by the renown, reputation, infamy, and captaincy systems.

## Stage 1: Castaway

- Discover Breaker's Rest
- Learn about the dimension
- Complete basic contracts
- Acquire initial equipment

## Stage 2: Sailor

- Explore nearby islands
- Acquire a small ship
- Begin recruiting companions
- Encounter faction conflicts

## Stage 3: Adventurer

- Pursue faction relationships
- Complete advanced contracts
- Discover treasure locations
- Choose faction membership or independence

## Stage 4: Captain

- Command a developed crew
- Upgrade vessels
- Establish a hideout
- Challenge named captains
- Influence territory ownership

## Stage 5: Pirate Legend

- Confront legendary threats
- Discover ancient secrets
- Acquire rare equipment
- Shape the balance of power
- Potentially resolve the Seven Seals storyline

Players should be able to approach these milestones through different activities.

---

# 24. First Playable Version: Vertical Slice

Do not attempt to build the entire dimension immediately.

Create a small but functional region demonstrating the intended gameplay loop.

## Required Scope for the First Playable Version

### Starter Island

- Breaker's Rest
- Dimension arrival gateway
- Neutral harbor
- Tavern
- Contract board
- Introductory NPC

### Factions

- One outpost per faction
- Basic faction standing
- Core hostile/friendly behavior
- Initial faction contracts

### Mobs

Implement and validate a small representative set of custom sailors, officers, and captains.

Prioritize one functional combat encounter for each faction before expanding to every variant.

### Player Progression

- Basic renown
- Basic faction standing
- Initial infamy design
- First ship/captaincy milestone

### Crew

- One small recruitable crew system
- At least two recruitable example characters
- Basic follower or station behavior, depending on technical feasibility

### Exploration

- One nearby treasure island
- One hostile location
- One introductory dungeon or shipwreck

### Story

- One mysterious journal
- One ancient symbol or artifact clue
- Introduction to the Seven Seals

### Quests

- One Crimson Corsairs quest
- One Iron Fleet quest
- One Drowned-related adventure
- One independent captain progression quest

The objective is a cohesive, testable gameplay loop.

---

# 25. Engineering and Architecture Considerations

Design the implementation around loosely coupled systems.

Candidate domains include:

- Dimension and gateway management
- World generation and structures
- Faction definitions
- Reputation and infamy
- NPC and mob variants
- Quest definitions and completion
- Crew recruitment and persistence
- Ship ownership and upgrades
- Territory ownership
- World events
- Lore discoveries
- Player progression
- Multiplayer synchronization

These are conceptual system boundaries, not mandatory classes or projects.

Use the existing repository's architectural style.

## Data-Driven Content

Prefer data-driven definitions where the platform supports them for:

- Faction properties
- Mob configurations
- Quest requirements
- Quest rewards
- Reputation changes
- Crew role definitions
- Dialogue
- Territory rules
- Named characters
- Loot tables

Avoid scattering hardcoded gameplay identifiers and balance values throughout unrelated code.

## Persistence

Persistent data should include, as appropriate:

- Per-player faction reputation
- Player renown
- Player infamy
- Faction membership
- Completed quests
- Story discoveries
- Crew ownership
- Ship ownership
- Territory ownership
- Relevant world-event state

Use appropriate save mechanisms supported by the actual Minecraft platform.

## Multiplayer

Design for multiple players who may:

- Join different factions
- Have different reputation values
- Maintain separate crews
- Complete quests independently
- Share the same world
- Encounter shared territory states

Do not assume that per-player progression and global world state are interchangeable.

## Performance

Avoid unnecessary global entity scans, excessive ticking, uncontrolled mob spawning, or expensive world-event simulations.

Favor localized, event-driven systems when possible.

## Maintainability

Prefer:

- Modular components
- Clear ownership of responsibilities
- Small focused changes
- Configurable balance values
- Idiomatic language and framework patterns
- Well-defined persistence boundaries
- Testable gameplay logic

Preserve existing working features unless a change is necessary.

---

# 26. Confirmed Decisions vs. Open Questions

## Confirmed Direction

- Custom pirate dimension
- Craftable Overworld dimension gate
- Pirate sandbox rather than linear campaign
- Three major factions
- Piglin Crimson Corsairs
- Illager Iron Fleet
- Mixed-undead Drowned
- Faction mob variants including crew, officers, and captains
- Players can join factions
- Players can independently captain their own crews
- Multiple forms of player progression
- Treasure hunting and exploration
- Hidden overarching storyline
- Neutral starter hub
- Optional quests and onboarding
- Freedom to return to the Overworld
- No mandatory linear quest progression

## Working Design Proposals

- The Shattered Seas dimension name
- Breaker's Rest starter island
- Old Salt Briggs
- The Rusty Anchor Tavern
- The Seven Seals mythology
- Specific faction leaders
- Named faction ranks
- Faction abilities
- Territory conquest
- Ship class progression
- Crew loyalty
- Three narrative endings

Preserve these proposals in planning documentation, but keep them configurable where feasible.

## Unresolved Decisions

- Minecraft version and mod loader
- Existing project architecture
- Ship movement implementation
- Naval combat capabilities
- Ocean/world generation strategy
- Island spacing and travel times
- Custom NPC framework
- Quest/dialogue implementation
- Exact reputation values and balancing
- How territory capture is triggered
- Crew AI and navigation
- Crew behavior across dimensions
- Respawn rules
- Multiplayer ownership and shared progression policies
- Exact nature of the Seven Seals
- Final boss and ending implementation

Do not silently invent final answers to these questions.

Provide recommendations supported by the repository and available technology.

---

# 27. Requested Codex Deliverables

Start by analyzing the existing codebase.

Then produce organized project documentation, preferably in an appropriate docs directory.

Suggested documentation:

**PROJECT_OVERVIEW.md**
- Vision
- Gameplay principles
- World concept
- Confirmed decisions
- Open questions

**FACTIONS.md**
- Faction identities
- Mob variants
- Reputation
- Ranks
- Combat roles
- Relationships

**PLAYER_PROGRESSION.md**
- Renown
- Infamy
- Faction standing
- Captaincy
- Crew recruitment
- Ship ownership

**WORLD_DESIGN.md**
- Dimension entry
- Breaker's Rest
- Island archetypes
- Points of interest
- Territory control
- World events

**QUESTS_AND_LORE.md**
- Quest architecture
- Example quests
- Seven Seals
- Legendary captains
- Nonlinear storytelling

**TECHNICAL_ARCHITECTURE.md**
- Existing repository assessment
- Reusable systems
- Proposed components
- Persistence
- Multiplayer concerns
- Dependencies and risks

**IMPLEMENTATION_ROADMAP.md**
- Prioritized development phases
- Dependencies
- First playable vertical slice
- Small, independently testable implementation tasks
- Acceptance criteria

Adapt filenames to the repository's existing documentation conventions.

Do not create redundant files if comparable documentation already exists.

---

# 28. Final Instructions

Treat this as the authoritative initial design brief for The Shattered Seas, subject to later refinement.

The primary objective is a fun, maintainable, extensible Minecraft pirate sandbox.

Do not allow the main storyline to interfere with open exploration.

Do not force players into a faction.

Do not force players to complete quests to access ordinary sandbox gameplay.

Do not make all factions mechanically identical.

Do not assume unverified Minecraft APIs, mods, or project dependencies.

Do not attempt a massive rewrite.

**Your immediate task:**

1. Inspect the repository.
2. Identify existing features and technical constraints.
3. Organize and document this design.
4. Highlight implementation risks and unresolved decisions.
5. Produce a prioritized vertical-slice implementation roadmap.
6. Recommend the smallest technically sound first implementation.
7. Stop after the assessment and documentation phase unless explicitly instructed to begin implementation.

The long-term vision is a dimension where players can start as unknown sailors, explore dangerous seas, recruit unique crews, rise through existing factions or create their own, discover legendary treasures, and eventually become a power capable of changing the fate of the Shattered Seas.

**The world should provide the opportunities. The player should write the legend.**



I'd call it the Logbound Compass for now.


How the Logbound Compass works
1. Receive the compass at Breaker's Rest. Old Salt Briggs gives players their first calibrated compass, which points toward a nearby beginner-friendly island.
2. Sail toward the island. The compass tracks the destination's direction, helping players navigate even when land isn't visible.
3. Discover the island. Once players reach the island's designated boundary, the compass starts recalibrating.
4. Explore while it recalibrates. Players spend approximately 8–10 minutes exploring, finding treasure, fighting enemies, and completing quests.
5. The next destination unlocks. The compass updates to point toward another island, allowing the adventure to continue.