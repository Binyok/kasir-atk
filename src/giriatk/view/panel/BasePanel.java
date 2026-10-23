package giriatk.view.panel;

import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;

public abstract class BasePanel extends JPanel {

    protected BasePanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public void onShow() {
    }

    protected JPanel pageHeader(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 2, 16, 2));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.display(Font.BOLD, 22));
        heading.setForeground(Theme.INK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(heading);

        if (subtitle != null && !subtitle.isBlank()) {
            JLabel sub = new JLabel(subtitle);
            sub.setFont(Theme.text(Font.PLAIN, 13));
            sub.setForeground(Theme.MUTED);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(Box.createVerticalStrut(3));
            texts.add(sub);
        }
        panel.add(texts, BorderLayout.WEST);
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
}
