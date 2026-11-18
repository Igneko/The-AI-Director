"""Genera texturas, modelos y estados de bloque de: puerta de refugio, radio, cajas de suministros,
alarma Crescendo, cóctel molotov y desfibrilador (sprites 32x32 y modelos 3D en mano).

Uso:  python gen_saferoom_assets.py <src/main/resources> [carpeta_de_vista_previa]
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

RES = Path(sys.argv[1])
PREVIEW = Path(sys.argv[2]) if len(sys.argv) > 2 else None
A = RES / 'assets' / 'aidirector'
TEX_ITEM = A / 'textures' / 'item'
TEX_BLOCK = A / 'textures' / 'block'
MODELS_BLOCK = A / 'models' / 'block'
MODELS_ITEM = A / 'models' / 'item'
STATES = A / 'blockstates'
for d in (TEX_ITEM, TEX_BLOCK, MODELS_BLOCK, MODELS_ITEM, STATES):
    d.mkdir(parents=True, exist_ok=True)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def dump(path, data):
    path.write_text(json.dumps(data, indent=2), encoding='utf-8')


class Px:
    """Lienzo de píxeles con utilidades de pixel art."""

    def __init__(self, w, h, seed=1):
        self.w, self.h = w, h
        self.px = [[None] * w for _ in range(h)]
        self.rng = random.Random(seed)

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h and c is not None:
            self.px[y][x] = c

    def get(self, x, y):
        return self.px[y][x] if 0 <= x < self.w and 0 <= y < self.h else None

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c(x, y) if callable(c) else c)

    def disc(self, cx, cy, r, c):
        for y in range(self.h):
            for x in range(self.w):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                if dx * dx + dy * dy <= r * r:
                    self.set(x, y, c(dx / r, dy / r) if callable(c) else c)

    def clear(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = None

    def outline(self, col):
        add = []
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x] is not None:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    v = self.get(x + dx, y + dy)
                    if v is not None and v != col:
                        add.append((x, y))
                        break
        for x, y in add:
            self.px[y][x] = col

    def noise(self, amount=0.08):
        for y in range(self.h):
            for x in range(self.w):
                c = self.px[y][x]
                if c is not None and c[3] > 0:
                    self.px[y][x] = shade(c, 1 + self.rng.uniform(-amount, amount))

    def image(self):
        img = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x] is not None:
                    img.putpixel((x, y), self.px[y][x])
        return img


# ====================================================================== puerta de refugio
RED, RED_L, RED_D, RED_DD = hexc('#931C17'), hexc('#B7322A'), hexc('#661210'), hexc('#420A08')
STEEL, STEEL_L, STEEL_D = hexc('#80868C'), hexc('#AEB4B9'), hexc('#4D5257')
WHITE = hexc('#ECECE4')


def painted_steel(p, x0, y0, x1, y1, seed):
    rng = random.Random(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c = RED
            if rng.random() < 0.08:
                c = RED_D
            elif rng.random() < 0.05:
                c = hexc('#7A3A2A')  # óxido asomando
            p.set(x, y, c)


def door_bottom():
    p = Px(16, 16, 3)
    painted_steel(p, 0, 0, 15, 15, 3)
    p.rect(0, 0, 0, 15, RED_DD)
    p.rect(15, 0, 15, 15, RED_DD)
    p.rect(1, 0, 1, 15, RED_L)
    # Placa de patada
    p.rect(1, 11, 14, 14, STEEL)
    p.rect(1, 11, 14, 11, STEEL_L)
    p.rect(1, 14, 14, 14, STEEL_D)
    for x in range(2, 14, 3):
        p.set(x, 12, STEEL_D)
        p.set(x + 1, 13, STEEL_L)
    p.rect(0, 15, 15, 15, RED_DD)
    # Barra antipánico y remaches
    p.rect(3, 2, 12, 3, STEEL)
    p.rect(3, 2, 12, 2, STEEL_L)
    p.set(2, 2, STEEL_D)
    p.set(13, 3, STEEL_D)
    for x, y in ((2, 6), (13, 6), (2, 9), (13, 9)):
        p.set(x, y, STEEL_L)
        p.set(x + 1, y + 1, RED_DD)
    # Franja de peligro sobre la placa
    for x in range(1, 15):
        p.set(x, 10, hexc('#E5B92A') if (x // 2) % 2 == 0 else hexc('#1E1E1E'))
    return p.image()


def door_top():
    p = Px(16, 16, 4)
    painted_steel(p, 0, 0, 15, 15, 4)
    p.rect(0, 0, 15, 0, RED_DD)
    p.rect(0, 0, 0, 15, RED_DD)
    p.rect(15, 0, 15, 15, RED_DD)
    p.rect(1, 1, 1, 15, RED_L)
    # Ojo de buey: anillo de acero y cristal transparente con rejilla
    cx, cy, r = 8.0, 6.5, 4.6
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r - 1.2:
                if x in (7, 8) or y in (6, 7):
                    p.set(x, y, STEEL_D)
                else:
                    p.px[y][x] = (0, 0, 0, 0)
            elif d <= r:
                p.set(x, y, STEEL_L if y < cy else STEEL)
    # Símbolo de refugio: casa blanca de estarcido
    house = [(8, 10), (7, 11), (9, 11), (6, 12), (10, 12), (5, 13), (11, 13)]
    for x, y in house:
        p.set(x, y, WHITE)
    p.rect(6, 13, 6, 15, WHITE)
    p.rect(10, 13, 10, 15, WHITE)
    p.rect(7, 15, 9, 15, WHITE)
    p.set(8, 14, hexc('#35C45A'))
    return p.image()


def door_item():
    p = Px(32, 32, 5)
    painted_steel(p, 9, 1, 22, 30, 5)
    p.rect(9, 1, 22, 1, RED_L)
    p.rect(22, 1, 22, 30, RED_D)
    cx, cy, r = 15.5, 8.5, 4.6
    for y in range(32):
        for x in range(32):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r - 1.3:
                p.set(x, y, STEEL_D if x in (15, 16) or y in (8, 9) else hexc('#9FD3E8'))
            elif d <= r and 9 <= x <= 22:
                p.set(x, y, STEEL_L)
    for x, y in [(15, 15), (16, 15), (14, 16), (17, 16), (13, 17), (18, 17), (12, 18), (19, 18)]:
        p.set(x, y, WHITE)
    p.rect(13, 18, 13, 22, WHITE)
    p.rect(18, 18, 18, 22, WHITE)
    p.rect(13, 22, 18, 22, WHITE)
    p.rect(15, 20, 16, 21, hexc('#35C45A'))
    p.rect(20, 15, 20, 18, STEEL_L)
    p.rect(9, 25, 22, 29, STEEL)
    p.rect(9, 25, 22, 25, STEEL_L)
    for x in range(9, 23):
        p.set(x, 24, hexc('#E5B92A') if (x // 2) % 2 == 0 else hexc('#1E1E1E'))
    p.outline(hexc('#1A1210'))
    return p.image()


# ====================================================================== cajas de suministros
WOOD, WOOD_L, WOOD_D = hexc('#8B6A42'), hexc('#A8865A'), hexc('#5A4228')

KINDS = {
    'medical': dict(paint=hexc('#E4E0D4'), stripe=hexc('#2E8B57'), dark=hexc('#B9B3A3')),
    'armory': dict(paint=hexc('#5A6B34'), stripe=hexc('#D9C24A'), dark=hexc('#3E4A22')),
    'throwables': dict(paint=hexc('#33302C'), stripe=hexc('#E58A1F'), dark=hexc('#1E1C1A')),
}


def crate_base(kind, seed):
    k = KINDS[kind]
    p = Px(16, 16, seed)
    for y in range(16):
        for x in range(16):
            plank = y // 4
            c = k['paint'] if (y % 4) else k['dark']
            if (x + plank * 5) % 11 == 0 and y % 4:
                c = shade(c, 0.9)
            p.set(x, y, c)
    p.noise(0.05)
    # Marco de madera con refuerzos en esquina
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            p.set(x, y, WOOD_D)
        for (x, y) in ((i, 1), (1, i)):
            p.set(x, y, WOOD)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        p.set(x, y, STEEL_L)
    return p


def crate_front(kind):
    p = crate_base(kind, hash(kind) & 255)
    k = KINDS[kind]
    if kind == 'medical':
        p.rect(4, 4, 11, 11, k['stripe'])
        p.rect(7, 5, 8, 10, WHITE)
        p.rect(5, 7, 10, 8, WHITE)
    elif kind == 'armory':
        for i, x in enumerate((4, 7, 10)):
            p.rect(x, 7, x + 1, 11, k['stripe'])
            p.rect(x, 5, x + 1, 6, hexc('#C98A2E'))
            p.set(x, 4, hexc('#C98A2E') if i != 1 else hexc('#B07020'))
        p.rect(3, 12, 12, 12, k['stripe'])
    else:
        for y in range(3, 7):
            for x in range(2, 14):
                p.set(x, y, k['stripe'] if ((x + y) // 2) % 2 == 0 else hexc('#151515'))
        p.disc(8, 10.5, 3, hexc('#5E6B3A'))
        p.rect(7, 7, 8, 8, STEEL)
        p.set(9, 7, hexc('#E5B92A'))
    return p.image()


def crate_side(kind):
    p = crate_base(kind, (hash(kind) >> 3) & 255)
    k = KINDS[kind]
    p.rect(2, 7, 13, 8, k['stripe'])
    return p.image()


def crate_top(kind):
    k = KINDS[kind]
    p = Px(16, 16, 9)
    for y in range(16):
        for x in range(16):
            p.set(x, y, k['paint'] if (x % 5) else k['dark'])
    p.noise(0.05)
    p.rect(0, 0, 15, 0, WOOD_D)
    p.rect(0, 15, 15, 15, WOOD_D)
    p.rect(0, 0, 0, 15, WOOD_D)
    p.rect(15, 0, 15, 15, WOOD_D)
    # Asas de cuerda
    p.rect(5, 2, 10, 2, hexc('#C2A36B'))
    p.rect(5, 13, 10, 13, hexc('#C2A36B'))
    return p.image()


def crate_lock():
    p = Px(16, 16, 11)
    gold, gold_d, gold_l = hexc('#C9A227'), hexc('#8A6C14'), hexc('#F2D36B')
    p.rect(0, 0, 15, 15, STEEL)  # cadena (zona 0-15 x 0-3)
    for x in range(16):
        p.set(x, 0, STEEL_L if x % 3 else STEEL_D)
        p.set(x, 1, STEEL if x % 3 else STEEL_D)
        p.set(x, 2, STEEL_D)
    p.rect(0, 4, 15, 15, gold)
    p.rect(0, 4, 15, 4, gold_l)
    p.rect(0, 15, 15, 15, gold_d)
    p.rect(7, 8, 8, 11, hexc('#2A2410'))
    return p.image()


# ====================================================================== radio y alarma
OLIVE, OLIVE_L, OLIVE_D = hexc('#4B5A2A'), hexc('#66783C'), hexc('#2F3A1A')


def radio_atlas():
    """Atlas 32x32 (1 píxel por unidad de modelo; UV = píxel / 2)."""
    p = Px(32, 32, 13)
    # frente 12x8 en (0,0)
    p.rect(0, 0, 11, 7, OLIVE)
    p.rect(0, 0, 11, 0, OLIVE_L)
    p.rect(0, 7, 11, 7, OLIVE_D)
    for y in range(2, 7):
        for x in range(1, 6):
            p.set(x, y, hexc('#1E2412') if (x + y) % 2 == 0 else hexc('#39441F'))
    p.rect(7, 2, 10, 3, hexc('#E8B04A'))
    p.set(8, 2, hexc('#C0392B'))
    p.rect(7, 5, 10, 5, hexc('#1E2412'))
    p.set(10, 6, hexc('#4CFF6A'))
    # trasera 12x8 en (12,0)
    p.rect(12, 0, 23, 7, OLIVE_D)
    for x in range(13, 23, 2):
        p.rect(x, 2, x, 5, hexc('#232B13'))
    # laterales 6x8 en (0,8) y (6,8)
    p.rect(0, 8, 11, 15, OLIVE)
    p.rect(0, 8, 11, 8, OLIVE_L)
    p.rect(2, 10, 3, 13, hexc('#2A2A2A'))
    p.rect(8, 10, 9, 13, hexc('#2A2A2A'))
    # tapa 12x6 en (12,8) y base en (12,14)
    p.rect(12, 8, 23, 13, OLIVE_L)
    p.rect(12, 8, 23, 8, hexc('#7E9450'))
    p.rect(12, 14, 23, 19, OLIVE_D)
    # mandos (2x2) en (24,0), asa en (24,4), antena en (28,0)
    p.rect(24, 0, 27, 3, hexc('#1C1C1C'))
    p.set(24, 0, hexc('#5A5A5A'))
    p.rect(24, 4, 31, 6, hexc('#2B2B2B'))
    p.rect(28, 0, 31, 3, STEEL_L)
    p.noise(0.04)
    return p.image()


def alarm_side():
    p = Px(16, 16, 17)
    p.rect(0, 0, 15, 15, hexc('#5C6166'))
    p.noise(0.05)
    p.rect(0, 0, 15, 0, STEEL_L)
    for y in range(9, 12):
        for x in range(16):
            p.set(x, y, hexc('#E5B92A') if ((x + y) // 2) % 2 == 0 else hexc('#1E1E1E'))
    for x in range(3, 13, 2):
        p.rect(x, 3, x, 7, hexc('#2C2F33'))
    return p.image()


def alarm_front():
    p = Px(16, 16, 18)
    p.rect(0, 0, 15, 15, hexc('#5C6166'))
    p.noise(0.05)
    p.rect(0, 0, 15, 0, STEEL_L)
    p.disc(8, 5, 3.6, hexc('#2C2F33'))
    p.disc(8, 5, 2.2, hexc('#C0392B'))
    p.set(7, 4, hexc('#FF8A7A'))
    for y in range(9, 12):
        for x in range(16):
            p.set(x, y, hexc('#E5B92A') if ((x + y) // 2) % 2 == 0 else hexc('#1E1E1E'))
    p.rect(5, 13, 10, 14, hexc('#2C2F33'))
    return p.image()


def alarm_top():
    p = Px(16, 16, 19)
    p.rect(0, 0, 15, 15, hexc('#6E737A'))
    p.noise(0.05)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        p.set(x, y, STEEL_L)
    return p.image()


def alarm_light(on):
    p = Px(16, 16, 20 + on)
    base = hexc('#FF4A2A') if on else hexc('#8E1E14')
    hi = hexc('#FFD27A') if on else hexc('#C0392B')
    for y in range(16):
        for x in range(16):
            t = abs(x - 7.5) / 8
            p.set(x, y, mix(base, shade(base, 0.6), t))
    p.rect(3, 2, 4, 6, hi)
    p.rect(0, 15, 15, 15, hexc('#3A3A3A'))
    return p.image()


# ====================================================================== molotov y desfibrilador (sprites 32x32)
OUTLINE = hexc('#1A1410')


def molotov_sprite():
    p = Px(32, 32, 31)
    glass = hexc('#7E9A6A')
    liquid, liquid_l, liquid_d = hexc('#B8742A'), hexc('#E0A050'), hexc('#7A4A18')
    for y in range(14, 30):
        for x in range(10, 22):
            if (x in (10, 21) and y in (14, 29)) or (y == 14 and x in (11, 20)):
                continue
            c = liquid if y >= 17 else mix(glass, hexc('#D8E8C8'), 0.3)
            if x == 11 and y < 27:
                c = liquid_l if y >= 17 else hexc('#E8F2DC')
            if x >= 20:
                c = shade(c, 0.72)
            if y == 29:
                c = liquid_d
            p.set(x, y, c)
    # etiqueta
    p.rect(10, 21, 21, 25, hexc('#E3D3A6'))
    p.rect(10, 23, 21, 23, hexc('#B33A2E'))
    p.rect(20, 21, 21, 25, hexc('#B8A880'))
    # cuello
    p.rect(14, 9, 17, 13, mix(glass, hexc('#D8E8C8'), 0.2))
    p.rect(14, 9, 14, 13, hexc('#E8F2DC'))
    p.rect(17, 9, 17, 13, shade(glass, 0.7))
    # trapo
    rag, rag_d = hexc('#E8E0CC'), hexc('#B9A98A')
    p.rect(13, 4, 18, 9, rag)
    p.rect(17, 4, 18, 9, rag_d)
    for x, y in ((14, 5), (16, 7), (15, 8)):
        p.set(x, y, hexc('#B33A2E'))
    for i, (x, y) in enumerate(((19, 8), (20, 9), (20, 10), (21, 11), (21, 12))):
        p.set(x, y, rag if i % 2 == 0 else rag_d)
    p.outline(OUTLINE)
    # llama
    flame = [(15, 3, '#FFB300'), (16, 3, '#FFB300'), (14, 2, '#FF6A00'), (15, 2, '#FFE14D'), (16, 2, '#FFB300'),
             (15, 1, '#FF6A00'), (16, 1, '#FFE14D'), (16, 0, '#FF6A00'), (17, 2, '#FF6A00'), (17, 1, '#FFB300')]
    for x, y, c in flame:
        p.set(x, y, hexc(c))
    return p.image()


def defib_sprite():
    p = Px(32, 32, 32)
    case, case_l, case_d = hexc('#F2B705'), hexc('#FFD54A'), hexc('#B98A00')
    for y in range(15, 29):
        for x in range(5, 27):
            if (x in (5, 26)) and (y in (15, 28)):
                continue
            c = case
            if y == 15 or x == 5:
                c = case_l
            if y == 28 or x == 26:
                c = case_d
            p.set(x, y, c)
    p.rect(8, 18, 23, 26, hexc('#2E2E2E'))
    p.rect(8, 18, 23, 18, hexc('#4A4A4A'))
    bolt = [(16, 19), (15, 20), (15, 21), (14, 22), (15, 22), (16, 22), (17, 22), (16, 23), (16, 24), (15, 25)]
    for x, y in bolt:
        p.set(x, y, hexc('#4CFF6A'))
    p.set(21, 20, hexc('#FF3B2F'))
    # asa
    p.rect(12, 11, 19, 12, hexc('#3A3A3A'))
    p.rect(12, 11, 12, 14, hexc('#3A3A3A'))
    p.rect(19, 11, 19, 14, hexc('#3A3A3A'))
    # palas a ambos lados con cables
    for side in (-1, 1):
        cx = 16 + side * 11
        p.disc(cx, 8.5, 3.4, lambda dx, dy: hexc('#C8CDD2') if dy < -0.2 else hexc('#7D848B'))
        p.rect(cx - 1, 11, cx + 1, 13, hexc('#E5701F'))
        for i in range(6):
            p.set(cx - side * (i // 2), 14 + i, hexc('#1E1E1E'))
    p.outline(OUTLINE)
    return p.image()


# ====================================================================== modelos 3D en mano (atlas automático)
class BoxModel:
    """Modelo de elementos con un atlas generado: cada cara recibe su propio rectángulo."""

    def __init__(self, name, size=64):
        self.name, self.size = name, size
        self.img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.x = self.y = self.row = 0
        self.elements = []

    def alloc(self, w, h):
        w, h = max(1, w), max(1, h)
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > self.size:
            raise ValueError(f'atlas lleno en {self.name}')
        r = (self.x, self.y, w, h)
        self.x += w
        self.row = max(self.row, h)
        return r

    def box(self, frm, to, paint, rotation=None):
        dx, dy, dz = (to[i] - frm[i] for i in range(3))
        dims = {'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy), 'up': (dx, dz), 'down': (dx, dz)}
        faces = {}
        k = 64 / self.size
        for face, (w, h) in dims.items():
            pw, ph = max(1, round(w)), max(1, round(h))
            x, y, pw, ph = self.alloc(pw, ph)
            for j in range(ph):
                for i in range(pw):
                    c = paint(face, i / max(1, pw - 1), j / max(1, ph - 1), i, j)
                    if c is not None:
                        self.img.putpixel((x + i, y + j), c)
            s = 16 / self.size
            faces[face] = {'uv': [x * s, y * s, (x + pw) * s, (y + ph) * s], 'texture': '#0'}
        e = {'from': list(frm), 'to': list(to), 'faces': faces}
        if rotation:
            e['rotation'] = rotation
        self.elements.append(e)

    def save(self, display):
        tex = f'aidirector:item/{self.name}_3d'
        self.img.save(TEX_ITEM / f'{self.name}_3d.png')
        dump(MODELS_ITEM / f'{self.name}_3d.json', {
            'texture_size': [self.size, self.size],
            'textures': {'0': tex, 'particle': tex},
            'elements': self.elements,
            'display': display,
        })


def lit(c):
    """Pintor de cara con luz cenital: arriba más claro, abajo y laterales oscurecidos."""
    def f(face, u, v, i, j):
        k = {'up': 1.15, 'down': 0.6, 'north': 0.95, 'south': 0.95, 'east': 0.82, 'west': 0.82}[face]
        base = shade(c, k * (1.0 - 0.12 * v if face not in ('up', 'down') else 1.0))
        return base
    return f


THROWN_DISPLAY = {
    'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [0, 2, 1.5], 'scale': [0.6, 0.6, 0.6]},
    'thirdperson_lefthand': {'rotation': [0, -90, 0], 'translation': [0, 2, 1.5], 'scale': [0.6, 0.6, 0.6]},
    'firstperson_righthand': {'rotation': [0, -20, 10], 'translation': [2, 3, -1], 'scale': [0.6, 0.6, 0.6]},
    'firstperson_lefthand': {'rotation': [0, 20, -10], 'translation': [2, 3, -1], 'scale': [0.6, 0.6, 0.6]},
    'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'gui': {'rotation': [0, 0, -45], 'scale': [0.45, 0.45, 0.45]},
    'head': {'translation': [0, 14, 0], 'scale': [0.6, 0.6, 0.6]},
    'fixed': {'scale': [0.8, 0.8, 0.8]},
}


def molotov_3d():
    m = BoxModel('molotov')
    liquid = hexc('#B8742A')
    glass = hexc('#A7C29A')

    def bottle(face, u, v, i, j):
        if face == 'up':
            return shade(glass, 1.1)
        c = liquid if v > 0.25 else mix(glass, hexc('#E8F2DC'), 0.3)
        if i == 1 and face in ('north', 'west'):
            c = shade(c, 1.35)
        return shade(c, {'east': 0.8, 'west': 0.85, 'down': 0.6}.get(face, 1.0))

    def label(face, u, v, i, j):
        if face in ('up', 'down'):
            return None if face == 'up' else hexc('#B8A880')
        return hexc('#B33A2E') if j == 1 else shade(hexc('#E3D3A6'), 0.85 if face in ('east', 'west') else 1)

    def rag(face, u, v, i, j):
        c = hexc('#E8E0CC') if (i + j) % 3 else hexc('#B33A2E')
        return shade(c, 0.8 if face in ('east', 'down') else 1.0)

    def flame(face, u, v, i, j):
        return [hexc('#FFE14D'), hexc('#FFB300'), hexc('#FF6A00')][min(2, int((1 - v) * 3))] if face != 'down' else hexc('#FF6A00')

    m.box([5.5, 0, 5.5], [10.5, 7, 10.5], bottle)
    m.box([5.3, 1.8, 5.3], [10.7, 4.2, 10.7], label)
    m.box([6.5, 7, 6.5], [9.5, 8, 9.5], lit(glass))
    m.box([7, 8, 7], [9, 11, 9], lit(mix(glass, hexc('#E8F2DC'), 0.2)))
    m.box([6.6, 10.5, 6.6], [9.4, 13, 9.4], rag)
    m.box([9.2, 8.5, 7.4], [9.9, 11.5, 8.6], rag)
    m.box([7.2, 13, 7.2], [8.8, 15.5, 8.8], flame)
    m.save(THROWN_DISPLAY)


def defib_3d():
    m = BoxModel('defibrillator')
    yellow = hexc('#F2B705')

    def case(face, u, v, i, j):
        if face == 'north':
            if 1 <= i <= 8 and 1 <= j <= 4:
                return hexc('#2E2E2E') if not (i == 4 and 1 < j < 4) else hexc('#4CFF6A')
        return shade(yellow, {'up': 1.1, 'down': 0.55, 'east': 0.8, 'west': 0.85}.get(face, 1.0))

    def paddle(face, u, v, i, j):
        if face == 'down':
            return hexc('#C8CDD2')
        return shade(hexc('#5E656C'), 1.1 if face == 'up' else 0.85)

    def grip(face, u, v, i, j):
        return shade(hexc('#E5701F'), 0.8 if face in ('east', 'down') else 1.0)

    def cable(face, u, v, i, j):
        return hexc('#1E1E1E')

    m.box([3, 0, 5], [13, 5, 11], case)
    m.box([6, 5, 7.5], [10, 6, 8.5], lit(hexc('#3A3A3A')))
    m.box([5.5, 5, 7.5], [6.5, 6.8, 8.5], lit(hexc('#3A3A3A')))
    m.box([9.5, 5, 7.5], [10.5, 6.8, 8.5], lit(hexc('#3A3A3A')))
    for x0 in (2.5, 9.5):
        m.box([x0, 5, 4.5], [x0 + 4, 6, 7.5], paddle)
        m.box([x0 + 1.5, 6, 5.5], [x0 + 2.5, 8, 6.5], grip)
    m.box([4.2, 1, 4.6], [4.8, 5, 5], cable)
    m.box([11.2, 1, 4.6], [11.8, 5, 5], cable)
    m.save({
        'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 3, 1], 'scale': [0.55, 0.55, 0.55]},
        'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 3, 1], 'scale': [0.55, 0.55, 0.55]},
        'firstperson_righthand': {'rotation': [0, -25, 0], 'translation': [1, 4, 0], 'scale': [0.55, 0.55, 0.55]},
        'firstperson_lefthand': {'rotation': [0, 25, 0], 'translation': [1, 4, 0], 'scale': [0.55, 0.55, 0.55]},
        'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
        'gui': {'rotation': [30, 225, 0], 'scale': [0.6, 0.6, 0.6]},
        'head': {'translation': [0, 14, 0], 'scale': [0.6, 0.6, 0.6]},
        'fixed': {'scale': [0.8, 0.8, 0.8]},
    })


def separate(name):
    dump(MODELS_ITEM / f'{name}.json', {
        'loader': 'forge:separate_transforms',
        'gui_light': 'front',
        'base': {'parent': f'aidirector:item/{name}_3d'},
        'perspectives': {'gui': {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'aidirector:item/{name}'}}},
    })


# ====================================================================== modelos de bloque y estados
Y_ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def face_all(tex, uv=None, cull=None):
    f = {'texture': tex}
    if uv:
        f['uv'] = uv
    if cull:
        f['cullface'] = cull
    return f


def door_models():
    for part in ('bottom_left', 'bottom_left_open', 'bottom_right', 'bottom_right_open',
                 'top_left', 'top_left_open', 'top_right', 'top_right_open'):
        dump(MODELS_BLOCK / f'safe_room_door_{part}.json', {
            'parent': f'minecraft:block/door_{part}',
            'render_type': 'minecraft:cutout',
            'textures': {'bottom': 'aidirector:block/safe_room_door_bottom', 'top': 'aidirector:block/safe_room_door_top'},
        })
    variants = {}
    base = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    for facing, y0 in base.items():
        for half in ('lower', 'upper'):
            for hinge in ('left', 'right'):
                for open_ in ('false', 'true'):
                    part = ('bottom' if half == 'lower' else 'top') + '_' + hinge + ('_open' if open_ == 'true' else '')
                    y = y0 + (0 if open_ == 'false' else (90 if hinge == 'left' else 270))
                    v = {'model': f'aidirector:block/safe_room_door_{part}'}
                    if y % 360:
                        v['y'] = y % 360
                    variants[f'facing={facing},half={half},hinge={hinge},open={open_}'] = v
    dump(STATES / 'safe_room_door.json', {'variants': variants})
    dump(MODELS_ITEM / 'safe_room_door.json', {'parent': 'minecraft:item/generated',
                                               'textures': {'layer0': 'aidirector:item/safe_room_door'}})


def radio_model():
    t = '#0'
    s = 0.5  # 32 px → 16 UV

    def uv(x, y, w, h):
        return [x * s, y * s, (x + w) * s, (y + h) * s]

    body = {'from': [2, 0, 5], 'to': [14, 8, 11], 'faces': {
        'north': face_all(t, uv(0, 0, 12, 8)), 'south': face_all(t, uv(12, 0, 12, 8)),
        'east': face_all(t, uv(0, 8, 6, 8)), 'west': face_all(t, uv(6, 8, 6, 8)),
        'up': face_all(t, uv(12, 8, 12, 6)), 'down': face_all(t, uv(12, 14, 12, 6), 'down')}}
    knob = lambda x: {'from': [x, 5, 4], 'to': [x + 1.5, 6.5, 5], 'faces': {f: face_all(t, uv(24, 0, 2, 2)) for f in
                                                                            ('north', 'east', 'west', 'up', 'down')}}
    handle = {'from': [4, 8, 7.5], 'to': [10, 9.5, 8.5], 'faces': {f: face_all(t, uv(24, 4, 6, 1)) for f in
                                                                   ('north', 'south', 'east', 'west', 'up', 'down')}}
    antenna = {'from': [12, 8, 9], 'to': [12.6, 15, 9.6], 'faces': {f: face_all(t, uv(28, 0, 1, 4)) for f in
                                                                    ('north', 'south', 'east', 'west', 'up')}}
    dump(MODELS_BLOCK / 'safe_room_radio.json', {
        'parent': 'minecraft:block/block',
        'textures': {'0': 'aidirector:block/safe_room_radio', 'particle': 'aidirector:block/safe_room_radio'},
        'elements': [body, knob(9.5), knob(11.5), handle, antenna],
    })
    dump(STATES / 'safe_room_radio.json', {'variants': {
        f'facing={f}': ({'model': 'aidirector:block/safe_room_radio', 'y': y} if y else {'model': 'aidirector:block/safe_room_radio'})
        for f, y in Y_ROT.items()}})
    dump(MODELS_ITEM / 'safe_room_radio.json', {'parent': 'aidirector:block/safe_room_radio'})


def crate_models():
    for kind in KINDS:
        tex = {'front': f'aidirector:block/supply_crate_{kind}_front', 'side': f'aidirector:block/supply_crate_{kind}_side',
               'top': f'aidirector:block/supply_crate_{kind}_top', 'particle': f'aidirector:block/supply_crate_{kind}_side',
               'lock': 'aidirector:block/supply_crate_lock'}
        body = {'from': [1, 0, 1], 'to': [15, 12, 15], 'faces': {
            'north': face_all('#front', [1, 4, 15, 16]), 'south': face_all('#side', [1, 4, 15, 16]),
            'east': face_all('#side', [1, 4, 15, 16]), 'west': face_all('#side', [1, 4, 15, 16]),
            'up': face_all('#top', [1, 1, 15, 15]), 'down': face_all('#top', [1, 1, 15, 15], 'down')}}
        lid = {'from': [0.5, 11, 0.5], 'to': [15.5, 12.5, 15.5], 'faces': {
            'north': face_all('#side', [0, 0, 15, 1.5]), 'south': face_all('#side', [0, 0, 15, 1.5]),
            'east': face_all('#side', [0, 0, 15, 1.5]), 'west': face_all('#side', [0, 0, 15, 1.5]),
            'up': face_all('#top', [0.5, 0.5, 15.5, 15.5]), 'down': face_all('#top', [0.5, 0.5, 15.5, 15.5])}}
        dump(MODELS_BLOCK / f'supply_crate_{kind}.json', {'parent': 'minecraft:block/block', 'textures': tex,
                                                          'elements': [body, lid]})
        chain = {'from': [0.8, 6, 0.6], 'to': [15.2, 7, 1], 'faces': {
            'north': face_all('#lock', [0, 0, 16, 1]), 'up': face_all('#lock', [0, 0, 16, 0.5]),
            'down': face_all('#lock', [0, 2, 16, 2.5])}}
        padlock = {'from': [6.5, 3, 0.2], 'to': [9.5, 6.5, 1], 'faces': {
            'north': face_all('#lock', [6, 5, 10, 13]), 'east': face_all('#lock', [0, 5, 1, 13]),
            'west': face_all('#lock', [0, 5, 1, 13]), 'up': face_all('#lock', [0, 4, 4, 5]), 'down': face_all('#lock', [0, 14, 4, 15])}}
        shackle = {'from': [7, 6.5, 0.4], 'to': [9, 8, 0.8], 'faces': {
            'north': face_all('#lock', [0, 0, 3, 2]), 'up': face_all('#lock', [0, 0, 3, 1])}}
        dump(MODELS_BLOCK / f'supply_crate_{kind}_locked.json', {'parent': f'aidirector:block/supply_crate_{kind}',
                                                                 'elements': [body, lid, chain, padlock, shackle]})
    variants = {}
    for f, y in Y_ROT.items():
        for kind in KINDS:
            for locked in ('false', 'true'):
                v = {'model': f'aidirector:block/supply_crate_{kind}' + ('_locked' if locked == 'true' else '')}
                if y:
                    v['y'] = y
                variants[f'facing={f},kind={kind},locked={locked}'] = v
    dump(STATES / 'supply_crate.json', {'variants': variants})
    overrides = []
    for idx, kind in enumerate(KINDS):
        for locked in (0, 1):
            value = idx + locked * 0.5
            if value == 0:
                continue
            overrides.append({'predicate': {'aidirector:crate': value / 4.0},
                              'model': f'aidirector:block/supply_crate_{kind}' + ('_locked' if locked else '')})
    overrides.sort(key=lambda o: o['predicate']['aidirector:crate'])
    dump(MODELS_ITEM / 'supply_crate.json', {'parent': 'aidirector:block/supply_crate_medical', 'overrides': overrides})


def alarm_models():
    for on in (False, True):
        name = 'crescendo_alarm' + ('_on' if on else '')
        base = {'from': [1, 0, 1], 'to': [15, 8, 15], 'faces': {
            'north': face_all('#front', [1, 8, 15, 16]), 'south': face_all('#side', [1, 8, 15, 16]),
            'east': face_all('#side', [1, 8, 15, 16]), 'west': face_all('#side', [1, 8, 15, 16]),
            'up': face_all('#top', [1, 1, 15, 15]), 'down': face_all('#top', [1, 1, 15, 15], 'down')}}
        post = {'from': [6, 8, 6], 'to': [10, 10, 10], 'faces': {f: face_all('#top', [6, 6, 10, 8]) for f in
                                                                 ('north', 'south', 'east', 'west', 'up')}}
        dome = {'from': [4.5, 10, 4.5], 'to': [11.5, 15, 11.5], 'faces': {
            **{f: face_all('#light', [4.5, 6, 11.5, 11]) for f in ('north', 'south', 'east', 'west')},
            'up': face_all('#light', [4.5, 4.5, 11.5, 11.5]), 'down': face_all('#light', [4.5, 4.5, 11.5, 11.5])}}
        dump(MODELS_BLOCK / f'{name}.json', {
            'parent': 'minecraft:block/block',
            'textures': {'front': 'aidirector:block/crescendo_alarm_front', 'side': 'aidirector:block/crescendo_alarm_side',
                         'top': 'aidirector:block/crescendo_alarm_top', 'particle': 'aidirector:block/crescendo_alarm_side',
                         'light': 'aidirector:block/crescendo_alarm_light' + ('_on' if on else '')},
            'elements': [base, post, dome]})
    variants = {}
    for f, y in Y_ROT.items():
        for active in ('false', 'true'):
            for powered in ('false', 'true'):
                v = {'model': 'aidirector:block/crescendo_alarm' + ('_on' if active == 'true' else '')}
                if y:
                    v['y'] = y
                variants[f'active={active},facing={f},powered={powered}'] = v
    dump(STATES / 'crescendo_alarm.json', {'variants': variants})
    dump(MODELS_ITEM / 'crescendo_alarm.json', {'parent': 'aidirector:block/crescendo_alarm'})


def ammo_pile_state():
    dump(STATES / 'ammo_pile.json', {'variants': {
        'infinite=false': {'model': 'aidirector:block/ammo_pile'},
        'infinite=true': {'model': 'aidirector:block/ammo_pile'}}})


# ====================================================================== principal
def main():
    out = {}
    out['block/safe_room_door_bottom'] = door_bottom()
    out['block/safe_room_door_top'] = door_top()
    out['item/safe_room_door'] = door_item()
    for kind in KINDS:
        out[f'block/supply_crate_{kind}_front'] = crate_front(kind)
        out[f'block/supply_crate_{kind}_side'] = crate_side(kind)
        out[f'block/supply_crate_{kind}_top'] = crate_top(kind)
    out['block/supply_crate_lock'] = crate_lock()
    out['block/safe_room_radio'] = radio_atlas()
    out['block/crescendo_alarm_side'] = alarm_side()
    out['block/crescendo_alarm_front'] = alarm_front()
    out['block/crescendo_alarm_top'] = alarm_top()
    out['block/crescendo_alarm_light'] = alarm_light(False)
    out['block/crescendo_alarm_light_on'] = alarm_light(True)
    out['item/molotov'] = molotov_sprite()
    out['item/defibrillator'] = defib_sprite()
    for key, img in out.items():
        folder, name = key.split('/')
        img.save((TEX_BLOCK if folder == 'block' else TEX_ITEM) / f'{name}.png')

    molotov_3d()
    defib_3d()
    separate('molotov')
    separate('defibrillator')
    door_models()
    radio_model()
    crate_models()
    alarm_models()
    ammo_pile_state()

    if PREVIEW:
        tiles = list(out.items())
        sheet = Image.new('RGBA', (len(tiles) * 136, 150), (40, 40, 44, 255))
        for i, (key, img) in enumerate(tiles):
            big = img.resize((128, 128), Image.NEAREST)
            sheet.paste(big, (i * 136 + 4, 4), big)
        sheet.save(PREVIEW)
    print('ok', len(out), 'texturas')


if __name__ == '__main__':
    main()
