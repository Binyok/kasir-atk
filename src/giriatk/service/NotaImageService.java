package giriatk.service;

import giriatk.dao.PengaturanDao;
import giriatk.model.StatusTransaksi;
import giriatk.model.TipeItem;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;
import giriatk.util.Fmt;
import giriatk.util.Theme;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class NotaImageService {

    private final PengaturanDao meDao = new PengaturanDao();

    public BufferedImage generateNotaImage(Transaksi transaksi) {
        Map<String, String> config;
        try {
            config = meDao.semua();
        } catch (Exception e) {
            config = Map.of();
        }
        String namaToko = config.getOrDefault("nama_toko", "GiriATK & Print");
        String alamat = config.getOrDefault("alamat", "Watugilang, Cukurguling, Lumbang, Pasuruan 67183");
        if (alamat.isBlank() || alamat.equalsIgnoreCase("Jl. Contoh No. 1")) {
            alamat = "Watugilang, Cukurguling, Lumbang, Pasuruan 67183";
        }
        String telepon = config.getOrDefault("telepon", "0812-3456-7890");
        if (telepon.isBlank() || telepon.equals("0812-0000-0000")) {
            telepon = "0812-3456-7890";
        }
        String footer = config.getOrDefault("footer_struk", "Terima kasih sudah berbelanja");

        int cardW = 500;
        int cardMarginX = 24;
        int cardMarginY = 24;
        int imgW = cardW + (cardMarginX * 2);

        int contentPadX = 32;
        int contentW = cardW - (contentPadX * 2);

        // Pre-calculate receipt height
        int receiptH = 32; // top padding
        receiptH += 38; // store name
        receiptH += 36; // address & phone
        receiptH += 18; // divider
        receiptH += 48; // meta (no struk, kasir, tgl, jam)
        if (transaksi.getNamaPelanggan() != null && !transaksi.getNamaPelanggan().isBlank()) {
            receiptH += 20;
        }
        if (transaksi.getStatus() == StatusTransaksi.VOID) {
            receiptH += 36;
        }
        receiptH += 18; // divider

        for (TransaksiItem ignored : transaksi.getItems()) {
            receiptH += 46;
        }

        receiptH += 14; // divider
        boolean adaDiskon = transaksi.getDiskon() != null && transaksi.getDiskon().compareTo(BigDecimal.ZERO) > 0;
        if (adaDiskon) {
            receiptH += 22; // subtotal
            receiptH += 22; // diskon
            receiptH += 4;
        }
        receiptH += 32; // total
        receiptH += 22; // bayar
        receiptH += 22; // kembali
        receiptH += 20; // divider
        receiptH += 26; // footer thanks
        receiptH += 38; // barcode
        receiptH += 22; // code caption
        receiptH += 24; // bottom padding inside receipt
        receiptH += 14; // perforation height

        int imgH = receiptH + (cardMarginY * 2) + 8;

        BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Canvas / outer background
        g2.setColor(Theme.CANVAS);
        g2.fillRect(0, 0, imgW, imgH);

        int rx = cardMarginX;
        int ry = cardMarginY;
        int rw = cardW;
        int rh = receiptH - 14;

        // Shadow offset (classic design-system hard offset)
        g2.setColor(Theme.INK);
        g2.fillRect(rx + 6, ry + 6, rw, rh);

        // Receipt body
        g2.setColor(Theme.SURFACE);
        g2.fillRect(rx, ry, rw, rh);

        // Micro-grain paper dots
        g2.setColor(new Color(28, 25, 23, 10));
        for (int y = ry + 8; y < ry + rh - 6; y += 14) {
            for (int x = rx + 8; x < rx + rw - 6; x += 14) {
                g2.fillOval(x, y, 2, 2);
            }
        }

        // Double border
        g2.setColor(Theme.INK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(rx, ry, rw, rh);
        g2.drawRect(rx + 2, ry + 2, rw - 4, rh - 4);

        // Draw perforation saw-tooth at the bottom of the card
        drawPerforation(g2, rx, ry + rh, rw);

        int cx = rx + contentPadX;
        int y = ry + 36;

        // Header: Store Name
        drawHeaderToko(g2, namaToko, rx, rw, y);
        y += 34;

        // Subtitle: Address & Phone
        g2.setFont(Theme.text(Font.PLAIN, 11));
        g2.setColor(Theme.MUTED);
        String line1 = alamat + (telepon.isBlank() ? "" : " · Telp " + telepon);
        drawCenteredText(g2, line1, rx, rw, y);
        y += 16;
        drawCenteredText(g2, "Buka setiap hari 07.00–21.00", rx, rw, y);
        y += 20;

        // Divider
        y = drawDashedDivider(g2, cx, cx + contentW, y, Theme.INK, 1.5f);
        y += 14;

        // Metadata 2 Kolom
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.of("id", "ID"));
        g2.setFont(Theme.mono(Font.PLAIN, 10));
        g2.setColor(Theme.INK);

        String kodeStr = "NO. STRUK : " + (transaksi.getKode() == null ? "-" : transaksi.getKode());
        String kasirStr = "KASIR : " + shortName(transaksi.getUserName());
        drawLeftRight(g2, kodeStr, kasirStr, cx, contentW, y);
        y += 18;

        String tglStr = "TANGGAL   : " + (transaksi.getTanggal() == null ? "-" : dFmt.format(transaksi.getTanggal()).toUpperCase());
        String jamStr = "PUKUL : " + Fmt.jam(transaksi.getTanggal()) + " WIB";
        drawLeftRight(g2, tglStr, jamStr, cx, contentW, y);
        y += 18;

        if (transaksi.getNamaPelanggan() != null && !transaksi.getNamaPelanggan().isBlank()) {
            drawLeftRight(g2, "PELANGGAN : " + transaksi.getNamaPelanggan(), "", cx, contentW, y);
            y += 18;
        }

        if (transaksi.getStatus() == StatusTransaksi.VOID) {
            g2.setColor(Theme.DANGER);
            String voidStr = "STATUS    : VOID / BATAL (" + (transaksi.getVoidAlasan() == null ? "" : transaksi.getVoidAlasan()) + ")";
            drawLeftRight(g2, voidStr, "", cx, contentW, y);
            y += 18;
            g2.setColor(Theme.INK);
        }

        y += 2;
        y = drawDashedDivider(g2, cx, cx + contentW, y, Theme.INK, 1.5f);
        y += 16;

        // Items List
        for (TransaksiItem item : transaksi.getItems()) {
            g2.setFont(Theme.text(Font.BOLD, 12));
            g2.setColor(Theme.INK);
            String namaItem = item.getNamaItem();
            if (item.getTipe() == TipeItem.JASA) {
                namaItem += " [JASA]";
            }

            String subtotalText = Fmt.angka(item.getSubtotal());
            drawLeftRight(g2, namaItem, subtotalText, cx, contentW, y);

            y += 16;
            g2.setFont(Theme.mono(Font.PLAIN, 11));
            g2.setColor(Theme.MUTED);
            String detailQty = item.getQty() + " × " + Fmt.angka(item.getHarga());
            g2.drawString(detailQty, cx, y);
            y += 24;
        }

        y = drawDashedDivider(g2, cx, cx + contentW, y, Theme.BORDER_STRONG, 1f);
        y += 16;

        // Stamp LUNAS / VOID
        drawStamp(g2, rx + rw - contentPadX - 70, y + 24, transaksi.getStatus() == StatusTransaksi.VOID);

        // Totals
        if (adaDiskon) {
            g2.setFont(Theme.text(Font.PLAIN, 12));
            g2.setColor(Theme.INK_SOFT);
            drawLeftRight(g2, "SUBTOTAL", Fmt.rupiah(transaksi.getSubtotal()), cx, contentW, y);
            y += 20;

            g2.setFont(Theme.text(Font.BOLD, 12));
            g2.setColor(Theme.DANGER);
            drawLeftRight(g2, "DISKON GROSIR", "- " + Fmt.rupiah(transaksi.getDiskon()), cx, contentW, y);
            y += 22;
        }

        g2.setFont(Theme.text(Font.BOLD, 14));
        g2.setColor(Theme.INK);
        g2.drawString("TOTAL", cx, y);

        g2.setFont(Theme.mono(Font.BOLD, 18));
        g2.setColor(Theme.INK);
        FontMetrics fmTotal = g2.getFontMetrics();
        String totalText = Fmt.rupiah(transaksi.getTotal());
        g2.drawString(totalText, cx + contentW - fmTotal.stringWidth(totalText), y);
        y += 28;

        String metode = transaksi.getMetodeBayar() == null ? "TUNAI" : transaksi.getMetodeBayar();
        g2.setFont(Theme.text(Font.PLAIN, 11));
        g2.setColor(Theme.MUTED);
        drawLeftRight(g2, "BAYAR (" + metode + ")", Fmt.rupiah(transaksi.getBayar()), cx, contentW, y);
        y += 20;

        g2.setFont(Theme.text(Font.BOLD, 12));
        g2.setColor(Theme.GREEN_FG);
        drawLeftRight(g2, "KEMBALI", Fmt.rupiah(transaksi.getKembali()), cx, contentW, y);
        y += 22;

        y = drawDashedDivider(g2, cx, cx + contentW, y, Theme.INK, 1.5f);
        y += 20;

        // Footer Thank You
        g2.setFont(Theme.display(Font.ITALIC, 14));
        g2.setColor(Theme.INK_SOFT);
        drawCenteredText(g2, "— " + footer + " —", rx, rw, y);
        y += 20;

        // Barcode visual
        y = drawBarcodeVisual(g2, rx, rw, y);
        y += 14;

        // Code caption
        g2.setFont(Theme.mono(Font.PLAIN, 9));
        g2.setColor(Theme.FAINT);
        drawCenteredText(g2, spaced(transaksi.getKode() == null ? "" : transaksi.getKode()), rx, rw, y);

        g2.dispose();
        return image;
    }

    public void simpanNotaJpg(Transaksi transaksi, File destinationFile) throws Exception {
        BufferedImage img = generateNotaImage(transaksi);
        ImageIO.write(img, "jpg", destinationFile);
    }

    private void drawHeaderToko(Graphics2D g2, String namaToko, int rx, int rw, int y) {
        if (namaToko != null && namaToko.contains("& Print")) {
            String part1 = namaToko.replace("& Print", "").trim();
            Font f1 = Theme.display(Font.BOLD, 24);
            Font f2 = Theme.display(Font.BOLD | Font.ITALIC, 24);

            FontMetrics fm1 = g2.getFontMetrics(f1);
            FontMetrics fm2 = g2.getFontMetrics(f2);
            int totalW = fm1.stringWidth(part1 + " ") + fm2.stringWidth("& Print");
            int startX = rx + (rw - totalW) / 2;

            g2.setFont(f1);
            g2.setColor(Theme.INK);
            g2.drawString(part1, startX, y);

            g2.setFont(f2);
            g2.setColor(Theme.ACCENT);
            g2.drawString("& Print", startX + fm1.stringWidth(part1 + " "), y);
        } else {
            g2.setFont(Theme.display(Font.BOLD, 24));
            g2.setColor(Theme.INK);
            drawCenteredText(g2, namaToko, rx, rw, y);
        }
    }

    private void drawPerforation(Graphics2D g2, int x, int y, int w) {
        int step = 14;
        Polygon pShadow = new Polygon();
        for (int curX = x; curX <= x + w; curX += step) {
            pShadow.addPoint(curX, y);
            pShadow.addPoint(curX + step / 2, y + 8);
        }
        pShadow.addPoint(x + w, y);

        // Shadow
        g2.setColor(Theme.INK);
        g2.translate(4, 2);
        g2.fillPolygon(pShadow);
        g2.translate(-4, -2);

        // Tear teeth
        g2.setColor(Theme.SURFACE);
        g2.fillPolygon(pShadow);

        g2.setColor(Theme.INK);
        g2.setStroke(new BasicStroke(1.2f));
        for (int curX = x; curX <= x + w - step; curX += step) {
            g2.drawLine(curX, y, curX + step / 2, y + 8);
            g2.drawLine(curX + step / 2, y + 8, curX + step, y);
        }
    }

    private int drawDashedDivider(Graphics2D g2, int x1, int x2, int y, Color color, float width) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 3.0f}, 0.0f));
        g2.drawLine(x1, y, x2, y);
        return y;
    }

    private void drawCenteredText(Graphics2D g2, String text, int rx, int rw, int y) {
        if (text == null || text.isBlank()) return;
        FontMetrics fm = g2.getFontMetrics();
        int x = rx + (rw - fm.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }

    private void drawLeftRight(Graphics2D g2, String left, String right, int startX, int width, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(left, startX, y);
        if (right != null && !right.isBlank()) {
            int rWidth = fm.stringWidth(right);
            g2.drawString(right, startX + width - rWidth, y);
        }
    }

    private void drawStamp(Graphics2D g2, int cx, int cy, boolean isVoid) {
        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(Math.toRadians(-6));

        String stamp = isVoid ? "VOID" : "LUNAS";
        g2.setFont(Theme.text(Font.BOLD, 22));
        g2.setColor(isVoid ? Theme.DANGER : Theme.ACCENT);
        FontMetrics fm = g2.getFontMetrics();
        int sw = fm.stringWidth(stamp);

        int boxW = sw + 26;
        int boxH = 38;
        int bx = -boxW / 2;
        int by = -boxH / 2;

        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRect(bx, by, boxW, boxH);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRect(bx + 3, by + 3, boxW - 6, boxH - 6);

        g2.drawString(stamp, -sw / 2, (boxH / 2) - 9);
        g2.setTransform(old);
    }

    private int drawBarcodeVisual(Graphics2D g2, int rx, int rw, int y) {
        int bcWidth = 240;
        int bcHeight = 32;
        int startX = rx + (rw - bcWidth) / 2;

        int[] widths = {2, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 4};
        int curX = startX;
        int i = 0;
        g2.setColor(Theme.INK);
        while (curX < startX + bcWidth) {
            int w = widths[i % widths.length];
            g2.fillRect(curX, y, w, bcHeight);
            curX += w + 3;
            i++;
        }
        return y + bcHeight;
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
