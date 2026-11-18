package com.igneko.aidirector.client.almanac;

import com.igneko.aidirector.core.TheAIDirector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Almanaque "AI Director": libro abierto de dos páginas.
 * <ul>
 *   <li><b>Portada:</b> título e introducción a la izquierda; capítulos clicables a la derecha.</li>
 *   <li><b>Capítulo:</b> descripción a la izquierda; entradas clicables (con icono) a la derecha.</li>
 *   <li><b>Entrada:</b> texto repartido en páginas izquierda/derecha con flechas, rueda y teclado.</li>
 * </ul>
 * Recuerda la última entrada abierta durante la sesión.
 */
@OnlyIn(Dist.CLIENT)
public class AlmanacScreen extends Screen {

    private static final ResourceLocation TEX = new ResourceLocation(TheAIDirector.MOD_ID, "textures/gui/almanac.png");
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int BOOK_W = 288, BOOK_H = 184;
    private static final int PAGE_W = 112, LINE_H = 10, PAGE_LINES = 14;
    private static final int LEFT_X = 22, RIGHT_X = 154, TEXT_Y = 18;
    private static final int ROW_H = 18, ROWS_PER_PAGE = 7;

    private static final int INK = 0x2B2418, INK_SOFT = 0x6E6150, ACCENT = 0x8E1B16, HOVER = 0xC0392B;

    private enum View { LANDING, CATEGORY, ENTRY }

    private record Click(int x, int y, int w, int h, Runnable action) {
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    // Marcador de sesión: vuelve a donde lo dejaste.
    private static View lastView = View.LANDING;
    private static int lastCategory = -1, lastEntry = -1, lastSpread = 0;

    private final AlmanacContent content;
    private View view = View.LANDING;
    private int category = -1, entry = -1, spread = 0;
    private List<List<FormattedCharSequence>> pages = List.of();
    private final List<Click> clicks = new ArrayList<>();
    private int left, top;

    public AlmanacScreen() {
        super(Component.literal("AI Director"));
        this.content = AlmanacContent.load();
        if (lastView != View.LANDING && lastCategory >= 0 && lastCategory < content.categories.size()) {
            if (lastView == View.ENTRY && lastEntry >= 0 && lastEntry < content.categories.get(lastCategory).entries().size()) {
                openEntry(lastCategory, lastEntry, false);
                spread = lastSpread;
            } else {
                openCategory(lastCategory, false);
            }
        }
    }

