package com.igneko.aidirector.client.almanac;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.igneko.aidirector.core.TheAIDirector;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Contenido del almanaque en el idioma del juego. Cada idioma es un JSON en
 * {@code assets/aidirector/books/almanac/<idioma>.json}; si no existe el exacto se usa el de la misma
 * familia (es_mx → es_es, pt_pt → pt_br...) y, en último término, el inglés.
 */
@OnlyIn(Dist.CLIENT)
public final class AlmanacContent {

    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "books/almanac/";
    private static final Map<String, String> FAMILY = Map.ofEntries(
            Map.entry("es", "es_es"), Map.entry("pt", "pt_br"), Map.entry("fr", "fr_fr"), Map.entry("de", "de_de"),
            Map.entry("it", "it_it"), Map.entry("ru", "ru_ru"), Map.entry("zh", "zh_cn"), Map.entry("ja", "ja_jp"),
            Map.entry("ko", "ko_kr"), Map.entry("pl", "pl_pl"), Map.entry("uk", "ru_ru"), Map.entry("en", "en_us"));

    public record Entry(String id, ItemStack icon, String title, List<String> text) {}

    public record Category(String id, ItemStack icon, String title, String description, List<Entry> entries) {}

    public final String title;
    public final String subtitle;
    public final List<String> intro;
    public final List<Category> categories;
    private final JsonObject ui;

    private AlmanacContent(JsonObject root) {
        title = str(root, "title", "AI Director");
        subtitle = str(root, "subtitle", "");
        intro = strings(root.getAsJsonArray("intro"));
        ui = root.has("ui") ? root.getAsJsonObject("ui") : new JsonObject();
        categories = new ArrayList<>();
        for (JsonElement c : root.getAsJsonArray("categories")) {
            JsonObject co = c.getAsJsonObject();
            List<Entry> entries = new ArrayList<>();
            for (JsonElement e : co.getAsJsonArray("entries")) {
                JsonObject eo = e.getAsJsonObject();
                entries.add(new Entry(str(eo, "id", ""), icon(str(eo, "icon", "minecraft:book")), str(eo, "title", "?"),
                        strings(eo.getAsJsonArray("text"))));
            }
            categories.add(new Category(str(co, "id", ""), icon(str(co, "icon", "minecraft:book")), str(co, "title", "?"),
                    str(co, "desc", ""), entries));
        }
    }

    public String ui(String key, String fallback) {
        return str(ui, key, fallback);
    }

    /** Carga el almanaque del idioma seleccionado (con respaldo a la familia y al inglés). */
    public static AlmanacContent load() {
        String code = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT);
        String family = FAMILY.get(code.contains("_") ? code.substring(0, code.indexOf('_')) : code);
        for (String candidate : new String[]{code, family, "en_us"}) {
            if (candidate == null) continue;
            JsonObject root = read(new ResourceLocation(TheAIDirector.MOD_ID, DIR + candidate + ".json"));
            if (root != null) {
                try {
                    return new AlmanacContent(root);
                } catch (RuntimeException e) {
                    LOG.error("[{}] Almanaque mal formado: {}", TheAIDirector.MOD_ID, candidate, e);
                }
            }
        }
        JsonObject empty = new JsonObject();
        empty.add("intro", new JsonArray());
        empty.add("categories", new JsonArray());
        return new AlmanacContent(empty);
    }

    private static JsonObject read(ResourceLocation rl) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) return null;
            try (Reader reader = res.get().openAsReader()) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) {
            LOG.error("[{}] No se pudo leer {}", TheAIDirector.MOD_ID, rl, e);
            return null;
        }
    }

    private static ItemStack icon(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        return new ItemStack(item == null || item == Items.AIR ? Items.BOOK : item);
    }

    private static String str(JsonObject o, String key, String def) {
        return o != null && o.has(key) ? o.get(key).getAsString() : def;
    }

    private static List<String> strings(JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr != null) arr.forEach(e -> out.add(e.getAsString()));
        return out;
    }
}
