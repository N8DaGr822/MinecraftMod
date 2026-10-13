from pathlib import Path
import ast,re,json
root=Path(__file__).resolve().parents[2]; guide=(root/'tools/docs/create-implementation-guide.py').read_text(encoding='utf-8'); readme=(root/'README.md').read_text(encoding='utf-8'); lang=json.loads((root/'src/main/resources/assets/darkspawn/lang/en_us.json').read_text(encoding='utf-8'))
def table(prefix):
 lines=readme.splitlines(); start=next(i for i,l in enumerate(lines) if l.startswith(prefix)); out=[]
 for l in lines[start+2:]:
  if not l.startswith('|'):break
  out.append([re.sub(r'[`*]','',x.strip()) for x in l.strip('|').split('|')])
 return out
def literal_table(prefix):
 for n in ast.walk(ast.parse(guide)):
  if isinstance(n,ast.Call) and isinstance(n.func,ast.Name) and n.func.id=='table' and len(n.args)>1:
   try:
    if ast.literal_eval(n.args[0])==prefix:return ast.literal_eval(n.args[1])
   except (ValueError,TypeError):pass
 raise ValueError(prefix)
entries=[]
def add(id,section,title,hint,body,entity='',site='',boss=''):
 entries.append(dict(id=id,section=section,title=title,hint=hint,body=body,entity=entity,site=site,boss=boss))
regions={x[0]:x[1] for x in table('| Region | Creatures and discovery route |')}
habitats={'ANCIENT_TREE_SPIRIT':'Forests','MUTANT_WOLF':'Taigas','MUTANT_ZOMBIE':'Plains and savannas','FOSSIL_TYRANT':'Deserts and badlands','THUNDER_BIRD':'Savannas','TITAN_BOA':'Jungles','BABA_YAGA':'Swamps','MOUNTAIN_TITAN':'Mountain peaks','ICE_WYRM':'Snowy regions','KRAKEN':'Oceans','CAVE_CRAWLER':'Caves','SHADOW_CREEPER_QUEEN':'Deep Dark','MYCELIAL_SOVEREIGN':'Mushroom fields','NETHERBORN':'Crimson and warped forests, Nether wastes and basalt deltas','SOULBOUND_COLOSSUS':'Soul Sand Valley','VOID_EYE':'The End'}
behaviors=dict(re.findall(r'([A-Z_]+)\|([^\r\n]+)',guide,re.M))
source=(root/'src/main/java/darkspawn/black/ecosystem/RegionalKind.java').read_text(encoding='utf-8')
for id,region,temper,hp in re.findall(r'^\s*(\w+)\(BossProfile\.(\w+), Form\.\w+, Move\.\w+, Behavior\.\w+, Temper\.(\w+), (\d+)',source,re.M):
 entity=id.lower();add('entity/'+entity,'creatures',lang['entity.darkspawn.'+entity],habitats[region]+'. Appears after the world dragon victory.',f'{temper.title()}. {hp} health points.\n\n'+behaviors[id],entity=entity)
for prefix,habitat in [('| Creature / entity ID | Behavior and rewards |','Forests'),('| Creature / entity ID | Behavior and player-kill drops |','Taigas')]:
 for row in table(prefix):
  m=re.search(r'/\s*(\w+)',row[0]);assert m,row
  entity=m[1];add('entity/'+entity,'creatures',lang['entity.darkspawn.'+entity],habitat+'. Appears after the world dragon victory.',row[1],entity=entity)
mountids={'Griffin':'griffin','Woolly Rhino':'woolly_rhino','Flying Squirrel':'flying_squirrel','Poptart Cat':'poptart_cat','Riding Dragon':'dragon_mount','Winged Unicorn':'winged_unicorn','Riding Golem':'riding_golem','Sasquatch':'sasquatch','Cerberus':'cerberus','Jungle Leopard':'leopard','Giant Seahorse':'seahorse','Giant Scorpion':'giant_scorpion','Swamp Crocodile':'crocodile','Giant Mole':'giant_mole'}
foods=dict(literal_table(['Mount','Preferred bonding item']))
abilities={row[0]: row[1]+' Cooldown: '+row[2]+'. Hold G (rebindable) while riding to use active abilities.' for row in literal_table(['Mount','Ability','Cooldown'])}
for name,habitat,travel in literal_table(['Mount','Natural habitat','Travel']):
 entity=mountids[name]
 body=f'{travel}.\n\nBonding item: {foods[name]}. Feed three times, at least two seconds apart. Owner-only riding; no saddle.\n\nWASD and mouse steer. Space jumps or rises; X descends in air or water. Shift dismounts. Giant Mole: hold V while moving to dig.\n\nSneak-use empty-handed opens care: Follow/Stay, nine cargo slots, breeding and accepted ownership transfers. Defeated owned mounts recover with a Veterinary Kit. Companion Capsules store unoccupied mounts. Breed healthy same-species adults belonging to one owner while both Stay.'
 if name in abilities: body+='\n\n'+abilities[name]
 if entity=='cerberus':body+='\n\nFire and lava immunity protects the mount and mounted owner; it ends immediately on dismount.'
 add('entity/'+entity,'mounts',name,habitat,body,entity=entity)
