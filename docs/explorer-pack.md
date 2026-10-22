# Explorer's Pack

October 20, 2026. Approved first version: six exploration-only slots, manual access and no nested containers. Implemented in source; no release JAR.

Hold the pack in either hand and use it to open a three-by-two grid. Each slot holds the item's normal maximum stack size: maps can stack; brushes and spyglasses cannot. Move tools back to your inventory before using them. There is no automatic tool selection or remote inventory access.

Accepted contents: empty and filled maps, ordinary and recovery compasses, clocks, spyglasses, brushes, the Landmark Compass and Logbound Compass. Item data such as map IDs, names, compass targets and brush durability stay attached to the stored item. Eligibility uses `darkspawn:explorer_pack_contents`; data-pack additions still cannot introduce nested storage components.

Craft with three leather, two string and a compass:

```text
S S
LCL
 L
```

The Tools and Utilities creative tab also contains the pack. Fresh Testing Camps supply one pack and five sample tools in barrel 09, with an explanation in the guide. Existing camps are unchanged.

## Ownership and controls

Use normal clicks, dragging and shift-click transfers. Pack items are locked in the inventory while this screen is open, including hotbar and offhand swaps. Close the screen to move a pack. The visible player inventory uses the same standard inventory layout as existing storage screens; move a pack or tool out of expanded storage first when needed.

Contents save to the pack's native item-container component on every menu mutation. They follow the pack through saves, ordinary storage, dimension inventory profiles and normal death/keep-inventory rules. Removing the bound pack externally invalidates the menu. Command-authored contents beyond six slots refuse to open rather than silently losing the extra items; resolve such data administratively. Existing invalid items within six slots can be removed, but new invalid items cannot be inserted.

The pack cannot go inside bundles, pouches, quivers, other packs or shulker boxes, including hopper insertion. Its icon layers existing bundle and compass artwork. The screen follows the existing storage panel style.

## Acceptance

Targeted checks cover eligibility, six-slot capacity, immediate persistence, full-pack transfer rejection, reopen/withdrawal, source locks, invalidated menus, hopper nesting, map components, profile serialization and death while open. Live-client acceptance remains for screen/icon readability, drag controls, reconnect and two-client observation.

For a Survival playtest, take the pack and tools from barrel 09. Open it in each hand, store a named filled map and a bound compass, close and reopen, then withdraw them and confirm their targets. Test shift-click, dragging, number keys, the offhand key, a full inventory, reconnect and ordinary death recovery. Ascension uses separate possessions; the camp's pack should remain in its original profile.

Validation: common/client compilation, 18 inventory unit tests and resource coverage passed. All five Explorer's Pack server tests passed, including drag distribution, full-inventory safety, compass targets, map IDs, profile serialization, death, stale-menu rejection and malformed overflow. All three updated Testing Camp tests also passed. See `build/explorer-pack-checks.log`, `build/explorer-pack-final.log` and `build/explorer-pack-camp.log`. Live-client acceptance remains open; no JAR was packaged.
