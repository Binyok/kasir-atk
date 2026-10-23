package giriatk.view.panel;

import giriatk.dao.TransaksiDao;
import giriatk.model.Transaksi;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Icons;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.AksiIkon;
import giriatk.view.dialog.TransaksiDetailDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RiwayatPanel extends BasePanel {

    private final TransaksiDao dao = new TransaksiDao();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"No. Nota / Kode", "Waktu Transaksi", "Kategori", "Total Tagihan", "Nominal Bayar", "Kembalian", "Kasir", ""}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final UiKit.AppTable table = new UiKit.AppTable(model);
    private final List<Transaksi> data = new ArrayList<>();
    private final Map<String, UiKit.ToggleButton> filterButtons = new LinkedHashMap<>();
    private final JTextField txtCari = UiKit.textField("Cari no nota atau kasir...");
    
    private final JLabel lblCount = new JLabel("0");
    private final JLabel lblOmzet = new JLabel("Rp 0");
    private final JLabel lblRata = new JLabel("Rp 0");

    private String periode = "HARI_INI";

    public RiwayatPanel() {
        super(new BorderLayout());

        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        // Top Summary Metric Strip
        root.add(buildKpiRow());
        root.add(Box.createVerticalStrut(14));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        addFilter(filterRow, "HARI_INI", "Hari Ini");
        addFilter(filterRow, "7_HARI", "7 Hari Terakhir");
        addFilter(filterRow, "30_HARI", "30 Hari Terakhir");
        addFilter(filterRow, "SEMUA", "Semua Data");

        txtCari.setPreferredSize(new Dimension(240, 38));
        txtCari.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { tampilkan(); }
            @Override
            public void removeUpdate(DocumentEvent e) { tampilkan(); }
            @Override
            public void changedUpdate(DocumentEvent e) { tampilkan(); }
        });

        UiKit.IconButton refresh = new UiKit.IconButton(Icons.Name.REFRESH, Theme.MUTED, 16, true);
        refresh.setToolTipText("Muat ulang");
        refresh.addActionListener(e -> reload());

        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        kanan.setOpaque(false);
        kanan.add(txtCari);
        kanan.add(refresh);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(filterRow, BorderLayout.WEST);
        toolbar.add(kanan, BorderLayout.EAST);

        table.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
        table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.BadgeRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT, true));
        table.getColumnModel().getColumn(4).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT));
        table.getColumnModel().getColumn(5).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT));
        table.getColumnModel().getColumn(6).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(0).setPreferredWidth(165);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(95);
        table.getColumnModel().getColumn(6).setPreferredWidth(85);
        AksiIkon.pasang(table, 7, Icons.Name.EYE, Theme.ACCENT);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                if ((col == 7 && e.getClickCount() == 1) || (e.getClickCount() == 2 && col != 7)) {
                    lihatDetail();
                }
            }
        });

        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 18, 20));
        card.add(toolbar, BorderLayout.NORTH);

        table.setEmptyState("Tidak ada riwayat transaksi", "Transaksi yang selesai diproses akan dicatat di sini.");
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        
        root.add(card);

        add(pageHeader("Riwayat Transaksi Penjualan", "Audit seluruh catatan transaksi, rincian pembayaran, dan detail nota."), BorderLayout.NORTH);
        add(root, BorderLayout.CENTER);
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        row.setPreferredSize(new Dimension(0, 74));

        row.add(kpiCard("TOTAL TRANSAKSI", lblCount, Theme.BLUE_FG, "Nota terbit"));
        row.add(kpiCard("TOTAL NILAI OMZET", lblOmzet, Theme.ACCENT, "Bruto penjualan"));
        row.add(kpiCard("RATA-RATA TRANSAKSI", lblRata, Theme.GREEN_FG, "Per transaksi"));
        return row;
    }

    private UiKit.SurfacePanel kpiCard(String label, JLabel valueLabel, Color accent, String sub) {
        UiKit.SurfacePanel card = card(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        
        JLabel t = new JLabel(label);
        t.setFont(Theme.mono(Font.BOLD, 10));
        t.setForeground(Theme.MUTED);
        top.add(t, BorderLayout.WEST);

        JLabel dot = new JLabel("●");
        dot.setFont(Theme.text(Font.PLAIN, 12));
        dot.setForeground(accent);
        top.add(dot, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bot.setOpaque(false);
        valueLabel.setFont(Theme.mono(Font.BOLD, 17));
        valueLabel.setForeground(accent);
        
        JLabel s = new JLabel(sub);
        s.setFont(Theme.text(Font.PLAIN, 11));
        s.setForeground(Theme.FAINT);

        bot.add(valueLabel);
        bot.add(s);
        card.add(bot, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void onShow() {
        reload();
    }

    private void addFilter(JPanel target, String key, String label) {
        UiKit.ToggleButton toggle = new UiKit.ToggleButton(label, Theme.ACCENT);
        toggle.addActionListener(e -> {
            periode = key;
            filterButtons.forEach((k, b) -> b.setActive(k.equals(key)));
            reload();
        });
        filterButtons.put(key, toggle);
        target.add(toggle);
        if (filterButtons.size() == 1) {
            toggle.setActive(true);
        }
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void reload() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate from;
            LocalDate to = today;
            switch (periode) {
                case "7_HARI":
                    from = today.minusDays(6);
                    break;
                case "30_HARI":
                    from = today.minusDays(29);
                    break;
                case "SEMUA":
                    from = LocalDate.of(2000, 1, 1);
                    break;
                default:
                    from = today;
                    break;
            }
            data.clear();
            data.addAll(dao.findRange(from, to, 1000));
            tampilkan();
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal memuat riwayat: " + ex.getMessage());
        }
    }

    private void tampilkan() {
        String keyword = txtCari.getText().trim().toLowerCase();
        model.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        int jumlah = 0;
        for (Transaksi t : data) {
            if (!keyword.isEmpty() && (t.getKode() == null || !t.getKode().toLowerCase().contains(keyword))) {
                continue;
            }
            model.addRow(new Object[]{
                t.getKode(),
                Fmt.tanggalWaktu(t.getTanggal()),
                t.getTipe().name(),
                Fmt.rupiah(t.getTotal()),
                Fmt.rupiah(t.getBayar()),
                Fmt.rupiah(t.getKembali()),
                t.getUserName() == null ? "-" : t.getUserName(),
                ""
            });
            total = total.add(t.getTotal());
            jumlah++;
        }
        
        lblCount.setText(String.valueOf(jumlah));
        lblOmzet.setText(Fmt.rupiah(total));
        lblRata.setText(jumlah > 0 ? Fmt.rupiah(total.divide(BigDecimal.valueOf(jumlah), 0, java.math.RoundingMode.HALF_UP)) : "Rp 0");
    }

    private void lihatDetail() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Dialogs.peringatan(this, "Pilih satu transaksi terlebih dahulu.");
            return;
        }
        Transaksi ringkas = dataTransaksiTampil().get(table.convertRowIndexToModel(row));
        try {
            Transaksi lengkap = dao.findById(ringkas.getId());
            if (lengkap == null) {
                Dialogs.peringatan(this, "Transaksi tidak ditemukan.");
                return;
            }
            new TransaksiDetailDialog(owner(), lengkap).setVisible(true);
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal membuka detail nota: " + ex.getMessage());
        }
    }

    private List<Transaksi> dataTransaksiTampil() {
        String keyword = txtCari.getText().trim().toLowerCase();
        List<Transaksi> hasil = new ArrayList<>();
        for (Transaksi t : data) {
            if (keyword.isEmpty() || (t.getKode() != null && t.getKode().toLowerCase().contains(keyword))) {
                hasil.add(t);
            }
        }
        return hasil;
    }
}
