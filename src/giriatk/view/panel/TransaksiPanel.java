package giriatk.view.panel;

import giriatk.dao.BarangDao;
import giriatk.dao.LayananDao;
import giriatk.model.Barang;
import giriatk.model.Layanan;
import giriatk.model.TipeItem;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;
import giriatk.service.TransaksiService;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Icons;
import giriatk.util.ImageUtil;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TransaksiPanel extends BasePanel {

    private static final int KOLOM_AKSI = 5;

    private final BarangDao barangDao = new BarangDao();
    private final LayananDao layananDao = new LayananDao();
    private final TransaksiService service = new TransaksiService();

    private final List<Katalog> katalogData = new ArrayList<>();
    private final List<TransaksiItem> keranjang = new ArrayList<>();

    private final JTextField txtCariKatalog = UiKit.textField("Cari barang atau layanan jasa...");
    private final DefaultTableModel modelKatalog;
    private final UiKit.AppTable tableKatalog;

    private final JPanel daftarKeranjang = new JPanel();

    private final JLabel lblSubtotal = new JLabel("Rp 0");
    private final JLabel lblTotal = new JLabel("Rp 0");
    private final JLabel lblKembali = new JLabel("Rp 0");
    private final JLabel lblItemCount = new JLabel("0 item");
    private final JTextField txtBayar = UiKit.textField("0");
    private final JTextField txtPelanggan = UiKit.textField("Nama Pelanggan (untuk antrian jasa)");
    private final JCheckBox chkBuatAntrian = UiKit.checkBox("Otomatis masukkan item jasa ke Antrian", true);

    private final UiKit.ToggleButton tabBarang = new UiKit.ToggleButton("Barang & ATK", Theme.ACCENT);
    private final UiKit.ToggleButton tabJasa = new UiKit.ToggleButton("Jasa Cetak / Print", Theme.ACCENT);

    private boolean modeBarang = true;
    private BigDecimal total = BigDecimal.ZERO;

    public TransaksiPanel() {
        super(new BorderLayout());

        modelKatalog = new DefaultTableModel(new String[]{"", "Item Katalog", "Satuan", "Harga Satuan", "Stok", "Aksi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableKatalog = new UiKit.AppTable(modelKatalog);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gcKatalog = new GridBagConstraints();
        gcKatalog.gridx = 0;
        gcKatalog.gridy = 0;
        gcKatalog.weightx = 0.58;
        gcKatalog.weighty = 1.0;
        gcKatalog.fill = GridBagConstraints.BOTH;
        gcKatalog.insets = new Insets(0, 0, 0, 16);
        body.add(buildKatalogCard(), gcKatalog);

        GridBagConstraints gcKeranjang = new GridBagConstraints();
        gcKeranjang.gridx = 1;
        gcKeranjang.gridy = 0;
        gcKeranjang.weightx = 0.42;
        gcKeranjang.weighty = 1.0;
        gcKeranjang.fill = GridBagConstraints.BOTH;
        body.add(buildKeranjangCard(), gcKeranjang);

        add(pageHeader("Terminal Kasir (POS)", "Pilih item dari katalog, sesuaikan kuantitas keranjang, dan proses pembayaran."), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        pilihMode(true);
        reloadKatalog();
        refreshKeranjang();
    }

    // ================= KATALOG =================

    private JPanel buildKatalogCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        tabBarang.addActionListener(e -> pilihMode(true));
        tabJasa.addActionListener(e -> pilihMode(false));
        
        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modeRow.setOpaque(false);
        modeRow.add(tabBarang);
        modeRow.add(tabJasa);

        txtCariKatalog.setPreferredSize(new Dimension(240, 38));
        txtCariKatalog.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { reloadKatalog(); }
            @Override
            public void removeUpdate(DocumentEvent e) { reloadKatalog(); }
            @Override
            public void changedUpdate(DocumentEvent e) { reloadKatalog(); }
        });

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(modeRow, BorderLayout.WEST);
        toolbar.add(txtCariKatalog, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        tableKatalog.setRowHeight(48);
        tableKatalog.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setIcon(value instanceof ImageIcon icon ? icon : null);
                setText("");
                return this;
            }
        });
        tableKatalog.getColumnModel().getColumn(0).setMinWidth(52);
        tableKatalog.getColumnModel().getColumn(0).setMaxWidth(54);
        tableKatalog.getColumnModel().getColumn(0).setPreferredWidth(52);
        tableKatalog.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
        tableKatalog.getColumnModel().getColumn(2).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
        tableKatalog.getColumnModel().getColumn(3).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT));
        tableKatalog.getColumnModel().getColumn(4).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
        tableKatalog.getColumnModel().getColumn(KOLOM_AKSI).setCellRenderer(new DefaultTableCellRenderer() {
            private final TambahCell cell = new TambahCell();

            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                return cell;
            }
        });
        tableKatalog.getColumnModel().getColumn(KOLOM_AKSI).setMinWidth(96);
        tableKatalog.getColumnModel().getColumn(KOLOM_AKSI).setMaxWidth(100);
        tableKatalog.getColumnModel().getColumn(KOLOM_AKSI).setPreferredWidth(96);

        tableKatalog.setEmptyState("Katalog tidak ditemukan", "Coba kata kunci lain atau tambahkan item.");
        tableKatalog.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tableKatalog.rowAtPoint(e.getPoint());
                int col = tableKatalog.columnAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                int modelRow = tableKatalog.convertRowIndexToModel(row);
                if (modelRow < 0 || modelRow >= katalogData.size()) {
                    return;
                }
                if (col == KOLOM_AKSI || e.getClickCount() == 2) {
                    tambahKeKeranjang(katalogData.get(modelRow));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tableKatalog);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private static class TambahCell extends JPanel {

        TambahCell() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = 28;
            int w = Math.min(getWidth() - 14, 82);
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;
            
            g2.setColor(Theme.ACCENT_SOFT);
            g2.fillRoundRect(x, y, w, h, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            g2.setColor(Theme.alpha(Theme.ACCENT, 100));
            g2.drawRoundRect(x, y, w - 1, h - 1, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);

            Graphics2D gi = (Graphics2D) g2.create();
            gi.translate(x + 8, y + (h - 12) / 2);
            Icons.paint(gi, Icons.Name.PLUS, 12, Theme.ACCENT);
            gi.dispose();

            g2.setColor(Theme.ACCENT);
            g2.setFont(Theme.text(Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("Tambah", x + 26, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ================= KERANJANG =================

    private JPanel buildKeranjangCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Keranjang Transaksi");
        title.setFont(Theme.display(Font.BOLD, 15));
        title.setForeground(Theme.INK);
        
        lblItemCount.setFont(Theme.mono(Font.PLAIN, 11));
        lblItemCount.setForeground(Theme.ACCENT);
        titleBox.add(title);
        titleBox.add(lblItemCount);
        head.add(titleBox, BorderLayout.WEST);

        UiKit.SoftButton kosong = new UiKit.SoftButton("Kosongkan", UiKit.Variant.SUBTLE);
        kosong.addActionListener(e -> kosongkan());
        head.add(kosong, BorderLayout.EAST);

        card.add(head, BorderLayout.NORTH);

        daftarKeranjang.setOpaque(false);
        daftarKeranjang.setLayout(new BoxLayout(daftarKeranjang, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(daftarKeranjang);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        card.add(buildCheckout(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildCheckout() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Box.createVerticalStrut(10));
        panel.add(barisInfo("Subtotal", lblSubtotal, 13, Theme.MUTED, Theme.INK_SOFT));
        panel.add(Box.createVerticalStrut(6));
        
        // Total Box
        JPanel totalBox = new JPanel(new BorderLayout());
        totalBox.setBackground(Theme.SURFACE_ELEVATED);
        totalBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_STRONG),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        totalBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        
        JLabel lblTotalTag = new JLabel("TOTAL TAGIHAN");
        lblTotalTag.setFont(Theme.mono(Font.BOLD, 11));
        lblTotalTag.setForeground(Theme.MUTED);
        
        lblTotal.setFont(Theme.mono(Font.BOLD, 20));
        lblTotal.setForeground(Theme.ACCENT);
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        
        totalBox.add(lblTotalTag, BorderLayout.WEST);
        totalBox.add(lblTotal, BorderLayout.EAST);
        panel.add(totalBox);
        panel.add(Box.createVerticalStrut(12));

        // Quick Cash Buttons (Uang Pas, 10k, 20k, 50k, 100k)
        JPanel quickCash = new JPanel(new GridLayout(1, 5, 4, 0));
        quickCash.setOpaque(false);
        quickCash.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        quickCash.add(quickCashBtn("Pas", () -> txtBayar.setText(Fmt.angka(total))));
        quickCash.add(quickCashBtn("10k", () -> txtBayar.setText("10000")));
        quickCash.add(quickCashBtn("20k", () -> txtBayar.setText("20000")));
        quickCash.add(quickCashBtn("50k", () -> txtBayar.setText("50000")));
        quickCash.add(quickCashBtn("100k", () -> txtBayar.setText("100000")));
        panel.add(quickCash);
        panel.add(Box.createVerticalStrut(8));

        // Uang Diterima / Bayar
        JPanel bayarRow = new JPanel(new BorderLayout(10, 0));
        bayarRow.setOpaque(false);
        bayarRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JLabel lblBayar = new JLabel("Uang Diterima");
        lblBayar.setFont(Theme.text(Font.BOLD, 12));
        lblBayar.setForeground(Theme.INK);
        lblBayar.setPreferredSize(new Dimension(95, 26));
        
        txtBayar.setHorizontalAlignment(SwingConstants.RIGHT);
        txtBayar.setFont(Theme.mono(Font.BOLD, 15));
        txtBayar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateKembali(); }
            @Override public void removeUpdate(DocumentEvent e) { updateKembali(); }
            @Override public void changedUpdate(DocumentEvent e) { updateKembali(); }
        });
        bayarRow.add(lblBayar, BorderLayout.WEST);
        bayarRow.add(txtBayar, BorderLayout.CENTER);
        panel.add(bayarRow);
        panel.add(Box.createVerticalStrut(6));

        // Kembali
        panel.add(barisInfo("Kembali", lblKembali, 14, Theme.MUTED, Theme.GREEN_FG));
        panel.add(Box.createVerticalStrut(10));

        // Info Pelanggan & Checkbox Antrian
        JPanel antrianRow = new JPanel(new BorderLayout(0, 4));
        antrianRow.setOpaque(false);
        antrianRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        txtPelanggan.setPreferredSize(new Dimension(0, 36));
        antrianRow.add(txtPelanggan, BorderLayout.NORTH);
        antrianRow.add(chkBuatAntrian, BorderLayout.CENTER);
        panel.add(antrianRow);
        panel.add(Box.createVerticalStrut(14));

        UiKit.SoftButton simpan = new UiKit.SoftButton("PROSES TRANSAKSI & CETAK", UiKit.Variant.PRIMARY);
        simpan.setAlignmentX(Component.LEFT_ALIGNMENT);
        simpan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        simpan.setPreferredSize(new Dimension(0, 46));
        simpan.setFont(Theme.text(Font.BOLD, 13));
        simpan.addActionListener(e -> simpanTransaksi());
        panel.add(simpan);
        return panel;
    }

    private UiKit.SoftButton quickCashBtn(String label, Runnable act) {
        UiKit.SoftButton b = new UiKit.SoftButton(label, UiKit.Variant.GHOST);
        b.setFont(Theme.mono(Font.PLAIN, 10));
        b.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        b.addActionListener(e -> act.run());
        return b;
    }

    private JPanel barisInfo(String label, JLabel value, int valSize, Color labelColor, Color valColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.text(Font.BOLD, 12));
        lbl.setForeground(labelColor);
        
        value.setFont(Theme.mono(Font.BOLD, valSize));
        value.setForeground(valColor);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        
        panel.add(lbl, BorderLayout.WEST);
        panel.add(value, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildItemRow(TransaksiItem item) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        row.setPreferredSize(new Dimension(0, 78));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        JLabel nama = new JLabel(item.getNamaItem());
        nama.setFont(Theme.text(Font.BOLD, 13));
        nama.setForeground(Theme.INK);

        JLabel subtotal = new JLabel(Fmt.rupiah(item.getSubtotal()));
        subtotal.setFont(Theme.mono(Font.BOLD, 13));
        subtotal.setForeground(Theme.ACCENT);

        JPanel baris1 = new JPanel(new BorderLayout(8, 0));
        baris1.setOpaque(false);
        baris1.setAlignmentX(Component.LEFT_ALIGNMENT);
        baris1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        baris1.add(nama, BorderLayout.CENTER);
        baris1.add(subtotal, BorderLayout.EAST);

        JLabel harga = new JLabel(Fmt.rupiah(item.getHarga()) + " / item");
        harga.setFont(Theme.mono(Font.PLAIN, 11));
        harga.setForeground(Theme.MUTED);

        UiKit.IconButton minus = new UiKit.IconButton(Icons.Name.MINUS, Theme.MUTED, 12, true);
        minus.setToolTipText("Kurangi");
        minus.addActionListener(e -> ubahQty(item, -1));

        QtyPill qty = new QtyPill(item.getQty());

        UiKit.IconButton plus = new UiKit.IconButton(Icons.Name.PLUS, Theme.ACCENT, 12, true);
        plus.setToolTipText("Tambah");
        plus.addActionListener(e -> ubahQty(item, 1));

        UiKit.IconButton hapus = new UiKit.IconButton(Icons.Name.TRASH, Theme.DANGER, 13);
        hapus.setToolTipText("Hapus item");
        hapus.addActionListener(e -> hapusItem(item));

        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        stepper.setOpaque(false);
        stepper.add(minus);
        stepper.add(qty);
        stepper.add(plus);
        stepper.add(Box.createHorizontalStrut(6));
        stepper.add(hapus);

        JPanel baris2 = new JPanel(new BorderLayout(8, 0));
        baris2.setOpaque(false);
        baris2.setAlignmentX(Component.LEFT_ALIGNMENT);
        baris2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        baris2.add(harga, BorderLayout.WEST);
        baris2.add(stepper, BorderLayout.EAST);

        row.add(baris1);
        row.add(Box.createVerticalStrut(4));
        row.add(baris2);
        return row;
    }

    private void pilihMode(boolean barang) {
        modeBarang = barang;
        tabBarang.setActive(barang);
        tabJasa.setActive(!barang);
        txtCariKatalog.setText("");
        reloadKatalog();
    }

    private void reloadKatalog() {
        try {
            katalogData.clear();
            String keyword = txtCariKatalog.getText().trim().toLowerCase();
            if (modeBarang) {
                for (Barang b : barangDao.findAktif()) {
                    if (cocok(keyword, b.getKode(), b.getNama(), b.getMerk(), b.getJenis(), b.getWarna(), b.getUkuran())) {
                        katalogData.add(new Katalog(TipeItem.BARANG, b.getId(), b.getNama(), b.getLabelLengkap(),
                                b.getSatuan(), b.getHargaJual(), b.getStok(), b.getHargaBeli(), b.getGambar()));
                    }
                }
            } else {
                for (Layanan l : layananDao.findAllAktif()) {
                    if (cocok(keyword, l.getNama())) {
                        katalogData.add(new Katalog(TipeItem.JASA, l.getId(), l.getNama(), l.getNama(),
                                l.getSatuan(), l.getHargaSatuan(), null, BigDecimal.ZERO, null));
                    }
                }
            }
            modelKatalog.setRowCount(0);
            for (Katalog k : katalogData) {
                modelKatalog.addRow(new Object[]{
                    ImageUtil.getThumbnail(k.gambar(), 34, 34),
                    k.label(),
                    k.satuan(),
                    Fmt.rupiah(k.harga()),
                    k.stok() == null ? "-" : String.valueOf(k.stok()),
                    ""
                });
            }
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal memuat katalog: " + ex.getMessage());
        }
    }

    private boolean cocok(String keyword, String... values) {
        if (keyword.isEmpty()) {
            return true;
        }
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void tambahKeKeranjang(Katalog katalog) {
        TransaksiItem existing = cariDiKeranjang(katalog);
        if (existing != null) {
            if (katalog.tipe() == TipeItem.BARANG && katalog.stok() != null && existing.getQty() >= katalog.stok()) {
                Dialogs.peringatan(this, "Stok \"" + katalog.nama() + "\" hanya tersisa " + katalog.stok() + ".");
                return;
            }
            existing.setQty(existing.getQty() + 1);
        } else {
            if (katalog.tipe() == TipeItem.BARANG && katalog.stok() != null && katalog.stok() <= 0) {
                Dialogs.peringatan(this, "Stok \"" + katalog.nama() + "\" sedang habis.");
                return;
            }
            TransaksiItem item = new TransaksiItem();
            item.setTipe(katalog.tipe());
            if (katalog.tipe() == TipeItem.BARANG) {
                item.setBarangId(katalog.id());
            } else {
                item.setLayananId(katalog.id());
            }
            item.setNamaItem(katalog.label());
            item.setQty(1);
            item.setHarga(katalog.harga());
            item.setHargaBeli(katalog.hargaBeli());
            keranjang.add(item);
        }
        refreshKeranjang();
    }

    private TransaksiItem cariDiKeranjang(Katalog katalog) {
        for (TransaksiItem item : keranjang) {
            if (katalog.tipe() == TipeItem.BARANG && item.getBarangId() != null && item.getBarangId() == katalog.id()) {
                return item;
            }
            if (katalog.tipe() == TipeItem.JASA && item.getLayananId() != null && item.getLayananId() == katalog.id()) {
                return item;
            }
        }
        return null;
    }

    private void ubahQty(TransaksiItem item, int delta) {
        int qty = item.getQty() + delta;
        if (qty < 1) {
            return;
        }
        if (item.getTipe() == TipeItem.BARANG && item.getBarangId() != null) {
            try {
                Barang barang = barangDao.findById(item.getBarangId());
                if (barang != null && qty > barang.getStok()) {
                    Dialogs.peringatan(this, "Stok \"" + barang.getNama() + "\" hanya tersisa " + barang.getStok() + ".");
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        item.setQty(qty);
        refreshKeranjang();
    }

    private void hapusItem(TransaksiItem item) {
        keranjang.remove(item);
        refreshKeranjang();
    }

    private void kosongkan() {
        if (keranjang.isEmpty()) {
            return;
        }
        if (!Dialogs.konfirmasi(this, "Kosongkan semua item dari keranjang?")) {
            return;
        }
        keranjang.clear();
        txtBayar.setText("");
        txtPelanggan.setText("");
        refreshKeranjang();
    }

    private void refreshKeranjang() {
        BigDecimal subtotal = BigDecimal.ZERO;
        int count = 0;
        for (TransaksiItem item : keranjang) {
            BigDecimal harga = item.getHarga() == null ? BigDecimal.ZERO : item.getHarga();
            item.setSubtotal(harga.multiply(BigDecimal.valueOf(item.getQty())));
            subtotal = subtotal.add(item.getSubtotal());
            count += item.getQty();
        }
        total = subtotal;
        lblSubtotal.setText(Fmt.rupiah(subtotal));
        lblTotal.setText(Fmt.rupiah(total));
        lblItemCount.setText(count + " item");

        rebuildKeranjangRows();
        updateKembali();

        boolean tampilAntrian = !keranjang.isEmpty() && adaJasa();
        chkBuatAntrian.setVisible(tampilAntrian);
        txtPelanggan.setVisible(tampilAntrian);
        revalidate();
        repaint();
    }

    private void rebuildKeranjangRows() {
        daftarKeranjang.removeAll();
        if (keranjang.isEmpty()) {
            JPanel kosong = new JPanel(new GridBagLayout());
            kosong.setOpaque(false);
            kosong.setPreferredSize(new Dimension(0, 160));
            JLabel info = new JLabel("<html><div style='text-align:center;'><b>Keranjang Masih Kosong</b><br>"
                    + "<span style='color:#64748B;'>Klik tombol Tambah pada daftar katalog</span></div></html>");
            info.setFont(Theme.text(Font.PLAIN, 12));
            info.setForeground(Theme.MUTED);
            info.setHorizontalAlignment(SwingConstants.CENTER);
            kosong.add(info);
            daftarKeranjang.add(kosong);
        } else {
            for (TransaksiItem item : keranjang) {
                daftarKeranjang.add(buildItemRow(item));
            }
            daftarKeranjang.add(Box.createVerticalGlue());
        }
        daftarKeranjang.revalidate();
        daftarKeranjang.repaint();
    }

    private void updateKembali() {
        BigDecimal bayar = Fmt.parseAngka(txtBayar.getText());
        if (bayar == null) {
            bayar = BigDecimal.ZERO;
        }
        BigDecimal kembali = bayar.subtract(total);
        if (kembali.signum() < 0) {
            lblKembali.setText("Kurang " + Fmt.rupiah(kembali.abs()));
            lblKembali.setForeground(Theme.DANGER);
        } else {
            lblKembali.setText(Fmt.rupiah(kembali));
            lblKembali.setForeground(Theme.GREEN_FG);
        }
    }

    private boolean adaJasa() {
        return keranjang.stream().anyMatch(i -> i.getTipe() == TipeItem.JASA);
    }

    private void simpanTransaksi() {
        if (keranjang.isEmpty()) {
            Dialogs.peringatan(this, "Keranjang masih kosong.");
            return;
        }
        BigDecimal bayar = Fmt.parseAngka(txtBayar.getText());
        if (bayar == null) {
            Dialogs.peringatan(this, "Masukkan jumlah uang yang diterima.");
            return;
        }
        if (bayar.compareTo(total) < 0) {
            Dialogs.peringatan(this, "Jumlah bayar kurang dari total tagihan.");
            return;
        }

        Transaksi transaksi = new Transaksi();
        transaksi.setUserId(Session.user() == null ? null : Session.user().getId());
        transaksi.setDiskon(BigDecimal.ZERO);
        transaksi.setBayar(bayar);
        transaksi.setBuatAntrian(chkBuatAntrian.isVisible() && chkBuatAntrian.isSelected());
        transaksi.setNamaPelanggan(txtPelanggan.getText().trim().isEmpty() ? null : txtPelanggan.getText().trim());
        transaksi.getItems().addAll(keranjang);

        try {
            Transaksi tersimpan = service.simpan(transaksi);
            Dialogs.info(this, "Transaksi Berhasil Disimpan!\n\n"
                    + "Kode Nota: " + tersimpan.getKode() + "\n"
                    + "Total: " + Fmt.rupiah(tersimpan.getTotal()) + "\n"
                    + "Bayar: " + Fmt.rupiah(tersimpan.getBayar()) + "\n"
                    + "Kembali: " + Fmt.rupiah(tersimpan.getKembali()));
            keranjang.clear();
            txtBayar.setText("");
            txtPelanggan.setText("");
            refreshKeranjang();
            reloadKatalog();
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal menyimpan transaksi:\n" + ex.getMessage());
        }
    }

    @Override
    public void onShow() {
        reloadKatalog();
    }

    private static class QtyPill extends JLabel {

        QtyPill(int qty) {
            super(String.valueOf(qty));
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(Theme.mono(Font.BOLD, 12));
            setForeground(Theme.INK);
            setPreferredSize(new Dimension(30, 26));
            setMaximumSize(new Dimension(30, 26));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SURFACE_ELEVATED);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private record Katalog(TipeItem tipe, int id, String nama, String label, String satuan,
            BigDecimal harga, Integer stok, BigDecimal hargaBeli, String gambar) {
    }
}
