package giriatk.view.panel;

import giriatk.dao.BarangDao;
import giriatk.dao.KategoriDao;
import giriatk.dao.RestockDao;
import giriatk.model.Barang;
import giriatk.model.Kategori;
import giriatk.model.Restock;
import giriatk.service.RestockService;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Icons;
import giriatk.util.ImageUtil;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.AksiIkon;
import giriatk.view.component.SegmentedTabs;
import giriatk.view.dialog.BarangDetailDialog;
import giriatk.view.dialog.BarangDialog;
import giriatk.view.dialog.KategoriDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.KeyStroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StokPanel extends BasePanel {

    private final SegmentedTabs tabs = new SegmentedTabs();

    public StokPanel() {
        super(new BorderLayout());
        tabs.addTab("barang", "Daftar Barang & Stok", new BarangTab(() -> tabs.show("restock")));
        tabs.addTab("kategori", "Kategori Produk", new KategoriTab());
        tabs.addTab("restock", "Manajemen Restock", new RestockTab());
        add(pageHeader("Inventaris & Stok Toko", "Pantau jumlah ketersediaan stok, batas minimum restock, dan kategori barang."), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
    }

    private abstract static class TabBase extends JPanel {

        protected final UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 14));

        TabBase() {
            super(new BorderLayout());
            setOpaque(false);
            card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
            add(card, BorderLayout.CENTER);
        }

        protected Window owner() {
            return SwingUtilities.getWindowAncestor(this);
        }

        protected UiKit.SoftButton button(String text, UiKit.Variant variant, Runnable action) {
            UiKit.SoftButton b = new UiKit.SoftButton(text, variant);
            b.addActionListener(e -> action.run());
            return b;
        }

        protected JScrollPane scroll(UiKit.AppTable table, String emptyTitle, String emptyHint) {
            table.setEmptyState(emptyTitle, emptyHint);
            JScrollPane pane = new JScrollPane(table);
            pane.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            pane.setOpaque(false);
            pane.getViewport().setOpaque(false);
            return pane;
        }

        protected static DefaultTableModel model(String... columns) {
            return new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
        }
    }

    private static class BarangTab extends TabBase {

        private final BarangDao dao = new BarangDao();
        private final JTextField txtCari = UiKit.textField("Scan barcode / cari nama, kode, merk...");
        private final DefaultTableModel model = model("", "Kode", "Nama Barang", "Merk", "Jenis", "Kategori", "Satuan", "Harga Beli", "Harga Jual", "Stok", "Status", "", "");
        private final UiKit.AppTable table = new UiKit.AppTable(model);
        private final List<Barang> data = new ArrayList<>();

        private final Runnable openRestock;

        BarangTab(Runnable openRestock) {
            this.openRestock = openRestock;
            txtCari.setPreferredSize(new Dimension(320, 38));
            txtCari.addActionListener(e -> scanAtauCariBarcode());
            txtCari.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { reload(); }
                @Override public void removeUpdate(DocumentEvent e) { reload(); }
                @Override public void changedUpdate(DocumentEvent e) { reload(); }
            });

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            actions.add(button("Tambah Barang", UiKit.Variant.PRIMARY, this::tambah));
            actions.add(button("Restock", UiKit.Variant.GHOST, openRestock));
            actions.add(button("Koreksi / Rusak", UiKit.Variant.GHOST, this::koreksiStok));
            UiKit.IconButton refresh = new UiKit.IconButton(Icons.Name.REFRESH, Theme.MUTED, 16, true);
            refresh.setToolTipText("Muat ulang");
            refresh.addActionListener(e -> reload());
            actions.add(refresh);

            JPanel toolbar = new JPanel(new BorderLayout());
            toolbar.setOpaque(false);
            toolbar.add(txtCari, BorderLayout.WEST);
            toolbar.add(actions, BorderLayout.EAST);
            card.add(toolbar, BorderLayout.NORTH);

            table.setRowHeight(46);
            for (int i : new int[]{0, 3, 4}) {
                table.getColumnModel().getColumn(i).setMinWidth(0);
                table.getColumnModel().getColumn(i).setMaxWidth(0);
                table.getColumnModel().getColumn(i).setPreferredWidth(0);
            }
            table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    l.setText("");
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                    if (value instanceof ImageIcon icon) {
                        l.setIcon(icon);
                    } else {
                        l.setIcon(null);
                    }
                    return l;
                }
            });
            table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
            table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
            table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
            table.getColumnModel().getColumn(4).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
            table.getColumnModel().getColumn(5).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
            table.getColumnModel().getColumn(6).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
            table.getColumnModel().getColumn(7).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT));
            table.getColumnModel().getColumn(8).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT, true));
            table.getColumnModel().getColumn(9).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
            table.getColumnModel().getColumn(10).setCellRenderer(new UiKit.BadgeRenderer());
            int[] lebar = {0, 90, 230, 0, 0, 110, 62, 92, 92, 50, 78, 30, 30};
            for (int i = 0; i < lebar.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
            }
            AksiIkon.pasang(table, 11, Icons.Name.EDIT, Theme.MUTED);
            AksiIkon.pasang(table, 12, Icons.Name.TRASH, Theme.DANGER);

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row < 0) {
                        return;
                    }
                    if (col == 11 && e.getClickCount() == 1) {
                        ubah();
                    } else if (col == 12 && e.getClickCount() == 1) {
                        hapus();
                    } else if (e.getClickCount() == 2 && col != 11 && col != 12) {
                        detail();
                    }
                }
            });

            card.add(scroll(table, "Belum ada data barang", "Tambahkan master barang untuk memulai pencatatan stok."), BorderLayout.CENTER);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            reload();
        }

        private void reload() {
            try {
                data.clear();
                data.addAll(dao.cari(txtCari.getText(), false));
                model.setRowCount(0);
                for (Barang b : data) {
                    model.addRow(new Object[]{
                        ImageUtil.getThumbnail(b.getGambar(), 30, 30),
                        b.getKode(),
                        b.getNama(),
                        b.getMerk() == null ? "-" : b.getMerk(),
                        b.getJenis() == null ? "-" : b.getJenis(),
                        b.getKategoriNama() == null ? "-" : b.getKategoriNama(),
                        b.getSatuan(),
                        Fmt.rupiah(b.getHargaBeli()),
                        Fmt.rupiah(b.getHargaJual()),
                        b.getStok() + (b.isAktif() ? "" : " (nonaktif)"),
                        b.getStatusStok(),
                        "",
                        ""
                    });
                }
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal memuat data barang: " + ex.getMessage());
            }
        }

        private Barang terpilih() {
            int row = table.getSelectedRow();
            if (row < 0) {
                Dialogs.peringatan(this, "Pilih satu barang terlebih dahulu.");
                return null;
            }
            int modelRow = table.convertRowIndexToModel(row);
            return data.get(modelRow);
        }

        private void detail() {
            Barang barang = terpilih();
            if (barang == null) {
                return;
            }
            BarangDetailDialog dlg = new BarangDetailDialog(owner(), barang);
            dlg.setVisible(true);
            if (dlg.isDiubah()) {
                reload();
            }
        }

        private void scanAtauCariBarcode() {
            String input = txtCari.getText().trim();
            if (input.isEmpty()) {
                return;
            }
            try {
                Barang b = dao.findByKode(input);
                if (b != null) {
                    for (int i = 0; i < data.size(); i++) {
                        if (data.get(i).getId() == b.getId()) {
                            int viewRow = table.convertRowIndexToView(i);
                            table.setRowSelectionInterval(viewRow, viewRow);
                            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                            break;
                        }
                    }
                    BarangDetailDialog dlg = new BarangDetailDialog(owner(), b);
                    dlg.setVisible(true);
                    if (dlg.isDiubah()) {
                        reload();
                    }
                    txtCari.setText("");
                } else {
                    BarangDialog dialog = new BarangDialog(owner(), null, input);
                    dialog.setVisible(true);
                    if (dialog.isTersimpan()) {
                        txtCari.setText("");
                        reload();
                    }
                }
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal memproses barcode: " + ex.getMessage());
            }
        }

        private void tambah() {
            BarangDialog dialog = new BarangDialog(owner(), null);
            dialog.setVisible(true);
            if (dialog.isTersimpan()) {
                reload();
            }
        }

        private void koreksiStok() {
            Barang barang = terpilih();
            if (barang == null) {
                return;
            }
            JTextField txtDelta = UiKit.textField("Contoh: -2 (untuk barang rusak) atau +5");
            JTextField txtAlasan = UiKit.textField("Contoh: Barang rusak / cacat / selisih opname");

            JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 0, 8));
            panel.add(new JLabel("Barang: " + barang.getNama() + " (Stok Saat Ini: " + barang.getStok() + " " + barang.getSatuan() + ")"));
            panel.add(new JLabel("Jumlah Penyesuaian (Gunakan tanda minus '-' jika mengurangi):"));
            panel.add(txtDelta);
            panel.add(new JLabel("Alasan Pengurangan / Koreksi:"));
            panel.add(txtAlasan);

            int res = javax.swing.JOptionPane.showConfirmDialog(owner(), panel, "Koreksi Stok / Catat Barang Rusak",
                    javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (res != javax.swing.JOptionPane.OK_OPTION) {
                return;
            }

            Integer delta = Fmt.parseBulat(txtDelta.getText());
            String alasan = txtAlasan.getText().trim();
            if (delta == null || delta == 0) {
                Dialogs.peringatan(this, "Jumlah penyesuaian harus angka selain 0 (misal: -1 atau -2 jika rusak).");
                return;
            }
            if (alasan.isEmpty()) {
                Dialogs.peringatan(this, "Alasan koreksi stok wajib diisi.");
                return;
            }
            if (delta < 0 && (barang.getStok() + delta) < 0) {
                Dialogs.peringatan(this, "Pengurangan melebihi stok yang ada (stok saat ini: " + barang.getStok() + ").");
                return;
            }

            if (!Dialogs.konfirmasi(this, "Simpan penyesuaian stok " + (delta > 0 ? "+" : "") + delta
                    + " untuk " + barang.getNama() + "?\nAlasan: " + alasan)) {
                return;
            }

            try {
                new RestockService().catatKoreksi(barang.getId(), delta, alasan, Session.getUserId());
                Dialogs.info(this, "Stok barang berhasil disesuaikan!\nStok baru: " + (barang.getStok() + delta));
                reload();
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal melakukan koreksi stok: " + ex.getMessage());
            }
        }

        private void ubah() {
            Barang barang = terpilih();
            if (barang == null) {
                return;
            }
            BarangDialog dialog = new BarangDialog(owner(), barang);
            dialog.setVisible(true);
            if (dialog.isTersimpan()) {
                reload();
            }
        }

        private void hapus() {
            Barang barang = terpilih();
            if (barang == null) {
                return;
            }
            if (!Dialogs.konfirmasi(this, "Hapus barang \"" + barang.getNama() + "\"?")) {
                return;
            }
            try {
                dao.delete(barang.getId());
                reload();
            } catch (Exception ex) {
                Dialogs.error(this, "Barang tidak bisa dihapus karena masih dipakai pada riwayat transaksi.\n"
                        + "Nonaktifkan saja barang ini melalui menu edit.\n\nDetail: " + ex.getMessage());
            }
        }
    }

    private static class KategoriTab extends TabBase {

        private final KategoriDao dao = new KategoriDao();
        private final DefaultTableModel model = model("Nama Kategori", "Keterangan / Catatan", "Jumlah Barang Terdaftar", "", "");
        private final UiKit.AppTable table = new UiKit.AppTable(model);
        private final List<Kategori> data = new ArrayList<>();

        KategoriTab() {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            actions.add(button("Tambah Kategori", UiKit.Variant.PRIMARY, this::tambah));
            UiKit.IconButton refresh = new UiKit.IconButton(Icons.Name.REFRESH, Theme.MUTED, 16, true);
            refresh.setToolTipText("Muat ulang");
            refresh.addActionListener(e -> reload());
            actions.add(refresh);
            card.add(actions, BorderLayout.NORTH);

            table.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
            table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
            table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
            AksiIkon.pasang(table, 3, Icons.Name.EDIT, Theme.MUTED);
            AksiIkon.pasang(table, 4, Icons.Name.TRASH, Theme.DANGER);

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row < 0) {
                        return;
                    }
                    if (col == 3 && e.getClickCount() == 1) {
                        ubah();
                    } else if (col == 4 && e.getClickCount() == 1) {
                        hapus();
                    } else if (e.getClickCount() == 2 && col != 3 && col != 4) {
                        ubah();
                    }
                }
            });

            card.add(scroll(table, "Belum ada kategori", "Tambahkan kategori untuk mengelompokkan barang."), BorderLayout.CENTER);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            reload();
        }

        private void reload() {
            try {
                data.clear();
                data.addAll(dao.findAll());
                model.setRowCount(0);
                for (Kategori k : data) {
                    model.addRow(new Object[]{
                        k.getNama(),
                        k.getKeterangan() == null ? "-" : k.getKeterangan(),
                        k.getJumlahBarang() + " item",
                        "",
                        ""
                    });
                }
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal memuat kategori: " + ex.getMessage());
            }
        }

        private Kategori terpilih() {
            int row = table.getSelectedRow();
            if (row < 0) {
                Dialogs.peringatan(this, "Pilih satu kategori terlebih dahulu.");
                return null;
            }
            return data.get(table.convertRowIndexToModel(row));
        }

        private void tambah() {
            KategoriDialog dialog = new KategoriDialog(owner(), null);
            dialog.setVisible(true);
            if (dialog.isTersimpan()) {
                reload();
            }
        }

        private void ubah() {
            Kategori k = terpilih();
            if (k == null) {
                return;
            }
            KategoriDialog dialog = new KategoriDialog(owner(), k);
            dialog.setVisible(true);
            if (dialog.isTersimpan()) {
                reload();
            }
        }

        private void hapus() {
            Kategori k = terpilih();
            if (k == null) {
                return;
            }
            if (!Dialogs.konfirmasi(this, "Hapus kategori \"" + k.getNama() + "\"?")) {
                return;
            }
            try {
                dao.delete(k.getId());
                reload();
            } catch (Exception ex) {
                Dialogs.error(this, "Kategori tidak bisa dihapus karena masih berisi barang.\n"
                        + "Pindahkan atau hapus barang di dalamnya terlebih dahulu.");
            }
        }
    }

    private static class RestockTab extends TabBase {

        private final BarangDao barangDao = new BarangDao();
        private final RestockDao restockDao = new RestockDao();
        private final RestockService restockService = new RestockService();

        private final JComboBox<BarangItem> cmbBarang = new JComboBox<>();
        private final JTextField txtScanBarcode = UiKit.textField("Scan barcode barang di sini...");
        private final JTextField txtJumlah = UiKit.textField("Contoh: 10");
        private final JTextField txtHargaBeli = UiKit.textField("Opsional (kosongkan jika tidak berubah)");
        private final JTextField txtKeterangan = UiKit.textField("Catatan pengadaan / supplier");
        private final JLabel lblStokSekarang = new JLabel("-");

        private final DefaultTableModel modelRiwayat = model("Waktu Restock", "Barang", "Jumlah Ditambah", "Harga Beli Baru", "Keterangan");
        private final UiKit.AppTable tableRiwayat = new UiKit.AppTable(modelRiwayat);

        RestockTab() {
            removeAll();
            setLayout(new BorderLayout());
            setOpaque(false);

            JPanel body = new JPanel(new GridBagLayout());
            body.setOpaque(false);

            GridBagConstraints gcForm = new GridBagConstraints();
            gcForm.gridx = 0;
            gcForm.gridy = 0;
            gcForm.weightx = 0.38;
            gcForm.weighty = 1.0;
            gcForm.fill = GridBagConstraints.BOTH;
            gcForm.insets = new Insets(0, 0, 0, 16);
            body.add(buildFormCard(), gcForm);

            GridBagConstraints gcTabel = new GridBagConstraints();
            gcTabel.gridx = 1;
            gcTabel.gridy = 0;
            gcTabel.weightx = 0.62;
            gcTabel.weighty = 1.0;
            gcTabel.fill = GridBagConstraints.BOTH;
            body.add(buildRiwayatCard(), gcTabel);

            add(body, BorderLayout.CENTER);

            cmbBarang.setFont(Theme.text(Font.PLAIN, 13));
            cmbBarang.setBackground(Theme.SURFACE_ELEVATED);
            cmbBarang.setForeground(Theme.INK);
            cmbBarang.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof BarangItem bi) {
                        setText("  " + bi.barang.getLabelLengkap() + " (Stok: " + bi.barang.getStok() + " " + bi.barang.getSatuan() + ")");
                        setIcon(ImageUtil.getThumbnail(bi.barang.getGambar(), 20, 20));
                    }
                    setBackground(isSelected ? Theme.SURFACE_HOVER : Theme.SURFACE_ELEVATED);
                    setForeground(isSelected ? Theme.ACCENT : Theme.INK);
                    return this;
                }
            });
            cmbBarang.addActionListener(e -> updateInfoStok());

            txtScanBarcode.addActionListener(e -> cariDenganBarcode());
        }

        private void cariDenganBarcode() {
            String code = txtScanBarcode.getText().trim();
            if (code.isEmpty()) return;
            try {
                Barang b = barangDao.findByKode(code);
                if (b != null) {
                    for (int i = 0; i < cmbBarang.getItemCount(); i++) {
                        BarangItem item = cmbBarang.getItemAt(i);
                        if (item.barang.getId() == b.getId()) {
                            cmbBarang.setSelectedIndex(i);
                            txtJumlah.requestFocusInWindow();
                            return;
                        }
                    }
                }
                Dialogs.peringatan(this, "Barang dengan kode/barcode \"" + code + "\" tidak ditemukan.");
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal mencari barcode: " + ex.getMessage());
            }
        }

        private JPanel buildFormCard() {
            UiKit.SurfacePanel panel = new UiKit.SurfacePanel(new BorderLayout(0, 14));
            panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

            JLabel title = new JLabel("Form Pengadaan Stok (Restock)");
            title.setFont(Theme.display(Font.BOLD, 15));
            title.setForeground(Theme.INK);
            panel.add(title, BorderLayout.NORTH);

            JPanel form = new JPanel();
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

            txtScanBarcode.setAlignmentX(Component.LEFT_ALIGNMENT);
            txtScanBarcode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            txtScanBarcode.setPreferredSize(new Dimension(0, 40));

            cmbBarang.setAlignmentX(Component.LEFT_ALIGNMENT);
            cmbBarang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            cmbBarang.setPreferredSize(new Dimension(0, 40));

            lblStokSekarang.setAlignmentX(Component.LEFT_ALIGNMENT);
            lblStokSekarang.setFont(Theme.mono(Font.PLAIN, 11));
            lblStokSekarang.setForeground(Theme.MUTED);

            txtJumlah.setAlignmentX(Component.LEFT_ALIGNMENT);
            txtJumlah.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            txtJumlah.setPreferredSize(new Dimension(0, 40));

            txtHargaBeli.setAlignmentX(Component.LEFT_ALIGNMENT);
            txtHargaBeli.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            txtHargaBeli.setPreferredSize(new Dimension(0, 40));

            txtKeterangan.setAlignmentX(Component.LEFT_ALIGNMENT);
            txtKeterangan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            txtKeterangan.setPreferredSize(new Dimension(0, 40));

            form.add(fieldLabel("Scan Barcode (Opsional)"));
            form.add(Box.createVerticalStrut(4));
            form.add(txtScanBarcode);
            form.add(Box.createVerticalStrut(14));

            form.add(fieldLabel("Pilih Barang"));
            form.add(Box.createVerticalStrut(4));
            form.add(cmbBarang);
            form.add(Box.createVerticalStrut(4));
            form.add(lblStokSekarang);
            form.add(Box.createVerticalStrut(14));

            form.add(fieldLabel("Jumlah Restock"));
            form.add(Box.createVerticalStrut(4));
            form.add(txtJumlah);
            form.add(Box.createVerticalStrut(14));

            form.add(fieldLabel("Harga Beli Baru (Opsional)"));
            form.add(Box.createVerticalStrut(4));
            form.add(txtHargaBeli);
            form.add(Box.createVerticalStrut(14));

            form.add(fieldLabel("Keterangan / Supplier"));
            form.add(Box.createVerticalStrut(4));
            form.add(txtKeterangan);
            form.add(Box.createVerticalStrut(20));

            UiKit.SoftButton simpan = new UiKit.SoftButton("SIMPAN RESTOCK", UiKit.Variant.PRIMARY);
            simpan.setAlignmentX(Component.LEFT_ALIGNMENT);
            simpan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            simpan.setPreferredSize(new Dimension(0, 44));
            simpan.setFont(Theme.text(Font.BOLD, 13));
            simpan.addActionListener(e -> simpanRestock());
            form.add(simpan);
            form.add(Box.createVerticalGlue());

            panel.add(form, BorderLayout.CENTER);
            return panel;
        }

        private JLabel fieldLabel(String text) {
            JLabel l = new JLabel(text);
            l.setFont(Theme.text(Font.BOLD, 12));
            l.setForeground(Theme.INK_SOFT);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            return l;
        }

        private JPanel buildRiwayatCard() {
            UiKit.SurfacePanel panel = new UiKit.SurfacePanel(new BorderLayout(0, 12));
            panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            JLabel title = new JLabel("Riwayat Pengadaan Terakhir");
            title.setFont(Theme.display(Font.BOLD, 15));
            title.setForeground(Theme.INK);
            top.add(title, BorderLayout.WEST);

            UiKit.IconButton refresh = new UiKit.IconButton(Icons.Name.REFRESH, Theme.MUTED, 16, true);
            refresh.setToolTipText("Muat ulang riwayat");
            refresh.addActionListener(e -> reload());
            top.add(refresh, BorderLayout.EAST);
            panel.add(top, BorderLayout.NORTH);

            tableRiwayat.setRowHeight(40);
            tableRiwayat.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
            tableRiwayat.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
            tableRiwayat.getColumnModel().getColumn(2).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
            tableRiwayat.getColumnModel().getColumn(3).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT));
            tableRiwayat.getColumnModel().getColumn(4).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));

            int[] widths = {130, 180, 80, 110, 140};
            for (int i = 0; i < widths.length && i < tableRiwayat.getColumnModel().getColumnCount(); i++) {
                tableRiwayat.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }

            tableRiwayat.setEmptyState("Belum ada riwayat restock", "Aktivitas penambahan stok akan tercatat di sini.");
            JScrollPane scroll = new JScrollPane(tableRiwayat);
            scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            panel.add(scroll, BorderLayout.CENTER);
            return panel;
        }

        @Override
        public void addNotify() {
            super.addNotify();
            reload();
        }

        private void reload() {
            try {
                cmbBarang.removeAllItems();
                for (Barang b : barangDao.findAktif()) {
                    cmbBarang.addItem(new BarangItem(b));
                }
                updateInfoStok();

                modelRiwayat.setRowCount(0);
                for (Restock r : restockDao.findAll(50)) {
                    modelRiwayat.addRow(new Object[]{
                        Fmt.tanggalWaktu(r.getTanggal()),
                        r.getBarangNama() == null ? "-" : r.getBarangNama(),
                        "+" + r.getJumlah(),
                        r.getHargaBeli() == null ? "-" : Fmt.rupiah(r.getHargaBeli()),
                        r.getCatatan() == null ? "-" : r.getCatatan()
                    });
                }
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal memuat data restock: " + ex.getMessage());
            }
        }

        private void updateInfoStok() {
            BarangItem item = (BarangItem) cmbBarang.getSelectedItem();
            if (item != null) {
                String extra = "";
                if (item.barang.getMerk() != null && !item.barang.getMerk().isEmpty()) {
                    extra += " • Merk: " + item.barang.getMerk();
                }
                if (item.barang.getJenis() != null && !item.barang.getJenis().isEmpty()) {
                    extra += " • Jenis: " + item.barang.getJenis();
                }
                lblStokSekarang.setText("Stok saat ini: " + item.barang.getStok() + " " + item.barang.getSatuan()
                        + " • Harga Beli saat ini: " + Fmt.rupiah(item.barang.getHargaBeli()) + extra);
                txtHargaBeli.setText(Fmt.angka(item.barang.getHargaBeli()));
            } else {
                lblStokSekarang.setText("-");
                txtHargaBeli.setText("");
            }
        }

        private void simpanRestock() {
            BarangItem item = (BarangItem) cmbBarang.getSelectedItem();
            if (item == null) {
                Dialogs.peringatan(this, "Pilih barang terlebih dahulu.");
                return;
            }
            Integer jumlah = Fmt.parseBulat(txtJumlah.getText());
            if (jumlah == null || jumlah <= 0) {
                Dialogs.peringatan(this, "Masukkan jumlah restock yang valid (minimal 1).");
                return;
            }
            BigDecimal hargaBeli = Fmt.parseAngka(txtHargaBeli.getText());

            Restock restock = new Restock();
            restock.setBarangId(item.barang.getId());
            restock.setJumlah(jumlah);
            restock.setHargaBeli(hargaBeli);
            restock.setCatatan(txtKeterangan.getText().trim());

            try {
                restockService.simpan(restock);
                Dialogs.info(this, "Stok \"" + item.barang.getNama() + "\" berhasil ditambah sebanyak +" + jumlah + ".");
                txtScanBarcode.setText("");
                txtJumlah.setText("");
                txtHargaBeli.setText("");
                txtKeterangan.setText("");
                reload();
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal melakukan restock: " + ex.getMessage());
            }
        }

        private record BarangItem(Barang barang) {
            @Override
            public String toString() {
                return barang.getNama();
            }
        }
    }
}
