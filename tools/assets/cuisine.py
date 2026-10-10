"""Generate the cuisine catalog and its recipes/models from the definitions below."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'src/main/resources'
JAVA = ROOT / 'src/main/java/darkspawn/black/cooking'

# ID | effect | category | station ingredients (one each) | vanilla presentation
MEALS = '''hunters_stew|HUNTER|Taiga|cooked_rabbit potato sweet_berries @onion|rabbit_stew
farmhouse_roast|HEARTY|Plains|cooked_beef carrot potato wheat|cooked_beef
spiced_steak|FIREPROOF|Desert|cooked_beef @pepper @garlic @salt|cooked_beef
tropical_skewer|IRON_STOMACH|Jungle|cooked_chicken melon_slice cocoa_beans @pepper|cooked_chicken
bog_gumbo|IRON_STOMACH|Swamp|cooked_cod brown_mushroom @onion @wild_herbs|mushroom_stew
miners_meal|MINER|Mountains|cooked_mutton potato bread brown_mushroom|rabbit_stew
winter_hotpot|WARMING|Frozen|cooked_porkchop potato @garlic @butter|rabbit_stew
seafood_rice|DEEP_BREATH|Ocean|cooked_salmon @rice @onion @salt|cooked_salmon
mushroom_risotto|SUSTAINED|Mushroom Fields|@rice red_mushroom @butter @garlic|mushroom_stew
nether_chili|FIREPROOF|Nether|cooked_porkchop @pepper @tomato @corn|beetroot_soup
ender_salad|FLOATING|End|chorus_fruit @corn @tomato @wild_herbs|chorus_fruit
garlic_bread|SUSTAINED|Everyday|bread @garlic @butter|bread
cheesy_potato|HEARTY|Everyday|baked_potato @cheese @butter|baked_potato
vegetable_pizza|SUSTAINED|Everyday|wheat @tomato @cheese @onion|bread
cornbread|SUSTAINED|Everyday|@corn wheat @butter|bread
farmers_lunch|ENERGIZED|Utility|@corn carrot bread @cheese|bread
fishermans_stew|FISHER|Utility|cooked_cod @tomato @wild_herbs bowl|rabbit_stew
builders_lunch|SUREFOOTED|Utility|bread cooked_mutton @cheese @tomato|bread
explorers_meal|ENERGIZED|Utility|cooked_rabbit @rice @pepper @garlic|cooked_rabbit
night_watch_stew|HERBAL|Utility|brown_mushroom golden_carrot @onion bowl|mushroom_stew
berry_pie|ENERGIZED|Desserts|sweet_berries wheat sugar egg|pumpkin_pie
chocolate_cake|MINER|Desserts|cocoa_beans milk_bucket wheat sugar|cake
honey_tart|SWEET|Desserts|honey_bottle wheat egg|pumpkin_pie
chorus_pie|FLOATING|Desserts|chorus_fruit wheat sugar|pumpkin_pie
captains_chowder|SEAFOOD|Ocean|cooked_cod @rice @butter @salt|rabbit_stew
frost_garlic_soup|WARMING|Frozen|@frost_garlic potato @butter bowl|rabbit_stew
jungle_pepper_skewer|IRON_STOMACH|Jungle|@jungle_pepper cooked_chicken @corn|cooked_chicken
marsh_rice_bowl|SUSTAINED|Swamp|@marsh_rice brown_mushroom @onion|mushroom_stew'''

# All boss families have a separate, renewable culinary reward and a meal.
BOSSES = '''ancient_tree_spirit|ancient_sap|ancient_sap_pudding|SWEET|oak_log
mutant_wolf|prime_beast_meat|prime_hunters_roast|FEAST|gray_wool
mutant_zombie|restored_marrow|restorative_broth|HEARTY|mossy_cobblestone
fossil_tyrant|marrow_extract|tyrants_stock|SAVORY|bone_block_side
thunder_bird|charged_yolk|storm_omelette|ENERGIZED|gold_block
titan_boa|titan_meat|titan_curry|SAVORY|moss_block
baba_yaga|witch_spice|witchs_gumbo|IRON_STOMACH|purple_wool
mountain_titan|mineral_salt|mountain_bisque|SUREFOOTED|stone
ice_wyrm|wyrm_fat|hot_wyrm_stew|WARMING|blue_ice
kraken|kraken_meat|kraken_platter|SEAFOOD|prismarine
cave_crawler|cave_truffle|cavern_stew|MINER|black_wool
shadow_creeper_queen|royal_jelly|shadow_stew|HERBAL|sculk
mycelial_sovereign|sovereign_cap|sovereign_risotto|SWEET|red_mushroom_block
netherborn|infernal_marrow|infernal_roast|FIREPROOF|magma
soulbound_colossus|soul_salt|soul_broth|SUSTAINED|soul_sand
void_eye|void_essence|void_delight|FLOATING|purpur_block'''

FEASTS = '''hunters_feast|FEAST|@prime_hunters_roast @hunters_stew bread @cheese
woodland_feast|SWEET|@woodland_stew @ancient_sap_pudding @garlic_bread @cornbread
ocean_feast|SEAFOOD|@kraken_platter @seafood_rice @fish_chowder @salt
nether_feast|FIREPROOF|@infernal_roast @nether_chili @spiced_steak @pepper
ender_banquet|FLOATING|@void_delight @chorus_pie @ender_salad @butter
hero_feast|FEAST|@prime_hunters_roast @kraken_platter @sovereign_risotto @void_delight'''
LOCKED = {'shadow_stew': 'Ancient City chests', 'infernal_roast': 'Bastion treasure chests',
          'captains_chowder': 'Shipwreck supply chests', 'bog_gumbo': 'Journeyman Chef',
          'hero_feast': 'Eat 20 distinct meals, or trade with a master Chef',
          'frost_garlic_soup': 'Igloo chests', 'jungle_pepper_skewer': 'Jungle temple chests',
          'marsh_rice_bowl': 'Dungeon chests'}

def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + '\n', encoding='utf-8')

def title(name): return name.replace('_', ' ').title()
def ids(parts): return [('darkspawn:' + p[1:] if p.startswith('@') else 'minecraft:' + p) for p in parts.split()]

def generate():
    lang = json.loads((RES / 'assets/darkspawn/lang/en_us.json').read_text(encoding='utf-8'))
    def item(name, texture):
        write(RES / f'assets/darkspawn/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'darkspawn:item/{name}'}})
        write(RES / f'assets/darkspawn/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/' + texture}})
        lang['item.darkspawn.' + name] = title(name)
    def recipe(name, parts):
        write(RES / f'data/darkspawn/recipe/{name}.json', {'type': 'darkspawn:cooking', 'ingredients': ids(parts), 'result': {'id': 'darkspawn:' + name}})
    def block(name, models, conditions):
        write(RES / f'assets/darkspawn/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': next(iter(models.values()))['model']}})
        write(RES / f'assets/darkspawn/blockstates/{name}.json', {'variants': models})
        write(RES / f'data/darkspawn/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'darkspawn:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}] + conditions}]})
        lang['block.darkspawn.' + name] = title(name)
    rows = [r.split('|') for r in MEALS.splitlines()]
    bosses = [r.split('|') for r in BOSSES.splitlines()]
    code = []
    for boss, ingredient, meal, effect, texture in bosses:
        rows.append([meal, effect, 'Boss Cuisine', f'@{ingredient} @rice @butter @wild_herbs', 'rabbit_stew'])
        item(ingredient, 'sugar' if 'salt' in ingredient else 'honey_bottle' if ingredient == 'ancient_sap' else 'rabbit')
        code.append(f'  INGREDIENTS.put(BossKind.{boss.upper()}, register("{ingredient}", Item::new));')
        name = boss + '_trophy'
        block(name, {'': {'model': 'darkspawn:block/' + name}}, [])
        elements = []
        for lo, hi, tex in [([3,0,3], [13,3,13], 'base'), ([6,3,6], [10,6,10], 'base'), ([4,6,4], [12,14,12], 'award')]:
            elements.append({'from': lo, 'to': hi, 'faces': {f: {'texture': '#' + tex} for f in ['up','down','north','south','east','west']}})
        write(RES / f'assets/darkspawn/models/block/{name}.json', {'parent': 'minecraft:block/block', 'textures': {'particle': 'minecraft:block/' + texture, 'base': 'minecraft:block/polished_deepslate', 'award': 'minecraft:block/' + texture}, 'elements': elements})
    for name, effect, category, parts, tex in rows:
        seconds = (120 if effect == 'SWEET' else 360) if category == 'Boss Cuisine' else (45 if effect == 'SWEET' else 180)
        code.append(f'  addMeal("{name}", MealEffects.{effect}, {12 if category == "Boss Cuisine" else 8}, {seconds}, "{category}", "{LOCKED.get(name, "")}", {str("bowl" in parts.split()).lower()});')
        item(name, tex); recipe(name, parts)
    for name, effect, parts in [r.split('|') for r in FEASTS.splitlines()]:
        code.append(f'  addFeast("{name}", MealEffects.{effect}, "{LOCKED.get(name, "")}");')
        recipe(name, parts)
        block(name, {f'servings={n}': {'model': f'darkspawn:block/{name}_{n}'} for n in range(6,0,-1)}, [{'condition': 'minecraft:block_state_property', 'block': 'darkspawn:' + name, 'properties': {'servings': '6'}}])
        for n in range(1,7):
            write(RES / f'assets/darkspawn/models/block/{name}_{n}.json', {'parent':'minecraft:block/block', 'textures': {'particle':'minecraft:block/cake_top', 'top':'minecraft:block/cake_top', 'side':'minecraft:block/cake_side', 'bottom':'minecraft:block/cake_bottom'}, 'elements':[{'from':[1,0,1], 'to':[1+n*14/6,8,15], 'faces':{f:{'texture':'#'+('top' if f=='up' else 'bottom' if f=='down' else 'side')} for f in ['up','down','north','south','east','west']}}]})
    for name in LOCKED: item(name + '_scroll', 'paper')
    item('cookbook', 'book')
    write(RES / 'data/darkspawn/recipe/cookbook.json', {'type':'minecraft:crafting_shapeless','ingredients':['minecraft:book','darkspawn:wild_herbs'],'result':{'id':'darkspawn:cookbook'}})
    source = (Path(__file__).with_name('cuisine.java.template')).read_text(encoding='utf-8').replace('// CATALOG', '\n'.join(code))
    (JAVA / 'Cuisine.java').write_text(source, encoding='utf-8')
    for key,label in {'sustained':'Hearty Sustenance','hunter':"Hunter's Instinct",'surefooted':'Surefooted','iron_stomach':'Iron Stomach','deep_breath':'Deep Breath','energized':'Energized','miner':"Miner's Focus",'fisher':"Fisher's Luck",'warming':'Warming','fireproof':'Spiced Protection','floating':'Lightfoot','feast':"Hunter's Feast"}.items():
        lang['effect.darkspawn.' + key + '_meal'] = 'Well Fed: ' + label
    for key,detail in {'herbal':'Night vision.', 'sweet':'Heal half a heart every 2.5 seconds.', 'savory':'+3 attack damage.',
                       'seafood':'Breathe underwater.', 'hearty':'Two temporary absorption hearts.', 'sustained':'20% less food exhaustion.',
                       'hunter':'+10% speed; hostile attackers glow for 3s.', 'surefooted':'+30% knockback resistance; 35% less fall damage.',
                       'iron_stomach':'Halves newly applied poison duration.', 'deep_breath':'Air drains half as often underwater.',
                       'energized':'+10% speed; 20% less exhaustion while sprinting.', 'miner':'+20% block-breaking speed.',
                       'fisher':'+1 Luck for fishing loot.', 'warming':'Prevents freezing.', 'fireproof':'Fire resistance.',
                       'floating':'Slow falling.', 'feast':'+3 attack damage and +20% speed.'}.items():
        lang['tooltip.darkspawn.' + key + '_meal_detail'] = detail
    lang.update({'message.darkspawn.recipe_known':'You already know this recipe.', 'message.darkspawn.recipe_learned':'Recipe learned: %s', 'entity.minecraft.villager.darkspawn.chef':'Chef', 'entity.darkspawn.chef':'Chef', 'container.darkspawn.recipe_locked':'Locked - see your cookbook.'})
    gameplay(bosses, lang)
    write(RES / 'assets/darkspawn/lang/en_us.json', lang)
    from cuisine_art import generate_art
    generate_art(rows, bosses)
    print(f'Generated {len(rows)} meals, 6 feasts, 16 culinary ingredients, 16 trophies.')

def gameplay(bosses, lang):
    def advancement(name, title_text, description, icon, parent='root', criteria=None):
        criteria = criteria or {'earned': {'trigger': 'minecraft:impossible'}}
        display = {'title': {'text': title_text}, 'description': {'text': description}, 'icon': {'id': icon}}
        obj = {'display': display, 'criteria': criteria, 'requirements': [[key] for key in criteria]}
        if parent is None:
            display.update({'background':'minecraft:gui/advancements/backgrounds/stone', 'show_toast':False, 'announce_to_chat':False})
        else: obj['parent'] = 'darkspawn:progress/' + parent
        write(RES / f'data/darkspawn/advancement/progress/{name}.json', obj)
    advancement('root','Darkspawn','A world beyond the dragon.','minecraft:dragon_egg',None,
                {'dragon': {'trigger':'minecraft:player_killed_entity','conditions':{'entity':{'type':'minecraft:entity_properties','entity':'this','predicate':{'minecraft:entity_type':'minecraft:ender_dragon'}}}}})
    advancement('first_summon','Something Stirs','Complete a biome boss ritual.','darkspawn:ancient_heartwood')
    for boss, ingredient, meal, effect, texture in bosses:
        advancement('defeat_'+boss, title(boss), 'Contribute to defeating this boss and remain nearby.', 'darkspawn:'+boss+'_trophy', 'first_summon')
        advancement('heart_'+boss, 'Heart: '+title(boss), 'Absorb this unique boss heart.', 'darkspawn:'+boss+'_heart', 'defeat_'+boss)
    advancement('conqueror','Conqueror','Defeat all 16 boss families.','minecraft:nether_star','first_summon',{b[0]:{'trigger':'minecraft:impossible'} for b in bosses})
    advancement('heart_collector','Heart Collector','Absorb eight unique boss hearts.','minecraft:golden_apple')
    advancement('beyond_mortal','Beyond Mortal','Reach all 15 heart upgrades.','minecraft:enchanted_golden_apple','heart_collector')
    advancement('varied_diet','Varied Diet','Eat three different foods in your last eight eating events.','darkspawn:berry_pie')
    advancement('well_rounded','Well Rounded','Eat 20 different meals; discover Hero Feast.','darkspawn:hero_feast','varied_diet')
    advancement('recipe_discovery','Kitchen Discoveries','Learn a recipe scroll.','darkspawn:cookbook')
    advancement('shared_table','A Shared Table','Eat a serving from a feast.','darkspawn:hunters_feast','recipe_discovery')
    write(RES/'data/minecraft/tags/point_of_interest_type/acquirable_job_site.json', {'replace':False, 'values':['darkspawn:chef']})
    # Two guaranteed choices per level. Vanilla owns profession levels, stock, price changes and restocking.
    trades = [
        [('minecraft:carrot',16,'minecraft:emerald',1,2),('minecraft:emerald',1,'darkspawn:butter',4,1)],
        [('minecraft:emerald',2,'darkspawn:cheese',4,5),('minecraft:emerald',3,'darkspawn:garlic_bread',2,5)],
        [('minecraft:emerald',8,'darkspawn:bog_gumbo_scroll',1,10),('minecraft:emerald',4,'darkspawn:hunters_stew',1,10)],
        [('minecraft:emerald',12,'darkspawn:captains_chowder_scroll',1,15),('minecraft:emerald',5,'darkspawn:builders_lunch',2,15)],
        [('minecraft:emerald',24,'darkspawn:hero_feast_scroll',1,30),('minecraft:emerald',6,'darkspawn:berry_pie',3,30)]
    ]
    for level, offers in enumerate(trades,1):
        names=[]
        for index,(want,cost,give,count,xp) in enumerate(offers):
            name=f'chef/{level}/offer_{index}'; names.append('darkspawn:'+name)
            write(RES/f'data/darkspawn/villager_trade/{name}.json', {'wants':{'id':want,'count':cost},'gives':{'id':give,'count':count},'max_uses':4 if 'scroll' in give else 12,'xp':xp,'reputation_discount':0.05})
        write(RES/f'data/darkspawn/trade_set/chef/level_{level}.json', {'amount':2,'trades':names})
    powers = [
        ('shield_rootbound','minecraft:shield','living_heartwood','Rootbound Shield: successful blocks slow the attacker for 2s.'),
        ('shield_stoneguard','minecraft:shield','mountain_titan_essence','Stoneguard Shield: +50% knockback resistance while raised.'),
        ('bow_stormshot','minecraft:bow','thunder_bird_essence','Stormshot: arrows slow their target and arc 3 damage to one nearby monster.'),
        ('bow_voidmark','minecraft:bow','void_eye_essence','Voidmark: arrows reveal targets for 5s and slow them for 3s.'),
        ('armor_winter','#minecraft:enchantable/armor','ice_wyrm_essence','Winter Ward: immune to freezing while worn. Duplicate pieces do not stack.'),
        ('armor_tide','#minecraft:enchantable/armor','kraken_essence','Tidal Armor: +35% water movement efficiency while worn. Does not stack.'),
        ('armor_shadow','#minecraft:enchantable/armor','shadow_creeper_queen_essence','Shadow Ward: removes Darkness while worn. Does not stack.'),
        ('shield_frostguard','minecraft:shield','ice_wyrm_essence','Frostguard: successful blocks weaken the attacker for 3s.'),
        ('bow_venom','minecraft:bow','titan_boa_essence','Venomshot: arrows apply Poison I for 3s.'),
        ('armor_highland','#minecraft:enchantable/armor','mountain_titan_essence','Highland Armor: 25% less fall damage. Duplicate pieces do not stack.')]
    for name,base,addition,tooltip in powers:
        write(RES/f'data/darkspawn/recipe/{name}_empowerment.json', {'type':'darkspawn:boss_empowerment','template':'minecraft:amethyst_shard','base':base,'addition':'darkspawn:'+addition,'power':name})
        lang['tooltip.darkspawn.'+name]=tooltip

if __name__ == '__main__': generate()
