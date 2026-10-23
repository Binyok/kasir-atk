package giriatk.dao;

import giriatk.config.Db;
import giriatk.model.Kategori;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class KategoriDao {

    public List<Kategori> findAll() throws SQLException {
        String sql = "SELECT k.id, k.nama, k.keterangan, "
                + "(SELECT COUNT(*) FROM barang b WHERE b.kategori_id = k.id) AS jumlah_barang "
                + "FROM kategori k ORDER BY k.nama";
        List<Kategori> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Kategori k = new Kategori();
                k.setId(rs.getInt("id"));
                k.setNama(rs.getString("nama"));
                k.setKeterangan(rs.getString("keterangan"));
                k.setJumlahBarang(rs.getInt("jumlah_barang"));
                list.add(k);
            }
        }
        return list;
    }

    public int insert(Kategori kategori) throws SQLException {
        String sql = "INSERT INTO kategori (nama, keterangan) VALUES (?, ?)";
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, kategori.getNama());
            ps.setString(2, kategori.getKeterangan());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    kategori.setId(keys.getInt(1));
                }
            }
        }
        return kategori.getId();
    }

    public void update(Kategori kategori) throws SQLException {
        String sql = "UPDATE kategori SET nama = ?, keterangan = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kategori.getNama());
            ps.setString(2, kategori.getKeterangan());
            ps.setInt(3, kategori.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM kategori WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean namaTaken(String nama, int exceptId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM kategori WHERE nama = ? AND id <> ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama);
            ps.setInt(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
