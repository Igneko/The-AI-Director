package com.igneko.aidirector.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.igneko.aidirector.core.TheAIDirector;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Construye las páginas del almanaque a partir de {@code assets/aidirector/books/almanac.json}
 * (secciones con título y párrafos) y las pagina con la fuente real del juego, de modo que el texto
 * se puede ampliar o corregir sin tocar código.
 */
@OnlyIn(Dist.CLIENT)
public final class AlmanacScreenOpener {

    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation SOURCE = new ResourceLocation(TheAIDirector.MOD_ID, "books/almanac.json");
    private static final int PAGE_WIDTH = 114;
    private static final int LINES_PER_PAGE = 14;

    private AlmanacScreenOpener() {}

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        List<FormattedText> pages = buildPages(mc.font);
        mc.setScreen(new BookViewScreen(new BookViewScreen.BookAccess() {
            @Override
            public int getPageCount() {
                return pages.size();
            }

            @Override
            public FormattedText getPageRaw(int index) {
                return pages.get(index);
            }
        }));
    }

    private static List<FormattedText> buildPages(Font font) {
        List<FormattedText> pages = new ArrayList<>();
        JsonObject root = load();
        if (root == null) {
            pages.add(Component.literal("AI Director"));
            return pages;
        }
        for (JsonElement el : root.getAsJsonArray("sections")) {
            JsonObject section = el.getAsJsonObject();
            String style = section.has("style") ? section.get("style").getAsString() : "normal";
            List<FormattedText> lines = new ArrayList<>();
            if (section.has("title")) {
                Component title = Component.literal(section.get("title").getAsString())
                        .withStyle(titleStyle(style));
                lines.addAll(font.getSplitter().splitLines(title, PAGE_WIDTH, Style.EMPTY));
                lines.add(FormattedText.EMPTY);
            }
            JsonArray paragraphs = section.getAsJsonArray("text");
            for (int i = 0; i < paragraphs.size(); i++) {
                MutableComponent p = parseParagraph(paragraphs.get(i).getAsString(), style);
                lines.addAll(font.getSplitter().splitLines(p, PAGE_WIDTH, Style.EMPTY));
                if (i < paragraphs.size() - 1) lines.add(FormattedText.EMPTY);
            }
            paginate(lines, pages);
        }
        return pages;
    }

    private static ChatFormatting[] titleStyle(String style) {
        return switch (style) {
            case "cover" -> new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD};
            case "letter" -> new ChatFormatting[]{ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD};
            case "beast" -> new ChatFormatting[]{ChatFormatting.DARK_GREEN, ChatFormatting.BOLD};
            default -> new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD, ChatFormatting.UNDERLINE};
        };
    }

    /** Párrafo con marcado mínimo: **negrita** y __cursiva__. */
    private static MutableComponent parseParagraph(String text, String style) {
        MutableComponent out = Component.empty();
        boolean bold = false, italic = false;
        StringBuilder buf = new StringBuilder();
        Style base = "letter".equals(style) ? Style.EMPTY.withItalic(true).withColor(ChatFormatting.DARK_GRAY) : Style.EMPTY;
        for (int i = 0; i < text.length(); i++) {
            if (text.startsWith("**", i) || text.startsWith("__", i)) {
                out.append(Component.literal(buf.toString()).withStyle(base.withBold(bold).withItalic(italic || base.isItalic())));
                buf.setLength(0);
                if (text.charAt(i) == '*') bold = !bold;
                else italic = !italic;
                i++;
                continue;
            }
            buf.append(text.charAt(i));
        }
        out.append(Component.literal(buf.toString()).withStyle(base.withBold(bold).withItalic(italic || base.isItalic())));
        return out;
    }

    /** Agrupa líneas en páginas de 14; cada sección empieza en página nueva. */
    private static void paginate(List<FormattedText> lines, List<FormattedText> pages) {
        for (int start = 0; start < lines.size(); start += LINES_PER_PAGE) {
            List<FormattedText> parts = new ArrayList<>();
            int end = Math.min(lines.size(), start + LINES_PER_PAGE);
            for (int i = start; i < end; i++) {
                parts.add(lines.get(i));
                if (i < end - 1) parts.add(FormattedText.of("\n"));
            }
            pages.add(FormattedText.composite(parts));
        }
    }

    private static JsonObject load() {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(SOURCE);
            if (res.isEmpty()) return null;
            try (Reader reader = res.get().openAsReader()) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) {
            LOG.error("[{}] No se pudo leer el almanaque", TheAIDirector.MOD_ID, e);
            return null;
        }
    }
}
