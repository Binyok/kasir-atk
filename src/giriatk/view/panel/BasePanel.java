package giriatk.view.panel;

import giriatk.util.Fmt;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public abstract class BasePanel extends JPanel {

    protected BasePanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public void onShow() {
    }

    protected JPanel pageHeader(String title, String subtitle) {
        return pageHeader(title, subtitle, false);
    }

    protected JPanel pageHeader(String title, String subtitle, boolean withDate) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(0, 0, 20, 0),
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.INK)));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.display(Font.BOLD, 30));
        heading.setForeground(Theme.INK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(heading);

        if (subtitle != null && !subtitle.isBlank()) {
            JLabel sub = new JLabel(subtitle.toUpperCase(Locale.of("id", "ID")));
            sub.setFont(Theme.mono(Font.PLAIN, 10));
            sub.setForeground(Theme.MUTED);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(Box.createVerticalStrut(4));
            texts.add(sub);
        }
        panel.add(texts, BorderLayout.WEST);

        if (withDate) {
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.setBorder(new EmptyBorder(0, 0, 4, 2));
            wrap.add(new DateBox(), BorderLayout.EAST);
            panel.add(wrap, BorderLayout.EAST);
        }
        return panel;
    }

    protected UiKit.SurfacePanel card(LayoutManager layout) {
        return new UiKit.SurfacePanel(layout);
    }

    protected JComponent placeholder(String message) {
        UiKit.SurfacePanel surface = new UiKit.SurfacePanel(new BorderLayout());
        surface.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        JLabel label = new JLabel(message);
        label.setFont(Theme.text(Font.PLAIN, 13));
        label.setForeground(Theme.MUTED);
        label.setHorizontalAlignment(JLabel.CENTER);
        surface.add(label, BorderLayout.CENTER);
        surface.setPreferredSize(new Dimension(100, 200));
        return surface;
    }

    private static final class DateBox extends JPanel {
        DateBox() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(0, 0, 3, 3));

            DateTimeFormatter day = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.of("id", "ID"));
            DateTimeFormatter time = DateTimeFormatter.ofPattern("HH.mm", Locale.ROOT);
            JLabel label = new JLabel((day.format(LocalDate.now()) + " · " + time.format(LocalTime.now())).toUpperCase());
            label.setFont(Theme.mono(Font.PLAIN, 12));
            label.setForeground(Theme.INK);
            label.setBorder(new EmptyBorder(8, 12, 8, 12));
            label.setOpaque(false);
            add(label, BorderLayout.CENTER);
            setPreferredSize(new Dimension(label.getPreferredSize().width + 30, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.INK);
            g2.fillRect(3, 3, getWidth() - 3, getHeight() - 3);
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, getWidth() - 3, getHeight() - 3);
            g2.setColor(Theme.INK);
            g2.drawRect(0, 0, getWidth() - 4, getHeight() - 4);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
