package giriatk.view.dialog;

import giriatk.dao.PengaturanDao;
import giriatk.dao.TransaksiDao;
import giriatk.model.StatusTransaksi;
import giriatk.model.TipeItem;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;
import giriatk.service.ThermalPrintService;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.LogoUtil;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pratinjau nota setelah transaksi.
 *
 * Lebar nota mengikuti pengaturan kertas (58mm / 80mm) dan seluruh isi
 * dibatasi pada lebar konten yang tetap:
 * - Nama item selalu wrap ke baris berikutnya (HTML width tetap), tidak
 *   lagi mendorong angka total keluar dari nota.
 * - Nama item dan qty x harga / subtotal dipisah dua baris.
 * - Stempel LUNAS/VOID adalah komponen baris sendiri di bawah total,
 *   tidak digambar pada koordinat tetap yang bisa menimpa item.
 * - Logo toko tampil di header bila sudah diatur; tanpa logo tetap rapi.
 * - Tombol SIMPAN JPG mengekspor panel nota persis seperti tampil.
 */
public class TransaksiDetailDialog extends JDialog {

    private final Transaksi transaksi;
    private final boolean kertas58;
    private final int lebarKonten;
    private JPanel receiptPanel;
    private boolean voided = false;

    public boolean isVoided() {
        return voided;
    }

