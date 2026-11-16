package giriatk.service;

import giriatk.dao.PengaturanDao;
import giriatk.model.TipeItem;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;
import giriatk.util.Fmt;
import giriatk.util.LogoUtil;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cetak struk thermal 58mm / 80mm.
 *
 * Layout struk bersifat adaptif terhadap lebar kertas:
 * - Nama toko, alamat, dan nama item selalu di-wrap per kata mengikuti
 *   lebar kertas (tidak pernah terpotong di tepi kanan).
 * - Nama item berdiri di baris sendiri; qty x harga dan subtotal item
 *   ada di baris berikutnya. Format ini terbaca baik di 58mm maupun 80mm.
 * - Di 58mm, metadata struk (no/tanggal/kasir/pukul) ditumpuk satu baris
 *   per field; di 80mm tampil dua kolom.
 * - Tinggi kertas dihitung dari isi struk, bukan rumus tetap.
 * - Logo toko (bila sudah diatur di Pengaturan / file images/logo.*)
 *   dicetak di tengah header; tanpa logo, header tetap rapi.
 */
public class ThermalPrintService {

    private final PengaturanDao meDao = new PengaturanDao();

    public List<String> getDaftarPrinter() {
        List<String> list = new ArrayList<>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : services) {
            list.add(ps.getName());
        }
        return list;
    }

    public PrintService cariPrinter(String namaPrinter) {
        if (namaPrinter == null || namaPrinter.isBlank()) {
            return PrintServiceLookup.lookupDefaultPrintService();
        }
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : services) {
            if (ps.getName().equalsIgnoreCase(namaPrinter.trim())) {
                return ps;
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    public void cetakStruk(Transaksi transaksi) throws Exception {
        Map<String, String> config = meDao.semua();
        String namaToko = config.getOrDefault("nama_toko", "GiriATK & Print");
        String alamat = config.getOrDefault("alamat", "Jl. Raya Giri No. 88, Banyuwangi");
        if (alamat.isBlank() || alamat.equalsIgnoreCase("Jl. Contoh No. 1")) {
            alamat = "Jl. Raya Giri No. 88, Banyuwangi";
        }
        String telepon = config.getOrDefault("telepon", "0812-3456-7890");
        if (telepon.isBlank() || telepon.equals("0812-0000-0000")) {
            telepon = "0812-3456-7890";
        }
        String footer = config.getOrDefault("footer_struk", "Terima kasih sudah berbelanja");
        String targetPrinter = config.getOrDefault("printer_nama", "");
        String lebarKertas = config.getOrDefault("printer_lebar", "58mm");

        PrintService ps = cariPrinter(targetPrinter);
        if (ps == null) {
            throw new PrinterException("Tidak ada printer yang terdeteksi pada sistem.");
        }

        boolean is80mm = "80mm".equalsIgnoreCase(lebarKertas);
        double widthPt = is80mm ? 220 : 160;

        ReceiptPrintable printable = new ReceiptPrintable(transaksi, namaToko, alamat, telepon, footer, is80mm);
        int contentWidth = (int) widthPt - 8;
        double heightPt = printable.estimateHeight(contentWidth) + 18;

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(ps);

        PageFormat pf = job.defaultPage();
        Paper paper = pf.getPaper();
        paper.setSize(widthPt, heightPt);
        paper.setImageableArea(4, 2, widthPt - 8, heightPt - 4);
        pf.setPaper(paper);

        job.setPrintable(printable, pf);
        job.print();
    }

    public void cetakUjiCoba() throws Exception {
        Transaksi t = new Transaksi();
        t.setKode("TRX-0001");
        t.setTanggal(LocalDateTime.now());
        t.setUserName("P. TOKO");
        t.setSubtotal(new BigDecimal("185750"));
        t.setDiskon(new BigDecimal("5750"));
        t.setTotal(new BigDecimal("180000"));
        t.setBayar(new BigDecimal("200000"));
        t.setKembali(new BigDecimal("20000"));

        TransaksiItem item1 = new TransaksiItem();
        item1.setNamaItem("Fotokopi");
        item1.setQty(3);
        item1.setHarga(new BigDecimal("250"));
        item1.setSubtotal(new BigDecimal("750"));
        item1.setTipe(TipeItem.JASA);
        t.getItems().add(item1);

        TransaksiItem item2 = new TransaksiItem();
        item2.setNamaItem("Kertas HVS A4 70gr · PaperOne · HVS · Putih A4");
        item2.setQty(3);
        item2.setHarga(new BigDecimal("55000"));
        item2.setSubtotal(new BigDecimal("165000"));
        item2.setTipe(TipeItem.BARANG);
        t.getItems().add(item2);

        TransaksiItem item3 = new TransaksiItem();
        item3.setNamaItem("Buku Tulis 38 Lembar · Sinar Dunia · Buku Tulis · Putih 38 lembar");
        item3.setQty(2);
        item3.setHarga(new BigDecimal("10000"));
        item3.setSubtotal(new BigDecimal("20000"));
        item3.setTipe(TipeItem.BARANG);
        t.getItems().add(item3);

        cetakStruk(t);
    }

    private static class ReceiptPrintable implements Printable {

        private final Transaksi transaksi;
        private final String namaToko;
        private final String alamat;
        private final String telepon;
        private final String footer;
        private final boolean is80mm;
        private final BufferedImage logo;

        ReceiptPrintable(Transaksi transaksi, String namaToko, String alamat, String telepon, String footer, boolean is80mm) {
            this.transaksi = transaksi;
            this.namaToko = namaToko;
            this.alamat = alamat;
            this.telepon = telepon;
            this.footer = footer;
            this.is80mm = is80mm;
            this.logo = LogoUtil.muatLogo();
        }

        // ---------- font per lebar kertas ----------
        private Font fTitle()  { return new Font("Georgia", Font.BOLD, is80mm ? 15 : 13); }
        private Font fSub()    { return new Font("SansSerif", Font.PLAIN, is80mm ? 8 : 7); }
        private Font fMeta()   { return new Font("Monospaced", Font.PLAIN, is80mm ? 8 : 7); }
        private Font fItem()   { return new Font("SansSerif", Font.BOLD, is80mm ? 9 : 8); }
        private Font fDetail() { return new Font("Monospaced", Font.PLAIN, is80mm ? 8 : 7); }
        private Font fTotL()   { return new Font("SansSerif", Font.BOLD, is80mm ? 10 : 9); }
        private Font fTotV()   { return new Font("Monospaced", Font.BOLD, is80mm ? 11 : 10); }
        private Font fPlnL()   { return new Font("SansSerif", Font.PLAIN, is80mm ? 8 : 7); }
        private Font fPlnV()   { return new Font("Monospaced", Font.PLAIN, is80mm ? 8 : 7); }
        private Font fThanks() { return new Font("Georgia", Font.ITALIC, is80mm ? 10 : 9); }
        private Font fCap()    { return new Font("Monospaced", Font.PLAIN, is80mm ? 7 : 6); }

        private int logoHeight(int width) {
            if (logo == null || logo.getWidth() <= 0) {
                return 0;
            }
            int maxW = is80mm ? 110 : 88;
            int maxH = is80mm ? 54 : 42;
            double skala = Math.min((double) maxW / logo.getWidth(), (double) maxH / logo.getHeight());
            return (int) Math.round(logo.getHeight() * skala) + 4;
        }

        private boolean adaDiskon() {
            return transaksi.getDiskon() != null && transaksi.getDiskon().signum() > 0;
        }

        /** Perkiraan tinggi isi struk (pt) — dihitung dengan wrap yang sama seperti saat mencetak. */
        int estimateHeight(int width) {
            BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scratch.createGraphics();
            int y = 14;
            y += logoHeight(width);
            y += wrapCount(g2, namaToko, fTitle(), width) * lineHeight(g2, fTitle());
            y += wrapCount(g2, alamat, fSub(), width) * lineHeight(g2, fSub());
            if (telepon != null && !telepon.isBlank()) {
                y += lineHeight(g2, fSub());
            }
            y += lineHeight(g2, fSub()); // jam buka
            y += 4 + 4 + 6;              // garis putus-putus + jarak
            y += (is80mm ? 2 : 4) * (lineHeight(g2, fMeta()) - 1);
            y += 4 + 4 + 6;
            for (TransaksiItem item : transaksi.getItems()) {
                String nama = namaItem(item);
                int baris = Math.min(3, wrapCount(g2, nama, fItem(), width));
                y += baris * (lineHeight(g2, fItem()) - 3);
                y += lineHeight(g2, fDetail()) - 1 + 2;
            }
            y += 4 + 4 + 6;
            if (adaDiskon()) {
                y += 2 * (lineHeight(g2, fPlnV()) - 1);
            }
            y += lineHeight(g2, fTotV()) - 1;
            y += 2 * (lineHeight(g2, fPlnV()) - 1);
            y += 34; // stempel LUNAS satu baris sendiri
            y += 6 + 4 + 8;
            y += wrapCount(g2, "— " + footer + " —", fThanks(), width) * lineHeight(g2, fThanks());
            y += 4 + 28 + 2 + lineHeight(g2, fCap()); // barcode + kode
            g2.dispose();
            return y;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.setColor(Color.BLACK);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = (int) pageFormat.getImageableWidth();
            int y = 14;

            // Logo (opsional) — di tengah, proporsional.
            if (logo != null && logo.getWidth() > 0) {
                int maxW = is80mm ? 110 : 88;
                int maxH = is80mm ? 54 : 42;
                double skala = Math.min((double) maxW / logo.getWidth(), (double) maxH / logo.getHeight());
                int w = (int) Math.round(logo.getWidth() * skala);
                int h = (int) Math.round(logo.getHeight() * skala);
                g2.drawImage(logo, (width - w) / 2, y - 10, w, h, null);
                y += h + 4;
            }

            // Header toko — semua teks di-wrap mengikuti lebar kertas.
            y = drawCenteredWrapped(g2, namaToko, width, y, fTitle());
            y = drawCenteredWrapped(g2, alamat, width, y, fSub());
            if (telepon != null && !telepon.isBlank()) {
                y = drawCenteredWrapped(g2, "Telp " + telepon, width, y, fSub());
            }
            y = drawCenteredWrapped(g2, "Buka setiap hari 07.00–21.00", width, y, fSub());

            y += 4;
            y = drawDashedLine(g2, width, y);
            y += 6;

            // Metadata struk
            DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.of("id", "ID"));
            DateTimeFormatter tFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.of("id", "ID"));
            String tglStr = transaksi.getTanggal() != null ? dFmt.format(transaksi.getTanggal()).toUpperCase(Locale.of("id", "ID")) : "-";
            String jamStr = transaksi.getTanggal() != null ? tFmt.format(transaksi.getTanggal()) + " WIB" : "-";
            String kodeStruk = transaksi.getKode() == null ? "-" : transaksi.getKode();
            String kasirName = shortName(transaksi.getUserName());
            if (is80mm) {
                y = drawTwoCols(g2, "NO. STRUK : " + kodeStruk, "KASIR : " + kasirName, width, y, fMeta());
                y = drawTwoCols(g2, "TANGGAL   : " + tglStr, "PUKUL : " + jamStr, width, y, fMeta());
            } else {
                // 58mm: satu field per baris supaya tidak saling tabrak.
                y = drawOneLine(g2, "NO. STRUK : " + kodeStruk, width, y, fMeta());
                y = drawOneLine(g2, "TANGGAL   : " + tglStr, width, y, fMeta());
                y = drawOneLine(g2, "KASIR     : " + kasirName, width, y, fMeta());
                y = drawOneLine(g2, "PUKUL     : " + jamStr, width, y, fMeta());
            }

            y += 4;
            y = drawDashedLine(g2, width, y);
            y += 6;

            // Item: nama (wrap, maks 3 baris) lalu qty x harga + subtotal.
            for (TransaksiItem item : transaksi.getItems()) {
                List<String> barisNama = wrap(g2, namaItem(item), fItem(), width);
                if (barisNama.size() > 3) {
                    barisNama = new ArrayList<>(barisNama.subList(0, 3));
                    String terakhir = barisNama.get(2);
                    barisNama.set(2, fitText(g2, terakhir, width, fItem()));
                }
                for (String baris : barisNama) {
                    g2.setFont(fItem());
                    g2.drawString(baris, 0, y);
                    y += lineHeight(g2, fItem()) - 3;
                }
                String qtyHarga = item.getQty() + " × " + Fmt.angka(item.getHarga());
                y = drawLeftRight(g2, qtyHarga, Fmt.angka(item.getSubtotal()), width, y, fDetail(), fDetail(), false);
                y += 2;
            }

            y += 4;
            y = drawDashedLine(g2, width, y);
            y += 6;

            // Total-total
            if (adaDiskon()) {
                y = drawLeftRight(g2, "Subtotal", Fmt.rupiah(transaksi.getSubtotal()), width, y, fPlnL(), fPlnV(), false);
                y = drawLeftRight(g2, "Diskon", "- " + Fmt.rupiah(transaksi.getDiskon()), width, y, fPlnL(), fPlnV(), false);
            }
            y = drawLeftRight(g2, "TOTAL", Fmt.rupiah(transaksi.getTotal()), width, y, fTotL(), fTotV(), false);
            y = drawLeftRight(g2, labelBayar(), Fmt.rupiah(transaksi.getBayar()), width, y, fPlnL(), fPlnV(), false);
            y = drawLeftRight(g2, "KEMBALI", Fmt.rupiah(transaksi.getKembali()), width, y, fPlnL(), fPlnV(), false);

            // Stempel LUNAS — baris sendiri di tengah, tidak menimpa angka.
            y += 6;
            drawStampLunas(g2, width / 2, y + 10);
            y += 28;

            y += 2;
            y = drawDashedLine(g2, width, y);
            y += 8;

            // Footer
            y = drawCenteredWrapped(g2, "— " + footer + " —", width, y, fThanks());
            y += 4;
            y = drawBarcodeVisual(g2, width, y);
            y += 2;
            drawCenteredText(g2, kodeStruk, width, y, fCap());

            return PAGE_EXISTS;
        }

        private String namaItem(TransaksiItem item) {
            String name = item.getNamaItem() == null ? "-" : item.getNamaItem();
            if (item.getTipe() == TipeItem.JASA) {
                name += " [JASA]";
            }
            return name;
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

        private void drawStampLunas(Graphics2D g2, int cx, int cy) {
            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(Math.toRadians(-6));

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, is80mm ? 13 : 11));
            String stamp = "LUNAS";
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(stamp);

            int boxW = sw + 16;
            int boxH = is80mm ? 22 : 19;
            int bx = -boxW / 2;
            int by = -boxH / 2;

            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(bx, by, boxW, boxH);
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawRect(bx + 2, by + 2, boxW - 4, boxH - 4);

            g2.drawString(stamp, -sw / 2, (boxH / 2) - 6);
            g2.setTransform(old);
        }

        private int drawBarcodeVisual(Graphics2D g2, int width, int y) {
            int bcWidth = is80mm ? 150 : 110;
            int bcHeight = 24;
            int startX = Math.max(0, (width - bcWidth) / 2);

            int[] widths = {2, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 4};
            int curX = startX;
            int i = 0;
            g2.setColor(Color.BLACK);
            while (curX < startX + bcWidth) {
                int w = widths[i % widths.length];
                g2.fillRect(curX, y, w, bcHeight);
                curX += w + 2;
                i++;
            }
            return y + bcHeight + 4;
        }

        private int drawDashedLine(Graphics2D g2, int width, int y) {
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 2.0f}, 0.0f));
            g2.drawLine(0, y, width, y);
            return y + 4;
        }

        private int lineHeight(Graphics2D g2, Font font) {
            g2.setFont(font);
            return g2.getFontMetrics().getHeight();
        }

        /** Pecah teks jadi baris-baris yang muat selebar maxWidth (word wrap). */
        private List<String> wrap(Graphics2D g2, String text, Font font, int maxWidth) {
            List<String> hasil = new ArrayList<>();
            if (text == null || text.isBlank()) {
                return hasil;
            }
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            StringBuilder baris = new StringBuilder();
            for (String kata : text.trim().split("\\s+")) {
                // Kata tunggal yang lebih lebar dari kertas: pecah paksa per karakter.
                while (fm.stringWidth(kata) > maxWidth && kata.length() > 1) {
                    int potong = kata.length() - 1;
                    while (potong > 1 && fm.stringWidth(kata.substring(0, potong)) > maxWidth) {
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
                if (fm.stringWidth(calon) <= maxWidth) {
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

        private int wrapCount(Graphics2D g2, String text, Font font, int maxWidth) {
            return Math.max(1, wrap(g2, text, font, maxWidth).size());
        }

        private int drawCenteredWrapped(Graphics2D g2, String text, int width, int y, Font font) {
            for (String baris : wrap(g2, text, font, width)) {
                y = drawCenteredText(g2, baris, width, y, font);
            }
            return y;
        }

        private int drawCenteredText(Graphics2D g2, String text, int width, int y, Font font) {
            if (text == null || text.isBlank()) {
                return y;
            }
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int x = Math.max(0, (width - fm.stringWidth(text)) / 2);
            g2.drawString(text, x, y);
            return y + fm.getHeight();
        }

        private int drawOneLine(Graphics2D g2, String text, int width, int y, Font font) {
            g2.setFont(font);
            g2.drawString(fitText(g2, text, width, font), 0, y);
            return y + g2.getFontMetrics().getHeight() - 1;
        }

        private int drawTwoCols(Graphics2D g2, String left, String right, int width, int y, Font font) {
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int rWidth = fm.stringWidth(right);
            int maxLeft = Math.max(20, width - rWidth - 8);
            g2.drawString(fitText(g2, left, maxLeft, font), 0, y);
            g2.drawString(right, width - rWidth, y);
            return y + fm.getHeight() - 1;
        }

        private String fitText(Graphics2D g2, String text, int maxWidth, Font font) {
            if (text == null) {
                return "";
            }
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(text) <= maxWidth) {
                return text;
            }
            String value = text;
            while (value.length() > 3 && fm.stringWidth(value + "..") > maxWidth) {
                value = value.substring(0, value.length() - 1);
            }
            return value + "..";
        }

        private int drawLeftRight(Graphics2D g2, String left, String right, int width, int y, Font fontL, Font fontR, boolean isItem) {
            g2.setFont(fontR);
            FontMetrics fmR = g2.getFontMetrics();
            int rWidth = right.isEmpty() ? 0 : fmR.stringWidth(right);

            g2.setFont(fontL);
            FontMetrics fmL = g2.getFontMetrics();
            int maxLWidth = width - rWidth - (rWidth > 0 ? 6 : 0);
            String printLeft = fitText(g2, left, Math.max(20, maxLWidth), fontL);

            g2.drawString(printLeft, 0, y);
            if (!right.isEmpty()) {
                g2.setFont(fontR);
                g2.drawString(right, width - rWidth, y);
            }
            return y + Math.max(fmL.getHeight(), fmR.getHeight()) - (isItem ? 3 : 1);
        }

        private String shortName(String name) {
            if (name == null || name.isBlank()) {
                return "-";
            }
            String[] parts = name.trim().split("\\s+");
            return parts.length == 1 ? parts[0].toUpperCase(Locale.of("id", "ID"))
                    : (parts[0].charAt(0) + ". " + parts[parts.length - 1]).toUpperCase(Locale.of("id", "ID"));
        }
    }
}