bossrows=table('| Boss / entity ID | Solo HP |')
bosses=[('ancient_tree_spirit','800','Moss block in a supported forest clearing; 21 by 21 clear floor and 28 blocks of height.'),('mutant_wolf','900','Bone block in a supported taiga at night.')]
for row in bossrows:bosses.append((row[0].split('/')[-1].strip(),row[1],row[2]))
for id,hp,ritual in bosses:
 add('entity/'+id,'bosses',lang['entity.darkspawn.'+id],habitats[id.upper()]+'. Find its landmark and ritual clues.',f'Solo health: {hp}.\n\nRitual: {ritual}\n\nRequires the world dragon victory, non-Peaceful difficulty and clear space. Failed rituals preserve the summon. Read the landmark ritual book and item tooltip for preparation.\n\nHealth scales at summoning for up to four nearby Survival players. Watch attack warnings; phases change at 70% and 35% health.\n\nA consumed heart is tracked separately from a credited kill. Fifteen different hearts finish the health track; there are sixteen families.',entity=id,boss=id)
for row in table('| Region | Landmark ID | Boss |'):
 site=row[1].replace('darkspawn:','');boss=dict(re.findall(r'Map.entry\("(\w+)", BossProfile\.(\w+)\)',(root/'src/main/java/darkspawn/black/worldgen/BossLandmarkStructure.java').read_text(encoding='utf-8')))[site].lower()
 add('landmark/'+site,'landmarks',site.replace('_',' ').title(),row[0]+'. Explore newly generated terrain.',f'Associated boss: {row[2]}.\n\nSearch for a ritual book and finite supplies. Existing explored chunks are preserved. Visiting records this landmark family; the Journal does not locate undiscovered structures.',site=site,boss=boss)
add('landmark/research_bunker','landmarks','Research Bunker','New Overworld plains, forests, birch forests and taigas.','Read the Quarantine Protocol and use the research archive. Research persists per player; an activated gate can be used by everyone.',site='research_bunker')
add('guide/starting','overview','Your expedition','', 'Discover creatures within 24 blocks with a clear line of sight. Enter native landmark bounds to record visits. Discoveries belong to you and survive death.\n\nCooking knowledge, consumed hearts and Forsaken research use your existing records. Boss defeats use Minecraft credited-kill statistics; historical shared participation cannot be reconstructed.\n\nRefresh after exploring to update this snapshot. The Journal does not load terrain, award progress, consume items or pause the world.')
add('guide/companion_care','overview','Companion care','','Feed species-specific foods to build trust. Only owners ride; no saddle is needed. Sneak-use opens commands and cargo.\n\nBreeding needs healthy adult parents of the same species and owner, both on Stay. Babies cannot be ridden. Defeated owned companions become recoverable using a Veterinary Kit.\n\nOwnership transfer requires a nearby recipient to accept. Cargo stays with the mount. Capsule and recovery travel respect OneBlock item separation.')
for id,title,body in [
 ('travel','Research and travel','Find a research bunker and use its archive. Repair the 5 by 5 obsidian gate border and use a Containment Key after the world dragon victory. Empty-hand use enters. The Extraction Terminal returns you near your entrance. Normal inventory and death rules apply.'),
 ('threats','Threats and contamination','Four infected roles patrol cities. Nests drop one Parasitic Core when destroyed. Combat, explosions and breaking blocks increase shared local pressure; quiet time reduces it. Contamination lasts up to 60 seconds. Honey, milk, extraction or death clears it. Quarantine Armor shortens exposure; the full set prevents new exposure.'),
 ('equipment','Expedition equipment','Smith Quarantine Armor from a Module template, iron armor and Biofiber. Smith the Pulse Blade from a Module, iron sword and Core. Charge Use for 1.5 seconds and release: four-block infected-only pulse, eight-second cooldown.\n\nA Containment Beacon costs 4 Alloy, 2 Modules, 2 Redstone and 1 Data. One Core fuels five minutes within 16 blocks; existing enemies remain.\n\nThe Bio-Scanner costs 1 Alloy, 2 Data, 1 Core and 1 Redstone. Sneak-use cycles nests, resources/caches and local pressure; scans share five seconds of cooldown.'),
 ('resources','Resources and rewards','Alloy: salvage caches. Biofiber: infected and medical caches. Core: destroyed nests. Research Data: annex and roof caches. Modules: occasional medical/research/roof cache rewards, or craft from 4 Biofiber, 2 Alloy, 2 Data and 1 Core. Caches do not restock.')]:
 add('forsaken/'+id,'forsaken',title,'Find an Overworld research bunker and use its archive.',body)
for site,boss in [('druid_shrine','ancient_tree_spirit'),('hunter_camp','mutant_wolf')]:
 add('landmark/'+site,'landmarks',site.replace('_',' ').title(),habitats[boss.upper()]+'. Explore new terrain.','Look for the ritual book and prepared clearing. Supplies are finite; visits record the family without locating other sites.',site=site,boss=boss)
for entity in ['infected_wanderer','infected_crawler','spore_spitter','assimilated_brute']:
 add('entity/'+entity,'forsaken',lang['entity.darkspawn.'+entity],'Ruined cities in Forsaken.','Read its attack warnings. Avoid nest clusters, carry honey or milk, and keep an extraction route clear. Quarantine Armor helps prevent contamination.',entity=entity)
assert len({e['id'] for e in entries})==len(entries)
p=root/'src/main/resources/data/darkspawn/journal/catalog.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(entries,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print('Catalog entries:',len(entries));print({s:sum(e['section']==s for e in entries) for s in sorted({e['section'] for e in entries})})
