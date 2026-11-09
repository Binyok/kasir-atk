package giriatk.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPS = new Properties();
    private static final Properties UI = new Properties();

    private static final String[] KEYS = {
        "db.host", "db.port", "db.name", "db.user", "db.password"
    };

    static {
        PROPS.setProperty("db.host", "localhost");
        PROPS.setProperty("db.port", "3306");
        PROPS.setProperty("db.name", "giriatk");
        PROPS.setProperty("db.user", "root");
        PROPS.setProperty("db.password", "");
        loadFile();
        loadSystemOverrides();
        loadUiFile();
    }

    private AppConfig() {
    }

    private static void loadFile() {
        Path[] candidates = {
            Paths.get("config", "db.properties"),
            Paths.get("db.properties")
        };
        for (Path path : candidates) {
            if (Files.isReadable(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    PROPS.load(in);
                    return;
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void loadSystemOverrides() {
        for (String key : KEYS) {
            String value = System.getProperty(key);
            if (value != null && !value.isEmpty()) {
                PROPS.setProperty(key, value);
            }
        }
    }

    public static String get(String key) {
        return PROPS.getProperty(key, "");
    }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Path uiPath() {
        Path dir = Paths.get("config");
        if (Files.isDirectory(dir)) {
            return dir.resolve("ui.properties");
        }
        return Paths.get("ui.properties");
    }

    private static void loadUiFile() {
        Path path = uiPath();
        if (!Files.isReadable(path)) {
            return;
        }
        try (InputStream in = Files.newInputStream(path)) {
            UI.load(in);
        } catch (IOException ignored) {
        }
    }

    public static boolean getUiBool(String key, boolean fallback) {
        String value = UI.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public static void setUi(String key, String value) {
        UI.setProperty(key, value);
        Path path = uiPath();
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (java.io.OutputStream out = Files.newOutputStream(path)) {
                UI.store(out, "GiriATK UI preferences");
            }
        } catch (IOException ignored) {
        }
    }
}
