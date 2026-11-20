package giriatk.util;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class ImageUtil {

    private static final String DIR = "images/barang";
    private static final Map<String, ImageIcon> cache = new HashMap<>();

    private ImageUtil() {
    }

    public static String simpanGambar(File sourceFile, String kodeBarang) {
        if (sourceFile == null || !sourceFile.exists()) {
            return null;
        }
        try {
            File dir = new File(DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String ext = getExtension(sourceFile.getName());
            String fileName = kodeBarang.replaceAll("[^a-zA-Z0-9_-]", "_") + "." + ext;
            File targetFile = new File(dir, fileName);
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            cache.remove(fileName);
            return fileName;
        } catch (Exception ex) {
            return null;
        }
    }

    public static ImageIcon getThumbnail(String fileName, int width, int height) {
        if (fileName == null || fileName.isBlank()) {
            return createPlaceholder(width, height);
        }
        String cacheKey = fileName + "_" + width + "x" + height;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        File file = cariFileGambar(fileName);
        return loadFromFile(file, width, height, cacheKey);
    }

    /**
     * Cari file gambar bukan hanya di folder kerja saat aplikasi dijalankan.
     * Aplikasi bisa dijalankan dari NetBeans (folder kerja = root projek),
     * dari jar di dist/ (folder kerja bisa dist/ atau root), atau dari
     * build/classes — folder images/barang selalu relatif terhadap salah
     * satu lokasi itu, jadi semua kandidat diperiksa berurutan.
     */
    static File cariFileGambar(String fileName) {
        java.util.List<File> kandidat = new java.util.ArrayList<>();
        kandidat.add(new File(DIR, fileName));
        try {
            java.net.URL lokasi = ImageUtil.class.getProtectionDomain().getCodeSource().getLocation();
            File appDir = new File(lokasi.toURI());
            if (appDir.isFile()) {
                appDir = appDir.getParentFile();
            }
            // appDir bisa: root projek (classes di root), dist/, build/, atau build/classes/
            File d = appDir;
            for (int i = 0; i < 4 && d != null; i++) {
                kandidat.add(new File(new File(d, DIR), fileName));
                d = d.getParentFile();
            }
        } catch (Exception ignored) {
        }
        for (File f : kandidat) {
            if (f.exists()) {
                return f;
            }
        }
        return new File(DIR, fileName);
    }

    public static ImageIcon getThumbnail(File file, int width, int height) {
        if (file == null || !file.exists()) {
            return createPlaceholder(width, height);
        }
        return loadFromFile(file, width, height, null);
    }

    private static ImageIcon loadFromFile(File file, int width, int height, String cacheKey) {
        if (!file.exists()) {
            return createPlaceholder(width, height);
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                return createPlaceholder(width, height);
            }
            // Render dua varian: ukuran normal + 2x untuk layar HiDPI
            // (scaling Windows 125%/150%). Tanpa varian 2x, thumbnail
            // 34px dipaksa membesar mengikuti scaling layar dan terlihat
            // blur. BaseMultiResolutionImage membuat Swing otomatis
            // memakai varian 2x di layar HiDPI, ukuran logisnya tetap.
            BufferedImage base = renderKeKanvas(img, width, height);
            BufferedImage dua = renderKeKanvas(img, width * 2, height * 2);
            java.awt.Image mr = new java.awt.image.BaseMultiResolutionImage(base, dua);
            ImageIcon icon = new ImageIcon(mr);
            if (cacheKey != null) {
                cache.put(cacheKey, icon);
            }
            return icon;
        } catch (Exception ex) {
            return createPlaceholder(width, height);
        }
    }

    /** Gambar sumber diskalakan proporsional (fit) lalu ditaruh di tengah
     *  kanvas width x height. */
    private static BufferedImage renderKeKanvas(BufferedImage img, int width, int height) {
        int srcW = img.getWidth();
        int srcH = img.getHeight();
        double scale = Math.min((double) width / srcW, (double) height / srcH);
        int dw = Math.max(1, (int) Math.round(srcW * scale));
        int dh = Math.max(1, (int) Math.round(srcH * scale));
        BufferedImage kecil = skalaBertahap(img, dw, dh);
        BufferedImage kanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = kanvas.createGraphics();
        g2.drawImage(kecil, (width - dw) / 2, (height - dh) / 2, null);
        g2.dispose();
        return kanvas;
    }

    /**
     * Perkecil gambar BERTAHAP (setengah-setengah) dengan bicubic.
     * Mengecilkan 400px ke 34px sekali jalan dengan bilinear membuat
     * hasil terlihat lembut/blur; penurunan bertahap menjaga detail
     * tepi jauh lebih baik (pola yang sama dipakai LogoUtil untuk logo
     * struk).
     */
    private static BufferedImage skalaBertahap(BufferedImage src, int targetW, int targetH) {
        BufferedImage cur = src;
        int w = src.getWidth();
        int h = src.getHeight();
        while (w > targetW * 2 || h > targetH * 2) {
            int nw = Math.max(targetW, w / 2);
            int nh = Math.max(targetH, h / 2);
            cur = skalaSatuLangkah(cur, nw, nh);
            w = nw;
            h = nh;
        }
        if (w != targetW || h != targetH) {
            cur = skalaSatuLangkah(cur, targetW, targetH);
        }
        return cur;
    }

    private static BufferedImage skalaSatuLangkah(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }

    public static ImageIcon createPlaceholder(int width, int height) {
        String cacheKey = "placeholder_" + width + "x" + height;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.SURFACE_HOVER);
        g2.fillRoundRect(0, 0, width, height, 8, 8);
        g2.setColor(Theme.MUTED);
        g2.drawRoundRect(0, 0, width - 1, height - 1, 8, 8);
        g2.setColor(Theme.INK_SOFT);
        int cx = width / 2;
        int cy = height / 2;
        g2.drawRect(cx - 6, cy - 5, 12, 10);
        g2.drawOval(cx - 3, cy - 3, 6, 6);
        g2.dispose();
        ImageIcon icon = new ImageIcon(img);
        cache.put(cacheKey, icon);
        return icon;
    }

    private static String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx > 0 && idx < name.length() - 1) {
            return name.substring(idx + 1).toLowerCase();
        }
        return "jpg";
    }
}
