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


# Puerto ---------------------------------------------------------------
table('port_industrial',
      pool(3, 6, item('iron_ingot', 6, 1, 5), item('copper_ingot', 6, 2, 8), item('coal', 6, 3, 12),
           item('redstone', 4, 2, 8), damaged('iron_pickaxe', 2), damaged('iron_axe', 2), item('shears', 2),
           item('flint_and_steel', 1), item('lava_bucket', 1), item('coal_block', 2, 1, 2), item('bread', 3, 1, 4),
           item('dried_kelp', 3, 4, 10), item('chain', 3, 2, 6), item('lantern', 2)),
      pool(0, 1, mod('pipe_bomb', 1), empty(4)))
table('port_medical',
      pool(2, 4, mod('first_aid_kit', 2), mod('pain_pills', 4), mod('adrenaline', 2), potion('healing', 2),
           item('milk_bucket', 2), item('honey_bottle', 2, 1, 2), item('golden_apple', 1), potion('regeneration', 1)))
table('port_textile',
      pool(3, 5, item('leather', 5, 2, 6), item('white_wool', 4, 2, 8), item('string', 3, 3, 10),
           enchanted('leather_chestplate', 2, 8), enchanted('leather_leggings', 2, 8), item('leather_boots', 2),
           item('leather_helmet', 2), item('rabbit_hide', 2, 2, 5)))
table('port_electronics',
      pool(3, 5, item('redstone', 6, 4, 12), item('repeater', 3, 1, 3), item('comparator', 2, 1, 2),
           item('daylight_detector', 1), item('observer', 2, 1, 2), item('redstone_torch', 3, 2, 6),
           item('redstone_block', 2), item('copper_ingot', 3, 2, 6), item('glowstone_dust', 2, 2, 6),
           item('compass', 1), item('clock', 1)))
table('port_commercial',
      pool(3, 5, item('gold_ingot', 2, 1, 3), item('emerald', 2, 1, 3), item('book', 2, 1, 3), item('paper', 3, 2, 6),
           item('glass_bottle', 2, 1, 3), item('leather', 3, 2, 4), mod('pain_pills', 2), mod('first_aid_kit', 1),
           item('candle', 2, 1, 3), item('spyglass', 1)))
table('port_tactical',
      pool(1, 1, gun('m700', 3), gun('kar98', 3), gun('ai_awp', 1), gun('sks_tactical', 3), gun('springfield1873', 2)),
      pool(1, 2, att('scope_standard_8x', 2), att('scope_mk5hd', 1), att('scope_vudu', 1), att('scope_acog_ta31', 2),
           att('scope_elcan_4x', 2), att('scope_lpvo_1_6', 2)),
      pool(2, 3, ammo('30_06', 3, 8, 20), ammo('792x57', 3, 8, 20), ammo('338', 1, 5, 12), ammo('762x39', 3, 15, 30),
           ammo('45_70', 2, 8, 16), item('spyglass', 1)))
table('port_tugboat',
      pool(3, 5, item('turtle_helmet', 2), potion('water_breathing', 4), potion('long_water_breathing', 2),
           damaged('trident', 1), item('firework_rocket', 4, 2, 6), item('fishing_rod', 2), item('cod', 3, 2, 6),
           item('lead', 2, 1, 2), item('nautilus_shell', 1), item('compass', 2)))
table('port_military',
      pool(1, 1, gun('m249', 2), gun('rpk', 2), gun('m107', 1), gun('fn_evolys', 2), gun('hk_g3', 2), gun('rpg7', 1)),
      pool(2, 3, ammo('556x45', 3, 20, 40), ammo('762x39', 3, 20, 40), ammo('50bmg', 1, 4, 10), ammo('308', 2, 10, 25),
           ammo('rpg_rocket', 1, 1, 2)),
      pool(1, 2, item('iron_chestplate', 3), item('iron_leggings', 3), item('iron_helmet', 3), item('diamond_chestplate', 1),
           item('diamond_helmet', 1), item('shield', 2)),
      pool(1, 2, item('map', 3, 1, 1, named('evacuation_map')), item('tripwire_hook', 2, 1, 1, named('master_key')),
           item('compass', 2), item('recovery_compass', 1)))

# Domo -----------------------------------------------------------------
table('dome_mechanic',
      pool(3, 6, item('piston', 4, 1, 3), item('iron_ingot', 4, 2, 6), item('lava_bucket', 1), item('iron_helmet', 2),
           enchanted('leather_chestplate', 2, 12), enchanted('leather_leggings', 2, 12), damaged('iron_pickaxe', 1),
           item('shears', 1), item('flint_and_steel', 1), item('chain', 3, 2, 5), item('redstone', 3, 2, 6),
           item('minecart', 1), item('black_wool', 2, 1, 4)))
