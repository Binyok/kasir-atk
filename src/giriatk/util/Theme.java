package giriatk.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Theme {

    private Theme() {
    }

    private static boolean dark = true;

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean isDark) {
        dark = isDark;
    }

    public static void toggle(Window window) {
        setDark(!isDark());
        try {
            if (isDark()) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("defaultFont", Theme.text(Font.PLAIN, 13));
            FlatLaf.updateUI();
            if (window != null) {
                SwingUtilities.updateComponentTreeUI(window);
                window.repaint();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static class DynamicColor extends Color {
        private final Color darkColor;
        private final Color lightColor;

        public DynamicColor(Color darkColor, Color lightColor) {
            super(darkColor.getRGB(), true);
            this.darkColor = darkColor;
            this.lightColor = lightColor;
        }

        public DynamicColor(int darkRgb, int lightRgb) {
            this(new Color(darkRgb), new Color(lightRgb));
        }

        private Color current() {
            return Theme.isDark() ? darkColor : lightColor;
        }

        @Override
        public int getRGB() {
            return current().getRGB();
        }

        @Override
        public int getRed() {
            return current().getRed();
        }

        @Override
        public int getGreen() {
            return current().getGreen();
        }

        @Override
        public int getBlue() {
            return current().getBlue();
        }

        @Override
        public int getAlpha() {
            return current().getAlpha();
        }

        @Override
        public float[] getRGBComponents(float[] compArray) {
            return current().getRGBComponents(compArray);
        }

        @Override
        public float[] getRGBColorComponents(float[] compArray) {
            return current().getRGBColorComponents(compArray);
        }

        @Override
        public float[] getComponents(float[] compArray) {
            return current().getComponents(compArray);
        }

        @Override
        public float[] getColorComponents(float[] compArray) {
            return current().getColorComponents(compArray);
        }
    }

    // Dynamic Tokens: (Dark / Light)
    public static final Color CANVAS = new DynamicColor(0x0B0D11, 0xF6F5F2);
    public static final Color SURFACE = new DynamicColor(0x13171F, 0xFFFFFF);
    public static final Color SURFACE_ELEVATED = new DynamicColor(0x1A202A, 0xFAF9F6);
    public static final Color SURFACE_MUTED = new DynamicColor(0x1F2633, 0xEDECE8);
    public static final Color SURFACE_HOVER = new DynamicColor(0x273142, 0xE5E3DE);

    public static final Color INK = new DynamicColor(0xF1F5F9, 0x111827);
    public static final Color INK_SOFT = new DynamicColor(0xCBD5E1, 0x374151);
    public static final Color MUTED = new DynamicColor(0x94A3B8, 0x6B7280);
    public static final Color FAINT = new DynamicColor(0x64748B, 0x9CA3AF);

    public static final Color BORDER = new DynamicColor(0x232B38, 0xE2DFD8);
    public static final Color BORDER_STRONG = new DynamicColor(0x333F52, 0xCAC5BA);
    public static final Color BORDER_HIGHLIGHT = new DynamicColor(0x475569, 0xA8A294);

    public static final Color ACCENT = new DynamicColor(0xF59E0B, 0xD97706);
    public static final Color ACCENT_HOVER = new DynamicColor(0xD97706, 0xB45309);
    public static final Color ACCENT_PRESSED = new DynamicColor(0xB45309, 0x92400E);
    public static final Color ACCENT_SOFT = new DynamicColor(0x2B2312, 0xFEF3C7);

    public static final Color BLUE_BG = new DynamicColor(0x132742, 0xEFF6FF);
    public static final Color BLUE_FG = new DynamicColor(0x60A5FA, 0x2563EB);
    public static final Color AMBER_BG = new DynamicColor(0x3B2808, 0xFEF3C7);
    public static final Color AMBER_FG = new DynamicColor(0xFBBF24, 0xB45309);
    public static final Color GREEN_BG = new DynamicColor(0x0B3322, 0xECFDF5);
    public static final Color GREEN_FG = new DynamicColor(0x34D399, 0x059669);
    public static final Color NEUTRAL_BG = new DynamicColor(0x1E2530, 0xF3F4F6);
    public static final Color NEUTRAL_FG = new DynamicColor(0x94A3B8, 0x4B5563);

    public static final Color DANGER = new DynamicColor(0xEF4444, 0xDC2626);
    public static final Color DANGER_HOVER = new DynamicColor(0xDC2626, 0xB91C1C);
    public static final Color DANGER_SOFT = new DynamicColor(0x381214, 0xFEE2E2);

    public static final int RADIUS_CARD = 8;
    public static final int RADIUS_CONTROL = 6;

    private static final Set<String> FAMILIES = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames(Locale.ROOT)));

    private static final String DISPLAY_FAMILY =
            firstAvailable("Segoe UI Variable Display", "Segoe UI Semibold", "Segoe UI", "SansSerif");
    private static final String TEXT_FAMILY =
            firstAvailable("Segoe UI Variable Text", "Segoe UI", "SansSerif");
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
