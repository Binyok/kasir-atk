package giriatk.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Theme {

    private Theme() {
    }

    public static final Color PAPER = new Color(0xE7DCC3);
    public static final Color CANVAS = new Color(0xFAF6EF);
    public static final Color SURFACE = new Color(0xFFFDF8);
    public static final Color SURFACE_ELEVATED = new Color(0xF3ECDC);
    public static final Color SURFACE_MUTED = new Color(0xECE7DB);
    public static final Color SURFACE_HOVER = new Color(0xF0E8D2);
    public static final Color ROW_LINE = new Color(0xF0E8D2);

    public static final Color INK = new Color(0x1C1917);
    public static final Color INK_SOFT = new Color(0x44403C);
    public static final Color MUTED = new Color(0x78716C);
    public static final Color FAINT = new Color(0xA8A29E);

    public static final Color BORDER = new Color(0xE0D7BF);
    public static final Color BORDER_STRONG = new Color(0xCBBFA0);
    public static final Color BORDER_HIGHLIGHT = new Color(0x1C1917);

    public static final Color ACCENT = new Color(0xC93A1E);
    public static final Color ACCENT_HOVER = new Color(0xA82E15);
    public static final Color ACCENT_PRESSED = new Color(0x8A2410);
    public static final Color ACCENT_SOFT = new Color(0xF9E3DC);

    public static final Color SIDEBAR = new Color(0x1C1917);
    public static final Color SIDEBAR_TEXT = new Color(0xE9E2D0);
    public static final Color SIDEBAR_MUTED = new Color(0xA8A29E);
    public static final Color SIDEBAR_FAINT = new Color(0x78716C);
    public static final Color SIDEBAR_LINE = new Color(0x3A3430);

    public static final Color BLUE_BG = new Color(0xE0E9F5);
    public static final Color BLUE_FG = new Color(0x2456A6);
    public static final Color BLUE_BORDER = new Color(0xB3C8E4);
    public static final Color AMBER_BG = new Color(0xF7E8CF);
    public static final Color AMBER_FG = new Color(0xB45309);
    public static final Color AMBER_BORDER = new Color(0xE8CF9E);
    public static final Color GREEN_BG = new Color(0xE2EFE4);
    public static final Color GREEN_FG = new Color(0x2F7D4F);
    public static final Color GREEN_BORDER = new Color(0xB9D6BF);
    public static final Color NEUTRAL_BG = new Color(0xECE7DB);
    public static final Color NEUTRAL_FG = new Color(0x6B655C);
    public static final Color NEUTRAL_BORDER = new Color(0xD4CBB6);

    public static final Color DANGER = new Color(0xB3261E);
    public static final Color DANGER_HOVER = new Color(0x8F1D17);
    public static final Color DANGER_SOFT = new Color(0xF9E0DC);

    public static final int RADIUS_CARD = 0;
    public static final int RADIUS_CONTROL = 0;

    private static final Set<String> FAMILIES = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames(Locale.ROOT)));

    private static final String DISPLAY_FAMILY =
            firstAvailable("Georgia", "Playfair Display", "Times New Roman", "Serif");
    private static final String TEXT_FAMILY =
            firstAvailable("Segoe UI", "Segoe UI Variable Text", "SansSerif");
    private static final String MONO_FAMILY =
            firstAvailable("Cascadia Mono", "Cascadia Code", "Consolas", "Monospaced");

    private static String firstAvailable(String... names) {
        for (String name : names) {
            if (FAMILIES.contains(name)) {
                return name;
            }
        }
        return "SansSerif";
    }

    public static Font display(int style, int size) {
        return new Font(DISPLAY_FAMILY, style, size);
    }

    public static Font text(int style, int size) {
        return new Font(TEXT_FAMILY, style, size);
    }

    public static Font mono(int style, int size) {
        return new Font(MONO_FAMILY, style, size);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Color mix(Color a, Color b, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * clamped);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        return new Color(r, g, bl);
    }
}
