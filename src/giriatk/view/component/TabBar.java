package giriatk.view.component;

import giriatk.util.Theme;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TabBar extends JPanel {

    private final List<String> keys = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final List<int[]> bounds = new ArrayList<>();
    private String active;
    private Consumer<String> onChange;

    public TabBar() {
        setOpaque(false);
        setLayout(null);
        setPreferredSize(new Dimension(520, 44));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                klik(e.getX());
            }
        });
    }

    public void addTab(String key, String label) {
        keys.add(key);
        labels.add(label);
        if (active == null) {
            active = key;
        }
        repaint();
    }

    public void setActive(String key) {
        this.active = key;
        repaint();
    }

    public String getActive() {
        return active;
    }

    public void setOnChange(Consumer<String> listener) {
        this.onChange = listener;
    }

    private void klik(int x) {
        for (int i = 0; i < bounds.size(); i++) {
            int[] b = bounds.get(i);
            if (x >= b[0] && x < b[0] + b[1]) {
                String key = keys.get(i);
                if (!key.equals(active)) {
                    active = key;
                    repaint();
                    if (onChange != null) {
                        onChange.accept(key);
                    }
                }
                return;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font normal = Theme.text(Font.PLAIN, 13);
        Font bold = Theme.text(Font.BOLD, 13);
        FontMetrics base = g2.getFontMetrics(normal);

        bounds.clear();
        int x = 0;
        int h = getHeight();
        for (int i = 0; i < labels.size(); i++) {
            String key = keys.get(i);
            String label = labels.get(i);
            boolean aktif = key.equals(active);
            int w = base.stringWidth(label) + 34;
            bounds.add(new int[]{x, w});

            g2.setFont(aktif ? bold : normal);
            g2.setColor(aktif ? Theme.ACCENT : Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent() - 2;
            g2.drawString(label, x + 17, ty);

            if (aktif) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(x + 12, h - 7, w - 24, 3, 3, 3);
            }
            x += w;
        }

        g2.setColor(Theme.BORDER);
        g2.drawLine(0, h - 1, getWidth(), h - 1);
        g2.dispose();
    }
}
