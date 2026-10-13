# Seasonal Fabric Minecraft Server
## Product, Gameplay & Technical Design Specification

**Version:** 0.1  
**Date:** October 13, 2026  
**Status:** Planning

### 1. Project Vision

Create a publicly advertised, paid-admission Minecraft Java Edition multiplayer server running a custom Fabric mod.

The world persists for an entire season before resetting on a published schedule. The gameplay combines exploration, building, survival, PvE, and open-world PvP.

The server should be competitive without becoming an anarchy server or constant bloodbath.

**Core principles**
- Equal-price admission; no loot crates or pay-to-win purchases.
- Players can pursue PvP or non-PvP achievements.
- Wilderness should be dangerous, but players need small protected homes.
- Seasonal progression resets; achievements and historical recognition persist.
- Cosmetic rewards recognize accomplishment without providing gameplay advantages.

### 2. Server Access and Monetization

**Agreed:** Players must purchase access before joining. The server is discoverable publicly but whitelist-controlled.

**Proposed:** Use a season pass rather than a subscription for an initial launch.

Possible pricing models include:
- One-time admission.
- Monthly subscription.
- A fixed-price pass for each season.

A 90-day season and $20 admission were discussed as examples, not final decisions.

**Recommended technical approach**
- Minecraft server authentication enabled.
- Server whitelist enforced.
- Payments verified by a separate backend.
- Payment entitlements linked to Minecraft player UUIDs.
- Players granted access only while their entitlement is valid.

The Fabric mod handles gameplay, not checkout or external-payment verification.

A future website could use Blazor, ASP.NET Core, Stripe, and a database. Initially, payment verification and whitelist administration can be manual.

### 3. Seasonal World System

**Agreed:** Reset the multiplayer world at scheduled intervals.

At the end of a season:
1. Stop leaderboard scoring.
2. Finalize rankings and validate winners.
3. Archive season results.
4. Grant permanent cosmetics.
5. Stop the Minecraft server.
6. Back up the world and season data.
7. Generate a new world.
8. Rebuild the Hall of Fame.
9. Restore archived champion trophies.
10. Reset seasonal statistics and bonfire claims.
11. Open the next season.

**Permanent data:** Player identities, seasonal awards, cosmetic unlocks, Hall of Fame records, and payment history.

**Reset data:** Terrain, player builds, ordinary inventories, current-season rankings, and bonfire claims, subject to the final season-reset policy.

### 4. Leaderboard System

**Agreed:** Offer a variety of rankings rather than rewarding PvP exclusively.

**Required categories**
- **PvP Champion:** Most qualifying player kills.
- **Master Miner:** Most qualifying blocks mined.

**Additional proposed categories**
- Monster Slayer — hostile mobs killed.
- Boss Hunter — boss kills or weighted boss points.
- Explorer — unique regions discovered.
- Treasure Hunter — naturally generated valuable resources collected.
- Survivor — longest eligible survival streak.
- Seasonal Champion — completion of a special seasonal objective.
- Builder Award — optional community or staff-judged construction award.

Every category has an independent leaderboard.

**Anti-exploitation requirements**

PvP kills should not be farmable by repeatedly killing friends or alternate accounts. Mining should not count blocks repeatedly placed and broken by players. Additional categories need clearly documented qualification rules.

All statistics are validated and recorded server-side.

A `/leaderboard` command or graphical interface should show current rankings, the player's position, and seasonal results.

### 5. Champion Rewards

**Agreed direction:** Winners receive cosmetic prestige, never competitive power.

Rewards under consideration:
- Permanent Hall of Fame trophies.
- Decorative crowns.
- Special cosmetic saddles.
- Unique titles.
- Other purely visual effects.

A crown should not improve armor statistics. A decorative saddle should not provide additional riding capability. Cosmetic achievements should be bound to the player account and survive world resets.

### 6. Permanent Hall of Fame

**Agreed:** Every newly generated seasonal world contains a Hall of Fame with trophies from previous seasons.

The structure may contain:
- Trophy pedestals.
- Champion names.
- Leaderboard categories.
- Season identifiers.
- Final winning statistics.
- Decorative representations of previous champions.

**Recommended implementation**

Store archived award records outside the disposable world save.

At the start of each season, the Fabric mod places a reusable structure template and populates its displays from historical records.

The building should be protected from PvP, theft, explosions, and unauthorized modification.

