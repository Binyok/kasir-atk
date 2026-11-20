package giriatk.view.panel;

import giriatk.dao.BarangDao;
import giriatk.dao.LayananDao;
import giriatk.model.Barang;
import giriatk.model.Layanan;
import giriatk.model.TipeItem;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;
import giriatk.service.ThermalPrintService;
import giriatk.service.TransaksiService;
import giriatk.view.dialog.MidtransPaymentDialog;
import giriatk.view.dialog.TransaksiDetailDialog;
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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.SwingUtilities;
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
import java.awt.Window;
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
    private final ThermalPrintService printService = new ThermalPrintService();

    private final List<Katalog> katalogData = new ArrayList<>();
    private final List<TransaksiItem> keranjang = new ArrayList<>();

    private final JTextField txtCariKatalog = UiKit.textField("Cari barang atau layanan jasa...");
    private final DefaultTableModel modelKatalog;
    private final UiKit.AppTable tableKatalog;

    private final JPanel daftarKeranjang = new JPanel();

    private final JLabel lblSubtotal = new JLabel("Rp 0");
    private final JLabel lblTotal = new JLabel("Rp 0");
    private final JLabel lblItemCount = new JLabel("0 item");
    private final JTextField txtDiskon = UiKit.textField("0");
    private final JTextField txtPelanggan = UiKit.textField("Nama Pelanggan (untuk antrian jasa)");
    private final JCheckBox chkBuatAntrian = UiKit.checkBox("Otomatis masukkan item jasa ke Antrian", true);

    private final UiKit.ToggleButton tabBarang = new UiKit.ToggleButton("Barang & ATK", Theme.ACCENT);
    private final UiKit.ToggleButton tabJasa = new UiKit.ToggleButton("Jasa Cetak / Print", Theme.ACCENT);

    private boolean modeBarang = true;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;

    public TransaksiPanel() {
        super(new BorderLayout());

        modelKatalog = new DefaultTableModel(new String[]{"", "Item Katalog", "Satuan", "Harga", "Stok", "Aksi"}, 0) {
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
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        tabBarang.addActionListener(e -> pilihMode(true));
        tabJasa.addActionListener(e -> pilihMode(false));

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modeRow.setOpaque(false);
        modeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeRow.add(tabBarang);
        modeRow.add(tabJasa);

        // Kolom pencarian dibuat satu baris penuh di bawah tab. Pola lama
        // (tab di kiri + pencarian di kanan dalam satu baris BorderLayout)
        // saling menekan di jendela sempit: teks tab dan placeholder
        // pencarian tumpang tindih dan tabel ikut bergeser.
        txtCariKatalog.setPreferredSize(new Dimension(0, 36));
        txtCariKatalog.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtCariKatalog.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtCariKatalog.addActionListener(e -> scanBarcodeAtauPilihFirstItem());
        txtCariKatalog.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { reloadKatalog(); }
            @Override
            public void removeUpdate(DocumentEvent e) { reloadKatalog(); }
            @Override
            public void changedUpdate(DocumentEvent e) { reloadKatalog(); }
        });

        JPanel toolbar = new JPanel();
        toolbar.setOpaque(false);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.add(modeRow);
        toolbar.add(Box.createVerticalStrut(8));
        toolbar.add(txtCariKatalog);
        card.add(toolbar, BorderLayout.NORTH);

        tableKatalog.setRowHeight(64);
        // AppTable bawaan memakai AUTO_RESIZE_ALL_COLUMNS yang membagi
        // penyusutan ke semua kolom. Di sini diganti supaya hanya kolom
        // Item yang menyerap perubahan lebar.
        tableKatalog.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        tableKatalog.getTableHeader().setResizingAllowed(false);
        tableKatalog.getTableHeader().setReorderingAllowed(false);
        // Renderer header bawaan AppTable memakai padding kiri-kanan 14px,
        // sehingga "SATUAN" dan "STOK" terpotong di kolomnya yang sempit.
        // Khusus tabel katalog, padding dipersempit.
        tableKatalog.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(Theme.SURFACE_ELEVATED);
                label.setForeground(Theme.MUTED);
                label.setFont(Theme.text(Font.BOLD, 10));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                        BorderFactory.createEmptyBorder(0, 8, 0, 6)));
                label.setText(value == null ? "" : value.toString().toUpperCase());
                return label;
            }
        });
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
        tableKatalog.getColumnModel().getColumn(0).setMinWidth(44);
        tableKatalog.getColumnModel().getColumn(0).setMaxWidth(46);
        tableKatalog.getColumnModel().getColumn(0).setPreferredWidth(44);
        // Lebar kolom dikunci dengan min/max supaya Satuan, Harga, dan Stok
        // tidak menyusut jadi "S...", "HARG...", "..." di jendela sempit.
        // Hanya kolom Item Katalog yang fleksibel menyerap sisa lebar.
        int[] lebarKatalog = {44, 170, 56, 90, 44, 80};
        int[] minKatalog = {44, 100, 56, 86, 44, 74};
        int[] maxKatalog = {46, Integer.MAX_VALUE, 60, 96, 48, 84};
        for (int i = 0; i < lebarKatalog.length; i++) {
            tableKatalog.getColumnModel().getColumn(i).setPreferredWidth(lebarKatalog[i]);
            tableKatalog.getColumnModel().getColumn(i).setMinWidth(minKatalog[i]);
            tableKatalog.getColumnModel().getColumn(i).setMaxWidth(maxKatalog[i]);
        }
        tableKatalog.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String teks = String.valueOf(value);
                int pisah = teks.indexOf(" · ");
                String utama = pisah > 0 ? teks.substring(0, pisah) : teks;
                String detail = pisah > 0 ? teks.substring(pisah + 3) : "";
                utama = utama.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                detail = detail.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                setText("<html><b>" + utama + "</b>" + (detail.isBlank() ? "" : "<br><font color='#78716C' size='2'>" + detail + "</font>") + "</html>");
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 6));
                return this;
            }
        });
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
        tableKatalog.getColumnModel().getColumn(KOLOM_AKSI).setMinWidth(74);
        tableKatalog.getColumnModel().getColumn(KOLOM_AKSI).setMaxWidth(84);
        tableKatalog.getColumnModel().getColumn(KOLOM_AKSI).setPreferredWidth(80);

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
            int w = Math.min(getWidth() - 6, 80);
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;
            
            g2.setColor(Theme.ACCENT_SOFT);
            g2.fillRoundRect(x, y, w, h, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            g2.setColor(Theme.alpha(Theme.ACCENT, 100));
            g2.drawRoundRect(x, y, w - 1, h - 1, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);

            Graphics2D gi = (Graphics2D) g2.create();
            gi.translate(x + 6, y + (h - 12) / 2);
            Icons.paint(gi, Icons.Name.PLUS, 12, Theme.ACCENT);
            gi.dispose();

            // Ukuran font teks dipilih yang paling besar tapi tetap muat di
            // dalam kotak tombol, supaya "Tambah" tidak pernah terpotong
            // di kolom sempit.
            g2.setColor(Theme.ACCENT);
            Font fontTombol = Theme.text(Font.BOLD, 11);
            for (int ukuran : new int[]{11, 10, 9}) {
                fontTombol = Theme.text(Font.BOLD, ukuran);
                if (g2.getFontMetrics(fontTombol).stringWidth("Tambah") <= w - 28) {
                    break;
                }
            }
            g2.setFont(fontTombol);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("Tambah", x + 22, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ================= KERANJANG =================

    private JPanel buildKeranjangCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

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

        panel.add(Box.createVerticalStrut(8));
        panel.add(barisInfo("Subtotal", lblSubtotal, 13, Theme.MUTED, Theme.INK_SOFT));
        panel.add(Box.createVerticalStrut(4));

        // Diskon manual (Rp) — satu baris ramping sejajar label, bukan
        // field tinggi penuh, supaya daftar keranjang tidak terdesak.
        JPanel diskonRow = new JPanel(new BorderLayout(10, 0));
        diskonRow.setOpaque(false);
        diskonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lblDiskon = new JLabel("Diskon Manual (Rp)");
        lblDiskon.setFont(Theme.text(Font.BOLD, 12));
        lblDiskon.setForeground(Theme.INK);
        lblDiskon.setPreferredSize(new Dimension(118, 26));
        txtDiskon.setHorizontalAlignment(SwingConstants.RIGHT);
        txtDiskon.setFont(Theme.mono(Font.BOLD, 13));
        txtDiskon.setPreferredSize(new Dimension(0, 30));
        txtDiskon.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { hitungTotal(); }
            @Override public void removeUpdate(DocumentEvent e) { hitungTotal(); }
            @Override public void changedUpdate(DocumentEvent e) { hitungTotal(); }
        });
        diskonRow.add(lblDiskon, BorderLayout.WEST);
        diskonRow.add(txtDiskon, BorderLayout.CENTER);
        panel.add(diskonRow);
        panel.add(Box.createVerticalStrut(6));

        // Total Box
        JPanel totalBox = new JPanel(new BorderLayout());
        totalBox.setBackground(Theme.SURFACE_ELEVATED);
        totalBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_STRONG),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        totalBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lblTotalTag = new JLabel("TOTAL TAGIHAN");
        lblTotalTag.setFont(Theme.mono(Font.BOLD, 11));
        lblTotalTag.setForeground(Theme.MUTED);

        lblTotal.setFont(Theme.mono(Font.BOLD, 18));
        lblTotal.setForeground(Theme.ACCENT);
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);

        totalBox.add(lblTotalTag, BorderLayout.WEST);
        totalBox.add(lblTotal, BorderLayout.EAST);
        panel.add(totalBox);
        panel.add(Box.createVerticalStrut(8));

        // Area uang (tombol cepat Pas/10k-100k, Uang Diterima, Kembali)
        // sengaja dihapus atas permintaan pemilik toko: pembayaran tunai
        // dianggap uang pas sebesar total tagihan, dan ruang vertikalnya
        // dipakai untuk memperpanjang daftar keranjang di atasnya
        // (panel checkout ini berada di BorderLayout.SOUTH, jadi makin
        // pendek checkout-nya makin panjang area keranjangnya).

        // Info Pelanggan & Checkbox Antrian — tingginya TETAP dan selalu
        // tampil. Pola lama menyembunyikan/menampilkan area ini tiap isi
        // keranjang berubah, sehingga panel checkout berubah tinggi dan
        // seluruh tampilan terasa bergeser/membesar sendiri. Sekarang
        // checkbox hanya di-nonaktifkan bila tidak ada item jasa.
        JPanel antrianRow = new JPanel(new BorderLayout(0, 4));
        antrianRow.setOpaque(false);
        antrianRow.setPreferredSize(new Dimension(0, 60));
        antrianRow.setMinimumSize(new Dimension(0, 60));
        antrianRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        txtPelanggan.setPreferredSize(new Dimension(0, 30));
        antrianRow.add(txtPelanggan, BorderLayout.NORTH);
        antrianRow.add(chkBuatAntrian, BorderLayout.CENTER);
        panel.add(antrianRow);
        panel.add(Box.createVerticalStrut(10));

        UiKit.SoftButton bayarMidtrans = new UiKit.SoftButton("BAYAR VIA MIDTRANS (QRIS/GoPay)", UiKit.Variant.GHOST);
        bayarMidtrans.setAlignmentX(Component.CENTER_ALIGNMENT);
        bayarMidtrans.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        bayarMidtrans.setPreferredSize(new Dimension(0, 34));
        bayarMidtrans.setFont(Theme.text(Font.BOLD, 11));
        bayarMidtrans.addActionListener(e -> simpanTransaksiMidtrans());
        panel.add(bayarMidtrans);
        panel.add(Box.createVerticalStrut(5));

        UiKit.SoftButton simpan = new UiKit.SoftButton("PROSES TRANSAKSI (TUNAI) & CETAK", UiKit.Variant.PRIMARY);
        simpan.setAlignmentX(Component.CENTER_ALIGNMENT);
        simpan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        simpan.setPreferredSize(new Dimension(0, 40));
        simpan.setFont(Theme.text(Font.BOLD, 12));
        simpan.addActionListener(e -> simpanTransaksi());
        panel.add(simpan);
        return panel;
    }

    /** Diskon manual dari field, dibatasi 0..subtotal. */
    private BigDecimal bacaDiskon() {
        BigDecimal d = Fmt.parseAngka(txtDiskon.getText());
        if (d == null || d.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return d.min(subtotal);
    }

    private void hitungTotal() {
        total = subtotal.subtract(bacaDiskon()).max(BigDecimal.ZERO);
        lblTotal.setText(Fmt.rupiah(total));
    }

    private JPanel barisInfo(String label, JLabel value, int valSize, Color labelColor, Color valColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setPreferredSize(new Dimension(0, 22));
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
        row.setPreferredSize(new Dimension(0, 70));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Nama item bisa sangat panjang (label lengkap barang). JLabel di
        // BorderLayout.CENTER akan terpotong ellipsis secara otomatis asal
        // lebar minimumnya kecil; nama lengkap tetap terbaca via tooltip,
        // dan angka subtotal di sisi kanan tidak pernah terdesak keluar.
        JLabel nama = new JLabel(item.getNamaItem());
        nama.setFont(Theme.text(Font.BOLD, 13));
        nama.setForeground(Theme.INK);
        nama.setToolTipText(item.getNamaItem());
        nama.setMinimumSize(new Dimension(40, 16));

        JLabel subtotal = new JLabel(Fmt.rupiah(item.getSubtotal()));
        subtotal.setFont(Theme.mono(Font.BOLD, 13));
        subtotal.setForeground(Theme.ACCENT);

        JPanel baris1 = new JPanel(new BorderLayout(8, 0));
        baris1.setOpaque(false);
        baris1.setAlignmentX(Component.CENTER_ALIGNMENT);
        baris1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        baris1.add(nama, BorderLayout.CENTER);
        baris1.add(subtotal, BorderLayout.EAST);

        JLabel harga = new JLabel(Fmt.rupiah(item.getHarga()) + " / item");
        harga.setFont(Theme.mono(Font.PLAIN, 11));
        harga.setForeground(Theme.MUTED);

        UiKit.IconButton minus = new UiKit.IconButton(Icons.Name.MINUS, Theme.MUTED, 12, true);
        minus.setToolTipText("Kurangi");
        minus.addActionListener(e -> ubahQty(item, -1));

        JTextField qty = buatFieldQty(item);

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
        baris2.setAlignmentX(Component.CENTER_ALIGNMENT);
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

    private void scanBarcodeAtauPilihFirstItem() {
        String input = txtCariKatalog.getText().trim();
        if (input.isEmpty()) {
            return;
        }

        if (modeBarang) {
            try {
                Barang b = barangDao.findByKode(input);
                if (b != null) {
                    if (!b.isAktif()) {
                        Dialogs.peringatan(this, "Barang \"" + b.getNama() + "\" ditemukan tapi statusnya nonaktif.");
                        return;
                    }
                    Katalog k = new Katalog(TipeItem.BARANG, b.getId(), b.getNama(), b.getLabelLengkap(),
                            b.getSatuan(), b.getHargaJual(), b.getStok(), b.getHargaBeli(), b.getGambar());
                    tambahKeKeranjang(k);
                    txtCariKatalog.setText("");
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        // Bukan kode yang terdaftar: hanya perlakukan sebagai pencarian NAMA
        // bila teksnya benar-benar cocok dengan nama/label salah satu hasil.
        // Kode/barcode yang tidak ditemukan TIDAK BOLEH memasukkan barang lain
        // ke keranjang — beri peringatan dan biarkan keranjang tidak berubah.
        for (Katalog k : katalogData) {
            if (k.label().equalsIgnoreCase(input) || k.nama().equalsIgnoreCase(input)) {
                tambahKeKeranjang(k);
                txtCariKatalog.setText("");
                return;
            }
        }
        if (modeBarang) {
            Dialogs.peringatan(this, "Barang dengan kode/nama \"" + input + "\" tidak ditemukan.");
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

    /**
     * Field jumlah di baris keranjang yang bisa diketik langsung, supaya
     * kasir tidak perlu menekan tombol + berkali-kali untuk jumlah besar
     * (mis. print 100 lembar). Nilai diterapkan saat Enter atau field
     * kehilangan fokus; selama mengetik keranjang tidak digambar ulang.
     */
    private JTextField buatFieldQty(TransaksiItem item) {
        JTextField field = new JTextField(String.valueOf(item.getQty()));
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setFont(Theme.mono(Font.BOLD, 12));
        field.setForeground(Theme.INK);
        field.setBackground(Theme.SURFACE_ELEVATED);
        field.setCaretColor(Theme.INK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        field.setPreferredSize(new Dimension(42, 26));
        field.setMaximumSize(new Dimension(42, 26));
        field.setToolTipText("Ketik jumlah, lalu Enter");
        // Hanya angka, maksimal 5 digit.
        ((javax.swing.text.AbstractDocument) field.getDocument()).setDocumentFilter(
                new javax.swing.text.DocumentFilter() {
                    @Override
                    public void insertString(FilterBypass fb, int offset, String text, javax.swing.text.AttributeSet attr)
                            throws javax.swing.text.BadLocationException {
                        if (text != null && text.chars().allMatch(Character::isDigit)
                                && fb.getDocument().getLength() + text.length() <= 5) {
                            super.insertString(fb, offset, text, attr);
                        }
                    }

                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attr)
                            throws javax.swing.text.BadLocationException {
                        if (text == null || text.isEmpty()
                                || (text.chars().allMatch(Character::isDigit)
                                        && fb.getDocument().getLength() - length + text.length() <= 5)) {
                            super.replace(fb, offset, length, text, attr);
                        }
                    }
                });
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.selectAll();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                terapkanQtyKetik(item, field);
            }
        });
        field.addActionListener(e -> terapkanQtyKetik(item, field));
        return field;
    }

    private void terapkanQtyKetik(TransaksiItem item, JTextField field) {
        String teks = field.getText().trim();
        int qtyBaru;
        try {
            qtyBaru = Integer.parseInt(teks);
        } catch (NumberFormatException ex) {
            field.setText(String.valueOf(item.getQty()));
            return;
        }
        if (qtyBaru == item.getQty()) {
            field.setText(String.valueOf(qtyBaru));
            return;
        }
        if (qtyBaru < 1) {
            field.setText(String.valueOf(item.getQty()));
            return;
        }
        if (item.getTipe() == TipeItem.BARANG && item.getBarangId() != null) {
            try {
                Barang barang = barangDao.findById(item.getBarangId());
                if (barang != null && qtyBaru > barang.getStok()) {
                    Dialogs.peringatan(this, "Stok \"" + barang.getNama() + "\" hanya tersisa " + barang.getStok() + ".");
                    field.setText(String.valueOf(item.getQty()));
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        item.setQty(qtyBaru);
        refreshKeranjang();
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
        txtDiskon.setText("");
        txtPelanggan.setText("");
        refreshKeranjang();
    }

    private void refreshKeranjang() {
        BigDecimal sub = BigDecimal.ZERO;
        int count = 0;
        for (TransaksiItem item : keranjang) {
            BigDecimal harga = item.getHarga() == null ? BigDecimal.ZERO : item.getHarga();
            item.setSubtotal(harga.multiply(BigDecimal.valueOf(item.getQty())));
            sub = sub.add(item.getSubtotal());
            count += item.getQty();
        }
        subtotal = sub;
        lblSubtotal.setText(Fmt.rupiah(subtotal));
        lblItemCount.setText(count + " item");

        rebuildKeranjangRows();
        hitungTotal();

        // Area antrian TIDAK lagi disembunyikan/ditampilkan — tingginya
        // tetap, jadi tampilan tidak bergeser saat item dipilih. Checkbox
        // cukup diaktifkan saat ada item jasa di keranjang.
        boolean adaJasa = adaJasa();
        boolean wasEnabled = chkBuatAntrian.isEnabled();
        chkBuatAntrian.setEnabled(adaJasa);
        if (!adaJasa) {
            chkBuatAntrian.setSelected(false);
        } else if (!wasEnabled) {
            chkBuatAntrian.setSelected(true);
        }
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

    private boolean adaJasa() {
        return keranjang.stream().anyMatch(i -> i.getTipe() == TipeItem.JASA);
    }

    private void simpanTransaksi() {
        if (keranjang.isEmpty()) {
            Dialogs.peringatan(this, "Keranjang masih kosong.");
            return;
        }
        // Tanpa kolom Uang Diterima lagi: pembayaran tunai dicatat
        // sebagai uang pas sebesar total tagihan (kembali Rp 0).
        Transaksi transaksi = new Transaksi();
        transaksi.setUserId(Session.user() == null ? null : Session.user().getId());
        transaksi.setDiskon(bacaDiskon());
        transaksi.setBayar(total);
        transaksi.setBuatAntrian(chkBuatAntrian.isEnabled() && chkBuatAntrian.isSelected());
        transaksi.setNamaPelanggan(txtPelanggan.getText().trim().isEmpty() ? null : txtPelanggan.getText().trim());
        transaksi.getItems().addAll(keranjang);

        try {
            Transaksi tersimpan = service.simpan(transaksi);

            boolean autoPrint = false;
            try {
                String val = new giriatk.dao.PengaturanDao().get("printer_auto", "1");
                autoPrint = "1".equals(val);
            } catch (Exception ignored) {
            }

            if (autoPrint) {
                try {
                    printService.cetakStruk(tersimpan);
                } catch (Exception printEx) {
                    Dialogs.peringatan(this, "Transaksi tersimpan, tetapi gagal mencetak ke hardware printer: " + printEx.getMessage());
                }
            }

            Window owner = SwingUtilities.getWindowAncestor(this);
            keranjang.clear();
            txtDiskon.setText("");
            txtPelanggan.setText("");
            refreshKeranjang();
            reloadKatalog();

            new TransaksiDetailDialog(owner, tersimpan).setVisible(true);
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal menyimpan transaksi:\n" + ex.getMessage());
        }
    }

    private void simpanTransaksiMidtrans() {
        if (keranjang.isEmpty()) {
            Dialogs.peringatan(this, "Keranjang masih kosong.");
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        // Order ID Midtrans harus unik per transaksi — format lama
        // ("TRX-M" + millis % 100000) cuma 5 angka dan gampang tabrakan,
        // order_id yang dobel ditolak Midtrans. Pakai timestamp penuh + acak.
        String tempKode = "TRX-M"
                + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000));
        String namaPlg = txtPelanggan.getText().trim().isEmpty() ? "Pelanggan Toko" : txtPelanggan.getText().trim();

        MidtransPaymentDialog dlg = new MidtransPaymentDialog(owner, tempKode, total, namaPlg);
        dlg.setVisible(true);

        if (!dlg.isPaid()) {
            Dialogs.peringatan(this, "Pembayaran Midtrans belum berhasil / dibatalkan.");
            return;
        }

        Transaksi transaksi = new Transaksi();
        transaksi.setUserId(Session.user() == null ? null : Session.user().getId());
        transaksi.setDiskon(bacaDiskon());
        transaksi.setBayar(total);
        // Simulasi lokal tidak boleh tercatat sebagai pembayaran Midtrans asli.
        transaksi.setMetodeBayar(dlg.isSimulation()
                ? "SIMULASI_" + dlg.getPaymentCode().toUpperCase()
                : "MIDTRANS_" + dlg.getPaymentCode().toUpperCase());
        transaksi.setPaymentRef(tempKode);
        transaksi.setBuatAntrian(chkBuatAntrian.isEnabled() && chkBuatAntrian.isSelected());
        transaksi.setNamaPelanggan(txtPelanggan.getText().trim().isEmpty() ? null : txtPelanggan.getText().trim());
        transaksi.getItems().addAll(keranjang);

        try {
            Transaksi tersimpan = service.simpan(transaksi);

            boolean autoPrint = false;
            try {
                String val = new giriatk.dao.PengaturanDao().get("printer_auto", "1");
                autoPrint = "1".equals(val);
            } catch (Exception ignored) {
            }

            if (autoPrint) {
                try {
                    printService.cetakStruk(tersimpan);
                } catch (Exception printEx) {
                    Dialogs.peringatan(this, "Transaksi tersimpan, tetapi gagal mencetak ke hardware printer: " + printEx.getMessage());
                }
            }

            keranjang.clear();
            txtDiskon.setText("");
            txtPelanggan.setText("");
            refreshKeranjang();
            reloadKatalog();

            new TransaksiDetailDialog(owner, tersimpan).setVisible(true);
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal menyimpan transaksi Midtrans:\n" + ex.getMessage());
        }
    }

    @Override
    public void onShow() {
        reloadKatalog();
    }

    private record Katalog(TipeItem tipe, int id, String nama, String label, String satuan,
            BigDecimal harga, Integer stok, BigDecimal hargaBeli, String gambar) {
    }
}