    public TransaksiDetailDialog(Window owner, Transaksi transaksi) {
        super(owner, "Detail Transaksi " + transaksi.getKode(), ModalityType.APPLICATION_MODAL);
        this.transaksi = transaksi;
        this.kertas58 = !"80mm".equalsIgnoreCase(bacaPengaturan("printer_lebar", "58mm"));
        this.lebarKonten = kertas58 ? 296 : 424;
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Theme.CANVAS);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INK, 2),
                new EmptyBorder(16, 20, 16, 20)));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        JPanel receiptWrap = new JPanel();
        receiptWrap.setOpaque(false);
        receiptWrap.setLayout(new BoxLayout(receiptWrap, BoxLayout.Y_AXIS));

        receiptWrap.add(buildReceipt(transaksi));
        receiptWrap.add(new Perforation());
        receiptWrap.add(Box.createVerticalStrut(14));
        receiptWrap.add(buildActions());
        receiptWrap.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(receiptWrap);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        center.add(scroll, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
        pack();
        int targetW = lebarKonten + (kertas58 ? 104 : 108);
        int targetH = Math.min(getHeight(), owner == null ? 800 : owner.getHeight() - 40);
        setSize(targetW, targetH);
        setLocationRelativeTo(owner);
    }

    private String bacaPengaturan(String kunci, String fallback) {
        try {
            return new PengaturanDao().get(kunci, fallback);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Map<String, String> semuaPengaturan() {
        try {
            return new PengaturanDao().semua();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private JComponent buildReceipt(Transaksi transaksi) {
        boolean isVoid = transaksi.getStatus() == StatusTransaksi.VOID;
        Map<String, String> cfg = semuaPengaturan();
        String namaToko = cfg.getOrDefault("nama_toko", "GiriATK & Print");
        String alamat = cfg.getOrDefault("alamat", "Jl. Raya Giri No. 88, Banyuwangi");
        if (alamat.isBlank() || alamat.equalsIgnoreCase("Jl. Contoh No. 1")) {
            alamat = "Jl. Raya Giri No. 88, Banyuwangi";
        }
        String telepon = cfg.getOrDefault("telepon", "0812-3456-7890");
        if (telepon.isBlank() || telepon.equals("0812-0000-0000")) {
            telepon = "0812-3456-7890";
        }
        String footer = cfg.getOrDefault("footer_struk", "Terima kasih sudah berbelanja");

        JPanel receipt = new ReceiptPanel();
        receipt.setLayout(new BoxLayout(receipt, BoxLayout.Y_AXIS));
        receipt.setBorder(new EmptyBorder(20, 24, 10, 24));
        receipt.setMaximumSize(new Dimension(lebarKonten + 48, Integer.MAX_VALUE));
        receipt.setMinimumSize(new Dimension(lebarKonten + 48, 0));
        this.receiptPanel = receipt;

        // Logo toko (opsional) — slot di tengah header.
        ImageIcon logo = LogoUtil.muatLogoIcon(kertas58 ? 84 : 120, kertas58 ? 46 : 62);
        if (logo != null) {
            JLabel lblLogo = new JLabel(logo);
            lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
            receipt.add(lblLogo);
            receipt.add(Box.createVerticalStrut(6));
        }

        // Nama & alamat memakai komponen WrapText (lebar tetap, wrap dihitung
        // dari FontMetrics). JLabel HTML tidak dipakai untuk teks panjang:
        // ukuran minimumnya selebar teks satu baris, sehingga mendorong
        // seluruh nota melebar dan memotong sisi kanan.
        String namaBersih = namaToko == null || namaToko.isBlank() ? "GiriATK & Print" : namaToko.trim();
        int idxAmp = namaBersih.indexOf('&');
        WrapText shop = new WrapText(namaBersih,
                Theme.display(Font.BOLD, kertas58 ? 19 : 23), Theme.INK,
                idxAmp >= 0 ? idxAmp : -1, Theme.ACCENT,
                SwingConstants.CENTER, lebarKonten);
        shop.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(shop);

        WrapText addr = new WrapText(alamat + " · Telp " + telepon,
                Theme.text(Font.PLAIN, kertas58 ? 10 : 11), Theme.MUTED,
                -1, null, SwingConstants.CENTER, lebarKonten);
        addr.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(Box.createVerticalStrut(3));
        receipt.add(addr);
        WrapText jam = new WrapText("Buka setiap hari 07.00–21.00",
                Theme.text(Font.PLAIN, kertas58 ? 10 : 11), Theme.MUTED,
                -1, null, SwingConstants.CENTER, lebarKonten);
        jam.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(jam);

        receipt.add(Box.createVerticalStrut(10));
        receipt.add(new DashedDivider(Theme.INK, 1.5f));
        receipt.add(Box.createVerticalStrut(8));

        // Metadata: satu field per baris (label kiri, nilai kanan) —
        // tidak ada lagi dua kolom yang saling memotong di kertas sempit.
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.of("id", "ID"));
        receipt.add(metaRow("NO. STRUK", transaksi.getKode()));
        receipt.add(metaRow("TANGGAL",
                transaksi.getTanggal() == null ? "-" : date.format(transaksi.getTanggal()).toUpperCase(Locale.of("id", "ID"))));
        receipt.add(metaRow("KASIR", shortName(transaksi.getUserName())));
        receipt.add(metaRow("PUKUL", Fmt.jam(transaksi.getTanggal()) + " WIB"));

        if (isVoid) {
            receipt.add(Box.createVerticalStrut(4));
            WrapText status = new WrapText("STATUS : VOID / BATAL",
                    Theme.mono(Font.BOLD, kertas58 ? 8 : 9), Theme.DANGER,
                    -1, null, SwingConstants.LEFT, lebarKonten);
            receipt.add(status);
            receipt.add(metaRow("VOID OLEH", shortName(transaksi.getVoidByName())));
            if (transaksi.getVoidAlasan() != null && !transaksi.getVoidAlasan().isBlank()) {
                WrapText alasan = new WrapText("ALASAN : " + transaksi.getVoidAlasan(),
                        Theme.mono(Font.PLAIN, kertas58 ? 8 : 9), Theme.DANGER,
                        -1, null, SwingConstants.LEFT, lebarKonten);
                receipt.add(alasan);
            }
        }

        receipt.add(Box.createVerticalStrut(6));
        receipt.add(new DashedDivider(Theme.INK, 1.5f));
        receipt.add(Box.createVerticalStrut(6));

        for (TransaksiItem item : transaksi.getItems()) {
            receipt.add(itemBlock(item));
            receipt.add(Box.createVerticalStrut(3));
        }

        receipt.add(Box.createVerticalStrut(2));
        receipt.add(new DashedDivider(Theme.BORDER_STRONG, 1f));
        receipt.add(Box.createVerticalStrut(6));

        if (transaksi.getDiskon() != null && transaksi.getDiskon().signum() > 0) {
            receipt.add(totalRow("Subtotal", Fmt.rupiah(transaksi.getSubtotal()), false, Theme.INK));
            receipt.add(totalRow("Diskon", "- " + Fmt.rupiah(transaksi.getDiskon()), false, Theme.DANGER));
        }
        receipt.add(totalRow("TOTAL", Fmt.rupiah(transaksi.getTotal()), true, Theme.INK));
        receipt.add(totalRow(labelBayar(), Fmt.rupiah(transaksi.getBayar()), false, Theme.INK));
        receipt.add(totalRow("KEMBALI", Fmt.rupiah(transaksi.getKembali()), false, Theme.GREEN_FG));

        // Stempel sebagai baris sendiri — tidak menimpa item/total dan
        // tidak terpotong tepi nota.
        receipt.add(Box.createVerticalStrut(6));
        receipt.add(new StampBadge(isVoid ? "VOID" : "LUNAS", isVoid));
        receipt.add(Box.createVerticalStrut(4));

        receipt.add(new DashedDivider(Theme.INK, 1.5f));
        receipt.add(Box.createVerticalStrut(6));

        WrapText thanks = new WrapText("— " + footer + " —",
                Theme.display(Font.ITALIC, kertas58 ? 12 : 14), Theme.INK_SOFT,
                -1, null, SwingConstants.CENTER, lebarKonten);
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(thanks);
        receipt.add(Box.createVerticalStrut(6));

        JPanel barcode = new Barcode(kertas58 ? 180 : 250);
        barcode.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(barcode);
        receipt.add(Box.createVerticalStrut(3));

        JLabel cap = new JLabel(spaced(transaksi.getKode()));
        cap.setFont(Theme.mono(Font.PLAIN, 8));
        cap.setForeground(Theme.FAINT);
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(cap);
        receipt.add(Box.createVerticalStrut(6));
        return receipt;
    }

    private String labelBayar() {
        String metode = transaksi.getMetodeBayar();
        if (metode == null || metode.isBlank() || "TUNAI".equalsIgnoreCase(metode)) {
            return "BAYAR (TUNAI)";
        }
        String bersih = metode.replace("MIDTRANS_", "").replace("SIMULASI_", "SIMULASI ")
                .replace('_', ' ').trim();
        return "BAYAR (" + bersih + ")";
    }

    private JComponent metaRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        JLabel l = new JLabel(label + " :");
        l.setFont(Theme.mono(Font.PLAIN, kertas58 ? 8 : 9));
        l.setForeground(Theme.MUTED);
        JLabel v = new JLabel(value == null || value.isBlank() ? "-" : value);
        v.setFont(Theme.mono(Font.BOLD, kertas58 ? 8 : 9));
        v.setForeground(Theme.INK);
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    /** Satu item = blok dua baris: nama (wrap penuh) lalu qty x harga + subtotal. */
    private JComponent itemBlock(TransaksiItem item) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setAlignmentX(Component.CENTER_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        boolean jasa = item.getTipe() == TipeItem.JASA;
        String namaItem = item.getNamaItem() == null ? "-" : item.getNamaItem();
        WrapText nama = new WrapText(jasa ? namaItem + " [JASA]" : namaItem,
                Theme.text(Font.BOLD, kertas58 ? 11 : 12), Theme.INK,
                jasa ? namaItem.length() : -1, new Color(0x2456A6),
                SwingConstants.LEFT, lebarKonten);
        nama.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(nama);

        JPanel detail = new JPanel(new BorderLayout(10, 0));
        detail.setOpaque(false);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        JLabel qty = new JLabel(item.getQty() + " × " + Fmt.angka(item.getHarga()));
        qty.setFont(Theme.mono(Font.PLAIN, kertas58 ? 9 : 10));
        qty.setForeground(Theme.MUTED);
        JLabel sub = new JLabel(Fmt.angka(item.getSubtotal()));
        sub.setFont(Theme.mono(Font.BOLD, kertas58 ? 10 : 11));
        sub.setForeground(Theme.INK);
        detail.add(qty, BorderLayout.WEST);
        detail.add(sub, BorderLayout.EAST);
        block.add(detail);
        return block;
    }

    private JComponent totalRow(String label, String value, boolean grand, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, grand ? 24 : 18));
        JLabel l = new JLabel(label);
        l.setFont(Theme.text(Font.BOLD, grand ? 13 : 11));
        l.setForeground(grand ? Theme.INK : Theme.MUTED);
        JLabel v = new JLabel(value);
        v.setFont(Theme.mono(Font.BOLD, grand ? (kertas58 ? 15 : 17) : 11));
        v.setForeground(valueColor);
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private JComponent buildActions() {
        boolean canVoid = transaksi.getStatus() == StatusTransaksi.SUKSES;
        int jumlah = canVoid ? 4 : 3;
        JPanel actions = new JPanel(new java.awt.GridLayout(1, jumlah, 8, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);
        actions.setBorder(new EmptyBorder(0, 6, 0, 6));
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        if (canVoid) {
            UiKit.SoftButton btnVoid = new UiKit.SoftButton("Void", UiKit.Variant.DANGER);
            btnVoid.setToolTipText("Void / batalkan transaksi ini");
            btnVoid.setFont(Theme.text(Font.BOLD, 10));
            btnVoid.setBorder(new EmptyBorder(8, 6, 9, 6));
            btnVoid.addActionListener(e -> konfirmasiVoid());
            actions.add(btnVoid);
        }

        UiKit.SoftButton btnJpg = new UiKit.SoftButton("Simpan JPG", UiKit.Variant.GHOST);
        btnJpg.setToolTipText("Simpan nota ini sebagai gambar JPG");
        btnJpg.setFont(Theme.text(Font.BOLD, 10));
        btnJpg.setBorder(new EmptyBorder(8, 6, 9, 6));
        btnJpg.addActionListener(e -> simpanNotaJpg());
        actions.add(btnJpg);

        UiKit.SoftButton print = new UiKit.SoftButton("Cetak Ulang", UiKit.Variant.GHOST);
        print.setToolTipText("Cetak ulang struk ke printer thermal");
        print.setFont(Theme.text(Font.BOLD, 10));
        print.setBorder(new EmptyBorder(8, 6, 9, 6));
        print.addActionListener(e -> {
            try {
                new ThermalPrintService().cetakStruk(transaksi);
                Dialogs.info(this, "Struk berhasil dikirim ke printer thermal.");
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal mencetak struk: " + ex.getMessage());
            }
        });
        actions.add(print);

        UiKit.SoftButton tutup = new UiKit.SoftButton("Tutup", UiKit.Variant.PRIMARY);
        tutup.setFont(Theme.text(Font.BOLD, 10));
        tutup.setBorder(new EmptyBorder(8, 6, 9, 6));
        tutup.addActionListener(e -> dispose());
        actions.add(tutup);
        return actions;
    }

    /** Ekspor panel nota persis seperti yang tampil menjadi file JPG (skala 2x). */
    private void simpanNotaJpg() {
        if (receiptPanel == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Simpan Nota sebagai JPG");
        chooser.setFileFilter(new FileNameExtensionFilter("Gambar JPG", "jpg", "jpeg"));
        chooser.setSelectedFile(new File("Nota-" + transaksi.getKode() + ".jpg"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".jpg") && !target.getName().toLowerCase().endsWith(".jpeg")) {
            target = new File(target.getParentFile(), target.getName() + ".jpg");
        }
        try {
            Dimension ukuran = receiptPanel.getSize();
            if (ukuran.width <= 0 || ukuran.height <= 0) {
                ukuran = receiptPanel.getPreferredSize();
                receiptPanel.setSize(ukuran);
                receiptPanel.doLayout();
            }
            int skala = 2;
            BufferedImage img = new BufferedImage(ukuran.width * skala, ukuran.height * skala,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, img.getWidth(), img.getHeight());
            g2.scale(skala, skala);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            receiptPanel.printAll(g2);
            g2.dispose();
            if (!ImageIO.write(img, "jpg", target)) {
                throw new IllegalStateException("Format JPG tidak didukung.");
            }
            Dialogs.info(this, "Nota berhasil disimpan ke:\n" + target.getAbsolutePath());
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal menyimpan nota JPG: " + ex.getMessage());
        }
    }

    private void konfirmasiVoid() {
        String inputAlasan = JOptionPane.showInputDialog(this,
                "Masukkan alasan void / pembatalan nota " + transaksi.getKode() + ":\n(Contoh: Pembeli batal beli / tukar barang)",
                "Konfirmasi Void Transaksi",
                JOptionPane.WARNING_MESSAGE);
        if (inputAlasan == null) {
            return;
        }
        String alasan = inputAlasan.trim();
        if (alasan.isEmpty()) {
            Dialogs.peringatan(this, "Alasan void wajib diisi.");
            return;
        }
        if (!Dialogs.konfirmasi(this, "Yakin ingin membatalkan transaksi " + transaksi.getKode() + "?\n"
                + "Stok barang yang ada di transaksi ini akan otomatis dikembalikan ke inventaris.")) {
            return;
        }
        try {
            new TransaksiDao().voidTransaksi(transaksi.getId(), alasan, Session.getUserId());
            Dialogs.info(this, "Transaksi " + transaksi.getKode() + " berhasil di-void.\nStok telah dikembalikan.");
            voided = true;
            dispose();
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal melakukan void transaksi: " + ex.getMessage());
        }
    }

    private String shortName(String name) {
        if (name == null || name.isBlank()) {
            return "-";
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1 ? parts[0].toUpperCase(Locale.of("id", "ID"))
                : (parts[0].charAt(0) + ". " + parts[parts.length - 1]).toUpperCase(Locale.of("id", "ID"));
    }

    private String spaced(String text) {
        if (text == null) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c).append(' ');
        }
        return sb.toString().trim();
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Teks dengan lebar tetap yang wrap-nya dihitung dari FontMetrics —
     * pengganti JLabel HTML untuk semua teks panjang di nota. Ukuran
     * minimum/preferensinya tidak pernah melebihi lebar konten, jadi
     * komponen ini tidak bisa mendorong nota melebar.
     * Mendukung satu segmen ekor berwarna lain (mis. "& Print" / "[JASA]").
     */
    private static final class WrapText extends JComponent {
        private static final Graphics2D UKUR =
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();

        private final String text;
        private final int suffixStart;
        private final Color suffixColor;
        private final int align;
        private final int lebar;

        WrapText(String text, Font font, Color color, int suffixStart, Color suffixColor,
                 int align, int lebar) {
            this.text = text == null ? "" : text;
            this.suffixStart = suffixStart;
            this.suffixColor = suffixColor;
            this.align = align;
            this.lebar = lebar;
            setFont(font);
            setForeground(color);
            int tinggi = baris().size() * (fontMetrics().getHeight());
            Dimension d = new Dimension(lebar, tinggi);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(new Dimension(lebar, Integer.MAX_VALUE));
        }

        private FontMetrics fontMetrics() {
            UKUR.setFont(getFont());
            return UKUR.getFontMetrics();
        }

        private List<String> baris() {
            return wrapTeks(fontMetrics(), text, lebar);
        }

        private static List<String> wrapTeks(FontMetrics fm, String teks, int maxW) {
            List<String> hasil = new ArrayList<>();
            if (teks == null || teks.isBlank()) {
                hasil.add("");
                return hasil;
            }
            StringBuilder baris = new StringBuilder();
            for (String kata : teks.trim().split("\\s+")) {
                while (fm.stringWidth(kata) > maxW && kata.length() > 1) {
                    int potong = kata.length() - 1;
                    while (potong > 1 && fm.stringWidth(kata.substring(0, potong)) > maxW) {
                        potong--;
                    }
                    if (baris.length() > 0) {
                        hasil.add(baris.toString());
                        baris.setLength(0);
                    }
                    hasil.add(kata.substring(0, potong));
                    kata = kata.substring(potong);
                }
                String calon = baris.length() == 0 ? kata : baris + " " + kata;
                if (fm.stringWidth(calon) <= maxW) {
                    baris.setLength(0);
                    baris.append(calon);
                } else {
                    if (baris.length() > 0) {
                        hasil.add(baris.toString());
                    }
                    baris.setLength(0);
                    baris.append(kata);
                }
            }
            if (baris.length() > 0) {
                hasil.add(baris.toString());
            }
            return hasil;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int w = Math.min(getWidth(), lebar);
            int offset = 0; // posisi karakter global untuk pewarnaan segmen ekor
            int y = fm.getAscent();
            for (String baris : baris()) {
                int x = align == SwingConstants.CENTER
                        ? Math.max(0, (w - fm.stringWidth(baris)) / 2) : 0;
                if (suffixStart < 0 || suffixColor == null) {
                    g2.setColor(getForeground());
                    g2.drawString(baris, x, y);
                } else {
                    int mulai = offset;
                    int selesai = offset + baris.length();
                    if (selesai <= suffixStart) {
                        g2.setColor(getForeground());
                        g2.drawString(baris, x, y);
                    } else if (mulai >= suffixStart) {
                        g2.setColor(suffixColor);
                        g2.drawString(baris, x, y);
                    } else {
                        int potong = suffixStart - mulai;
                        String kiri = baris.substring(0, potong);
                        String kanan = baris.substring(potong);
                        g2.setColor(getForeground());
                        g2.drawString(kiri, x, y);
                        g2.setColor(suffixColor);
                        g2.drawString(kanan, x + fm.stringWidth(kiri), y);
                    }
                }
                offset += baris.length() + 1; // +1 untuk spasi pemisah yang hilang saat wrap
                y += fm.getHeight();
            }
            g2.dispose();
        }
    }

    /** Panel kertas nota: latar + bingkai. Stempel tidak digambar di sini lagi. */
    private static final class ReceiptPanel extends JPanel {

        ReceiptPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.INK);
            g2.fillRect(6, 6, getWidth() - 6, getHeight() - 6);

            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, getWidth() - 6, getHeight() - 6);
            g2.setColor(new Color(28, 25, 23, 12));
            for (int y = 8; y < getHeight() - 6; y += 12) {
                for (int x = 8; x < getWidth() - 6; x += 12) {
                    g2.fillOval(x, y, 2, 2);
                }
            }
            g2.setColor(Theme.INK);
            g2.drawRect(0, 0, getWidth() - 7, getHeight() - 7);
            g2.drawRect(1, 1, getWidth() - 9, getHeight() - 9);
            g2.dispose();
        }
    }

    /** Stempel LUNAS/VOID sebagai komponen baris sendiri, selalu utuh di dalam nota. */
    private static final class StampBadge extends JPanel {
        private final String text;
        private final boolean isVoid;

        StampBadge(String text, boolean isVoid) {
            this.text = text;
            this.isVoid = isVoid;
            setOpaque(false);
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setPreferredSize(new Dimension(0, 38));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color warna = isVoid ? Theme.DANGER : Theme.ACCENT;
            g2.setColor(warna);
            g2.setFont(Theme.text(Font.BOLD, 17));
            int sw = g2.getFontMetrics().stringWidth(text);
            int boxW = sw + 30;
            int boxH = 32;
            AffineTransform old = g2.getTransform();
            g2.rotate(Math.toRadians(-5), getWidth() / 2.0, getHeight() / 2.0);
            int bx = (getWidth() - boxW) / 2;
            int by = (getHeight() - boxH) / 2;
            g2.setStroke(new java.awt.BasicStroke(2.5f));
            g2.drawRect(bx, by, boxW, boxH);
            g2.setStroke(new java.awt.BasicStroke(1.2f));
            g2.drawRect(bx + 3, by + 3, boxW - 6, boxH - 6);
            g2.drawString(text, (getWidth() - sw) / 2, by + boxH / 2 + 6);
            g2.setTransform(old);
            g2.dispose();
        }
    }

    private static final class Perforation extends JPanel {
        Perforation() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 6;
            int step = 14;
            java.awt.Polygon p = new java.awt.Polygon();
            for (int x = 0; x <= w; x += step) {
                p.addPoint(x, 0);
                p.addPoint(x + step / 2, 9);
            }
            p.addPoint(w, 0);
            g2.setColor(Theme.INK);
            g2.translate(2, 1);
            g2.fillPolygon(p);
            g2.translate(-2, -1);
            g2.setColor(Theme.SURFACE);
            g2.fillPolygon(p);
            g2.dispose();
        }
    }

    private static final class DashedDivider extends JPanel {
        private final Color color;
        private final float width;

        DashedDivider(Color color, float width) {
            this.color = color;
            this.width = width;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(color);
            g2.setStroke(new java.awt.BasicStroke(width));
            for (int x = 0; x < getWidth(); x += 7) {
                g2.drawLine(x, 1, Math.min(x + 3, getWidth()), 1);
            }
            g2.dispose();
        }
    }

    private static final class Barcode extends JPanel {
        Barcode(int lebar) {
            setOpaque(false);
            setPreferredSize(new Dimension(lebar, 38));
            setMaximumSize(new Dimension(lebar, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Theme.INK);
            int[] widths = {2, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 4};
            int x = 0;
            int i = 0;
            while (x < getWidth()) {
                int w = widths[i % widths.length];
                g2.fillRect(x, 0, w, getHeight());
                x += w + 3;
                i++;
            }
            g2.dispose();
        }
    }
}