An expandable layout with seasonal wings is proposed.

The public trophy is a permanent memorial, while a personal cosmetic unlock belongs to the winner.

### 7. PvP and World Zones

**Agreed:** Most of the world should allow PvP, with selected safe zones.

Three main zone types are proposed.

**Safe zones**
- No PvP.
- No unauthorized griefing.
- Intended for spawn, community areas, the Hall of Fame, and bonfire-protected homes.

**Wilderness**
- PvP enabled.
- Normal exploration, mining, building, and survival.
- Combat encounters provide danger without requiring active participation in competitive PvP.

**Contested zones**
- PvP enabled.
- Higher-risk locations such as dungeons, ruins, rare-resource areas, or seasonal objectives.
- Stronger risk and reward balance.

Player combat and building protection must be handled as separate permission systems.

### 8. Zone-Based Death Penalties

**Agreed:** Inventory and death consequences should depend on the zone.

**Proposed wilderness rules**
- Players retain equipped armor, weapons, and tools.
- A portion of collected resources may drop.
- Some experience may be lost.

A 25% resource-drop example was discussed but is not finalized.

**Proposed contested-zone rules**
- Full ordinary inventory drops.
- Permanent cosmetic unlocks remain safe.

**Safe-zone rules**
- PvP deaths are prevented.
- Environmental deaths require a separate policy.

Combat tagging should prevent players from exploiting zone boundaries to avoid combat consequences. The exact tag duration and safe-zone entry behavior still need design decisions.

### 9. Bonfire Sanctuary System

**Agreed:** The existing bonfire mechanic becomes the foundation of small protected player homes.

**Protection size:** 12 × 12 × 12 blocks.

**Total protected block positions:** 1,728.

The protection volume is a three-dimensional cube-like region around the bonfire, not a full-height land column.

This distinction is essential.

A surface bonfire must not create a protected cave deep underground. Protection only applies to blocks and players within the actual volume.

**Within the sanctuary**
- PvP is disabled.
- Unauthorized breaking and placing of blocks is prevented.
- Containers and storage are protected.
- Indirect griefing through explosions, fire, pistons, fluids, and other mechanisms must be addressed.

**Outside the sanctuary**
- The normal world-zone rules apply.
- Player structures extending beyond the cube are not automatically protected.
- Caves underneath the protected boundary remain wilderness.

The sanctuary should be large enough for a compact home with a basement and second story, without enabling massive protected compounds.

**Coordinate detail**

A bonfire block plus six blocks above and six below equals 13 vertical layers. An exact 12-block height requires a defined asymmetric coordinate range.

A possible interval is bonfire Y − 6 through bonfire Y + 5, inclusive.

The precise X, Y, and Z offsets must be finalized and tested.

**Proposed safeguards**
- One active protective bonfire per player or team.
- Optional trusted-player permissions.
- No overlapping claims.
- No claims in protected landmarks or contested zones.
- No new sanctuary activation during active combat.
- Visible temporary boundary preview.
- Decorative bonfires can exist without adding protected areas.

These safeguards are recommendations, not final decisions.

### 10. PvP Fairness and Player Protection

The goal is to allow combat without encouraging persistent harassment.

**Proposed protections**
- New-player grace period.
- Brief respawn protection.
- Combat tagging.
- Restrictions against spawn camping.
- Repeat-kill restrictions for leaderboard scoring.
- Protected community landmarks.
- Enforcement against griefing exploits.

Damage handling must consider arrows, pets, explosives, lingering effects, and attacks crossing zone boundaries.

### 11. Fabric Mod Architecture

The mod should use server-authoritative systems with clearly separated responsibilities.

**Proposed components**

- `ZoneManager` — resolves zone types and protected regions.
- `BonfireClaimManager` — manages bonfire ownership, claims, and geometry.
- `ProtectionService` — enforces block, entity, container, and indirect-grief permissions.
- `PvPManager` — evaluates whether player damage is permitted.
- `CombatTagManager` — maintains combat state.
- `DeathRuleService` — applies zone-specific death penalties.
- `LeaderboardManager` — tracks validated statistics.
- `SeasonManager` — manages season states and score finalization.
- `CosmeticUnlockService` — tracks permanent cosmetic entitlements.
- `HallOfFameManager` — regenerates and populates historical memorial displays.

