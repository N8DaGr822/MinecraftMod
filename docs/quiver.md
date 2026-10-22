# Quiver

October 20, 2026. User-selected scope: all arrow types, automatic bow feeding, one native bundle's capacity. Implemented in source; no release JAR.

Craft with three leather, one string and one ordinary arrow:

```text
 S
LAL
 L
```

S = string, L = leather, A = arrow. Fresh Testing Camps include a quiver and bow in barrel 10, ordinary/spectral/slowness arrows in barrel 11, and a guide page. Existing camps are not rebuilt. Quivers also appear in the Combat creative tab.

## Storage and selection

Use the Ingredient Pouch's native bundle controls: left-click to insert, right-click with an empty cursor to extract, and scroll over the bundle preview to select a stack. Using a held quiver drops its contents. The capacity bar and preview show native bundle contents.

Ordinary, spectral and tipped arrows share one bundle of capacity: normally 64 arrows total, not 64 per type. Different potion effects and names retain their components. Modded arrow items are eligible when they are actual arrow items in the native arrow tag. Items with customized smaller stack limits consume proportionally more capacity.

Non-arrows and items carrying container components are rejected. A quiver cannot be nested in another bundle or shulker box, including through a hopper. Ordinary chests and barrels can store it.

## Automatic bow feeding

Bows retain native priority for held and loose arrows. If none are available, they use the first usable quiver in inventory order, including unlocked expanded slots and the offhand. The selected eligible stack wins within that quiver; otherwise its first eligible stack is used.

Only a successful charged release consumes ammunition. The server uses the native enchantment ammo-cost calculation. Infinity saves ordinary arrows; tipped and spectral arrows are still consumed. Creative shots preserve the chosen arrow's components without consuming it. Free projectiles keep native pickup restrictions. Bow durability and projectile creation remain native.

After a paid shot, the remainder of the fired stack moves to the front. This preserves the chosen arrow type through inventory synchronization and saves, since native bundle selection itself is temporary. When that stack is exhausted, the first remaining eligible stack becomes available. Multiple quivers do not combine partial stacks to satisfy an increased ammunition cost.

Crossbows continue using loose ammunition. Quivers do not auto-collect arrows, supply arrows from remote storage or require an empty inventory slot to fire. Remove loose arrows if you want the quiver's selected type to take priority.

## Ownership and acceptance

Contents belong to the item, using the existing bundle component. Sorting, favorites, profile separation, ordinary storage and normal death/keep-inventory rules apply. A quiver is not insurance against item loss.

The icon layers existing bundle and arrow artwork; the open preview uses native bundle models. Live acceptance remains for icon readability, scrolling and selection with multiple tipped stacks, reconnect, two-client firing and dimension travel. Automated checks cover storage, actual arrow entities/components, Infinity, creative, early release, removed quivers, full inventories, expanded inventory, save/profile restoration and death drops.

For a quick Survival check, take the bow and quiver from barrel 10, insert samples from barrel 11 and keep no loose arrows in your inventory. Select slowness arrows, fire twice and confirm the quiver loses two arrows of that type. Add a loose ordinary arrow and confirm it fires first. Repeat with an Infinity bow: ordinary arrows should remain, while special arrows should be consumed. Then test selection after reconnect and returning from Ascension.

Validation: common/client compilation, 18 inventory unit checks, resource coverage, six Quiver server tests, four Ingredient Pouch regressions, three Testing Camp tests and the existing legendary-bow cost test passed. Logs: build/quiver-checks.log, build/quiver-pouch-regression.log, build/quiver-camp-regression.log, build/quiver-bow-regression.log. No packaged build or live-client acceptance is implied.
