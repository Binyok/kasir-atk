package giriatk.view.panel;

import giriatk.dao.AntrianDao;
import giriatk.dao.BarangDao;
import giriatk.dao.LaporanDao;
import giriatk.model.Antrian;
import giriatk.model.BarisLaporan;
import giriatk.model.Barang;
import giriatk.model.Ringkasan;
import giriatk.util.Fmt;
import giriatk.util.Icons;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.ScrollablePanel;
import giriatk.view.component.TrendChart;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.util.List;

public class DashboardPanel extends BasePanel {

    private final LaporanDao laporanDao = new LaporanDao();
    private final BarangDao barangDao = new BarangDao();
    private final AntrianDao antrianDao = new AntrianDao();

    private JLabel lblOmzet;
    private JLabel lblLaba;
    private JLabel lblTransaksi;
    private JLabel lblAntrian;
    private PeringatanStok peringatanStok;
    private TrendChart chart;

    private DefaultTableModel modelMenipis;
    private DefaultTableModel modelAntrian;

    public DashboardPanel() {
        super(new BorderLayout());
        build();
    }

    private void build() {
        JPanel body = new ScrollablePanel();
        body.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1.0;

        // Peringatan stok takes full width at top if active
        peringatanStok = new PeringatanStok();
        peringatanStok.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weighty = 0;
        body.add(peringatanStok, gbc);

        // Top Row: 4 Metric Cards (full width span)
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weighty = 0;
        body.add(buildStatGrid(), gbc);

        // Center Bento Grid: Left 65% Chart, Right 35% Antrian Tracker
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; 
        gbc.weightx = 0.65; gbc.weighty = 1.0;
        body.add(buildChartCard(), gbc);

        gbc.gridx = 1; gbc.gridy = 2; 
        gbc.weightx = 0.35; gbc.weighty = 1.0;
        body.add(buildAntrianCard(), gbc);

        // Bottom Row: Stok Kritis Inventory Table
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.weighty = 0.8;
        body.add(buildMenipisCard(), gbc);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        add(pageHeader("Dashboard Ringkasan", "Tinjauan telemetri penjualan, status operasional toko, dan antrian cetak."), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildStatGrid() {
        lblOmzet = valueLabel();
        lblLaba = valueLabel();
        lblTransaksi = valueLabel();
        lblAntrian = valueLabel();

        boolean owner = Session.isOwner();
        int cols = owner ? 4 : 3;
        JPanel row = new JPanel(new GridLayout(1, cols, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        
        row.add(statCard("OMZET HARI INI", lblOmzet, Icons.Name.MONEY, Theme.ACCENT, "Total bruto"));
        if (owner) {
            row.add(statCard("LABA BERSIH", lblLaba, Icons.Name.CHART, Theme.GREEN_FG, "Margin estimasi"));
        }
        row.add(statCard("TRANSAKSI", lblTransaksi, Icons.Name.RECEIPT, Theme.BLUE_FG, "Selesai dibayar"));
        row.add(statCard("ANTRIAN AKTIF", lblAntrian, Icons.Name.QUEUE, Theme.AMBER_FG, "Dalam pengerjaan"));
        
        return row;
    }

    private UiKit.SurfacePanel statCard(String title, JLabel valueLabel, Icons.Name icon, Color accent, String subtext) {
        UiKit.SurfacePanel card = card(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.mono(Font.BOLD, 10));
        titleLabel.setForeground(Theme.MUTED);
        top.add(titleLabel, BorderLayout.WEST);

        JComponent iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Icons.paint(g2, icon, 16, accent);
                g2.dispose();
            }
        };
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(16, 16));
        top.add(iconPanel, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(valueLabel);

        JLabel sub = new JLabel(subtext);
        sub.setFont(Theme.text(Font.PLAIN, 11));
        sub.setForeground(Theme.FAINT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(Box.createVerticalStrut(2));
        center.add(sub);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JLabel valueLabel() {
        JLabel label = new JLabel("-");
        label.setFont(Theme.mono(Font.BOLD, 22));
        label.setForeground(Theme.INK);
        return label;
    }

    private JComponent buildChartCard() {
        UiKit.SurfacePanel card = card(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Grafik Tren Omzet 7 Hari Terakhir");
        title.setFont(Theme.display(Font.BOLD, 14));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);

        JLabel sub = new JLabel("[ REKAP MINGGUAN ]");
        sub.setFont(Theme.mono(Font.BOLD, 10));
        sub.setForeground(Theme.ACCENT);
        top.add(sub, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);

        chart = new TrendChart();
        chart.setPreferredSize(new Dimension(0, 260));
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildAntrianCard() {
        UiKit.SurfacePanel card = card(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Antrian Cetak Berjalan");
        title.setFont(Theme.display(Font.BOLD, 14));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);

        JLabel tag = new JLabel("[ LIVE ]");
        tag.setFont(Theme.mono(Font.BOLD, 10));
        tag.setForeground(Theme.GREEN_FG);
        top.add(tag, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);

        modelAntrian = nonEditable("Pelanggan", "Layanan", "Status");
        UiKit.AppTable table = new UiKit.AppTable(modelAntrian);
        table.setEmptyState("Tidak ada antrian aktif", "Pesanan cetak akan langsung muncul di sini.");
        table.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
        table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.BadgeRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(0, 260));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildMenipisCard() {
        UiKit.SurfacePanel card = card(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Peringatan Stok Barang Kritis");
        title.setFont(Theme.display(Font.BOLD, 14));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);

        JLabel sub = new JLabel("Daftar item yang memerlukan pengadaan / restock segera");
        sub.setFont(Theme.text(Font.PLAIN, 12));
        sub.setForeground(Theme.MUTED);
        top.add(sub, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);

        modelMenipis = nonEditable("Kode", "Nama Barang", "Kategori", "Sisa Stok", "Batas Min", "Status");
        UiKit.AppTable table = new UiKit.AppTable(modelMenipis);
        table.setEmptyState("Semua stok aman", "Tidak ada barang yang mendekati batas minimum.");
        table.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
        table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER, true));
        table.getColumnModel().getColumn(4).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
        table.getColumnModel().getColumn(5).setCellRenderer(new UiKit.BadgeRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(0, 180));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private DefaultTableModel nonEditable(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    @Override
    public void onShow() {
        try {
            Ringkasan ringkasan = laporanDao.ringkasanHariIni();
            lblOmzet.setText(Fmt.rupiah(ringkasan.getOmzet()));
            lblLaba.setText(Fmt.rupiah(ringkasan.getLaba()));
            lblTransaksi.setText(String.valueOf(ringkasan.getJumlahTransaksi()));
            lblAntrian.setText(String.valueOf(ringkasan.getAntrianAktif()));

            if (ringkasan.getStokMenipis() > 0) {
                peringatanStok.setPesan("PERINGATAN SISTEM: " + ringkasan.getStokMenipis()
                        + " barang telah mencapai batas stok minimum. Segera lakukan restock.");
                peringatanStok.setVisible(true);
            } else {
                peringatanStok.setVisible(false);
            }
            bodyRevalidate();

            isiMenipis();
            isiAntrian();
            isiChart();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void bodyRevalidate() {
        revalidate();
        repaint();
    }

    private void isiMenipis() throws Exception {
        modelMenipis.setRowCount(0);
        List<Barang> list = barangDao.findMenipis();
        for (Barang b : list) {
            modelMenipis.addRow(new Object[]{
                b.getKode(),
                b.getNama(),
                b.getKategoriNama() == null ? "-" : b.getKategoriNama(),
                b.getStok() + " " + b.getSatuan(),
                b.getStokMinimum(),
                b.getStatusStok()
            });
        }
    }

    private void isiAntrian() throws Exception {
        modelAntrian.setRowCount(0);
        List<Antrian> list = antrianDao.findAktifTerbaru(8);
        for (Antrian a : list) {
            modelAntrian.addRow(new Object[]{
                a.getNamaPelanggan() == null || a.getNamaPelanggan().isBlank() ? "Umum" : a.getNamaPelanggan(),
                a.getLayananNama() == null ? "-" : a.getLayananNama(),
                a.getStatus().name()
            });
        }
    }

    private void isiChart() throws Exception {
        LocalDate today = LocalDate.now();
        List<BarisLaporan> data = laporanDao.omzetPerHariLengkap(today.minusDays(6), today);
        chart.setData(data);
    }

    private static class PeringatanStok extends JPanel {

        private final JLabel label = new JLabel();

        PeringatanStok() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(14, 48, 14, 18));
            label.setFont(Theme.mono(Font.BOLD, 12));
            label.setForeground(Theme.DANGER);
            add(label, BorderLayout.CENTER);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        }

        void setPesan(String pesan) {
            label.setText(pesan);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.DANGER_SOFT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_CARD, Theme.RADIUS_CARD);
            g2.setColor(Theme.alpha(Theme.DANGER, 100));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS_CARD, Theme.RADIUS_CARD);
            g2.translate(18, (getHeight() - 18) / 2);
            Icons.paint(g2, Icons.Name.WARNING, 18, Theme.DANGER);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
