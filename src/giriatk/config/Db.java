package giriatk.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String PARAMS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta&characterEncoding=UTF-8";
    private static boolean driverLoaded;

    private Db() {
    }

    private static String baseUrl() {
        return "jdbc:mysql://" + AppConfig.get("db.host") + ":" + AppConfig.get("db.port");
    }

    private static String databaseUrl() {
        return baseUrl() + "/" + AppConfig.get("db.name") + PARAMS;
    }

    private static String serverUrl() {
        return baseUrl() + "/" + PARAMS;
    }

    private static void loadDriver() throws SQLException {
        if (!driverLoaded) {
            try {
                Class.forName(DRIVER);
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL (mysql-connector-j) tidak ditemukan.", e);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(databaseUrl(), AppConfig.get("db.user"), AppConfig.get("db.password"));
    }

    public static void bootstrap() throws SQLException {
        loadDriver();
        String user = AppConfig.get("db.user");
        String password = AppConfig.get("db.password");
        String dbName = AppConfig.get("db.name");
        try (Connection server = DriverManager.getConnection(serverUrl(), user, password);
                var st = server.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        try (Connection conn = getConnection(); var st = conn.createStatement()) {
            for (String sql : Schema.statements()) {
                st.execute(sql);
            }
            migrate(conn);
        }
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("images/barang"));
        } catch (Exception ignored) {
        }
    }

    private static void migrate(Connection conn) throws SQLException {
        var meta = conn.getMetaData();
        checkAndAddColumn(conn, meta, "merk", "VARCHAR(100)");
        checkAndAddColumn(conn, meta, "jenis", "VARCHAR(100)");
        checkAndAddColumn(conn, meta, "warna", "VARCHAR(50)");
        checkAndAddColumn(conn, meta, "ukuran", "VARCHAR(50)");
        checkAndAddColumn(conn, meta, "gambar", "VARCHAR(255)");
    }

    private static void checkAndAddColumn(Connection conn, java.sql.DatabaseMetaData meta, String colName, String colType) throws SQLException {
        try (var rs = meta.getColumns(null, null, "barang", colName)) {
            if (!rs.next()) {
                try (var st = conn.createStatement()) {
                    st.executeUpdate("ALTER TABLE barang ADD COLUMN `" + colName + "` " + colType);
                }
            }
        }
    }
}
