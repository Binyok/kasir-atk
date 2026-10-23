package giriatk.service;

import giriatk.config.Db;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BackupService {

    public Path backup(Path target) throws SQLException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Backup database GiriATK & Print\n");
        sb.append("-- Dibuat: ").append(LocalDateTime.now()).append("\n");
        sb.append("-- Impor lewat phpMyAdmin atau: mysql -u root giriatk < file.sql\n\n");

        try (Connection conn = Db.getConnection()) {
            List<String> tables = daftarTabel(conn);
            for (String table : tables) {
                sb.append("-- Tabel ").append(table).append('\n');
                eksporTabel(conn, table, sb);
                sb.append('\n');
            }
        }
        Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
        return target;
    }

    private List<String> daftarTabel(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private void eksporTabel(Connection conn, String table, StringBuilder sb) throws SQLException {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM `" + table + "`")) {
            ResultSetMetaData rm = rs.getMetaData();
            int n = rm.getColumnCount();
            while (rs.next()) {
                sb.append("INSERT INTO `").append(table).append("` (");
                for (int i = 1; i <= n; i++) {
                    if (i > 1) {
                        sb.append(", ");
                    }
                    sb.append('`').append(rm.getColumnName(i)).append('`');
                }
                sb.append(") VALUES (");
                for (int i = 1; i <= n; i++) {
                    if (i > 1) {
                        sb.append(", ");
                    }
                    sb.append(literal(rs.getObject(i)));
                }
                sb.append(");\n");
            }
        }
    }

    private String literal(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof BigDecimal bd) {
            return bd.toPlainString();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        String text = value.toString();
        text = text.replace("\\", "\\\\").replace("'", "\\'");
        return "'" + text + "'";
    }
}
