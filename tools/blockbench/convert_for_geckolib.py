"""Convierte animaciones exportadas desde Blockbench (formato Bedrock) al sistema de GeckoLib.

GeckoLib invierte las rotaciones X e Y de los fotogramas clave al cargarlas, así que una animación
que en la vista previa Bedrock de Blockbench se inclina hacia delante aparecería inclinada hacia
atrás en el juego. Este script invierte X e Y una vez (marca el archivo para no hacerlo dos veces).

Uso:  python convert_for_geckolib.py <archivo.animation.json> [...]
"""
import json
import sys

MARK = "aidirector_geckolib_converted"


def flip(value):
    if isinstance(value, list) and len(value) == 3:
        x, y, z = value
        return [-x if isinstance(x, (int, float)) else x, -y if isinstance(y, (int, float)) else y, z]
    if isinstance(value, dict):
        out = dict(value)
        for key in ("vector", "pre", "post"):
            if key in out:
                out[key] = flip(out[key])
        return out
    return value


def convert(path):
    data = json.load(open(path, encoding="utf-8"))
    if data.get(MARK):
        return f"{path}: ya convertido"
    count = 0
    for anim in data.get("animations", {}).values():
        for bone in anim.get("bones", {}).values():
            rot = bone.get("rotation")
            if rot is None:
                continue
            if isinstance(rot, dict) and not {"vector", "pre", "post"} & rot.keys():
                for t in rot:
                    rot[t] = flip(rot[t])
                    count += 1
            else:
                bone["rotation"] = flip(rot)
                count += 1
    data[MARK] = True
    json.dump(data, open(path, "w", encoding="utf-8"), indent=1)
    return f"{path}: {count} fotogramas convertidos"


if __name__ == "__main__":
    for p in sys.argv[1:]:
        print(convert(p))
