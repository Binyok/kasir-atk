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
        File file = new File(DIR, fileName);
        return loadFromFile(file, width, height, cacheKey);
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
            int srcW = img.getWidth();
            int srcH = img.getHeight();
            double scale = Math.min((double) width / srcW, (double) height / srcH);
            int dw = (int) (srcW * scale);
            int dh = (int) (srcH * scale);
            int ox = (width - dw) / 2;
            int oy = (height - dh) / 2;
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(img, ox, oy, dw, dh, null);
            g2.dispose();
            ImageIcon icon = new ImageIcon(scaled);
            if (cacheKey != null) {
                cache.put(cacheKey, icon);
            }
            return icon;
        } catch (Exception ex) {
            return createPlaceholder(width, height);
        }
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
