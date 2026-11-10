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
        checkAndAddColumn(conn, meta, "barang", "merk", "VARCHAR(100)");
        checkAndAddColumn(conn, meta, "barang", "jenis", "VARCHAR(100)");
        checkAndAddColumn(conn, meta, "barang", "warna", "VARCHAR(50)");
        checkAndAddColumn(conn, meta, "barang", "ukuran", "VARCHAR(50)");
        checkAndAddColumn(conn, meta, "barang", "gambar", "VARCHAR(255)");
        checkAndAddColumn(conn, meta, "transaksi", "status", "ENUM('SUKSES','VOID') NOT NULL DEFAULT 'SUKSES'");
        checkAndAddColumn(conn, meta, "transaksi", "void_alasan", "VARCHAR(255)");
        checkAndAddColumn(conn, meta, "transaksi", "void_at", "DATETIME");
        checkAndAddColumn(conn, meta, "transaksi", "void_by", "INT");
        checkAndAddColumn(conn, meta, "transaksi", "metode_bayar", "VARCHAR(100) NOT NULL DEFAULT 'TUNAI'");
        try (var st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE `transaksi` MODIFY COLUMN `metode_bayar` VARCHAR(100) NOT NULL DEFAULT 'TUNAI'");
        } catch (Exception ignored) {
        }
        checkAndAddColumn(conn, meta, "transaksi", "payment_ref", "VARCHAR(150)");
    }

    private static void checkAndAddColumn(Connection conn, java.sql.DatabaseMetaData meta, String tableName, String colName, String colType) throws SQLException {
        try (var rs = meta.getColumns(null, null, tableName, colName)) {
            if (!rs.next()) {
                try (var st = conn.createStatement()) {
                    st.executeUpdate("ALTER TABLE `" + tableName + "` ADD COLUMN `" + colName + "` " + colType);
                }
            }
        }
    }
}
