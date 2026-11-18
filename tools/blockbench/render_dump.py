"""Renderiza un volcado de estructura (aidirector_dump/<variante>.txt del GameTest) como imagen
isométrica y cortes por planta, para revisar los refugios sin abrir el juego.

Uso:  python render_dump.py <dump.txt> <salida.png> [y_de_corte ...]
"""
import sys
from PIL import Image, ImageDraw

COLORS = [
    ('safe_room_door', (200, 40, 32)), ('safe_room_radio', (90, 200, 90)), ('supply_crate', (240, 200, 60)),
    ('crescendo_alarm', (255, 60, 200)), ('ammo_pile', (60, 220, 220)),
    ('glass', (170, 210, 230)), ('iron_bars', (150, 150, 155)), ('iron', (205, 205, 210)),
    ('oxidized', (80, 160, 140)), ('weathered', (110, 150, 120)), ('exposed', (170, 130, 100)), ('copper', (190, 110, 70)),
    ('deepslate', (70, 70, 78)), ('blackstone', (45, 40, 48)), ('basalt', (60, 60, 64)), ('tuff', (110, 112, 100)),
    ('andesite', (140, 140, 138)), ('cobble', (115, 115, 115)), ('stone_brick', (125, 125, 125)), ('stone', (128, 128, 128)),
    ('mud_brick', (140, 105, 80)), ('packed_mud', (150, 110, 80)), ('mud', (60, 55, 50)), ('gravel', (130, 125, 120)),
    ('dirt_path', (150, 125, 70)), ('coarse', (110, 80, 55)), ('rooted', (125, 90, 60)), ('podzol', (90, 65, 35)),
    ('dirt', (120, 85, 58)), ('grass', (90, 150, 60)), ('moss', (90, 125, 45)), ('fern', (70, 140, 50)),
    ('leaves', (70, 120, 40)), ('azalea', (95, 140, 60)), ('vine', (60, 110, 40)), ('berry', (80, 100, 40)),
    ('lichen', (120, 180, 140)), ('green_wool', (85, 110, 40)), ('gray_wool', (100, 100, 100)), ('white_wool', (230, 230, 230)),
    ('wool', (180, 180, 180)), ('carpet', (130, 100, 80)), ('bed', (170, 60, 60)),
    ('dark_oak', (65, 45, 25)), ('spruce', (110, 80, 50)), ('oak', (160, 130, 80)), ('stripped', (150, 120, 80)),
    ('red_terracotta', (150, 60, 45)), ('blue_terracotta', (70, 85, 120)), ('brown_terracotta', (95, 65, 45)),
    ('cyan_terracotta', (85, 95, 95)), ('terracotta', (160, 100, 75)), ('glazed', (220, 140, 60)),
    ('concrete', (160, 165, 170)), ('coal', (25, 25, 25)), ('lantern', (255, 210, 120)), ('torch', (255, 120, 60)),
    ('campfire', (200, 110, 50)), ('chest', (170, 120, 50)), ('barrel', (130, 95, 55)), ('lamp', (255, 220, 140)),
    ('farmland', (90, 60, 35)), ('wheat', (210, 190, 80)), ('carrot', (230, 140, 40)), ('potato', (190, 170, 90)),
    ('water', (60, 100, 220)), ('hay', (210, 180, 60)), ('target', (230, 80, 80)), ('dripstone', (130, 100, 80)),
    ('note_block', (110, 70, 50)), ('trip', (220, 220, 220)), ('banner', (220, 220, 220)), ('lever', (150, 120, 90)),
    ('button', (60, 60, 60)), ('hopper', (70, 70, 75)), ('cauldron', (60, 60, 65)), ('chain', (90, 90, 95)),
    ('fence', (110, 80, 50)), ('wall', (115, 115, 115)), ('slab', (140, 140, 140)), ('stairs', (140, 140, 140)),
    ('bedrock', (30, 30, 30)), ('sand', (220, 205, 150)), ('snow', (240, 240, 250)),
]


def color(name):
    for key, c in COLORS:
        if key in name:
            return c
    return (200, 0, 200)


def load(path):
    with open(path) as f:
        sx, sy, sz = map(int, f.readline().split())
        blocks = {}
        for line in f:
            x, y, z, n = line.split()
            blocks[(int(x), int(y), int(z))] = n
    return (sx, sy, sz), blocks


def iso(size, blocks, scale=6):
    sx, sy, sz = size
    w = (sx + sz) * scale * 2 + 40
    h = (sx + sz) * scale + sy * scale * 2 + 40
    img = Image.new('RGB', (w, h), (30, 32, 36))
    d = ImageDraw.Draw(img)
    ox, oy = sz * scale * 2 + 20, sy * scale * 2 + 20

    def proj(x, y, z):
        return ox + (x - z) * scale * 2, oy + (x + z) * scale - y * scale * 2

    order = sorted(blocks, key=lambda p: (p[0] + p[2], p[1]))
    for (x, y, z) in order:
        c = color(blocks[(x, y, z)])
        top = [proj(x, y + 1, z), proj(x + 1, y + 1, z), proj(x + 1, y + 1, z + 1), proj(x, y + 1, z + 1)]
        left = [proj(x, y + 1, z + 1), proj(x + 1, y + 1, z + 1), proj(x + 1, y, z + 1), proj(x, y, z + 1)]
        right = [proj(x + 1, y + 1, z), proj(x + 1, y + 1, z + 1), proj(x + 1, y, z + 1), proj(x + 1, y, z)]
        d.polygon(top, fill=tuple(min(255, int(v * 1.1)) for v in c))
        d.polygon(left, fill=tuple(int(v * 0.75) for v in c))
        d.polygon(right, fill=tuple(int(v * 0.55) for v in c))
    return img


def slice_img(size, blocks, y, scale=8):
    sx, sy, sz = size
    img = Image.new('RGB', (sx * scale, sz * scale), (20, 20, 22))
    d = ImageDraw.Draw(img)
    for x in range(sx):
        for z in range(sz):
            n = blocks.get((x, y, z))
            if n:
                c = color(n)
                d.rectangle([x * scale, (sz - 1 - z) * scale, x * scale + scale - 1, (sz - 1 - z) * scale + scale - 1], fill=c)
            else:
                below = blocks.get((x, y - 1, z))
                if below:
                    c = tuple(int(v * 0.35) for v in color(below))
                    d.rectangle([x * scale, (sz - 1 - z) * scale, x * scale + scale - 1, (sz - 1 - z) * scale + scale - 1], fill=c)
    d.text((4, 4), f'y={y}', fill=(255, 255, 255))
    return img


def main():
    size, blocks = load(sys.argv[1])
    views = [iso(size, blocks)]
    for arg in sys.argv[3:]:
        if arg.startswith('maxy='):
            cut = int(arg[5:])
            views.append(iso(size, {p: n for p, n in blocks.items() if p[1] <= cut}))
        else:
            views.append(slice_img(size, blocks, int(arg)))
    w = sum(v.width for v in views) + 10 * len(views)
    h = max(v.height for v in views)
    out = Image.new('RGB', (w, h), (30, 32, 36))
    x = 0
    for v in views:
        out.paste(v, (x, 0))
        x += v.width + 10
    out.save(sys.argv[2])
    names = {}
    for n in blocks.values():
        names[n] = names.get(n, 0) + 1
    unknown = sorted(n for n in names if color(n) == (200, 0, 200))
    print(size, len(blocks), 'bloques; sin color:', unknown)


if __name__ == '__main__':
    main()
