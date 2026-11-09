package giriatk.view.component;

import giriatk.util.Icons;
import giriatk.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

public class TitleBar extends JPanel {

    private final JFrame frame;
    private final JLabel titleLabel = new JLabel();
    private Point pressScreen;
    private Point pressWindow;

    public TitleBar(JFrame frame, String title) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(0, 46));
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.INK));

        add(buildLeft(title), BorderLayout.WEST);
        add(buildButtons(), BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    toggleMaximize();
                    return;
                }
                pressScreen = e.getLocationOnScreen();
                pressWindow = frame.getLocation();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressScreen == null || frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                frame.setLocation(pressWindow.x + now.x - pressScreen.x,
                        pressWindow.y + now.y - pressScreen.y);
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);
    }

    private JPanel buildLeft(String title) {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        JLabel glyph = new JLabel();
        glyph.setOpaque(true);
        glyph.setBackground(Theme.ACCENT);
        glyph.setBorder(BorderFactory.createLineBorder(Theme.INK, 1));
        glyph.setPreferredSize(new Dimension(13, 13));
        left.add(glyph);

        JLabel label = new JLabel(title);
        label.setFont(Theme.display(Font.PLAIN, 15));
        label.setForeground(Theme.INK);
        left.add(label);
        titleLabel.setFont(Theme.display(Font.PLAIN, 15));
        titleLabel.setForeground(Theme.INK);
        titleLabel.setText(title);
        left.remove(label);
        left.add(titleLabel);

        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    toggleMaximize();
                    return;
                }
                pressScreen = e.getLocationOnScreen();
                pressWindow = frame.getLocation();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressScreen == null || frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                frame.setLocation(pressWindow.x + now.x - pressScreen.x,
                        pressWindow.y + now.y - pressScreen.y);
            }
        };
        left.addMouseListener(drag);
        left.addMouseMotionListener(drag);
        return left;
    }

    private JPanel buildButtons() {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        right.add(new WinButton(Icons.Name.MINIMIZE, false, e -> frame.setState(JFrame.ICONIFIED)));
        right.add(new WinButton(Icons.Name.MAXIMIZE, false, e -> toggleMaximize()));
        right.add(new WinButton(Icons.Name.CLOSE, true, e ->
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING))));
        return right;
    }

    public void setWindowTitle(String title) {
        titleLabel.setText(title);
    }

    private void toggleMaximize() {
        if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
            frame.setExtendedState(JFrame.NORMAL);
        } else {
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private static final class WinButton extends JButton {
        private final Icons.Name icon;
        private final boolean danger;

        WinButton(Icons.Name icon, boolean danger, java.awt.event.ActionListener action) {
            this.icon = icon;
            this.danger = danger;
            setPreferredSize(new Dimension(28, 28));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.CENTER);
            addActionListener(action);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover();
            if (hover) {
                g2.setColor(danger ? Theme.ACCENT : Theme.SURFACE_ELEVATED);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();

            Graphics2D gi = (Graphics2D) g.create();
            gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gi.translate((getWidth() - 15) / 2, (getHeight() - 15) / 2);
            Icons.paint(gi, icon, 15, hover && danger ? Theme.SURFACE : Theme.INK);
            gi.dispose();
        }
    }
}