The final APIs and event hooks depend on the target Minecraft and Fabric versions.

### 12. Data Persistence

Separate temporary seasonal data from permanent server history.

**Seasonal records**
- Current leaderboard values.
- Active bonfire claims.
- Seasonal player progression.
- World-specific state.

**Permanent records**
- Player UUIDs and name history.
- Completed seasons.
- Winner awards.
- Permanent cosmetic unlocks.
- Payment and admission records.

SQLite may be sufficient for an early server. PostgreSQL is an option for greater operational scale.

High-frequency events such as mining should be aggregated efficiently rather than causing synchronous database writes on every event.

### 13. Moderation and Fair Competition

Publish rules covering:
- Griefing and claim bypasses.
- Combat harassment and spawn camping.
- Leaderboard manipulation.
- Exploits and alternate-account collusion.
- Abusive conduct.
- Disputes and moderation appeals.

Archive final leaderboard results with an audit trail.

Staff should have tools for investigating suspicious scores without silently modifying historical achievements.

### 14. Minecraft Monetization Compliance

Review Minecraft's official Usage Guidelines and EULA before accepting payments.

The intended business model is charging for admission to an independently hosted server, not selling the Fabric mod or granting gameplay advantages through purchases.

The official guidelines impose conditions on paid admission, pricing, mod access, purchases, and advertising. They also restrict real-money play-to-earn functionality and certain cape-like cosmetics.

Cash prizes, gift cards, and cash-convertible rewards are outside the proposed design.

**Official references**
- https://www.minecraft.net/en-us/usage-guidelines
- https://www.minecraft.net/en-us/eula

Compliance must be reviewed again before launch.

### 15. Financial Planning

An illustrative $20 season pass produces the following gross revenue:

| Paying players | Gross revenue per season |
|---|---:|
| 25 | $500 |
| 50 | $1,000 |
| 100 | $2,000 |
| 200 | $4,000 |
| 500 | $10,000 |

These are hypothetical projections, not industry averages or guaranteed revenue.

Operating costs include hosting, payment processing, storage, backups, security, moderation, taxes, refunds, and development time.

The initial business goal should be a sustainable community and sufficient revenue to cover operations.

### 16. Testing and Acceptance Criteria

Before launching:
- Unpaid Minecraft accounts cannot bypass the whitelist.
- Authorized accounts can connect successfully.
- Bonfire claims protect exactly the intended 12 × 12 × 12 volume.
- Deep underground caves are not inadvertently protected.
- Unauthorized block modification and indirect griefing fail inside sanctuaries.
- PvP permissions are correct at every zone boundary.
- Inventory loss follows the selected zone rules.
- Leaderboards reject obvious stat-farming exploits.
- World resets preserve historical awards and cosmetics.
- Hall of Fame generation is repeatable and does not duplicate trophies.
- Server restart and backup restoration do not corrupt persistent data.

### 17. Remaining Decisions

These items still require agreement:

1. Season length, admission price, and payment terms.
2. Exact Minecraft/Fabric versions.
3. Claim ownership and trusted-player permissions.
4. Precise bonfire boundary offsets.
5. Bonfire fuel, destruction, and deactivation behavior.
6. Exact wilderness and contested-zone loot penalties.
7. Combat tagging, respawn protection, and safe-zone entry behavior.
8. Final leaderboard categories and validation rules.
9. Hall of Fame layout and cosmetic rendering.
10. Hosting capacity and moderation requirements.

### 18. Implementation Roadmap

**Milestone A — Bonfire Protection**

Implement bounded 3D claims, persistence, ownership, and comprehensive anti-grief rules.

**Milestone B — Zones and Combat**

Implement safe areas, wilderness, contested zones, combat tagging, and zone-specific death rules.

**Milestone C — Leaderboards**

Implement validated PvP and mining statistics, followed by additional categories and anti-farming protections.

**Milestone D — Hall of Fame**

Implement seasonal archives, permanent cosmetic unlocks, trophies, and automatic memorial reconstruction after wipes.

**Milestone E — Public Launch**

Finalize admission pricing, policy review, whitelisting, backups, moderation, payment integration, and operational monitoring.

---

**Project direction:** A seasonal Fabric survival server where players earn recognition through achievement, establish small protected bonfire homes, experience meaningful open-world risk, and contribute to a permanent history preserved in the Hall of Fame.