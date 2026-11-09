package giriatk.view.component;

import giriatk.model.BarisLaporan;
import giriatk.util.Fmt;
import giriatk.util.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class TrendChart extends JPanel {

    private List<BarisLaporan> data = List.of();
    private int hoverIndex = -1;
    private int[] xs;
    private int[] bars;

    public TrendChart() {
        setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = cariIndex(e.getX());
                if (idx != hoverIndex) {
                    hoverIndex = idx;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverIndex != -1) {
                    hoverIndex = -1;
                    repaint();
                }
            }
        };
        addMouseMotionListener(mouse);
        addMouseListener(mouse);
    }

    public void setData(List<BarisLaporan> data) {
        this.data = data == null ? List.of() : data;
        hoverIndex = -1;
        repaint();
    }

    private int cariIndex(int mouseX) {
        if (xs == null || bars == null) {
            return -1;
        }
        for (int i = 0; i < xs.length; i++) {
            if (mouseX >= xs[i] - bars[i] / 2 && mouseX <= xs[i] + bars[i] / 2) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int left = 14;
        int right = w - 14;
        int top = 26;
        int bottom = h - 34;

        if (data.isEmpty()) {
            g2.setColor(Theme.MUTED);
            g2.setFont(Theme.text(Font.PLAIN, 12));
            String msg = "Belum ada data transaksi pada periode ini.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            g2.dispose();
            return;
        }

        double max = 0;
        for (BarisLaporan b : data) {
            max = Math.max(max, b.getOmzet().doubleValue());
        }
        if (max <= 0) {
            max = 1;
        }

        boolean kosong = true;
        for (BarisLaporan b : data) {
            if (b.getOmzet().doubleValue() > 0) {
                kosong = false;
                break;
            }
        }

        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3, 4}, 0));
        for (int i = 1; i <= 3; i++) {
            int y = bottom - (int) Math.round((bottom - top) * (i / 4.0));
            g2.setColor(Theme.BORDER);
            g2.drawLine(left, y, right, y);
        }

        g2.setColor(Theme.INK);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(left, bottom, right, bottom);

        int n = data.size();
        double slot = (right - left) / (double) n;
        int bw = (int) Math.max(6, Math.min(44, slot * 0.56));
        xs = new int[n];
        bars = new int[n];
        boolean rapat = n > 14;

        g2.setFont(Theme.mono(Font.BOLD, 10));
        for (int i = 0; i < n; i++) {
            BarisLaporan b = data.get(i);
            xs[i] = (int) Math.round(left + slot * i + slot / 2);
            bars[i] = bw;
            double ratio = b.getOmzet().doubleValue() / max;
            int bh = (int) Math.max(kosong ? 0 : 3, Math.round((bottom - top) * ratio));
            int x = xs[i] - bw / 2;
            int y = bottom - bh;
            boolean today = i == n - 1;
            boolean hover = i == hoverIndex;

            if (!kosong) {
                Color fill = hover || today ? Theme.ACCENT : Theme.INK;
                g2.setColor(fill);
                g2.fillRect(x, y, bw, bh);

                if (!rapat || hover || today) {
                    String val = ringkas(b.getOmzet().doubleValue());
                    g2.setColor(today ? Theme.ACCENT : Theme.INK);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(val, xs[i] - fm.stringWidth(val) / 2, y - 8);
                }
            }

            if (!rapat || i % 2 == 0 || today) {
                String label = labelTanggal(b.getLabel());
                g2.setFont(Theme.mono(today ? Font.BOLD : Font.PLAIN, 9));
                g2.setColor(today ? Theme.ACCENT : Theme.MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, xs[i] - fm.stringWidth(label) / 2, bottom + 18);
                g2.setFont(Theme.mono(Font.BOLD, 10));
            }
        }

        if (kosong) {
            String msg = "Belum ada omzet tercatat pada periode ini.";
            g2.setFont(Theme.text(Font.PLAIN, 12));
            g2.setColor(Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, top + (bottom - top) / 2);
        }

        if (!kosong && hoverIndex >= 0 && hoverIndex < n) {
            gambarTooltip(g2, hoverIndex, left, right, top, bottom);
        }

        g2.dispose();
    }

    private void gambarTooltip(Graphics2D g2, int index, int left, int right, int top, int bottom) {
        BarisLaporan b = data.get(index);
        String nilai = Fmt.rupiah(b.getOmzet());
        String tanggal = labelTanggal(b.getLabel()) + " · " + b.getJumlahTransaksi() + " transaksi";

        g2.setFont(Theme.mono(Font.BOLD, 12));
        FontMetrics fmNilai = g2.getFontMetrics();
        g2.setFont(Theme.mono(Font.PLAIN, 9));
        FontMetrics fmTanggal = g2.getFontMetrics();

        int wBox = Math.max(fmNilai.stringWidth(nilai), fmTanggal.stringWidth(tanggal)) + 24;
        int hBox = 46;
        int x = xs[index] - wBox / 2;
        x = Math.max(left, Math.min(x, right - wBox));
        int y = Math.max(top, bottom - hBox - 30);

        g2.setColor(Theme.INK);
        g2.fill(new RoundRectangle2D.Double(x + 3, y + 3, wBox, hBox, 0, 0));
        g2.setColor(Theme.SURFACE);
        g2.fill(new RoundRectangle2D.Double(x, y, wBox, hBox, 0, 0));
        g2.setColor(Theme.INK);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(x, y, wBox, hBox, 0, 0));

        g2.setColor(Theme.ACCENT);
        g2.setFont(Theme.mono(Font.BOLD, 12));
        g2.drawString(nilai, x + 12, y + 18);

        g2.setColor(Theme.MUTED);
        g2.setFont(Theme.mono(Font.PLAIN, 9));
        g2.drawString(tanggal, x + 12, y + 34);
    }

    private String ringkas(double value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000).replace(".0M", "M");
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.0fK", value / 1_000);
        }
        return String.valueOf((long) value);
    }

    private String labelTanggal(String value) {
        if (value == null) {
            return "-";
        }
        try {
            LocalDate d = LocalDate.parse(value);
            return String.format(Locale.ROOT, "%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
        } catch (Exception e) {
            return value.length() > 8 ? value.substring(0, 8) : value;
        }
    }
}
