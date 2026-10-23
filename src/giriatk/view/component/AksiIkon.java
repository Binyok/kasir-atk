package giriatk.view.component;

import giriatk.util.Icons;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class AksiIkon {

    private AksiIkon() {
    }

    public static void pasang(JTable table, int kolom, Icons.Name icon, Color color) {
        table.getColumnModel().getColumn(kolom).setCellRenderer(renderer(icon, color));
        table.getColumnModel().getColumn(kolom).setMinWidth(46);
        table.getColumnModel().getColumn(kolom).setMaxWidth(54);
        table.getColumnModel().getColumn(kolom).setPreferredWidth(50);
    }

    public static DefaultTableCellRenderer renderer(Icons.Name icon, Color color) {
        return new DefaultTableCellRenderer() {
            private final IkonCell cell = new IkonCell(icon, color);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                return cell;
            }
        };
    }

    private static final class IkonCell extends JPanel {

        private final Icons.Name icon;
        private final Color color;

        IkonCell(Icons.Name icon, Color color) {
            this.icon = icon;
            this.color = color;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = 15;
            g2.translate((getWidth() - size) / 2, (getHeight() - size) / 2);
            Icons.paint(g2, icon, size, color);
            g2.dispose();
        }
    }
}
