package giriatk.dao;

import giriatk.config.Db;
import giriatk.model.Layanan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LayananDao {

    public List<Layanan> findAll() throws SQLException {
        return query("SELECT id, nama, satuan, harga_satuan, keterangan, aktif FROM layanan ORDER BY nama");
    }

    public List<Layanan> findAllAktif() throws SQLException {
        return query("SELECT id, nama, satuan, harga_satuan, keterangan, aktif FROM layanan WHERE aktif = 1 ORDER BY nama");
    }

    private List<Layanan> query(String sql) throws SQLException {
        List<Layanan> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Layanan findById(int id) throws SQLException {
        String sql = "SELECT id, nama, satuan, harga_satuan, keterangan, aktif FROM layanan WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Layanan layanan) throws SQLException {
        String sql = "INSERT INTO layanan (nama, satuan, harga_satuan, keterangan, aktif) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, layanan.getNama());
            ps.setString(2, layanan.getSatuan());
            ps.setBigDecimal(3, layanan.getHargaSatuan());
            ps.setString(4, layanan.getKeterangan());
            ps.setBoolean(5, layanan.isAktif());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    layanan.setId(keys.getInt(1));
                }
            }
        }
        return layanan.getId();
    }

    public void update(Layanan layanan) throws SQLException {
        String sql = "UPDATE layanan SET nama = ?, satuan = ?, harga_satuan = ?, keterangan = ?, aktif = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, layanan.getNama());
            ps.setString(2, layanan.getSatuan());
            ps.setBigDecimal(3, layanan.getHargaSatuan());
            ps.setString(4, layanan.getKeterangan());
            ps.setBoolean(5, layanan.isAktif());
            ps.setInt(6, layanan.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM layanan WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean namaTaken(String nama, int exceptId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM layanan WHERE nama = ? AND id <> ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama);
            ps.setInt(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Layanan map(ResultSet rs) throws SQLException {
        Layanan l = new Layanan();
        l.setId(rs.getInt("id"));
        l.setNama(rs.getString("nama"));
        l.setSatuan(rs.getString("satuan"));
        BigDecimal harga = rs.getBigDecimal("harga_satuan");
        l.setHargaSatuan(harga == null ? BigDecimal.ZERO : harga);
        l.setKeterangan(rs.getString("keterangan"));
        l.setAktif(rs.getBoolean("aktif"));
        return l;
    }
}