    @Override
    protected void init() {
        left = (width - BOOK_W) / 2;
        top = (height - BOOK_H) / 2;
        if (view == View.ENTRY) {
            pages = paginate(content.categories.get(category).entries().get(entry));
            spread = Math.min(spread, maxSpread());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================== navegación

    private void openCategory(int index, boolean sound) {
        view = View.CATEGORY;
        category = index;
        entry = -1;
        spread = 0;
        if (sound) turnSound();
    }

    private void openEntry(int cat, int index, boolean sound) {
        view = View.ENTRY;
        category = cat;
        entry = index;
        spread = 0;
        // La fuente solo existe tras init(): al restaurar el marcador desde el constructor se pagina allí.
        pages = font != null ? paginate(content.categories.get(cat).entries().get(index)) : List.of();
        if (sound) turnSound();
    }

    private void goBack() {
        if (view == View.ENTRY) openCategory(category, true);
        else if (view == View.CATEGORY) {
            view = View.LANDING;
            category = -1;
            spread = 0;
            turnSound();
        }
    }

    private int maxSpread() {
        return switch (view) {
            case ENTRY -> Math.max(0, (pages.size() - 1) / 2);
            case CATEGORY -> Math.max(0, (content.categories.get(category).entries().size() - 1) / ROWS_PER_PAGE);
            default -> 0;
        };
    }

    private void turn(int delta) {
        int next = Math.max(0, Math.min(maxSpread(), spread + delta));
        if (next != spread) {
            spread = next;
            turnSound();
        }
    }

    private void turnSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    @Override
    public void removed() {
        lastView = view;
        lastCategory = category;
        lastEntry = entry;
        lastSpread = spread;
    }

    // ================================================================== render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        clicks.clear();
        g.blit(TEX, left, top, 0, 0, BOOK_W, BOOK_H, TEX_W, TEX_H);

        switch (view) {
            case LANDING -> renderLanding(g, mouseX, mouseY);
            case CATEGORY -> renderCategory(g, mouseX, mouseY);
            case ENTRY -> renderEntry(g);
        }
        renderButtons(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderLanding(GuiGraphics g, int mx, int my) {
        int y = banner(g, left + LEFT_X, top + TEXT_Y, content.title, content.subtitle);
        List<FormattedCharSequence> lines = wrap(content.intro);
        for (int i = 0; i < lines.size() && y + LINE_H <= top + TEXT_Y + PAGE_LINES * LINE_H + 6; i++, y += LINE_H) {
            g.drawString(font, lines.get(i), left + LEFT_X, y, INK, false);
        }

        int rx = left + RIGHT_X;
        int ry = top + TEXT_Y;
        centered(g, Component.literal(content.ui("chapters", "Chapters")).withStyle(ChatFormatting.BOLD), rx + PAGE_W / 2, ry, ACCENT);
        divider(g, rx, ry + 11);
        ry += 17;
        // Con muchos capítulos las filas se compactan para que quepan en la página.
        int rowH = Math.max(16, Math.min(ROW_H, (PAGE_LINES * LINE_H - 12) / Math.max(1, content.categories.size())));
        for (int i = 0; i < content.categories.size(); i++, ry += rowH) {
            AlmanacContent.Category c = content.categories.get(i);
            final int index = i;
            row(g, rx, ry, rowH, c.icon(), c.title(), mx, my, () -> openCategory(index, true));
        }
    }

    private void renderCategory(GuiGraphics g, int mx, int my) {
        AlmanacContent.Category c = content.categories.get(category);
        int y = banner(g, left + LEFT_X, top + TEXT_Y, c.title(), null);
        g.renderItem(c.icon(), left + LEFT_X + PAGE_W / 2 - 8, y);
        y += 22;
        for (FormattedCharSequence line : wrap(List.of(c.description()))) {
            if (y > top + TEXT_Y + PAGE_LINES * LINE_H) break;
            g.drawString(font, line, left + LEFT_X, y, INK, false);
            y += LINE_H;
        }

        // Entradas: columna derecha; si no caben, continúan en la siguiente doble página.
        List<AlmanacContent.Entry> entries = c.entries();
        int start = spread * ROWS_PER_PAGE;
        int rx = left + RIGHT_X;
        int ry = top + TEXT_Y;
        centered(g, Component.literal(content.ui("entries", "Entries")).withStyle(ChatFormatting.BOLD), rx + PAGE_W / 2, ry, ACCENT);
        divider(g, rx, ry + 11);
        ry += 17;
        for (int i = start; i < Math.min(entries.size(), start + ROWS_PER_PAGE); i++, ry += ROW_H) {
            AlmanacContent.Entry e = entries.get(i);
            final int index = i;
            row(g, rx, ry, e.icon(), e.title(), mx, my, () -> openEntry(category, index, true));
        }
    }

    private void renderEntry(GuiGraphics g) {
        for (int side = 0; side < 2; side++) {
            int pageIndex = spread * 2 + side;
            if (pageIndex >= pages.size()) break;
            int x = left + (side == 0 ? LEFT_X : RIGHT_X);
            int y = top + TEXT_Y;
            if (pageIndex == 0) {
                AlmanacContent.Entry e = content.categories.get(category).entries().get(entry);
                g.renderItem(e.icon(), x, y - 2);
                g.drawString(font, Component.literal(e.title()).withStyle(ChatFormatting.BOLD), x + 19, y + 2, ACCENT, false);
                divider(g, x, y + 16);
                y += 22;
            }
            for (FormattedCharSequence line : pages.get(pageIndex)) {
                g.drawString(font, line, x, y, INK, false);
                y += LINE_H;
            }
            String num = (pageIndex + 1) + " / " + pages.size();
            g.drawString(font, num, x + PAGE_W / 2 - font.width(num) / 2, top + BOOK_H - 20, INK_SOFT, false);
        }
    }

    private void renderButtons(GuiGraphics g, int mx, int my) {
        int by = top + BOOK_H - 22;
        if (spread > 0) button(g, left + 14, by, 0, mx, my, () -> turn(-1));
        if (spread < maxSpread()) button(g, left + BOOK_W - 32, by, 1, mx, my, () -> turn(1));
        if (view != View.LANDING) button(g, left + BOOK_W / 2 - 9, top + BOOK_H - 12, 2, mx, my, this::goBack);
    }

    /** Botones del atlas: 0 = anterior, 1 = siguiente, 2 = volver (18×10; fila inferior = hover). */
    private void button(GuiGraphics g, int x, int y, int kind, int mx, int my, Runnable action) {
        boolean hover = mx >= x && mx < x + 18 && my >= y && my < y + 10;
        g.blit(TEX, x, y, 300 + kind * 20, hover ? 12 : 0, 18, 10, TEX_W, TEX_H);
        clicks.add(new Click(x, y, 18, 10, action));
    }

    private int banner(GuiGraphics g, int x, int y, String title, String subtitle) {
        g.blit(TEX, x - 4, y - 3, 300, 30, PAGE_W + 8, 20, TEX_W, TEX_H);
        Component t = Component.literal(title).withStyle(ChatFormatting.BOLD);
        g.drawString(font, t, x + PAGE_W / 2 - font.width(t) / 2, y + 3, 0xF4E7C4, false);
        y += 22;
        if (subtitle != null && !subtitle.isEmpty()) {
            Component s = Component.literal(subtitle).withStyle(ChatFormatting.ITALIC);
            g.drawString(font, s, x + PAGE_W / 2 - font.width(s) / 2, y, INK_SOFT, false);
            y += 14;
        }
        return y;
    }

    private void row(GuiGraphics g, int x, int y, ItemStack icon, String title, int mx, int my, Runnable action) {
        row(g, x, y, ROW_H, icon, title, mx, my, action);
    }

    private void row(GuiGraphics g, int x, int y, int rowH, ItemStack icon, String title, int mx, int my, Runnable action) {
        boolean hover = mx >= x - 2 && mx < x + PAGE_W + 2 && my >= y - 1 && my < y + rowH - 1;
        if (hover) g.fill(x - 2, y - 1, x + PAGE_W + 2, y + rowH - 1, 0x228E1B16);
        g.renderItem(icon, x, y);
        String text = font.plainSubstrByWidth(title, PAGE_W - 22);
        g.drawString(font, Component.literal(text).withStyle(hover ? Style.EMPTY.withUnderlined(true) : Style.EMPTY),
                x + 20, y + 5, hover ? HOVER : INK, false);
        clicks.add(new Click(x - 2, y - 1, PAGE_W + 4, rowH, action));
    }

    private void divider(GuiGraphics g, int x, int y) {
        g.blit(TEX, x, y, 300, 52, PAGE_W, 3, TEX_W, TEX_H);
    }

    private void centered(GuiGraphics g, Component text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, false);
    }

    // ================================================================== texto

    private List<FormattedCharSequence> wrap(List<String> paragraphs) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (int i = 0; i < paragraphs.size(); i++) {
            lines.addAll(font.split(parse(paragraphs.get(i)), PAGE_W));
            if (i < paragraphs.size() - 1) lines.add(FormattedCharSequence.EMPTY);
        }
        return lines;
    }

    /** Reparte una entrada en páginas; la primera reserva espacio para el título. */
    private List<List<FormattedCharSequence>> paginate(AlmanacContent.Entry e) {
        List<FormattedCharSequence> lines = wrap(e.text());
        List<List<FormattedCharSequence>> out = new ArrayList<>();
        int i = 0;
        boolean first = true;
        while (i < lines.size() || first) {
            int capacity = first ? PAGE_LINES - 2 : PAGE_LINES;
            List<FormattedCharSequence> page = new ArrayList<>();
            while (i < lines.size() && page.size() < capacity) {
                if (page.isEmpty() && lines.get(i) == FormattedCharSequence.EMPTY && !first) { i++; continue; }
                page.add(lines.get(i++));
            }
            out.add(page);
            first = false;
        }
        return out;
    }

    /** Marcado mínimo: **negrita**, __cursiva__ y {{acento}} en rojo. */
    private static MutableComponent parse(String text) {
        MutableComponent out = Component.empty();
        boolean bold = false, italic = false, accent = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String two = i + 1 < text.length() ? text.substring(i, i + 2) : "";
            if (two.equals("**") || two.equals("__") || two.equals("{{") || two.equals("}}")) {
                flush(out, buf, bold, italic, accent);
                switch (two) {
                    case "**" -> bold = !bold;
                    case "__" -> italic = !italic;
                    case "{{" -> accent = true;
                    default -> accent = false;
                }
                i++;
                continue;
            }
            buf.append(text.charAt(i));
        }
        flush(out, buf, bold, italic, accent);
        return out;
    }

    private static void flush(MutableComponent out, StringBuilder buf, boolean bold, boolean italic, boolean accent) {
        if (buf.isEmpty()) return;
        Style s = Style.EMPTY.withBold(bold).withItalic(italic);
        if (accent) s = s.withColor(ACCENT);
        out.append(Component.literal(buf.toString()).withStyle(s));
        buf.setLength(0);
    }

    // ================================================================== entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (Click c : new ArrayList<>(clicks)) {
                if (c.hit(mx, my)) {
                    c.action().run();
                    return true;
                }
            }
        } else if (button == 1) {
            goBack();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        turn(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> { turn(-1); return true; }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> { turn(1); return true; }
            case GLFW.GLFW_KEY_BACKSPACE -> { goBack(); return true; }
            default -> { return super.keyPressed(key, scan, mods); }
        }
    }
}
