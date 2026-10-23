package giriatk.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPS = new Properties();

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
}
