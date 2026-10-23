package giriatk.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class Schema {

    private Schema() {
    }

    public static List<String> statements() {
        String script = readScript();
        List<String> statements = new ArrayList<>();
        if (script == null) {
            return statements;
        }
        for (String raw : script.split(";")) {
            String sql = raw.strip();
            if (sql.isEmpty()) {
                continue;
            }
            String upper = sql.toUpperCase();
            if (upper.startsWith("CREATE DATABASE") || upper.startsWith("USE ")) {
                continue;
            }
            statements.add(sql);
        }
        return statements;
    }

    private static String readScript() {
        Path[] candidates = {
            Paths.get("database.sql"),
            Paths.get("..", "database.sql"),
            Paths.get("config", "database.sql")
        };
        for (Path path : candidates) {
            if (Files.isReadable(path)) {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                }
            }
        }
        return readFromClasspath();
    }

    private static String readFromClasspath() {
        try (InputStream in = Schema.class.getResourceAsStream("/giriatk/resources/database.sql")) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