table('dome_medical',
      pool(3, 5, mod('first_aid_kit', 3), mod('pain_pills', 4), mod('adrenaline', 2), potion('regeneration', 2),
           potion('healing', 2), item('golden_carrot', 3, 2, 6), item('cooked_beef', 2, 2, 5), item('lantern', 2),
           item('torch', 2, 4, 10)),
      pool(0, 1, dyed('leather_chestplate', 1, 16766720, 'hazmat'), dyed('leather_leggings', 1, 16766720, 'hazmat'),
           dyed('leather_helmet', 1, 16766720, 'hazmat'), dyed('leather_boots', 1, 16766720, 'hazmat'), empty(4)))
table('dome_police',
      pool(1, 1, gun('glock_17', 3), gun('p320', 2), gun('m1911', 3), gun('deagle', 1), gun('cz75', 2), gun('hk_mp5a5', 1)),
      pool(2, 3, ammo('9mm', 4, 12, 30), ammo('45acp', 3, 12, 24), ammo('50ae', 1, 7, 14)),
      pool(1, 2, item('shield', 3), item('iron_helmet', 2), item('bread', 2, 1, 3), potion('healing', 1),
           item('leather', 2, 1, 3), mod('pain_pills', 2)))
table('dome_research',
      pool(1, 1, gun('vector45', 3), gun('p90', 3), gun('fn_evolys', 2), gun('aa12', 2), gun('qbz_95', 2)),
      pool(1, 2, ammo('45acp', 3, 20, 40), ammo('57x28', 3, 25, 50), ammo('308', 2, 15, 30), ammo('12g', 2, 8, 16),
           ammo('58x42', 2, 20, 40)),
      pool(2, 3, potion('strength', 2), potion('swiftness', 2), potion('regeneration', 2), potion('fire_resistance', 1),
           potion('night_vision', 2), potion('healing', 2), mod('adrenaline', 2),
           item('paper', 2, 1, 1, named('access_card')), item('diamond_chestplate', 1), item('iron_chestplate', 2)))

# Base avanzada ----------------------------------------------------------
table('fob_guard',
      pool(0, 1, gun('glock_17', 3), gun('m1911', 3), gun('uzi', 2), gun('hk_mp5a5', 1), gun('ump45', 1), empty(2)),
      pool(2, 3, ammo('9mm', 4, 12, 30), ammo('45acp', 3, 12, 24), item('firework_rocket', 3, 2, 5), item('spyglass', 2),
           potion('water', 4), item('bread', 2, 1, 3), item('dried_kelp', 2, 3, 8)))
table('fob_tactical',
      pool(1, 1, gun('ak47', 3), gun('m4a1', 3), gun('hk416d', 2), gun('g36k', 2), gun('qbz_95', 1)),
      pool(2, 3, ammo('762x39', 3, 20, 40), ammo('556x45', 4, 20, 40), ammo('58x42', 1, 20, 40)),
      pool(2, 3, item('chainmail_chestplate', 2), item('iron_chestplate', 2), dyed('leather_chestplate', 2, 12755563),
           mod('first_aid_kit', 2), mod('adrenaline', 1), item('compass', 2), item('map', 2)))
table('fob_assault',
      pool(1, 1, gun('mk14', 3), gun('sks_tactical', 3), gun('scar_h', 2), gun('fn_fal', 2), gun('hk_g3', 1)),
      pool(1, 1, att('scope_acog_ta31', 2), att('scope_elcan_4x', 2), att('scope_hamr', 1), att('scope_lpvo_1_6', 2), empty(2)),
      pool(2, 3, ammo('308', 4, 15, 30), ammo('762x39', 3, 20, 40)),
      pool(1, 2, mod('pipe_bomb', 3), mod('molotov', 1), damaged('iron_chestplate', 2), damaged('iron_helmet', 2)))
table('fob_artillery',
      pool(1, 1, gun('rpg7', 2), gun('m320', 2), gun('m249', 2), gun('m107', 1)),
      pool(1, 2, ammo('rpg_rocket', 3, 1, 3), ammo('40mm', 3, 2, 5), ammo('556x45', 2, 20, 40), ammo('50bmg', 1, 4, 10)),
      pool(2, 4, item('tnt', 4, 1, 4), item('redstone_block', 3, 1, 3), att('scope_vudu', 1), att('scope_mk5hd', 1),
           att('scope_standard_8x', 1), mod('pipe_bomb', 2), item('diamond', 1)))

# Trampas ----------------------------------------------------------------
table('trap_arrows', pool(1, 1, item('arrow', 1, 8, 16)))

print('datos de las zonas de interés generados')
