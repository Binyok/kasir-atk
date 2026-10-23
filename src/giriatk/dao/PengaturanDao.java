package giriatk.dao;

import giriatk.config.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PengaturanDao {

    public Map<String, String> semua() throws SQLException {
        String sql = "SELECT nama_kunci, nilai FROM pengaturan";
        Map<String, String> map = new LinkedHashMap<>();
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("nama_kunci"), rs.getString("nilai"));
            }
        }
        return map;
    }

    public String get(String kunci, String fallback) throws SQLException {
        String sql = "SELECT nilai FROM pengaturan WHERE nama_kunci = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kunci);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nilai") : fallback;
            }
        }
    }

    public void simpan(String kunci, String nilai) throws SQLException {
        String sql = "INSERT INTO pengaturan (nama_kunci, nilai) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE nilai = VALUES(nilai)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kunci);
            ps.setString(2, nilai == null ? "" : nilai);
            ps.executeUpdate();
        }
    }

    public void simpanSemua(Map<String, String> data) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO pengaturan (nama_kunci, nilai) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE nilai = VALUES(nilai)")) {
                for (Map.Entry<String, String> e : data.entrySet()) {
                    ps.setString(1, e.getKey());
                    ps.setString(2, e.getValue() == null ? "" : e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
