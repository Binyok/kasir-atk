package giriatk.dao;

import giriatk.config.Db;
import giriatk.model.Restock;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RestockDao {

    private static final String BASE_SELECT =
            "SELECT r.id, r.barang_id, b.nama AS barang_nama, r.jumlah, r.harga_beli, r.supplier, r.catatan, "
            + "r.user_id, u.nama_lengkap AS user_nama, r.tanggal "
            + "FROM restock r LEFT JOIN barang b ON b.id = r.barang_id LEFT JOIN users u ON u.id = r.user_id ";

    public List<Restock> findAll(int limit) throws SQLException {
        String sql = BASE_SELECT + "ORDER BY r.tanggal DESC, r.id DESC LIMIT " + Math.max(1, limit);
        List<Restock> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<Restock> findByBarang(int barangId) throws SQLException {
        String sql = BASE_SELECT + "WHERE r.barang_id = ? ORDER BY r.tanggal DESC, r.id DESC";
        List<Restock> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, barangId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public int insert(Connection conn, Restock restock) throws SQLException {
        String sql = "INSERT INTO restock (barang_id, jumlah, harga_beli, supplier, catatan, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, restock.getBarangId());
            ps.setInt(2, restock.getJumlah());
            ps.setBigDecimal(3, restock.getHargaBeli());
            ps.setString(4, restock.getSupplier());
            ps.setString(5, restock.getCatatan());
            if (restock.getUserId() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, restock.getUserId());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    restock.setId(keys.getInt(1));
                }
            }
        }
        return restock.getId();
    }

    private Restock map(ResultSet rs) throws SQLException {
        Restock r = new Restock();
        r.setId(rs.getInt("id"));
        r.setBarangId(rs.getInt("barang_id"));
        r.setBarangNama(rs.getString("barang_nama"));
        r.setJumlah(rs.getInt("jumlah"));
        BigDecimal harga = rs.getBigDecimal("harga_beli");
        r.setHargaBeli(harga == null ? BigDecimal.ZERO : harga);
        r.setSupplier(rs.getString("supplier"));
        r.setCatatan(rs.getString("catatan"));
        int userId = rs.getInt("user_id");
        r.setUserId(rs.wasNull() ? null : userId);
        r.setUserName(rs.getString("user_nama"));
        Timestamp ts = rs.getTimestamp("tanggal");
        r.setTanggal(ts == null ? null : ts.toLocalDateTime());
        return r;
    }
}
