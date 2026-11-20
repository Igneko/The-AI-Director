"""Genera los datos de las grandes zonas de interés (puerto, domo, base avanzada):
estructuras, conjunto de colocación, etiquetas de bioma/estructura y tablas de botín.

Uso (desde la raíz del repositorio):  python tools/data/gen_landmark_data.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'data')
NS = 'aidirector'


def write(rel, obj):
    path = os.path.join(ROOT, *rel.split('/'))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


# ---------------------------------------------------------------- estructuras

LANDMARKS = {
    'port': ('beard_box', ['#minecraft:is_beach', '#minecraft:is_ocean', 'minecraft:plains', 'minecraft:sunflower_plains',
                           'minecraft:savanna', 'minecraft:desert', 'minecraft:forest', 'minecraft:birch_forest',
                           'minecraft:taiga', 'minecraft:snowy_plains', 'minecraft:meadow', 'minecraft:swamp']),
    'dome': ('beard_thin', ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:meadow', 'minecraft:forest',
                            'minecraft:birch_forest', 'minecraft:flower_forest', 'minecraft:savanna', 'minecraft:taiga']),
    'fob': ('beard_thin', ['minecraft:desert', 'minecraft:badlands', 'minecraft:wooded_badlands', 'minecraft:eroded_badlands',
                           'minecraft:savanna', 'minecraft:savanna_plateau']),
}

for name, (adapt, biomes) in LANDMARKS.items():
    write(f'{NS}/worldgen/structure/landmark_{name}.json', {
        'type': f'{NS}:landmark',
        'landmark': name,
        'biomes': f'#{NS}:has_structure/landmark_{name}',
        'step': 'surface_structures',
        'spawn_overrides': {},
        'terrain_adaptation': adapt,
    })
    write(f'{NS}/tags/worldgen/biome/has_structure/landmark_{name}.json', {'replace': False, 'values': biomes})

write(f'{NS}/tags/worldgen/structure/landmarks.json', {
    'replace': False, 'values': [f'{NS}:landmark_{n}' for n in LANDMARKS]})

# Una zona por celda de 80 chunks (1280 bloques), nunca a menos de 896 bloques de otra
# y nunca encima de un refugio.
write(f'{NS}/worldgen/structure_set/landmarks.json', {
    'structures': [{'structure': f'{NS}:landmark_{n}', 'weight': 1} for n in LANDMARKS],
    'placement': {
        'type': 'minecraft:random_spread',
        'spacing': 80,
        'separation': 56,
        'spread_type': 'triangular',
        'salt': 51893517,
        'exclusion_zone': {'other_set': f'{NS}:safe_rooms', 'chunk_count': 10},
    },
})

# ---------------------------------------------------------------- botín

GUNS = {
    'm700': 'SEMI', 'ai_awp': 'SEMI', 'kar98': 'SEMI', 'springfield1873': 'SEMI', 'm107': 'SEMI',
    'sks_tactical': 'SEMI', 'mk14': 'SEMI', 'scar_h': 'SEMI', 'fn_fal': 'SEMI', 'hk_g3': 'SEMI',
    'glock_17': 'SEMI', 'm1911': 'SEMI', 'cz75': 'AUTO', 'p320': 'SEMI', 'deagle': 'SEMI',
    'uzi': 'AUTO', 'hk_mp5a5': 'AUTO', 'ump45': 'AUTO', 'vector45': 'AUTO', 'p90': 'AUTO',
    'ak47': 'AUTO', 'm4a1': 'AUTO', 'hk416d': 'AUTO', 'g36k': 'AUTO', 'qbz_95': 'AUTO',
    'm249': 'AUTO', 'rpk': 'AUTO', 'fn_evolys': 'AUTO', 'aa12': 'SEMI', 'rpg7': 'SEMI', 'm320': 'SEMI',
}


def count(a, b):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': a, 'max': b}}


def item(name, w, a=1, b=1, *fns):
    e = {'type': 'minecraft:item', 'name': name if ':' in name else 'minecraft:' + name, 'weight': w}
    f = ([count(a, b)] if (a, b) != (1, 1) else []) + list(fns)
    if f:
        e['functions'] = f
    return e


def mod(name, w, a=1, b=1):
    return item(f'{NS}:{name}', w, a, b)


def nbt(tag):
    return {'function': 'minecraft:set_nbt', 'tag': tag}


def named(key):
    return {'function': 'minecraft:set_name', 'name': {'translate': f'loot.{NS}.{key}', 'italic': False}}


def gun(gid, w):
    return item('tacz:modern_kinetic_gun', w, 1, 1,
                nbt('{GunId:"tacz:%s",GunFireMode:"%s",GunCurrentAmmoCount:0,HasBulletInBarrel:0b}' % (gid, GUNS[gid])))


def ammo(aid, w, a, b):
    return item('tacz:ammo', w, a, b, nbt('{AmmoId:"tacz:%s"}' % aid))


def att(aid, w):
    return item('tacz:attachment', w, 1, 1, nbt('{AttachmentId:"tacz:%s"}' % aid))


def potion(pid, w, name='potion'):
    return item(name, w, 1, 1, {'function': 'minecraft:set_potion', 'id': 'minecraft:' + pid})


def enchanted(name, w, levels):
    return item(name, w, 1, 1, {'function': 'minecraft:enchant_with_levels', 'levels': levels})


def damaged(name, w):
    return item(name, w, 1, 1, {'function': 'minecraft:set_damage', 'damage': {'type': 'minecraft:uniform', 'min': 0.3, 'max': 0.7}})


def dyed(name, w, color, key=None):
    fns = [nbt('{display:{color:%d}}' % color)]
    if key:
        fns.append(named(key))
    return item(name, w, 1, 1, *fns)


def empty(w):
    return {'type': 'minecraft:empty', 'weight': w}


def pool(a, b, *entries):
    return {'rolls': {'type': 'minecraft:uniform', 'min': a, 'max': b}, 'entries': list(entries)}


def table(name, *pools):
    write(f'{NS}/loot_tables/chests/{name}.json', {'type': 'minecraft:chest', 'pools': list(pools)})


# ---------------------------------------------------------------- etiquetas de ítems (compatibilidad con otros mods)
#
# Cada tabla de botín puede sacar un objeto al azar de una etiqueta "aidirector:loot/<nombre>". Las etiquetas
# traen objetos de vanilla (siempre hay algo) y entradas OPCIONALES de otros mods y etiquetas comunes de Forge: si el
# mod no está instalado, la entrada se ignora sin errores. Los creadores de modpacks pueden ampliarlas con un datapack.

def opt(i):
    return {'id': i, 'required': False}


ITEM_TAGS = {
    'industrial': (
        ['minecraft:iron_ingot', 'minecraft:copper_ingot', 'minecraft:coal', 'minecraft:iron_nugget', 'minecraft:chain', 'minecraft:gunpowder'],
        ['#forge:ingots/tin', '#forge:ingots/lead', '#forge:ingots/zinc', '#forge:ingots/bronze', '#forge:ingots/brass',
         '#forge:ingots/steel', '#forge:ingots/nickel', '#forge:plates', '#forge:gears',
         'create:andesite_alloy', 'create:iron_sheet', 'create:copper_sheet', 'create:brass_ingot', 'create:zinc_ingot',
         'immersiveengineering:ingot_steel', 'immersiveengineering:plate_iron', 'thermal:tin_ingot', 'thermal:lead_ingot',
         'mekanism:ingot_steel', 'mekanism:ingot_bronze', 'mekanism:ingot_tin']),
    'tech': (
        ['minecraft:redstone', 'minecraft:repeater', 'minecraft:comparator', 'minecraft:observer', 'minecraft:daylight_detector',
         'minecraft:glowstone_dust'],
        ['create:electron_tube', 'create:precision_mechanism', 'mekanism:basic_control_circuit', 'mekanism:alloy_infused',
         'immersiveengineering:component_electronic', 'immersiveengineering:component_iron', 'thermal:rf_coil',
         'appliedenergistics2:certus_quartz_crystal', 'ae2:certus_quartz_crystal', 'ae2:logic_processor']),
    'medical': (
        ['minecraft:honey_bottle', 'minecraft:glistering_melon_slice', 'minecraft:golden_carrot', 'minecraft:paper'],
        ['firstaid:bandage', 'firstaid:plaster', 'firstaid:morphine', 'firstaid:tonic', 'toughasnails:purified_water_bottle',
         'farmersdelight:hot_cocoa', 'farmersdelight:honey_cookie']),
    'food': (
        ['minecraft:cooked_beef', 'minecraft:cooked_chicken', 'minecraft:cooked_porkchop', 'minecraft:baked_potato',
         'minecraft:bread', 'minecraft:cookie', 'minecraft:dried_kelp', 'minecraft:apple'],
        ['#forge:cooked_beef', '#forge:cooked_chicken', '#forge:cooked_pork', '#forge:cooked_mutton', '#forge:cooked_fishes',
         '#forge:bread', 'farmersdelight:bacon_and_eggs', 'farmersdelight:beef_stew', 'farmersdelight:chicken_soup',
         'farmersdelight:vegetable_soup', 'farmersdelight:fried_rice', 'farmersdelight:pasta_with_meatballs']),
    'tools': (
        ['minecraft:shears', 'minecraft:flint_and_steel', 'minecraft:fishing_rod', 'minecraft:spyglass', 'minecraft:compass',
         'minecraft:lead', 'minecraft:bucket'],
        ['#forge:tools/wrenches', '#forge:tools/knives', '#forge:tools/hammers', 'create:wrench', 'farmersdelight:iron_knife']),
    'gear': (
        ['minecraft:leather_helmet', 'minecraft:leather_chestplate', 'minecraft:leather_leggings', 'minecraft:leather_boots',
         'minecraft:chainmail_helmet', 'minecraft:chainmail_boots', 'minecraft:shield'],
        ['create:copper_diving_helmet', 'create:copper_backtank', 'sophisticatedbackpacks:backpack']),
    'valuables': (
        ['minecraft:gold_ingot', 'minecraft:emerald', 'minecraft:lapis_lazuli', 'minecraft:amethyst_shard'],
        ['#forge:ingots/silver', '#forge:ingots/platinum', '#forge:gems/ruby', '#forge:gems/sapphire', '#forge:gems/amethyst',
         '#forge:gems/topaz']),
}

for name, (vanilla, modded) in ITEM_TAGS.items():
    write(f'{NS}/tags/items/loot/{name}.json', {'replace': False, 'values': vanilla + [opt(i) for i in modded]})


def mtag(name, w, a=1, b=1):
    """Un objeto al azar de la etiqueta aidirector:loot/<name> (vanilla + otros mods si están instalados)."""
    e = {'type': 'minecraft:tag', 'name': f'{NS}:loot/{name}', 'expand': True, 'weight': w}
    if (a, b) != (1, 1):
        e['functions'] = [count(a, b)]
    return e


# Puerto ---------------------------------------------------------------
# Calidad: industrial/textil/electrónica/comercial/médico = común; tactical/tugboat = poco común;
# military y stash = raro (la bodega sellada, el puente, los escondites).
table('port_industrial',
      pool(2, 4, item('iron_ingot', 5, 1, 4), item('copper_ingot', 5, 2, 6), item('coal', 5, 3, 10), item('redstone', 3, 2, 6),
           mtag('industrial', 5, 1, 3), mtag('tools', 3), item('chain', 2, 2, 5), item('coal_block', 1), item('lantern', 2),
           damaged('iron_pickaxe', 1), damaged('iron_axe', 1)),
      pool(0, 1, mod('pipe_bomb', 1), item('lava_bucket', 1), empty(7)))
table('port_medical',
      pool(1, 3, mod('first_aid_kit', 2), mod('pain_pills', 4), mod('adrenaline', 1), potion('healing', 2), item('milk_bucket', 2),
           mtag('medical', 4, 1, 2), item('golden_apple', 1)))
table('port_textile',
      pool(2, 4, item('leather', 5, 2, 6), item('white_wool', 4, 2, 8), item('string', 3, 3, 10), item('rabbit_hide', 2, 2, 5),
           mtag('gear', 4), enchanted('leather_chestplate', 1, 8), enchanted('leather_leggings', 1, 8)))
table('port_electronics',
      pool(2, 4, item('redstone', 5, 4, 12), item('copper_ingot', 3, 2, 6), item('glowstone_dust', 2, 2, 6), mtag('tech', 7, 1, 3),
           item('redstone_block', 1), item('compass', 1), item('clock', 1)))
table('port_commercial',
      pool(2, 4, mtag('valuables', 4, 1, 3), item('book', 2, 1, 3), item('paper', 3, 2, 6), item('glass_bottle', 2, 1, 3),
           mtag('food', 4, 1, 4), mod('pain_pills', 2), item('candle', 2, 1, 3), item('spyglass', 1)))
table('port_tactical',
      pool(0, 1, gun('m700', 3), gun('kar98', 3), gun('sks_tactical', 3), gun('springfield1873', 2), gun('ai_awp', 1), empty(5)),
      pool(0, 1, att('scope_acog_ta31', 2), att('scope_elcan_4x', 2), att('scope_lpvo_1_6', 2), att('scope_standard_8x', 1),
           att('scope_mk5hd', 1), att('scope_vudu', 1), empty(6)),
      pool(2, 3, ammo('30_06', 3, 6, 16), ammo('792x57', 3, 6, 16), ammo('762x39', 3, 12, 24), ammo('45_70', 2, 6, 12),
           ammo('338', 1, 4, 8), item('spyglass', 1), mtag('food', 2)))
table('port_tugboat',
      pool(2, 4, item('turtle_helmet', 1), potion('water_breathing', 3), item('firework_rocket', 4, 2, 6), item('fishing_rod', 2),
           item('cod', 3, 2, 6), item('lead', 2, 1, 2), item('compass', 2), mtag('tools', 3), mtag('food', 3, 1, 3),
           damaged('trident', 1)))
table('port_military',
      pool(0, 1, gun('m249', 2), gun('rpk', 2), gun('fn_evolys', 2), gun('hk_g3', 2), gun('m107', 1), gun('rpg7', 1), empty(6)),
      pool(1, 2, ammo('556x45', 3, 15, 30), ammo('762x39', 3, 15, 30), ammo('308', 2, 10, 20), ammo('50bmg', 1, 3, 8),
           ammo('rpg_rocket', 1, 1, 2)),
      pool(0, 1, item('iron_chestplate', 3), item('iron_leggings', 3), item('iron_helmet', 3), item('diamond_chestplate', 1),
           item('shield', 2), empty(5)),
      pool(0, 2, item('map', 3, 1, 1, named('evacuation_map')), item('tripwire_hook', 2, 1, 1, named('master_key')),
           item('compass', 2), item('recovery_compass', 1), mtag('valuables', 2, 1, 2), mtag('tech', 2), empty(3)))
table('port_stash',
      pool(1, 1, gun('m1911', 2), gun('glock_17', 2), gun('uzi', 2), gun('hk_mp5a5', 2), gun('ump45', 1), gun('sks_tactical', 2),
           gun('mk14', 1), gun('fn_fal', 1), gun('ak47', 1), empty(5)),
      pool(2, 3, ammo('9mm', 3, 12, 24), ammo('45acp', 3, 12, 24), ammo('762x39', 2, 12, 24), ammo('308', 2, 8, 16),
           mtag('valuables', 3, 1, 3), mtag('industrial', 3, 2, 4), mtag('tech', 3, 1, 3), mod('first_aid_kit', 2),
           mod('adrenaline', 1), mtag('food', 2, 2, 4)),
      pool(0, 1, enchanted('iron_chestplate', 1, 12), enchanted('iron_helmet', 1, 12), item('diamond', 1), potion('strength', 1), empty(8)))

# Domo -----------------------------------------------------------------
table('dome_mechanic',
      pool(2, 4, item('piston', 3, 1, 3), item('iron_ingot', 4, 2, 5), mtag('industrial', 5, 1, 3), mtag('tools', 3),
           item('chain', 3, 2, 5), item('redstone', 3, 2, 6), item('black_wool', 2, 1, 4), item('iron_helmet', 1),
           enchanted('leather_chestplate', 1, 12), item('minecart', 1)),
      pool(0, 1, item('lava_bucket', 1), item('flint_and_steel', 1), empty(5)))
table('dome_medical',
      pool(2, 3, mod('first_aid_kit', 3), mod('pain_pills', 4), mod('adrenaline', 1), potion('regeneration', 1), potion('healing', 2),
           mtag('medical', 4, 1, 2), mtag('food', 3, 1, 3), item('torch', 2, 4, 10)),
      pool(0, 1, dyed('leather_chestplate', 1, 16766720, 'hazmat'), dyed('leather_leggings', 1, 16766720, 'hazmat'),
           dyed('leather_helmet', 1, 16766720, 'hazmat'), dyed('leather_boots', 1, 16766720, 'hazmat'), empty(5)))
table('dome_police',
      pool(0, 1, gun('glock_17', 3), gun('p320', 2), gun('m1911', 3), gun('deagle', 1), gun('cz75', 2), gun('hk_mp5a5', 1), empty(5)),
      pool(1, 3, ammo('9mm', 4, 10, 24), ammo('45acp', 3, 10, 20), ammo('50ae', 1, 5, 10), item('shield', 2),
           item('iron_helmet', 1), mtag('food', 2, 1, 2), mod('pain_pills', 2), potion('healing', 1)))
table('dome_research',
      pool(0, 1, gun('vector45', 3), gun('p90', 3), gun('fn_evolys', 2), gun('aa12', 1), gun('qbz_95', 2), empty(5)),
      pool(1, 2, ammo('45acp', 3, 15, 30), ammo('57x28', 3, 20, 40), ammo('308', 2, 10, 20), ammo('12g', 2, 6, 12),
           ammo('58x42', 2, 15, 30)),
      pool(1, 3, potion('strength', 1), potion('swiftness', 2), potion('regeneration', 2), potion('fire_resistance', 1),
           potion('night_vision', 2), mod('adrenaline', 2), mtag('tech', 4, 1, 3), mtag('valuables', 2, 1, 2),
           item('paper', 2, 1, 1, named('access_card')), item('iron_chestplate', 1)))
table('dome_stash',
      pool(1, 1, gun('vector45', 2), gun('p90', 2), gun('hk_mp5a5', 2), gun('qbz_95', 1), gun('g36k', 1), gun('hk416d', 1),
           gun('m4a1', 1), empty(5)),
      pool(2, 3, ammo('45acp', 3, 15, 30), ammo('57x28', 3, 20, 40), ammo('556x45', 3, 15, 30), mod('first_aid_kit', 2),
           mod('adrenaline', 2), mtag('tech', 3, 1, 3), mtag('industrial', 2, 2, 4), mtag('valuables', 2, 1, 2),
           potion('night_vision', 1), item('golden_apple', 1)),
      pool(0, 1, enchanted('iron_chestplate', 1, 14), enchanted('iron_leggings', 1, 14), item('diamond', 1), mod('molotov', 1), empty(7)))

# Base avanzada ----------------------------------------------------------
table('fob_guard',
      pool(0, 1, gun('glock_17', 3), gun('m1911', 3), gun('uzi', 2), gun('hk_mp5a5', 1), gun('ump45', 1), empty(6)),
      pool(1, 3, ammo('9mm', 4, 10, 24), ammo('45acp', 3, 10, 20), item('firework_rocket', 3, 2, 5), item('spyglass', 2),
           potion('water', 3), mtag('food', 3, 1, 3), mtag('tools', 2)))
table('fob_tactical',
      pool(0, 1, gun('ak47', 3), gun('m4a1', 3), gun('hk416d', 2), gun('g36k', 2), gun('qbz_95', 1), empty(5)),
      pool(1, 3, ammo('762x39', 3, 15, 30), ammo('556x45', 4, 15, 30), ammo('58x42', 1, 15, 30)),
      pool(1, 3, item('chainmail_chestplate', 2), item('iron_chestplate', 1), dyed('leather_chestplate', 2, 12755563),
           mod('first_aid_kit', 2), mod('adrenaline', 1), item('compass', 2), item('map', 2), mtag('gear', 3), mtag('medical', 2)))
table('fob_assault',
      pool(0, 1, gun('mk14', 3), gun('sks_tactical', 3), gun('scar_h', 2), gun('fn_fal', 2), gun('hk_g3', 1), empty(5)),
      pool(0, 1, att('scope_acog_ta31', 2), att('scope_elcan_4x', 2), att('scope_hamr', 1), att('scope_lpvo_1_6', 2), empty(7)),
      pool(1, 2, ammo('308', 4, 10, 20), ammo('762x39', 3, 15, 30)),
      pool(0, 2, mod('pipe_bomb', 3), mod('molotov', 1), damaged('iron_chestplate', 2), damaged('iron_helmet', 2), mtag('tools', 2), empty(3)))
table('fob_artillery',
      pool(0, 1, gun('rpg7', 2), gun('m320', 2), gun('m249', 2), gun('m107', 1), empty(5)),
      pool(1, 2, ammo('rpg_rocket', 2, 1, 2), ammo('40mm', 3, 2, 4), ammo('556x45', 2, 15, 30), ammo('50bmg', 1, 3, 8)),
      pool(1, 3, item('tnt', 4, 1, 3), item('redstone_block', 2, 1, 2), mod('pipe_bomb', 2), mtag('industrial', 3, 2, 4),
           mtag('tech', 3, 1, 2), att('scope_vudu', 1), att('scope_mk5hd', 1), item('diamond', 1)))
table('fob_stash',
      pool(1, 1, gun('ak47', 2), gun('m4a1', 2), gun('hk416d', 1), gun('mk14', 1), gun('scar_h', 1), gun('fn_fal', 1),
           gun('sks_tactical', 2), gun('m1911', 2), empty(5)),
      pool(2, 3, ammo('762x39', 3, 15, 30), ammo('556x45', 3, 15, 30), ammo('308', 2, 10, 20), ammo('45acp', 2, 12, 24),
           mod('first_aid_kit', 2), mod('adrenaline', 1), mtag('valuables', 3, 1, 3), mtag('industrial', 3, 2, 4),
           mtag('tools', 2), mtag('food', 2, 2, 4)),
      pool(0, 1, enchanted('iron_chestplate', 1, 12), item('chainmail_chestplate', 1), item('diamond', 1), mod('pipe_bomb', 1), empty(7)))

# Trampas ----------------------------------------------------------------
table('trap_arrows', pool(1, 1, item('arrow', 1, 8, 16)))

print('datos de las zonas de interés generados')
