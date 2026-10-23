package giriatk.view.component;

import giriatk.model.BarisLaporan;
import giriatk.util.Fmt;
import giriatk.util.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class TrendChart extends JPanel {

    private List<BarisLaporan> data = List.of();
    private int hoverIndex = -1;

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

    private int padLeft() {
        return 64;
    }

    private int padRight() {
        return 24;
    }

    private int padTop() {
        return 28;
    }

    private int padBottom() {
        return 32;
    }

    private int[] xs;
    private double[] ys;

    private int cariIndex(int mouseX) {
        if (xs == null || xs.length == 0) {
            return -1;
        }
        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            int d = Math.abs(mouseX - xs[i]);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return bestDist <= 60 ? best : -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int left = padLeft();
        int right = w - padRight();
        int top = padTop();
        int bottom = h - padBottom();

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
        max = niceCeil(max);

        // Grid + Y-Axis Labels
        g2.setFont(Theme.mono(Font.PLAIN, 10));
        int garis = 4;
        for (int i = 0; i <= garis; i++) {
            int y = bottom - (int) Math.round((bottom - top) * (i / (double) garis));
            g2.setColor(Theme.BORDER);
            g2.drawLine(left, y, right, y);
            
            g2.setColor(Theme.FAINT);
            String label = ringkas(max * i / garis);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, left - 10 - fm.stringWidth(label), y + 4);
        }

        int n = data.size();
        xs = new int[n];
        ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = n == 1 ? (left + right) / 2
                    : left + (int) Math.round((right - left) * (i / (double) (n - 1)));
            double ratio = data.get(i).getOmzet().doubleValue() / max;
            ys[i] = bottom - ratio * (bottom - top);
        }

        // Area Fill + Smooth Curve
        Path2D line = new Path2D.Double();
        line.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            double x0 = xs[i - 1];
            double y0 = ys[i - 1];
            double x1 = xs[i];
            double y1 = ys[i];
            double cx = (x1 - x0) / 3.0;
            line.curveTo(x0 + cx, y0, x1 - cx, y1, x1, y1);
        }

        Path2D area = (Path2D) line.clone();
        area.lineTo(xs[n - 1], bottom);
        area.lineTo(xs[0], bottom);
        area.closePath();

        // Dark Amber Gradient Glow
        g2.setPaint(new GradientPaint(0, top, Theme.alpha(Theme.ACCENT, 90), 0, bottom, Theme.alpha(Theme.ACCENT, 0)));
        g2.fill(area);

        // Main Curve Line
        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);

        // X-Axis Date Labels
        g2.setFont(Theme.mono(Font.PLAIN, 10));
        boolean banyak = n > 12;
        for (int i = 0; i < n; i++) {
            if (banyak && i % 2 != 0) {
                continue;
            }
            String label = labelTanggal(data.get(i).getLabel());
            g2.setColor(Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, xs[i] - fm.stringWidth(label) / 2, bottom + 18);
        }

        // Data Dots
        for (int i = 0; i < n; i++) {
            boolean aktif = i == hoverIndex;
            int r = aktif ? 6 : 3;
            g2.setColor(Theme.SURFACE);
            g2.fill(new Ellipse2D.Double(xs[i] - r - 2, ys[i] - r - 2, (r + 2) * 2, (r + 2) * 2));
            g2.setColor(aktif ? Theme.INK : Theme.ACCENT);
            g2.fill(new Ellipse2D.Double(xs[i] - r, ys[i] - r, r * 2, r * 2));
            if (aktif) {
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(xs[i] - r - 2, ys[i] - r - 2, (r + 2) * 2, (r + 2) * 2));
            }
        }

        // Hover Vertical Guide Line + Tooltip
        if (hoverIndex >= 0 && hoverIndex < n) {
            g2.setColor(Theme.alpha(Theme.INK, 60));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
            g2.drawLine(xs[hoverIndex], top, xs[hoverIndex], bottom);
            gambarTooltip(g2, hoverIndex, left, right, top);
        }

        g2.dispose();
    }

    private void gambarTooltip(Graphics2D g2, int index, int left, int right, int top) {
        BarisLaporan b = data.get(index);
        String nilai = Fmt.rupiah(b.getOmzet());
        String tanggal = labelTanggal(b.getLabel()) + " • " + b.getJumlahTransaksi() + " Transaksi";

        g2.setFont(Theme.display(Font.BOLD, 13));
        FontMetrics fmNilai = g2.getFontMetrics();
        g2.setFont(Theme.mono(Font.PLAIN, 10));
        FontMetrics fmTanggal = g2.getFontMetrics();

        int wBox = Math.max(fmNilai.stringWidth(nilai), fmTanggal.stringWidth(tanggal)) + 26;
        int hBox = 50;
        int x = xs[index] - wBox / 2;
        x = Math.max(left, Math.min(x, right - wBox));
        int y = Math.max(top, (int) ys[index] - hBox - 16);

        // Tooltip container
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fill(new RoundRectangle2D.Double(x + 1, y + 2, wBox, hBox, 8, 8));
        g2.setColor(Theme.SURFACE_ELEVATED);
        g2.fill(new RoundRectangle2D.Double(x, y, wBox, hBox, 8, 8));
        g2.setColor(Theme.BORDER_STRONG);
        g2.draw(new RoundRectangle2D.Double(x, y, wBox, hBox, 8, 8));

        g2.setColor(Theme.ACCENT);
        g2.setFont(Theme.display(Font.BOLD, 13));
        g2.drawString(nilai, x + 12, y + 20);

        g2.setColor(Theme.MUTED);
        g2.setFont(Theme.mono(Font.PLAIN, 10));
        g2.drawString(tanggal, x + 12, y + 38);
    }

    private double niceCeil(double value) {
        if (value <= 0) {
            return 1;
        }
        double mag = Math.pow(10, Math.floor(Math.log10(value)));
        double norm = value / mag;
        double step;
        if (norm <= 1) {
            step = 1;
        } else if (norm <= 2) {
            step = 2;
        } else if (norm <= 2.5) {
            step = 2.5;
        } else if (norm <= 5) {
            step = 5;
        } else {
            step = 10;
        }
        return step * mag;
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
