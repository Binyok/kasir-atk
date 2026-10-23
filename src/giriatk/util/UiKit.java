package giriatk.util;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class UiKit {

    private UiKit() {
    }

    public static class SurfacePanel extends JPanel {
        private final int arc;
        private final boolean shadow;

        public SurfacePanel(LayoutManager layout) {
            this(layout, Theme.RADIUS_CARD, true);
        }

        public SurfacePanel(LayoutManager layout, int arc, boolean shadow) {
            super(layout);
            this.arc = arc;
            this.shadow = shadow;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Outer double-bezel shadow/hairline
            if (shadow) {
                g2.setColor(Theme.isDark() ? new Color(0, 0, 0, 80) : new Color(0, 0, 0, 15));
                g2.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 2, arc, arc);
            }
            
            // Surface fill
            g2.setColor(Theme.SURFACE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            
            // Top inner highlight line (1px)
            g2.setColor(Theme.isDark() ? new Color(255, 255, 255, 12) : new Color(255, 255, 255, 140));
            g2.drawLine(arc / 2, 1, getWidth() - 1 - arc / 2, 1);
            
            // Outer border
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public enum Variant { PRIMARY, INK, GHOST, DANGER, SUBTLE }

    public static class SoftButton extends JButton {
        private final Variant variant;

        public SoftButton(String text, Variant variant) {
            super(text);
            this.variant = variant;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(Theme.text(Font.BOLD, 12));
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setForeground(Theme.INK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean roll = getModel().isRollover();
            boolean press = getModel().isPressed() && isEnabled();
            int shift = press ? 1 : 0;
            int w = getWidth();
            int h = getHeight() - shift;

            Color bg;
            Color fg;
            Color border = null;

            switch (variant) {
                case PRIMARY:
                    bg = press ? Theme.ACCENT_PRESSED : (roll ? Theme.ACCENT_HOVER : Theme.ACCENT);
                    fg = Theme.isDark() ? new Color(0x0A0C10) : Color.WHITE;
                    break;
                case INK:
                    bg = press ? new Color(0x10141B) : (roll ? new Color(0x273142) : Theme.SURFACE_ELEVATED);
                    fg = Theme.INK;
                    border = Theme.BORDER_STRONG;
                    break;
                case GHOST:
                    bg = roll ? Theme.SURFACE_HOVER : Theme.SURFACE_ELEVATED;
                    fg = Theme.INK;
                    border = Theme.BORDER;
                    break;
                case DANGER:
                    bg = roll ? Theme.DANGER_SOFT : Theme.SURFACE;
                    fg = Theme.DANGER;
                    border = Theme.alpha(Theme.DANGER, 120);
                    break;
                case SUBTLE:
                default:
                    bg = roll ? Theme.SURFACE_HOVER : Theme.SURFACE_ELEVATED;
                    fg = Theme.MUTED;
                    border = Theme.BORDER;
                    break;
            }

            if (!isEnabled()) {
                bg = Theme.SURFACE;
                fg = Theme.FAINT;
                border = Theme.BORDER;
            }

            g2.setColor(bg);
            g2.fillRoundRect(0, shift, w, h, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, shift, w - 1, h - 1, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.ACCENT);
                g2.drawRoundRect(1, shift + 1, w - 3, h - 3, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            }
            g2.dispose();

            setForeground(fg);
            Graphics gShift = g.create();
            gShift.translate(0, shift);
            super.paintComponent(gShift);
            gShift.dispose();
        }
    }

    public static class IconButton extends JButton {
        private final Icons.Name icon;
        private final Color color;
        private final int iconSize;
        private final boolean boxed;

        public IconButton(Icons.Name icon, Color color, int iconSize) {
            this(icon, color, iconSize, false);
        }

        public IconButton(Icons.Name icon, Color color, int iconSize, boolean boxed) {
            this.icon = icon;
            this.color = color;
            this.iconSize = iconSize;
            this.boxed = boxed;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            int d = iconSize + 16;
            setPreferredSize(new Dimension(d, d));
            setMaximumSize(new Dimension(d, d));
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setToolTipText(tooltip(icon));
        }

        private static String tooltip(Icons.Name icon) {
            switch (icon) {
                case PLUS: return "Tambah";
                case MINUS: return "Kurangi";
                case TRASH: return "Hapus";
                case EDIT: return "Ubah";
                default: return null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = (getModel().isRollover() || getModel().isPressed()) && isEnabled();
            if (boxed) {
                g2.setColor(hover ? Theme.SURFACE_HOVER : Theme.SURFACE_ELEVATED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            } else if (hover) {
                g2.setColor(Theme.SURFACE_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            }
            g2.dispose();
            
            Graphics2D gi = (Graphics2D) g.create();
            gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gi.translate((getWidth() - iconSize) / 2, (getHeight() - iconSize) / 2);
            Icons.paint(gi, icon, iconSize, isEnabled() ? color : Theme.FAINT);
            gi.dispose();
        }
    }

    public static class ToggleButton extends JButton {
        private final Color activeFill;
        private boolean active;

        public ToggleButton(String text, Color activeFill) {
            super(text);
            this.activeFill = activeFill;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(Theme.text(Font.BOLD, 12));
            setBorder(new EmptyBorder(8, 16, 8, 16));
        }

        public void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        public boolean isActive() {
            return active;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (active) {
                g2.setColor(activeFill);
                g2.fillRoundRect(0, 0, w, h, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            } else {
                g2.setColor(getModel().isRollover() ? Theme.SURFACE_HOVER : Theme.SURFACE_ELEVATED);
                g2.fillRoundRect(0, 0, w, h, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            }
            g2.dispose();
            setForeground(active ? new Color(0x0A0C10) : Theme.MUTED);
            super.paintComponent(g);
        }
    }

    public static class ThemeToggleSwitch extends javax.swing.JComponent {
        private final java.awt.Window window;
        private Runnable onToggle;

        public ThemeToggleSwitch(java.awt.Window window) {
            this.window = window;
            setPreferredSize(new Dimension(58, 28));
            setMaximumSize(new Dimension(58, 28));
            setMinimumSize(new Dimension(58, 28));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setToolTipText(Theme.isDark() ? "Beralih ke Mode Terang (Light)" : "Beralih ke Mode Gelap (Dark)");

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Theme.toggle(window);
                    setToolTipText(Theme.isDark() ? "Beralih ke Mode Terang (Light)" : "Beralih ke Mode Gelap (Dark)");
                    if (onToggle != null) {
                        onToggle.run();
                    }
                    repaint();
                }
            });
        }

        public void setOnToggle(Runnable onToggle) {
            this.onToggle = onToggle;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean dark = Theme.isDark();

            // Track Pill
            g2.setColor(Theme.SURFACE_ELEVATED);
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(Theme.BORDER_STRONG);
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

            int pad = 3;
            int knobSize = h - (pad * 2);

            int sunX = pad + (knobSize / 2);
            int sunY = h / 2;
            int moonX = w - pad - (knobSize / 2);
            int moonY = h / 2;

            // Inactive icons
            if (dark) {
                g2.setColor(Theme.FAINT);
                paintSunIcon(g2, sunX, sunY, 11);
            } else {
                g2.setColor(Theme.FAINT);
                paintMoonIcon(g2, moonX, moonY, 11);
            }

            // Sliding Active Knob
            int knobX = dark ? (w - knobSize - pad) : pad;
            int knobY = pad;

            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillOval(knobX + 1, knobY + 1, knobSize, knobSize);

            if (dark) {
                g2.setColor(Theme.ACCENT);
                g2.fillOval(knobX, knobY, knobSize, knobSize);
                g2.setColor(new Color(0x0A0C10));
                paintMoonIcon(g2, knobX + (knobSize / 2), knobY + (knobSize / 2), 11);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillOval(knobX, knobY, knobSize, knobSize);
                g2.setColor(Theme.BORDER_STRONG);
                g2.drawOval(knobX, knobY, knobSize - 1, knobSize - 1);
                g2.setColor(Theme.ACCENT);
                paintSunIcon(g2, knobX + (knobSize / 2), knobY + (knobSize / 2), 11);
            }

            g2.dispose();
        }

        private void paintSunIcon(Graphics2D g2, int cx, int cy, int size) {
            int r = size / 3;
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int rayLen = size / 2;
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0;
                int x1 = (int) Math.round(cx + Math.cos(angle) * (r + 1));
                int y1 = (int) Math.round(cy + Math.sin(angle) * (r + 1));
                int x2 = (int) Math.round(cx + Math.cos(angle) * rayLen);
                int y2 = (int) Math.round(cy + Math.sin(angle) * rayLen);
                g2.drawLine(x1, y1, x2, y2);
            }
        }

        private void paintMoonIcon(Graphics2D g2, int cx, int cy, int size) {
            int r = size / 2;
            java.awt.geom.Area moon = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            moon.subtract(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(cx - r + (r * 0.65), cy - r - (r * 0.3), r * 1.7, r * 1.7)));
            g2.fill(moon);
        }
    }

    public static class NavItem extends JButton {
        private final Icons.Name icon;
        private final boolean alignLeft;
        private boolean active;

        public NavItem(String text, Icons.Name icon) {
            this(text, icon, false);
        }

        public NavItem(String text, Icons.Name icon, boolean alignLeft) {
            super(text);
            this.icon = icon;
            this.alignLeft = alignLeft;
            setHorizontalAlignment(alignLeft ? SwingConstants.LEFT : SwingConstants.CENTER);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(Theme.text(Font.PLAIN, 13));
            setBorder(new EmptyBorder(0, 16, 0, 16));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    repaint();
                }
            });
        }

        public void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 26, 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            
            if (active) {
                // Active surface fill
                g2.setColor(Theme.SURFACE_ELEVATED);
                g2.fillRoundRect(0, 0, w, h, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
                g2.setColor(Theme.BORDER_STRONG);
                g2.drawRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
                
                // Left Amber indicator bar
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(2, 6, 3, h - 12, 2, 2);
            } else if (getModel().isRollover()) {
                g2.setColor(Theme.SURFACE_HOVER);
                g2.fillRoundRect(0, 0, w, h, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            }
            g2.dispose();

            Color fg = active ? Theme.ACCENT : (getModel().isRollover() ? Theme.INK : Theme.MUTED);
            Font font = Theme.text(active ? Font.BOLD : Font.PLAIN, 13);
            FontMetrics fm = getFontMetrics(font);
            int iconSize = 17;
            int gap = 10;
            int contentW = iconSize + gap + fm.stringWidth(getText());
            int startX = alignLeft ? 16 : Math.max(12, (w - contentW) / 2);
            int iconY = (h - iconSize) / 2;

            Graphics2D gi = (Graphics2D) g.create();
            gi.translate(startX, iconY);
            Icons.paint(gi, icon, iconSize, fg);
            gi.dispose();

            Graphics2D gt = (Graphics2D) g.create();
            gt.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gt.setColor(fg);
            gt.setFont(font);
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            gt.drawString(getText(), startX + iconSize + gap, textY);
            gt.dispose();

            if (isFocusOwner()) {
                Graphics2D gf = (Graphics2D) g.create();
                gf.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gf.setColor(Theme.alpha(Theme.ACCENT, 120));
                gf.drawRoundRect(1, 1, w - 3, h - 3, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
                gf.dispose();
            }
        }
    }

    public static class AppTable extends JTable {
        private int hoverRow = -1;
        private String emptyTitle = "Belum ada data";
        private String emptyHint = "Data akan tampil di sini setelah ditambahkan.";

        public AppTable(DefaultTableModel model) {
            super(model);
            setRowHeight(48);
            setFillsViewportHeight(true);
            setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            setShowVerticalLines(false);
            setShowHorizontalLines(true);
            setGridColor(Theme.BORDER);
            setIntercellSpacing(new Dimension(0, 1));
            setBorder(null);
            setBackground(Theme.SURFACE);
            setSelectionBackground(Theme.SURFACE_MUTED);
            setSelectionForeground(Theme.INK);
            setFont(Theme.text(Font.PLAIN, 13));
            getTableHeader().setReorderingAllowed(false);
            styleHeader();
            setDefaultRenderer(Object.class, new PaddedRenderer(SwingConstants.LEFT));

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int row = rowAtPoint(e.getPoint());
                    if (row != hoverRow) {
                        hoverRow = row;
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoverRow != -1) {
                        hoverRow = -1;
                        repaint();
                    }
                }
            });
        }

        public void setEmptyState(String title, String hint) {
            this.emptyTitle = title;
            this.emptyHint = hint;
            repaint();
        }

        private void styleHeader() {
            JTableHeader header = getTableHeader();
            header.setFont(Theme.text(Font.BOLD, 11));
            header.setBackground(Theme.SURFACE_ELEVATED);
            header.setForeground(Theme.FAINT);
            header.setPreferredSize(new Dimension(100, 42));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    label.setOpaque(true);
                    label.setBackground(Theme.SURFACE_ELEVATED);
                    label.setForeground(Theme.FAINT);
                    label.setFont(Theme.text(Font.BOLD, 10));
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    label.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                            new EmptyBorder(0, 14, 0, 14)));
                    label.setText(value == null ? "" : value.toString().toUpperCase());
                    return label;
                }
            });
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            if (!isRowSelected(row)) {
                c.setBackground(row == hoverRow ? Theme.SURFACE_HOVER : Theme.SURFACE);
                c.setForeground(Theme.INK);
            } else {
                c.setBackground(Theme.SURFACE_MUTED);
                c.setForeground(Theme.ACCENT);
            }
            return c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = Math.max(90, getHeight() / 2);

                g2.setColor(Theme.SURFACE_ELEVATED);
                g2.fillOval(cx - 24, cy - 64, 48, 48);
                g2.setColor(Theme.BORDER_STRONG);
                g2.drawOval(cx - 24, cy - 64, 48, 48);
                
                g2.setColor(Theme.FAINT);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 8, cy - 42, cx + 8, cy - 42);
                g2.drawLine(cx - 8, cy - 36, cx + 4, cy - 36);

                g2.setFont(Theme.text(Font.BOLD, 14));
                g2.setColor(Theme.INK_SOFT);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(emptyTitle, cx - fm.stringWidth(emptyTitle) / 2, cy - 2);

                g2.setFont(Theme.text(Font.PLAIN, 12));
                g2.setColor(Theme.MUTED);
                fm = g2.getFontMetrics();
                g2.drawString(emptyHint, cx - fm.stringWidth(emptyHint) / 2, cy + 18);
                g2.dispose();
            }
        }
    }

    public static class PaddedRenderer extends DefaultTableCellRenderer {
        private final int alignment;
        private final boolean bold;

        public PaddedRenderer(int alignment) {
            this(alignment, false);
        }

        public PaddedRenderer(int alignment, boolean bold) {
            this.alignment = alignment;
            this.bold = bold;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(alignment);
            label.setFont(Theme.text(bold ? Font.BOLD : Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(0, 14, 0, 14));
            return label;
        }
    }

    public static class BadgeLabel extends JLabel {
        private Color pill = Theme.NEUTRAL_BG;

        public BadgeLabel() {
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(Theme.text(Font.BOLD, 11));
        }

        public void setPill(Color pill) {
            this.pill = pill;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = Math.min(getHeight() - 14, 24);
            int textW = getFontMetrics(getFont()).stringWidth(getText());
            int w = Math.min(getWidth() - 12, textW + 20);
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;
            g2.setColor(pill);
            g2.fillRoundRect(x, y, w, h, 6, 6);
            g2.setColor(new Color(255, 255, 255, 15));
            g2.drawRoundRect(x, y, w - 1, h - 1, 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class BadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            BadgeLabel label = new BadgeLabel();
            label.setText(value == null ? "" : value.toString());
            Color[] colors = pillFor(value == null ? "" : value.toString());
            label.setPill(colors[0]);
            label.setForeground(colors[1]);
            return label;
        }
    }

    public static Color[] pillFor(String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        switch (s) {
            case "MENUNGGU":
            case "MENIPIS":
            case "JASA":
                return new Color[]{Theme.AMBER_BG, Theme.AMBER_FG};
            case "PROSES":
            case "BARANG":
                return new Color[]{Theme.BLUE_BG, Theme.BLUE_FG};
            case "SELESAI":
            case "AMAN":
            case "AKTIF":
                return new Color[]{Theme.GREEN_BG, Theme.GREEN_FG};
            case "DIAMBIL":
            case "NONAKTIF":
                return new Color[]{Theme.NEUTRAL_BG, Theme.NEUTRAL_FG};
            case "HABIS":
                return new Color[]{Theme.DANGER_SOFT, Theme.DANGER};
            default:
                return new Color[]{Theme.NEUTRAL_BG, Theme.NEUTRAL_FG};
        }
    }

    public static JTextField textField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setFont(Theme.text(Font.PLAIN, 13));
        field.setBackground(Theme.SURFACE_ELEVATED);
        field.setForeground(Theme.INK);
        field.setCaretColor(Theme.ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(0, 42));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.ACCENT),
                        new EmptyBorder(8, 12, 8, 12)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER),
                        new EmptyBorder(8, 12, 8, 12)));
            }
        });
        return field;
    }

    public static JPasswordField passwordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setFont(Theme.text(Font.PLAIN, 13));
        field.setBackground(Theme.SURFACE_ELEVATED);
        field.setForeground(Theme.INK);
        field.setCaretColor(Theme.ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(0, 42));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.ACCENT),
                        new EmptyBorder(8, 12, 8, 12)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER),
                        new EmptyBorder(8, 12, 8, 12)));
            }
        });
        return field;
    }

    public static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.text(Font.BOLD, 12));
        label.setForeground(Theme.INK_SOFT);
        return label;
    }

    public static JLabel valueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.text(Font.PLAIN, 13));
        label.setForeground(Theme.INK);
        return label;
    }

    public static JCheckBox checkBox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setOpaque(false);
        box.setFont(Theme.text(Font.PLAIN, 12));
        box.setForeground(Theme.INK_SOFT);
        box.setFocusPainted(false);
        return box;
    }
}
