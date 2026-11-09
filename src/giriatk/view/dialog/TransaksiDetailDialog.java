package giriatk.view.dialog;

import giriatk.dao.TransaksiDao;
import giriatk.model.StatusTransaksi;
import giriatk.model.TipeItem;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;
import giriatk.service.ThermalPrintService;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TransaksiDetailDialog extends JDialog {

    private final Transaksi transaksi;
    private boolean voided = false;

    public boolean isVoided() {
        return voided;
    }

    public TransaksiDetailDialog(Window owner, Transaksi transaksi) {
        super(owner, "Detail Transaksi " + transaksi.getKode(), ModalityType.APPLICATION_MODAL);
        this.transaksi = transaksi;
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

        JComponent receipt = buildReceipt(transaksi);
        receiptWrap.add(receipt);
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
        int targetW = 540;
        int targetH = Math.min(getHeight(), owner == null ? 800 : owner.getHeight() - 40);
        setSize(targetW, targetH);
        setLocationRelativeTo(owner);
    }

    private JComponent buildReceipt(Transaksi transaksi) {
        boolean isVoid = transaksi.getStatus() == StatusTransaksi.VOID;
        JPanel receipt = new ReceiptPanel(isVoid);
        receipt.setLayout(new BoxLayout(receipt, BoxLayout.Y_AXIS));
        receipt.setBorder(new EmptyBorder(24, 30, 8, 30));

        JLabel shop = new JLabel("<html><div style='text-align:center'><font face='Georgia' size='6'><b>GiriATK "
                + "<i><font color='#C93A1E'>&amp; Print</font></i></b></font></div></html>");
        shop.setFont(Theme.display(Font.BOLD, 25));
        shop.setForeground(Theme.INK);
        shop.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(shop);

        JLabel addr = new JLabel("<html><div style='text-align:center'>Jl. Raya Giri No. 88, Banyuwangi · Telp 0812-3456-7890<br>"
                + "Buka setiap hari 07.00–21.00</div></html>");
        addr.setFont(Theme.text(Font.PLAIN, 11));
        addr.setForeground(Theme.MUTED);
        addr.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(Box.createVerticalStrut(3));
        receipt.add(addr);

        receipt.add(Box.createVerticalStrut(12));
        receipt.add(new DashedDivider(Theme.INK, 1.5f));
        receipt.add(Box.createVerticalStrut(10));

        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.of("id", "ID"));
        receipt.add(metaRow(
                "<span style='color:#78716C'>NO. STRUK :</span> " + transaksi.getKode(),
                "<span style='color:#78716C'>KASIR :</span> " + shortName(transaksi.getUserName())));
        receipt.add(metaRow(
                "<span style='color:#78716C'>TANGGAL :</span> "
                        + (transaksi.getTanggal() == null ? "-" : date.format(transaksi.getTanggal()).toUpperCase()),
                "<span style='color:#78716C'>PUKUL :</span> " + Fmt.jam(transaksi.getTanggal()) + " WIB"));

        if (isVoid) {
            receipt.add(Box.createVerticalStrut(6));
            receipt.add(metaRow(
                    "<span style='color:#DC2626'>STATUS :</span> <b style='color:#DC2626'>VOID / BATAL</b>",
                    "<span style='color:#78716C'>VOID OLEH :</span> " + shortName(transaksi.getVoidByName())));
            if (transaksi.getVoidAlasan() != null && !transaksi.getVoidAlasan().isBlank()) {
                receipt.add(metaRow(
                        "<span style='color:#DC2626'>ALASAN :</span> " + escape(transaksi.getVoidAlasan()),
                        "<span style='color:#78716C'>WAKTU :</span> " + Fmt.jam(transaksi.getVoidAt())));
            }
        }

        receipt.add(Box.createVerticalStrut(8));
        receipt.add(new DashedDivider(Theme.INK, 1.5f));
        receipt.add(Box.createVerticalStrut(6));

        for (TransaksiItem item : transaksi.getItems()) {
            receipt.add(itemRow(item));
        }

        receipt.add(Box.createVerticalStrut(4));
        receipt.add(new DashedDivider(Theme.BORDER_STRONG, 1f));
        receipt.add(Box.createVerticalStrut(6));

        receipt.add(totalRow("TOTAL", Fmt.rupiah(transaksi.getTotal()), true, Theme.INK));
        receipt.add(totalRow("BAYAR (TUNAI)", Fmt.rupiah(transaksi.getBayar()), false, Theme.INK));
        receipt.add(totalRow("KEMBALI", Fmt.rupiah(transaksi.getKembali()), false, Theme.GREEN_FG));

        receipt.add(Box.createVerticalStrut(10));
        receipt.add(new DashedDivider(Theme.INK, 1.5f));
        receipt.add(Box.createVerticalStrut(4));

        JLabel thanks = new JLabel("— Terima kasih sudah berbelanja —");
        thanks.setFont(Theme.display(Font.ITALIC, 14));
        thanks.setForeground(Theme.INK_SOFT);
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(thanks);
        receipt.add(Box.createVerticalStrut(6));

        JPanel barcode = new Barcode();
        barcode.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(barcode);
        receipt.add(Box.createVerticalStrut(3));

        JLabel cap = new JLabel(spaced(transaksi.getKode()));
        cap.setFont(Theme.mono(Font.PLAIN, 8));
        cap.setForeground(Theme.FAINT);
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(cap);
        receipt.add(Box.createVerticalStrut(8));
        return receipt;
    }

    private JComponent metaRow(String left, String right) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel l = new JLabel("<html><div style='font-size:9px'>" + left + "</div></html>");
        l.setFont(Theme.mono(Font.PLAIN, 9));
        l.setForeground(Theme.INK);
        JLabel r = new JLabel("<html><div style='font-size:9px'>" + right + "</div></html>");
        r.setFont(Theme.mono(Font.PLAIN, 9));
        r.setForeground(Theme.INK);
        row.add(l, BorderLayout.WEST);
        row.add(r, BorderLayout.EAST);
        return row;
    }

    private JComponent itemRow(TransaksiItem item) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setBorder(new EmptyBorder(3, 0, 3, 0));

        String tipe = item.getTipe() == TipeItem.JASA
                ? " <font color='#2456A6'>[JASA]</font>" : "";
        JLabel left = new JLabel("<html><b>" + escape(item.getNamaItem()) + "</b>" + tipe
                + "<br><font face='Cascadia Mono, Consolas' color='#78716C'>"
                + item.getQty() + " × " + Fmt.angka(item.getHarga()) + "</font></html>");
        left.setFont(Theme.text(Font.PLAIN, 12));
        left.setForeground(Theme.INK);
        row.add(left, BorderLayout.CENTER);

        JLabel right = new JLabel(Fmt.angka(item.getSubtotal()));
        right.setFont(Theme.mono(Font.BOLD, 11));
        right.setForeground(Theme.INK);
        right.setVerticalAlignment(SwingConstants.TOP);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent totalRow(String label, String value, boolean grand, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, grand ? 26 : 20));
        JLabel l = new JLabel(label);
        l.setFont(Theme.text(Font.BOLD, grand ? 13 : 11));
        l.setForeground(grand ? Theme.INK : Theme.MUTED);
        JLabel v = new JLabel(value);
        v.setFont(Theme.mono(Font.BOLD, grand ? 17 : 11));
        v.setForeground(valueColor);
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private JComponent buildActions() {
        boolean canVoid = transaksi.getStatus() == StatusTransaksi.SUKSES;
        JPanel actions = new JPanel(new java.awt.GridLayout(1, canVoid ? 3 : 2, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);
        actions.setBorder(new EmptyBorder(0, 10, 0, 10));
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        if (canVoid) {
            UiKit.SoftButton btnVoid = new UiKit.SoftButton("Void Transaksi", UiKit.Variant.DANGER);
            btnVoid.addActionListener(e -> konfirmasiVoid());
            actions.add(btnVoid);
        }

        UiKit.SoftButton print = new UiKit.SoftButton("Cetak Ulang Struk", UiKit.Variant.GHOST);
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
        tutup.addActionListener(e -> dispose());
        actions.add(tutup);
        return actions;
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
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c).append(' ');
        }
        return sb.toString().trim();
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;");
    }

    private static final class ReceiptPanel extends JPanel {
        private final boolean isVoid;

        ReceiptPanel(boolean isVoid) {
            this.isVoid = isVoid;
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
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

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            AffineTransform old = g2.getTransform();
            g2.rotate(Math.toRadians(-6), getWidth() - 120, 210);
            g2.setFont(Theme.text(Font.BOLD, 26));
            g2.setColor(isVoid ? Theme.DANGER : Theme.ACCENT);
            String stamp = isVoid ? "VOID" : "LUNAS";
            int sw = g2.getFontMetrics().stringWidth(stamp);
            g2.drawString(stamp, getWidth() - 116 - sw / 2, 208);
            g2.setStroke(new java.awt.BasicStroke(3f));
            g2.drawRect(getWidth() - 116 - sw / 2 - 14, 176, sw + 28, 44);
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.drawRect(getWidth() - 116 - sw / 2 - 9, 181, sw + 18, 34);
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
        Barcode() {
            setOpaque(false);
            setPreferredSize(new Dimension(250, 38));
            setMaximumSize(new Dimension(250, 38));
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
