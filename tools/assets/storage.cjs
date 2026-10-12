// Storage Resources: Regenerate the four storage blocks without rewriting unrelated assets.
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../../src/main/resources');
function write(file, value) {
  const target = path.join(root, file);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, JSON.stringify(value, null, '\t') + '\n');
}
const blocks = {
  dump_chest: ['oak_planks', 'orange_concrete'],
  sorting_chest: ['spruce_planks', 'cyan_concrete'],
  bank_chest: ['dark_oak_planks', 'purple_concrete'],
  sorting_tube: ['iron_block', 'cyan_stained_glass']
};
function cube(from, to, texture) {
  return { from, to, faces: Object.fromEntries(['north', 'south', 'east', 'west', 'up', 'down'].map(face => [face, { texture }])) };
}
for (const [id, [body, accent]] of Object.entries(blocks)) {
  const tube = id === 'sorting_tube';
  const elements = tube ? [cube([4, 4, 0], [12, 12, 16], '#body'), cube([3, 3, 1], [13, 13, 3], '#accent'), cube([3, 3, 13], [13, 13, 15], '#accent')]
    : [cube([0, 0, 0], [16, 16, 16], '#body'), cube([0, 12, 0], [16, 14, 16], '#accent'), cube([6, 6, -.01], [10, 10, 0], '#accent')];
  // The gold arrow points toward the output face (north before block-state rotation).
  const top = tube ? 12 : 16;
  elements.push(cube([7, top, 5], [9, top + .05, 13], '#arrow'));
  elements.push(cube([5, top, 5], [11, top + .05, 7], '#arrow'));
  elements.push(cube([6, top, 4], [10, top + .05, 5], '#arrow'));
  elements.push(cube([7, top, 3], [9, top + .05, 4], '#arrow'));
  write(`assets/darkspawn/models/block/${id}.json`, { parent: 'minecraft:block/block', textures: { body: `minecraft:block/${body}`, accent: `minecraft:block/${accent}`, arrow: 'minecraft:block/gold_block', particle: `minecraft:block/${body}` }, elements });
  const orientations = { north: {}, east: { y: 90 }, south: { y: 180 }, west: { y: 270 }, up: { x: 90 }, down: { x: 270 } };
  write(`assets/darkspawn/blockstates/${id}.json`, { variants: Object.fromEntries(Object.entries(orientations).map(([facing, transform]) => [`facing=${facing}`, { model: `darkspawn:block/${id}`, ...transform }])) });
  write(`assets/darkspawn/items/${id}.json`, { model: { type: 'minecraft:model', model: `darkspawn:block/${id}` } });
  // The Bank always drops its block; its Java drop hook attaches the saved contents.
  write(`data/darkspawn/loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: `darkspawn:${id}` }], ...(id === 'bank_chest' ? {} : { conditions: [{ condition: 'minecraft:survives_explosion' }] }) }] });
}
const recipes = {
  dump_chest: { pattern: [' I ', ' C ', ' H '], key: { I: 'minecraft:iron_ingot', C: 'minecraft:chest', H: 'minecraft:hopper' } },
  sorting_chest: { pattern: [' I ', 'RCR', ' I '], key: { I: 'minecraft:iron_ingot', C: 'minecraft:chest', R: 'minecraft:redstone' } },
  bank_chest: { pattern: ['IDI', 'ECE', 'IOI'], key: { I: 'minecraft:iron_ingot', D: 'minecraft:diamond', E: 'minecraft:ender_pearl', C: 'minecraft:chest', O: 'minecraft:obsidian' } },
  sorting_tube: { pattern: ['III', 'GRG', 'III'], key: { I: 'minecraft:iron_nugget', G: 'minecraft:glass', R: 'minecraft:redstone' } }
};
for (const [id, recipe] of Object.entries(recipes)) {
  write(`data/darkspawn/recipe/${id}.json`, { type: 'minecraft:crafting_shaped', category: 'redstone', ...recipe, result: { id: `darkspawn:${id}`, count: id === 'sorting_tube' ? 8 : 1 } });
  write(`data/darkspawn/advancement/recipes/${id}.json`, { parent: 'minecraft:recipes/root', criteria: { has_ingredient: { trigger: 'minecraft:inventory_changed', conditions: { items: [{ items: id === 'sorting_tube' ? 'minecraft:iron_nugget' : 'minecraft:chest' }] } }, has_the_recipe: { trigger: 'minecraft:recipe_unlocked', conditions: { recipes: `darkspawn:${id}` } } }, requirements: [['has_ingredient', 'has_the_recipe']], rewards: { recipes: [`darkspawn:${id}`] } });
}
// Existing mining tags must retain their current entries and formatting.
for (const [tool, values] of Object.entries({ axe: ['dump_chest', 'sorting_chest', 'bank_chest'], pickaxe: ['sorting_tube'] })) {
  const file = path.join(root, `data/minecraft/tags/block/mineable/${tool}.json`);
  let content = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '{\n\t"replace": false,\n\t"values": []\n}\n';
  const existing = JSON.parse(content).values;
  const missing = values.map(id => `darkspawn:${id}`).filter(id => !existing.includes(id));
  if (missing.length) {
    content = content.replace(/("values"\s*:\s*\[)([^\]]*)(\])/, (_, open, entries, close) => open + entries.trimEnd() + (existing.length ? ', ' : '') + missing.map(id => JSON.stringify(id)).join(', ') + close);
    fs.writeFileSync(file, content);
  }
}
const translations = {
  'block.darkspawn.dump_chest': 'Dump Chest', 'block.darkspawn.sorting_chest': 'Sorting Chest',
  'block.darkspawn.bank_chest': 'Bank Chest', 'block.darkspawn.sorting_tube': 'Sorting Tube',
  'message.darkspawn.storage_output': 'Output: %s',
  'storage.darkspawn.sort_chest': 'Sort chest', 'storage.darkspawn.sort_chest_hint': 'Combine compatible stacks and sort all 27 chest slots by item ID.',
  'storage.darkspawn.view_bank': 'View Bank', 'storage.darkspawn.view_trash': 'View Trash',
  'storage.darkspawn.search': 'Search name or item ID', 'storage.darkspawn.refresh': 'Find / Refresh',
  'storage.darkspawn.refresh_hint': 'Apply search and refresh the item list. Newly arriving types appear on refresh.',
  'storage.darkspawn.order': 'Name / Count', 'storage.darkspawn.order_hint': 'Toggle alphabetical or highest-quantity ordering.',
  'storage.darkspawn.previous': '<', 'storage.darkspawn.previous_hint': 'Previous page',
  'storage.darkspawn.next': '>', 'storage.darkspawn.next_hint': 'Next page',
  'storage.darkspawn.tab': 'Bank / Trash', 'storage.darkspawn.tab_hint': 'Switch between stored items and recoverable trash.',
  'storage.darkspawn.deposit': 'Deposit', 'storage.darkspawn.deposit_hint': 'Deposit main inventory and unlocked extra rows. Keep hotbar, armor, and offhand.',
  'storage.darkspawn.resort': 'Re-sort', 'storage.darkspawn.resort_hint': 'Send stored items to matching chests on the connected Dump route. Trash is left untouched.',
  'storage.darkspawn.trash': 'Empty trash', 'storage.darkspawn.trash_hint': 'Click twice within five seconds to permanently delete this Bank\'s trash. New arrivals cancel confirmation.',
  'storage.darkspawn.confirm': 'Confirm?', 'storage.darkspawn.upgrade': 'Upgrade',
  'storage.darkspawn.upgrade_hint': 'Increase distinct item capacity.', 'storage.darkspawn.upgrade_cost': 'Upgrade costs %s diamond block(s). Maximum: 2,048 variants.',
  'storage.darkspawn.filters': 'Filters', 'storage.darkspawn.bank_page': 'Bank %s / %s', 'storage.darkspawn.trash_page': 'Trash %s / %s',
  'storage.darkspawn.capacity': 'Variants: %s / %s', 'storage.darkspawn.resorting': 'Re-sorting stored items...',
  'storage.darkspawn.withdraw_hint': 'Hover for counts / controls', 'storage.darkspawn.dump_hint': 'Deposit here to feed your sorting route.',
  'storage.darkspawn.filter_hint': 'Three filters choose accepted items.',
  'storage.darkspawn.direction_hint': 'Sneak + empty-hand use: rotate output.',
  'storage.darkspawn.ghost_hint': 'Click with an item to set. Right-click to clear. No items consumed.',
  'storage.darkspawn.trash_filter_hint': 'Overflow matching this item enters Trash. Click to set; right-click to clear.',
  'storage.darkspawn.quantity': 'Stored: %s', 'storage.darkspawn.withdraw_detail': 'Left: stack. Right: one. Shift-click: fill inventory.',
  'storage.darkspawn.bank_item_hint': 'Stored + trash: %s items; level %s. Contents travel with this block.',
  'storage.darkspawn.block_hint': 'Gold arrow = output. Empty-hand use opens; sneak-use rotates.',
  'storage.darkspawn.tube_hint': 'Gold arrow = output. Empty-hand use rotates. Follows up to 128 loaded blocks.'
};
const langPath = path.join(root, 'assets/darkspawn/lang/en_us.json');
// Preserve legacy language-file bytes; all additions in this generator are ASCII.
let langText = fs.readFileSync(langPath, 'latin1');
const existing = JSON.parse(langText);
const missing = Object.entries(translations).filter(([key]) => !(key in existing));
if (missing.length) {
  langText = langText.trimEnd().replace(/\s*}$/, ',\n' + missing.map(([key, value]) => '\t' + JSON.stringify(key) + ': ' + JSON.stringify(value)).join(',\n') + '\n}\n');
  fs.writeFileSync(langPath, langText, 'latin1');
}
