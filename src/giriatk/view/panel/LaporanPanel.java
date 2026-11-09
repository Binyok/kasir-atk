package giriatk.view.panel;

import giriatk.dao.LaporanDao;
import giriatk.dao.TransaksiDao;
import giriatk.model.BarisLaporan;
import giriatk.model.ItemTerlaris;
import giriatk.model.Transaksi;
import giriatk.service.PdfExporter;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Icons;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.ScrollablePanel;
import giriatk.view.component.TrendChart;
import giriatk.view.component.SegmentedTabs;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

public class LaporanPanel extends BasePanel {

    private enum Periode {HARIAN, MINGGUAN, BULANAN, KUSTOM}

    private static final DateTimeFormatter BULAN = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("id", "ID"));

    private final LaporanDao laporanDao = new LaporanDao();
    private final TransaksiDao transaksiDao = new TransaksiDao();

    private Periode periode = Periode.HARIAN;
    private LocalDate anchor = LocalDate.now();
    private LocalDate from = LocalDate.now();
    private LocalDate to = LocalDate.now();

    private final JLabel lblPeriode = new JLabel();
    private final JLabel lblOmzet = nilai();
    private final JLabel lblLaba = nilai();
    private final JLabel lblTransaksi = nilai();
    private final JLabel lblRata = nilai();
    private final TrendChart chart = new TrendChart();

    private final JTextField txtDari = UiKit.textField("yyyy-MM-dd");
    private final JTextField txtSampai = UiKit.textField("yyyy-MM-dd");
    private final JPanel kustomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

    private final DefaultTableModel modelHarian = nonEditable("Tanggal", "Transaksi", "Omzet Bruto", "Laba Bersih");
    private final DefaultTableModel modelTerlaris = nonEditable("Nama Produk / Item", "Tipe", "Qty Terjual", "Total Omzet", "Estimasi Laba");
    private final DefaultTableModel modelKategori = nonEditable("Kategori Produk", "Jumlah Transaksi", "Total Omzet", "Total Laba");
    private final DefaultTableModel modelTransaksi = nonEditable("Kode Nota", "Waktu", "Tipe", "Total Tagihan", "Kasir Bertugas");

    private final java.util.Map<Periode, UiKit.ToggleButton> togglePeriode = new java.util.LinkedHashMap<>();

    public LaporanPanel() {
        super(new BorderLayout());

        JPanel body = new ScrollablePanel();
        body.setLayout(new java.awt.GridBagLayout());

        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        int[] row = {0};
        java.util.function.BiConsumer<java.awt.Component, Integer> addRow = (comp, gap) -> {
            gc.gridy = row[0]++;
            gc.insets = new java.awt.Insets(0, 0, gap, 0);
            body.add(comp, gc);
        };

        addRow.accept(buildControl(), 18);
        addRow.accept(buildStatRow(), 14);
        addRow.accept(buildChartCard(), 14);

        JPanel tabs = buildTabs();
        tabs.setPreferredSize(new Dimension(0, 380));
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));
        addRow.accept(tabs, 10);

        add(pageHeader("Laporan Keuangan & Penjualan",
                "Analisis performa omzet, estimasi laba, produk terlaris, dan ekspor ke PDF."),
                BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        add(scroll, BorderLayout.CENTER);
    }

    private static JLabel nilai() {
        JLabel label = new JLabel("-");
        label.setFont(Theme.mono(Font.BOLD, 18));
        label.setForeground(Theme.INK);
        return label;
    }

    private static DefaultTableModel nonEditable(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel buildControl() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 2, 14, 2));

        JPanel periodeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        periodeRow.setOpaque(false);
        addToggle(periodeRow, Periode.HARIAN, "Harian");
        addToggle(periodeRow, Periode.MINGGUAN, "Mingguan");
        addToggle(periodeRow, Periode.BULANAN, "Bulanan");
        addToggle(periodeRow, Periode.KUSTOM, "Kustom");
        togglePeriode.get(Periode.HARIAN).setActive(true);

        UiKit.IconButton prev = new UiKit.IconButton(Icons.Name.CHEVRON_LEFT, Theme.INK, 14, true);
        prev.addActionListener(e -> geser(-1));

        UiKit.IconButton next = new UiKit.IconButton(Icons.Name.CHEVRON_RIGHT, Theme.INK, 14, true);
        next.addActionListener(e -> geser(1));

        lblPeriode.setFont(Theme.mono(Font.BOLD, 13));
        lblPeriode.setForeground(Theme.INK);
        lblPeriode.setHorizontalAlignment(SwingConstants.CENTER);
        lblPeriode.setPreferredSize(new Dimension(280, 30));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        nav.setOpaque(false);
        nav.add(prev);
        nav.add(lblPeriode);
        nav.add(next);

        UiKit.SoftButton ekspor = new UiKit.SoftButton("Ekspor Laporan PDF", UiKit.Variant.PRIMARY);
        ekspor.addActionListener(e -> eksporPdf());

        JPanel kiri = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        kiri.setOpaque(false);
        kiri.add(periodeRow);

        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        kanan.setOpaque(false);
        kanan.add(ekspor);

        panel.add(kiri, BorderLayout.WEST);
        panel.add(nav, BorderLayout.CENTER);
        panel.add(kanan, BorderLayout.EAST);

        kustomRow.setOpaque(false);
        kustomRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JLabel d = new JLabel("Dari:");
        d.setFont(Theme.mono(Font.BOLD, 11));
        d.setForeground(Theme.INK_SOFT);
        JLabel s = new JLabel("Sampai:");
        s.setFont(Theme.mono(Font.BOLD, 11));
        s.setForeground(Theme.INK_SOFT);
        txtDari.setPreferredSize(new Dimension(130, 36));
        txtSampai.setPreferredSize(new Dimension(130, 36));
        txtDari.setText(LocalDate.now().withDayOfMonth(1).toString());
        txtSampai.setText(LocalDate.now().toString());
        UiKit.SoftButton terapkan = new UiKit.SoftButton("Terapkan Rentang", UiKit.Variant.GHOST);
        terapkan.addActionListener(e -> {
            applyKustom();
            reload();
        });
        kustomRow.add(d);
        kustomRow.add(txtDari);
        kustomRow.add(s);
        kustomRow.add(txtSampai);
        kustomRow.add(terapkan);
        kustomRow.setVisible(false);

        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(panel);
        wrap.add(kustomRow);
        return wrap;
    }

    private void addToggle(JPanel parent, Periode p, String label) {
        UiKit.ToggleButton toggle = new UiKit.ToggleButton(label, Theme.ACCENT);
        toggle.addActionListener(e -> {
            periode = p;
            togglePeriode.forEach((k, b) -> b.setActive(k == p));
            kustomRow.setVisible(p == Periode.KUSTOM);
            revalidate();
            reload();
        });
        togglePeriode.put(p, toggle);
        parent.add(toggle);
    }

    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        row.setPreferredSize(new Dimension(0, 105));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(statCard("OMZET PERIODE", lblOmzet, Theme.ACCENT, "Total bruto penjualan"));
        row.add(statCard("LABA PERIODE", lblLaba, Theme.GREEN_FG, "Estimasi margin bersih"));
        row.add(statCard("JUMLAH TRANSAKSI", lblTransaksi, Theme.BLUE_FG, "Nota diselesaikan"));
        row.add(statCard("RATA-RATA TRANSAKSI", lblRata, Theme.INK_SOFT, "Rata-rata belanja"));
        return row;
    }

    private JPanel statCard(String title, JLabel value, Color accent, String sub) {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        
        JLabel t = new JLabel(title);
        t.setFont(Theme.mono(Font.BOLD, 10));
        t.setForeground(Theme.MUTED);
        card.add(t, BorderLayout.NORTH);
        
        value.setForeground(accent);
        card.add(value, BorderLayout.CENTER);
        
        JLabel s = new JLabel(sub);
        s.setFont(Theme.text(Font.PLAIN, 11));
        s.setForeground(Theme.FAINT);
        card.add(s, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildChartCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Grafik Tren Omzet Periode Terpilih");
        title.setFont(Theme.display(Font.BOLD, 14));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);
        
        JLabel tag = new JLabel("[ VISUALISASI FLUKTUASI ]");
        tag.setFont(Theme.mono(Font.BOLD, 10));
        tag.setForeground(Theme.ACCENT);
        top.add(tag, BorderLayout.EAST);
        
        card.add(top, BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(0, 160));
        card.add(chart, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 220));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel buildTabs() {
        SegmentedTabs tabs = new SegmentedTabs();
        tabs.addTab("harian", "Rekap Harian", tableScroll(modelHarian,
                new int[]{SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT, SwingConstants.RIGHT},
                "Belum ada transaksi", "Belum ada transaksi tercatat pada periode ini."));
        tabs.addTab("terlaris", "Item Terlaris", tableScroll(modelTerlaris,
                new int[]{SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.CENTER,
                    SwingConstants.RIGHT, SwingConstants.RIGHT},
                "Belum ada data penjualan", "Item terlaris akan dihitung berdasarkan kuantitas transaksi."));
        tabs.addTab("kategori", "Rekap Kategori", tableScroll(modelKategori,
                new int[]{SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT, SwingConstants.RIGHT},
                "Belum ada data kategori", "Rekap penjualan barang dikelompokkan per kategori."));
        tabs.addTab("transaksi", "Daftar Transaksi", tableScroll(modelTransaksi,
                new int[]{SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.CENTER,
                    SwingConstants.RIGHT, SwingConstants.LEFT},
                "Belum ada transaksi", "Daftar nota transaksi akan tercatat di sini."));
        return tabs;
    }

    private JScrollPane tableScroll(DefaultTableModel model, int[] align, String emptyTitle, String emptyHint) {
        UiKit.AppTable table = new UiKit.AppTable(model);
        for (int i = 0; i < align.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new UiKit.PaddedRenderer(align[i]));
        }
        if (model.getColumnCount() > 1 && model.getColumnName(1).equalsIgnoreCase("Tipe")) {
            table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.BadgeRenderer());
        }
        table.setEmptyState(emptyTitle, emptyHint);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    @Override
    public void onShow() {
        reload();
    }

    private void geser(int arah) {
        switch (periode) {
            case HARIAN:
                anchor = anchor.plusDays(arah);
                break;
            case MINGGUAN:
                anchor = anchor.plusWeeks(arah);
                break;
            case BULANAN:
                anchor = anchor.plusMonths(arah);
                break;
            case KUSTOM:
                break;
        }
        reload();
    }

    private void hitungRentang() {
        switch (periode) {
            case HARIAN:
                from = anchor;
                to = anchor;
                lblPeriode.setText(Fmt.tanggal(anchor));
                break;
            case MINGGUAN:
                from = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                to = anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                lblPeriode.setText(Fmt.tanggal(from) + "  –  " + Fmt.tanggal(to));
                break;
            case BULANAN:
                from = anchor.withDayOfMonth(1);
                to = anchor.withDayOfMonth(anchor.lengthOfMonth());
                lblPeriode.setText(BULAN.format(anchor));
                break;
            case KUSTOM:
                applyKustom();
                break;
        }
    }

    private void applyKustom() {
        try {
            from = LocalDate.parse(txtDari.getText().trim());
            to = LocalDate.parse(txtSampai.getText().trim());
            if (from.isAfter(to)) {
                LocalDate temp = from;
                from = to;
                to = temp;
                txtDari.setText(from.toString());
                txtSampai.setText(to.toString());
            }
            lblPeriode.setText(Fmt.tanggal(from) + "  –  " + Fmt.tanggal(to));
        } catch (Exception ex) {
            Dialogs.peringatan(this, "Format tanggal salah. Gunakan yyyy-MM-dd (contoh: 2026-09-01).");
        }
    }

    private void reload() {
        hitungRentang();
        try {
            List<BarisLaporan> harian = laporanDao.omzetPerHariLengkap(from, to);
            chart.setData(harian);

            modelHarian.setRowCount(0);
            for (BarisLaporan b : harian) {
                modelHarian.addRow(new Object[]{
                    Fmt.tanggal(LocalDate.parse(b.getLabel())),
                    b.getJumlahTransaksi(),
                    Fmt.rupiah(b.getOmzet()),
                    Fmt.rupiah(b.getLaba())
                });
            }

            List<ItemTerlaris> terlaris = laporanDao.itemTerlaris(from, to, 20);
            modelTerlaris.setRowCount(0);
            for (ItemTerlaris it : terlaris) {
                modelTerlaris.addRow(new Object[]{
                    it.getNama(),
                    it.getTipe() == null ? "BARANG" : it.getTipe(),
                    it.getQty(),
                    Fmt.rupiah(it.getOmzet()),
                    Fmt.rupiah(it.getLaba())
                });
            }

            List<BarisLaporan> kategori = laporanDao.rekapPerKategori(from, to);
            modelKategori.setRowCount(0);
            for (BarisLaporan k : kategori) {
                modelKategori.addRow(new Object[]{
                    k.getLabel(),
                    k.getJumlahTransaksi(),
                    Fmt.rupiah(k.getOmzet()),
                    Fmt.rupiah(k.getLaba())
                });
            }

            List<Transaksi> daftar = transaksiDao.findRange(from, to, 500);
            modelTransaksi.setRowCount(0);
            for (Transaksi t : daftar) {
                modelTransaksi.addRow(new Object[]{
                    t.getKode(),
                    Fmt.tanggalWaktu(t.getTanggal()),
                    t.getTipe().name(),
                    Fmt.rupiah(t.getTotal()),
                    t.getUserName() == null ? "-" : t.getUserName()
                });
            }

            BigDecimal totalOmzet = BigDecimal.ZERO;
            BigDecimal totalLaba = BigDecimal.ZERO;
            int totalTrx = 0;
            for (BarisLaporan b : harian) {
                totalOmzet = totalOmzet.add(b.getOmzet());
                totalLaba = totalLaba.add(b.getLaba());
                totalTrx += b.getJumlahTransaksi();
            }
            lblOmzet.setText(Fmt.rupiah(totalOmzet));
            lblLaba.setText(Fmt.rupiah(totalLaba));
            lblTransaksi.setText(String.valueOf(totalTrx));
            lblRata.setText(totalTrx > 0 ? Fmt.rupiah(totalOmzet.divide(BigDecimal.valueOf(totalTrx), 0, java.math.RoundingMode.HALF_UP)) : "Rp 0");

        } catch (Exception ex) {
            Dialogs.error(this, "Gagal memuat laporan: " + ex.getMessage());
        }
    }

    private void eksporPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Simpan Laporan PDF");
        chooser.setSelectedFile(new java.io.File("laporan-" + from + "-" + to + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            PdfExporter pdf = new PdfExporter();
            pdf.setTitle("LAPORAN KEUANGAN & PENJUALAN - GIRI ATK");
            pdf.setSubtitle("Tanggal Cetak: " + Fmt.tanggalWaktu(java.time.LocalDateTime.now()));
            pdf.setPeriode(Fmt.tanggal(from) + " s/d " + Fmt.tanggal(to));

            pdf.setColumns(new String[]{"Metrik / Kategori", "Keterangan", "Nilai"}, new float[]{0.45f, 0.25f, 0.30f});

            pdf.section("--- RINGKASAN PERFORMA KEUANGAN ---");
            pdf.row("Total Omzet Penjualan", "Bruto Nota Sukses", lblOmzet.getText());
            pdf.row("Estimasi Laba Bersih", "Margin Bruto", lblLaba.getText());
            pdf.row("Jumlah Transaksi", "Nota Diterbitkan", lblTransaksi.getText() + " transaksi");
            pdf.row("Rata-rata Transaksi", "Per Belanja Pelanggan", lblRata.getText());

            pdf.section("--- REKAP HARIAN PERIODE ---");
            for (int i = 0; i < modelHarian.getRowCount(); i++) {
                pdf.row(
                    String.valueOf(modelHarian.getValueAt(i, 0)),
                    modelHarian.getValueAt(i, 1) + " trx",
                    "Omzet: " + modelHarian.getValueAt(i, 2)
                );
            }

            pdf.save(target);
            Dialogs.info(this, "Laporan PDF berhasil disimpan:\n" + target.toAbsolutePath());
        } catch (IOException ex) {
            Dialogs.error(this, "Gagal mengekspor file PDF: " + ex.getMessage());
        }
    }
}
