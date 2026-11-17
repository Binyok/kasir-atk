package giriatk.util;

import giriatk.dao.PengaturanDao;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Slot logo nota/struk.
 *
 * Urutan pencarian logo:
 * 1. Path yang dipilih lewat Pengaturan &gt; Profil Toko (kunci "logo_path").
 * 2. File logo.* / logo-toko.* di folder images/ atau folder aplikasi.
 *
 * Bila belum ada logo, semua pemakai cukup melewatkan bagian logo —
 * nota tetap tampil rapi tanpa logo.
 */
public final class LogoUtil {

    private LogoUtil() {
    }

    public static File cariFileLogo() {
        try {
            String path = new PengaturanDao().get("logo_path", "");
            if (path != null && !path.isBlank()) {
                File f = new File(path.trim());
                if (f.exists()) {
                    return f;
                }
            }
        } catch (Exception ignored) {
        }
        String[] kandidat = {
            "images/logo.png", "images/logo.jpg", "images/logo.jpeg",
            "images/logo-toko.png", "images/logo-toko.jpg",
            "logo.png", "logo.jpg", "logo.jpeg"
        };
        for (String nama : kandidat) {
            File f = new File(nama);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    public static BufferedImage muatLogo() {
        File f = cariFileLogo();
        if (f == null) {
            return null;
        }
        try {
            return ImageIO.read(f);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Logo sebagai ikon yang diskalakan proporsional (tidak pernah diperbesar). */
    public static ImageIcon muatLogoIcon(int maxW, int maxH) {
        BufferedImage img = muatLogo();
        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
            return null;
        }
        double skala = Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight());
        skala = Math.min(skala, 1.0);
        int w = Math.max(1, (int) Math.round(img.getWidth() * skala));
        int h = Math.max(1, (int) Math.round(img.getHeight() * skala));
        return new ImageIcon(skalaTajam(img, w, h));
    }

    /**
     * Perkecil gambar secara bertahap (setengah-setengah) dengan bicubic.
     * Mengecilkan langsung dari resolusi besar (mis. 1760px) ke kecil
     * (120px) dalam satu langkah membuat teks kecil di logo jadi buram;
     * penurunan bertahap menjaga tepian tetap tajam.
     */
    public static BufferedImage skalaTajam(BufferedImage src, int targetW, int targetH) {
        if (src == null || targetW <= 0 || targetH <= 0
                || (src.getWidth() == targetW && src.getHeight() == targetH)) {
            return src;
        }
        int type = src.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : src.getType();
        BufferedImage cur = src;
        int w = src.getWidth();
        int h = src.getHeight();
        while (w > targetW * 2 || h > targetH * 2) {
            w = Math.max(targetW, w / 2);
            h = Math.max(targetH, h / 2);
            cur = gambarKe(cur, w, h, type);
        }
        if (w != targetW || h != targetH) {
            cur = gambarKe(cur, targetW, targetH, type);
        }
        return cur;
    }

    private static BufferedImage gambarKe(BufferedImage src, int w, int h, int type) {
        BufferedImage out = new BufferedImage(w, h, type);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
