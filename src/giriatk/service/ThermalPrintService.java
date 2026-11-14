package giriatk.service;

import giriatk.dao.PengaturanDao;
import giriatk.model.TipeItem;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;
import giriatk.util.Fmt;

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
        String lebarKertas = config.getOrDefault("printer_lebar", "80mm");

        PrintService ps = cariPrinter(targetPrinter);
        if (ps == null) {
            throw new PrinterException("Tidak ada printer yang terdeteksi pada sistem.");
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(ps);

        PageFormat pf = job.defaultPage();
        Paper paper = pf.getPaper();

        boolean is80mm = "80mm".equalsIgnoreCase(lebarKertas);
        double widthPt = is80mm ? 220 : 160;
        int itemCount = Math.max(1, transaksi.getItems().size());
        double heightPt = 220 + (itemCount * 30) + 120;

        paper.setSize(widthPt, heightPt);
        paper.setImageableArea(2, 2, widthPt - 4, heightPt - 4);
        pf.setPaper(paper);

        ReceiptPrintable printable = new ReceiptPrintable(transaksi, namaToko, alamat, telepon, footer, is80mm);
        job.setPrintable(printable, pf);

        job.print();
    }

    public void cetakUjiCoba() throws Exception {
        Transaksi t = new Transaksi();
        t.setKode("TRX-0001");
        t.setTanggal(LocalDateTime.now());
        t.setUserName("P. TOKO");
        t.setTotal(new BigDecimal("327250"));
        t.setBayar(new BigDecimal("350000"));
        t.setKembali(new BigDecimal("22750"));

        TransaksiItem item1 = new TransaksiItem();
        item1.setNamaItem("Fotokopi");
        item1.setQty(3);
        item1.setHarga(new BigDecimal("250"));
        item1.setSubtotal(new BigDecimal("750"));
        item1.setTipe(TipeItem.JASA);
        t.getItems().add(item1);

        TransaksiItem item2 = new TransaksiItem();
        item2.setNamaItem("Kertas HVS A4 70gr · PaperOne");
        item2.setQty(3);
        item2.setHarga(new BigDecimal("55000"));
        item2.setSubtotal(new BigDecimal("165000"));
        item2.setTipe(TipeItem.BARANG);
        t.getItems().add(item2);

        TransaksiItem item3 = new TransaksiItem();
        item3.setNamaItem("Buku Tulis 38 Lembar · Sinar Dunia");
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

        ReceiptPrintable(Transaksi transaksi, String namaToko, String alamat, String telepon, String footer, boolean is80mm) {
            this.transaksi = transaksi;
            this.namaToko = namaToko;
            this.alamat = alamat;
            this.telepon = telepon;
            this.footer = footer;
            this.is80mm = is80mm;
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

            // Header Toko
            Font fTitle = new Font("Georgia", Font.BOLD, is80mm ? 14 : 12);
            y = drawCenteredText(g2, namaToko, width, y, fTitle);

            Font fSub = new Font("SansSerif", Font.PLAIN, is80mm ? 8 : 7);
            String fullAddr = alamat + (telepon != null && !telepon.isBlank() ? " · Telp " + telepon : "");
            y = drawCenteredText(g2, fullAddr, width, y, fSub);
            y = drawCenteredText(g2, "Buka setiap hari 07.00–21.00", width, y, fSub);

            y += 4;
            y = drawDashedLine(g2, width, y);
            y += 6;

            // Metadata Row (2 kolom)
            Font fMeta = new Font("Monospaced", Font.PLAIN, is80mm ? 8 : 7);
            String kasirName = shortName(transaksi.getUserName());
            String kodeStruk = transaksi.getKode() == null ? "-" : transaksi.getKode();
            String noStrukText = (is80mm ? "NO. STRUK : " : "NO : ") + fitText(g2, kodeStruk, width / 2 - 10);
            String kasirText = (is80mm ? "KASIR : " : "KSR : ") + kasirName;
            y = drawTwoCols(g2, noStrukText, kasirText, width, y, fMeta);

            DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.of("id", "ID"));
            DateTimeFormatter tFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.of("id", "ID"));
            String tglStr = transaksi.getTanggal() != null ? dFmt.format(transaksi.getTanggal()).toUpperCase() : "-";
            String jamStr = transaksi.getTanggal() != null ? tFmt.format(transaksi.getTanggal()) + " WIB" : "-";
            y = drawTwoCols(g2, "TANGGAL   : " + tglStr, "PUKUL : " + jamStr, width, y, fMeta);

            y += 4;
            y = drawDashedLine(g2, width, y);
            y += 6;

            // Items
            Font fItemName = new Font("SansSerif", Font.BOLD, is80mm ? 9 : 8);
            Font fDetail = new Font("Monospaced", Font.PLAIN, is80mm ? 8 : 7);

            for (TransaksiItem item : transaksi.getItems()) {
                String name = item.getNamaItem();
                if (item.getTipe() == TipeItem.JASA) {
                    name += " [JASA]";
                }
                String subtotalStr = Fmt.angka(item.getSubtotal());

                // Baris 1: Nama item + Subtotal di kanan
                y = drawLeftRight(g2, name, subtotalStr, width, y, fItemName, fDetail, true);

                // Baris 2: Qty x Harga satuan
                String qtyHarga = item.getQty() + " × " + Fmt.angka(item.getHarga());
                y = drawLeftRight(g2, qtyHarga, "", width, y, fDetail, fDetail, false);
                y += 2;
            }

            y += 4;
            y = drawDashedLine(g2, width, y);
            y += 6;

            // Stempel LUNAS
            drawStampLunas(g2, width - (is80mm ? 70 : 54), y + 4);

            // Totals
            Font fTotalLabel = new Font("SansSerif", Font.BOLD, is80mm ? 10 : 9);
            Font fTotalVal = new Font("Monospaced", Font.BOLD, is80mm ? 11 : 10);
            Font fPlainLabel = new Font("SansSerif", Font.PLAIN, is80mm ? 8 : 7);
            Font fPlainVal = new Font("Monospaced", Font.PLAIN, is80mm ? 8 : 7);

            y = drawLeftRight(g2, "TOTAL", Fmt.rupiah(transaksi.getTotal()), width, y, fTotalLabel, fTotalVal, false);
            y = drawLeftRight(g2, "BAYAR (TUNAI)", Fmt.rupiah(transaksi.getBayar()), width, y, fPlainLabel, fPlainVal, false);
            y = drawLeftRight(g2, "KEMBALI", Fmt.rupiah(transaksi.getKembali()), width, y, fPlainLabel, fPlainVal, false);

            y += 6;
            y = drawDashedLine(g2, width, y);
            y += 8;

            // Footer
            Font fThanks = new Font("Georgia", Font.ITALIC, is80mm ? 10 : 9);
            y = drawCenteredText(g2, "— " + footer + " —", width, y, fThanks);

            y += 4;
            // Barcode visual
            y = drawBarcodeVisual(g2, width, y);
            y += 2;

            // Code caption
            Font fCap = new Font("Monospaced", Font.PLAIN, is80mm ? 7 : 6);
            String receiptCode = transaksi.getKode() == null ? "-" : transaksi.getKode();
            drawCenteredText(g2, receiptCode, width, y, fCap);

            return PAGE_EXISTS;
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
            int boxH = 22;
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
            int bcWidth = is80mm ? 160 : 120;
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

        private int drawCenteredText(Graphics2D g2, String text, int width, int y, Font font) {
            if (text == null || text.isBlank()) return y;
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int stringWidth = fm.stringWidth(text);
            int x = Math.max(0, (width - stringWidth) / 2);
            g2.drawString(text, x, y);
            return y + fm.getHeight();
        }

        private int drawTwoCols(Graphics2D g2, String left, String right, int width, int y, Font font) {
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int rWidth = fm.stringWidth(right);
            int maxLeft = Math.max(20, width - rWidth - 8);
            String safeLeft = fitText(g2, left, maxLeft);
            g2.drawString(safeLeft, 0, y);
            g2.drawString(right, width - rWidth, y);
            return y + fm.getHeight() - 1;
        }

        private String fitText(Graphics2D g2, String text, int maxWidth) {
            if (text == null || g2.getFontMetrics().stringWidth(text) <= maxWidth) return text;
            String value = text;
            while (value.length() > 3 && g2.getFontMetrics().stringWidth(value + "..") > maxWidth) {
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

            String printLeft = left;
            if (fmL.stringWidth(printLeft) > maxLWidth && maxLWidth > 20) {
                while (printLeft.length() > 3 && fmL.stringWidth(printLeft + "..") > maxLWidth) {
                    printLeft = printLeft.substring(0, printLeft.length() - 1);
                }
                printLeft += "..";
            }

            g2.drawString(printLeft, 0, y);
            if (!right.isEmpty()) {
                g2.setFont(fontR);
                g2.drawString(right, width - rWidth, y);
            }
            return y + Math.max(fmL.getHeight(), fmR.getHeight()) - (isItem ? 3 : 1);
        }

        private String shortName(String name) {
            if (name == null || name.isBlank()) return "-";
            String[] parts = name.trim().split("\\s+");
            return parts.length == 1 ? parts[0].toUpperCase(Locale.of("id", "ID"))
                    : (parts[0].charAt(0) + ". " + parts[parts.length - 1]).toUpperCase(Locale.of("id", "ID"));
        }

        private String spaced(String text) {
            if (text == null) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                sb.append(c).append(' ');
            }
            return sb.toString().trim();
        }
    }
}
