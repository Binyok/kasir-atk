package giriatk.view.panel;

import giriatk.dao.AntrianDao;
import giriatk.model.Antrian;
import giriatk.model.StatusAntrian;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Icons;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.AksiIkon;
import giriatk.view.dialog.AntrianDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
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
import java.util.ArrayList;
import java.util.List;

public class AntrianPanel extends BasePanel {

    private static final int KOLOM_STATUS = 5;
    private static final int KOLOM_UBAH = 6;
    private static final int KOLOM_HAPUS = 7;

    private final AntrianDao dao = new AntrianDao();
    private final List<Antrian> data = new ArrayList<>();

    private final JLabel lblCountMenunggu = new JLabel("0");
    private final JLabel lblCountProses = new JLabel("0");
    private final JLabel lblCountSelesai = new JLabel("0");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Pelanggan", "Layanan Cetak", "Qty", "Keterangan", "Waktu Masuk", "Status Pengerjaan", "", ""}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == KOLOM_STATUS;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column != KOLOM_STATUS || !(value instanceof StatusAntrian baru)) {
                super.setValueAt(value, row, column);
                return;
            }
            if (row < 0 || row >= data.size()) {
                return;
            }
            Antrian antrian = data.get(row);
            if (antrian.getStatus() == baru) {
                return;
            }
            if (!transisiDiizinkan(antrian.getStatus(), baru)) {
                Dialogs.peringatan(AntrianPanel.this, pesanTolak(antrian.getStatus(), baru));
                return;
            }
            try {
                dao.updateStatus(antrian.getId(), baru);
                antrian.setStatus(baru);
                super.setValueAt(baru, row, column);
                SwingUtilities.invokeLater(AntrianPanel.this::reload);
            } catch (Exception ex) {
                Dialogs.error(AntrianPanel.this, "Gagal mengubah status: " + ex.getMessage());
            }
        }
    };

    private final UiKit.AppTable table = new UiKit.AppTable(model);
    private final giriatk.view.component.TabBar filterBar = new giriatk.view.component.TabBar();

    private String filter = "AKTIF";

    public AntrianPanel() {
        super(new BorderLayout());

        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        // Top Telemetry Queue Chips
        root.add(buildKpiRow());
        root.add(Box.createVerticalStrut(14));

        filterBar.addTab("AKTIF", "Semua Aktif");
        filterBar.addTab("MENUNGGU", "Menunggu");
        filterBar.addTab("PROSES", "Sedang Diproses");
        filterBar.addTab("SELESAI", "Siap Diambil");
        filterBar.addTab("DIAMBIL", "Riwayat Diambil");
        filterBar.addTab("SEMUA", "Semua Status");
        filterBar.setActive("AKTIF");
        filterBar.setOnChange(key -> {
            filter = key;
            reload();
        });

        UiKit.IconButton refresh = new UiKit.IconButton(Icons.Name.REFRESH, Theme.MUTED, 16, true);
        refresh.setToolTipText("Muat ulang data");
        refresh.addActionListener(e -> reload());
        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        kanan.setOpaque(false);
        kanan.add(refresh);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(filterBar, BorderLayout.WEST);
        toolbar.add(kanan, BorderLayout.EAST);

        table.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
        table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
        table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(4).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(KOLOM_STATUS).setCellRenderer(new UiKit.BadgeRenderer());
        AksiIkon.pasang(table, KOLOM_UBAH, Icons.Name.EDIT, Theme.MUTED);
        AksiIkon.pasang(table, KOLOM_HAPUS, Icons.Name.TRASH, Theme.DANGER);

        JComboBox<StatusAntrian> combo = new JComboBox<>(StatusAntrian.values());
        combo.setFont(Theme.text(Font.PLAIN, 13));
        combo.setBackground(Theme.SURFACE_ELEVATED);
        combo.setForeground(Theme.INK);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof StatusAntrian s) {
                    setText("  " + labelStatus(s));
                }
                setBackground(isSelected ? Theme.SURFACE_HOVER : Theme.SURFACE_ELEVATED);
                setForeground(isSelected ? Theme.ACCENT : Theme.INK);
                return this;
            }
        });
        DefaultCellEditor editor = new DefaultCellEditor(combo);
        editor.setClickCountToStart(1);
        table.getColumnModel().getColumn(KOLOM_STATUS).setCellEditor(editor);
        table.getColumnModel().getColumn(KOLOM_STATUS).setMinWidth(130);
        table.getColumnModel().getColumn(KOLOM_STATUS).setPreferredWidth(140);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col == KOLOM_STATUS) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow < 0 || modelRow >= data.size()) {
                    return;
                }
                Antrian antrian = data.get(modelRow);
                if (col == KOLOM_UBAH) {
                    ubah(antrian);
                } else if (col == KOLOM_HAPUS) {
                    hapus(antrian);
                }
            }
        });

        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 18, 20));
        card.add(toolbar, BorderLayout.NORTH);

        table.setEmptyState("Tidak ada antrian pengerjaan", "Antrian jasa print/cetak baru akan otomatis muncul di sini.");
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        
        root.add(card);

        add(pageHeader("Antrian Jasa Cetak", "Pantau dan perbarui status pengerjaan print, fotokopi, jilid, dan laminating."),
                BorderLayout.NORTH);
        add(root, BorderLayout.CENTER);
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        row.setPreferredSize(new Dimension(0, 74));

        row.add(kpiCard("ANTRIAN MENUNGGU", lblCountMenunggu, Theme.AMBER_FG, "Belum mulai diproses"));
        row.add(kpiCard("SEDANG PROSES", lblCountProses, Theme.BLUE_FG, "Sedang dalam pengerjaan"));
        row.add(kpiCard("SELESAI / SIAP DIAMBIL", lblCountSelesai, Theme.GREEN_FG, "Menunggu diambil pelanggan"));
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
        valueLabel.setFont(Theme.mono(Font.BOLD, 18));
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

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void reload() {
        try {
            data.clear();
            List<Antrian> semua = dao.findAll();
            
            int cMenunggu = 0;
            int cProses = 0;
            int cSelesai = 0;
            for (Antrian a : semua) {
                if (a.getStatus() == StatusAntrian.MENUNGGU) cMenunggu++;
                else if (a.getStatus() == StatusAntrian.PROSES) cProses++;
                else if (a.getStatus() == StatusAntrian.SELESAI) cSelesai++;

                if (cocokFilter(a)) {
                    data.add(a);
                }
            }

            lblCountMenunggu.setText(String.valueOf(cMenunggu));
            lblCountProses.setText(String.valueOf(cProses));
            lblCountSelesai.setText(String.valueOf(cSelesai));

            model.setRowCount(0);
            for (Antrian a : data) {
                model.addRow(new Object[]{
                    a.getNamaPelanggan() == null || a.getNamaPelanggan().isBlank() ? "Umum" : a.getNamaPelanggan(),
                    a.getLayananNama() == null ? "-" : a.getLayananNama(),
                    a.getJumlah(),
                    a.getDeskripsi() == null ? "-" : a.getDeskripsi(),
                    Fmt.tanggalWaktu(a.getWaktuMasuk()),
                    a.getStatus(),
                    "",
                    ""
                });
            }
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal memuat antrian: " + ex.getMessage());
        }
    }

    private boolean cocokFilter(Antrian a) {
        switch (filter) {
            case "SEMUA":
                return true;
            case "AKTIF":
                return a.getStatus() == StatusAntrian.MENUNGGU || a.getStatus() == StatusAntrian.PROSES || a.getStatus() == StatusAntrian.SELESAI;
            default:
                return a.getStatus().name().equals(filter);
        }
    }

    private boolean transisiDiizinkan(StatusAntrian dari, StatusAntrian ke) {
        if (dari == ke) {
            return true;
        }
        if (dari == StatusAntrian.DIAMBIL) {
            return false;
        }
        if (ke == StatusAntrian.DIAMBIL) {
            return dari == StatusAntrian.SELESAI;
        }
        return true;
    }

    private String pesanTolak(StatusAntrian dari, StatusAntrian ke) {
        if (dari == StatusAntrian.DIAMBIL) {
            return "Pesanan sudah diambil oleh pelanggan.\nStatus tidak dapat diubah kembali.";
        }
        return "Status \"Diambil\" hanya bisa dipilih setelah status pengerjaan \"Selesai\".";
    }

    private String labelStatus(StatusAntrian status) {
        switch (status) {
            case MENUNGGU:
                return "Menunggu";
            case PROSES:
                return "Sedang Proses";
            case SELESAI:
                return "Selesai";
            default:
                return "Sudah Diambil";
        }
    }

    private void ubah(Antrian antrian) {
        AntrianDialog dialog = new AntrianDialog(owner(), antrian);
        dialog.setVisible(true);
        if (dialog.isTersimpan()) {
            reload();
        }
    }

    private void hapus(Antrian antrian) {
        String nama = antrian.getNamaPelanggan() == null || antrian.getNamaPelanggan().isBlank()
                ? "antrian ini" : "antrian " + antrian.getNamaPelanggan();
        if (!Dialogs.konfirmasi(this, "Hapus data " + nama + "?")) {
            return;
        }
        try {
            dao.delete(antrian.getId());
            reload();
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal menghapus antrian: " + ex.getMessage());
        }
    }
}
