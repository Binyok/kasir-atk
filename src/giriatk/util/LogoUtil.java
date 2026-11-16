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
        return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }
}
