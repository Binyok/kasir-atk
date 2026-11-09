package giriatk.util;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
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
        private final boolean shadow;

        public SurfacePanel(LayoutManager layout) {
            this(layout, Theme.RADIUS_CARD, false);
        }

        public SurfacePanel(LayoutManager layout, int arc, boolean shadow) {
            super(layout);
            this.shadow = shadow;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int off = shadow ? 4 : 0;
            if (shadow) {
                g2.setColor(Theme.INK);
                g2.fillRect(off, off, getWidth() - off, getHeight() - off);
            }
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, getWidth() - off, getHeight() - off);
            g2.setColor(shadow ? Theme.INK : Theme.BORDER);
            g2.drawRect(0, 0, getWidth() - off - 1, getHeight() - off - 1);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public enum Variant { PRIMARY, INK, GHOST, DANGER, SUBTLE }

    public static class SoftButton extends JButton {
        private final Variant variant;

        public SoftButton(String text, Variant variant) {
            super(text == null ? "" : text.toUpperCase());
            this.variant = variant;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(Theme.text(Font.BOLD, 11));
            setBorder(new EmptyBorder(12, 20, 13, 20));
            setForeground(Theme.INK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean roll = getModel().isRollover() && isEnabled();
            boolean press = getModel().isPressed() && isEnabled();
            int shift = press ? 1 : 0;
            int w = getWidth();
            int h = getHeight();
            boolean hard = variant == Variant.PRIMARY;
            int off = hard ? 3 : 0;

            Color bg;
            Color fg;
            Color border;

            switch (variant) {
                case PRIMARY:
                    bg = press ? Theme.ACCENT_PRESSED : (roll ? Theme.ACCENT_HOVER : Theme.ACCENT);
                    fg = Theme.SURFACE;
                    border = Theme.INK;
                    break;
                case INK:
                    bg = press ? Theme.INK : (roll ? Theme.INK_SOFT : Theme.INK);
                    fg = Theme.CANVAS;
                    border = Theme.INK;
                    break;
                case GHOST:
                    bg = roll ? Theme.SURFACE_ELEVATED : Theme.SURFACE;
                    fg = Theme.INK;
                    border = Theme.INK;
                    break;
                case DANGER:
                    bg = press ? Theme.DANGER_HOVER : (roll ? Theme.DANGER_SOFT : Theme.SURFACE);
                    fg = Theme.DANGER;
                    border = Theme.DANGER;
                    break;
                case SUBTLE:
                default:
                    bg = roll ? Theme.SURFACE_HOVER : Theme.SURFACE_ELEVATED;
                    fg = Theme.INK_SOFT;
                    border = Theme.BORDER_STRONG;
                    break;
            }

            if (!isEnabled()) {
                bg = Theme.SURFACE_ELEVATED;
                fg = Theme.FAINT;
                border = Theme.BORDER;
            }

            if (hard && isEnabled()) {
                g2.setColor(Theme.INK);
                g2.fillRect(off, off, w - off, h - off);
            }
            g2.setColor(bg);
            g2.fillRect(shift, shift, w - off - shift, h - off - shift);
            g2.setColor(border);
            g2.drawRect(shift, shift, w - off - shift - 1, h - off - shift - 1);
            if (isFocusOwner()) {
                g2.setColor(Theme.ACCENT);
                g2.drawRect(shift + 2, shift + 2, w - off - shift - 5, h - off - shift - 5);
            }
            g2.dispose();

            setForeground(fg);
            Graphics gShift = g.create();
            gShift.translate(shift, shift);
            super.paintComponent(gShift);
            gShift.dispose();
        }
    }

    public static class IconButton extends JButton {
        private Icons.Name icon;
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
                case CHEVRON_LEFT: return "Ciutkan panel";
                case CHEVRON_RIGHT: return "Bentangkan panel";
                default: return null;
            }
        }

        public void setIconName(Icons.Name icon) {
            this.icon = icon;
            setToolTipText(tooltip(icon));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = (getModel().isRollover() || getModel().isPressed()) && isEnabled();
            if (boxed) {
                g2.setColor(hover ? Theme.SURFACE_ELEVATED : Theme.SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.INK);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            } else if (hover) {
                g2.setColor(Theme.SURFACE_HOVER);
                g2.fillRect(0, 0, getWidth(), getHeight());
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
            setFont(Theme.text(Font.BOLD, 11));
            setBorder(new EmptyBorder(9, 16, 9, 16));
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
                g2.fillRect(0, 0, w, h);
                g2.setColor(Theme.INK);
                g2.drawRect(0, 0, w - 1, h - 1);
            } else {
                g2.setColor(getModel().isRollover() ? Theme.SURFACE_HOVER : Theme.SURFACE);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Theme.BORDER_STRONG);
                g2.drawRect(0, 0, w - 1, h - 1);
            }
            g2.dispose();
            setForeground(active ? Theme.SURFACE : Theme.MUTED);
            super.paintComponent(g);
        }
    }

    public static class NavItem extends JButton {
        private final Icons.Name icon;
        private final boolean alignLeft;
        private boolean active;
        private boolean rail;

        public NavItem(String text, Icons.Name icon) {
            this(text, icon, false);
        }

        public NavItem(String text, Icons.Name icon, boolean alignLeft) {
            super(text);
            this.icon = icon;
            this.alignLeft = alignLeft;
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(Theme.text(Font.PLAIN, 13));
            setBorder(new EmptyBorder(0, 10, 0, 8));
            setToolTipText(text);
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

        public void setRail(boolean rail) {
            this.rail = rail;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 20, 38);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (active) {
                g2.setColor(Theme.CANVAS);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Theme.ACCENT);
                g2.fillRect(0, 0, 4, h);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(250, 246, 239, 18));
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();

            Color fg = active ? Theme.INK : Theme.SIDEBAR_TEXT;
            Font font = Theme.text(active ? Font.BOLD : Font.PLAIN, 13);
            FontMetrics fm = getFontMetrics(font);
            int iconSize = 17;
            int gap = 11;
            int textW = rail ? 0 : fm.stringWidth(getText());
            int contentW = iconSize + (rail ? 0 : gap + textW);
            int startX = rail || alignLeft ? (rail ? (w - iconSize) / 2 : 10) : Math.max(12, (w - contentW) / 2);
            if (!rail && !alignLeft) {
                startX = Math.max(12, (w - contentW) / 2);
            }
            int iconY = (h - iconSize) / 2;

            Graphics2D gi = (Graphics2D) g.create();
            gi.translate(startX, iconY);
            Icons.paint(gi, icon, iconSize, fg);
            gi.dispose();

            if (!rail) {
                Graphics2D gt = (Graphics2D) g.create();
                gt.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gt.setColor(fg);
                gt.setFont(font);
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                gt.drawString(getText(), startX + iconSize + gap, textY);
                gt.dispose();
            }
        }
    }

    public static class AppTable extends JTable {
        private int hoverRow = -1;
        private String emptyTitle = "Belum ada data";
        private String emptyHint = "Data akan tampil di sini setelah ditambahkan.";

        public AppTable(DefaultTableModel model) {
            super(model);
            setRowHeight(46);
            setFillsViewportHeight(true);
            setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            setShowVerticalLines(false);
            setShowHorizontalLines(true);
            setGridColor(Theme.ROW_LINE);
            setIntercellSpacing(new Dimension(0, 1));
            setBorder(null);
            setBackground(Theme.SURFACE);
            setSelectionBackground(Theme.SURFACE_ELEVATED);
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
                    setCursor(row >= 0
                            ? new Cursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoverRow != -1) {
                        hoverRow = -1;
                        repaint();
                    }
                    setCursor(Cursor.getDefaultCursor());
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
            header.setFont(Theme.text(Font.BOLD, 10));
            header.setBackground(Theme.SURFACE_ELEVATED);
            header.setForeground(Theme.MUTED);
            header.setPreferredSize(new Dimension(100, 40));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    label.setOpaque(true);
                    label.setBackground(Theme.SURFACE_ELEVATED);
                    label.setForeground(Theme.MUTED);
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
                c.setBackground(Theme.SURFACE_ELEVATED);
                c.setForeground(Theme.INK);
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

                g2.setColor(Theme.MUTED);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 8, cy - 42, cx + 8, cy - 42);
                g2.drawLine(cx - 8, cy - 36, cx + 4, cy - 36);

                g2.setFont(Theme.display(Font.BOLD, 16));
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
        private final boolean mono;

        public PaddedRenderer(int alignment) {
            this(alignment, false);
        }

        public PaddedRenderer(int alignment, boolean bold) {
            this(alignment, bold, false);
        }

        public PaddedRenderer(int alignment, boolean bold, boolean mono) {
            this.alignment = alignment;
            this.bold = bold;
            this.mono = mono;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(alignment);
            label.setFont(mono ? Theme.mono(bold ? Font.BOLD : Font.PLAIN, 12)
                    : Theme.text(bold ? Font.BOLD : Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(0, 14, 0, 14));
            return label;
        }
    }

    public static class BadgeLabel extends JLabel {
        private Color pill = Theme.NEUTRAL_BG;

        public BadgeLabel() {
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(Theme.text(Font.BOLD, 10));
        }

        public void setPill(Color pill) {
            this.pill = pill;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = 20;
            int textW = getFontMetrics(getFont()).stringWidth(getText());
            int w = Math.min(getWidth() - 8, textW + 16);
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;
            g2.setColor(pill);
            g2.fillRoundRect(x, y, w, h, 5, 5);
            g2.setColor(Theme.mix(getForeground(), pill, 0.45f));
            g2.drawRoundRect(x, y, w - 1, h - 1, 5, 5);
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
        return textField(placeholder, false);
    }

    public static JTextField textField(String placeholder, boolean mono) {
        JTextField field = new JTextField();
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setFont(mono ? Theme.mono(Font.PLAIN, 13) : Theme.text(Font.PLAIN, 13));
        field.setBackground(Theme.SURFACE);
        field.setForeground(Theme.INK);
        field.setCaretColor(Theme.ACCENT);
        field.setSelectionColor(Theme.ACCENT_SOFT);
        field.setSelectedTextColor(Theme.INK);
        field.setBorder(idleBorder());
        field.setPreferredSize(new Dimension(0, 44));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(focusBorder());
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(idleBorder());
            }
        });
        return field;
    }

    public static JPasswordField passwordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setFont(Theme.mono(Font.PLAIN, 13));
        field.setBackground(Theme.SURFACE);
        field.setForeground(Theme.INK);
        field.setCaretColor(Theme.ACCENT);
        field.setSelectionColor(Theme.ACCENT_SOFT);
        field.setSelectedTextColor(Theme.INK);
        field.setEchoChar('\u2022');
        field.setBorder(idleBorder());
        field.setPreferredSize(new Dimension(0, 44));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(focusBorder());
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(idleBorder());
            }
        });
        return field;
    }

    private static javax.swing.border.Border idleBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_STRONG),
                new EmptyBorder(8, 11, 8, 11));
    }

    private static javax.swing.border.Border focusBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.ACCENT, 2),
                new EmptyBorder(7, 10, 7, 10));
    }

    public static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.text(Font.BOLD, 11));
        label.setForeground(Theme.MUTED);
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

    public static void dashedBottom(JComponent component) {
        component.setBorder(BorderFactory.createCompoundBorder(
                component.getBorder(),
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_STRONG)));
    }
}
