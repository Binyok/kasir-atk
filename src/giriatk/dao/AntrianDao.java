package giriatk.dao;

import giriatk.config.Db;
import giriatk.model.Antrian;
import giriatk.model.StatusAntrian;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AntrianDao {

    private static final String BASE_SELECT =
            "SELECT a.id, a.transaksi_id, a.layanan_id, l.nama AS layanan_nama, a.nama_pelanggan, "
            + "a.deskripsi, a.jumlah, a.status, a.waktu_masuk, a.waktu_selesai, a.user_id, u.nama_lengkap AS user_nama "
            + "FROM antrian a LEFT JOIN layanan l ON l.id = a.layanan_id LEFT JOIN users u ON u.id = a.user_id ";

    private static final String ORDER_STATUS =
            "ORDER BY CASE a.status WHEN 'MENUNGGU' THEN 1 WHEN 'PROSES' THEN 2 WHEN 'SELESAI' THEN 3 ELSE 4 END, a.waktu_masuk ";

    public List<Antrian> findAll() throws SQLException {
        return query(BASE_SELECT + ORDER_STATUS);
    }

    public List<Antrian> findAktif() throws SQLException {
        return query(BASE_SELECT + "WHERE a.status IN ('MENUNGGU','PROSES') " + ORDER_STATUS);
    }

    public List<Antrian> findAktifTerbaru(int limit) throws SQLException {
        return query(BASE_SELECT + "WHERE a.status IN ('MENUNGGU','PROSES') " + ORDER_STATUS + "LIMIT " + limit);
    }

    public int countAktif() throws SQLException {
        String sql = "SELECT COUNT(*) FROM antrian WHERE status IN ('MENUNGGU','PROSES')";
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private List<Antrian> query(String sql) throws SQLException {
        List<Antrian> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Antrian findById(int id) throws SQLException {
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(BASE_SELECT + "WHERE a.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Antrian antrian) throws SQLException {
        String sql = "INSERT INTO antrian (transaksi_id, layanan_id, nama_pelanggan, deskripsi, jumlah, status, user_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (antrian.getTransaksiId() == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, antrian.getTransaksiId());
            }
            if (antrian.getLayananId() == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, antrian.getLayananId());
            }
            ps.setString(3, antrian.getNamaPelanggan());
            ps.setString(4, antrian.getDeskripsi());
            ps.setInt(5, antrian.getJumlah());
            ps.setString(6, antrian.getStatus().name());
            if (antrian.getUserId() == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, antrian.getUserId());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    antrian.setId(keys.getInt(1));
                }
            }
        }
        return antrian.getId();
    }

    public int insert(Antrian antrian) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return insert(conn, antrian);
        }
    }

    public void updateStatus(int id, StatusAntrian status) throws SQLException {
        String sql = "UPDATE antrian SET status = ?, waktu_selesai = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (status.isSelesai()) {
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            }
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void update(Antrian antrian) throws SQLException {
        String sql = "UPDATE antrian SET layanan_id = ?, nama_pelanggan = ?, deskripsi = ?, jumlah = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (antrian.getLayananId() == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, antrian.getLayananId());
            }
            ps.setString(2, antrian.getNamaPelanggan());
            ps.setString(3, antrian.getDeskripsi());
            ps.setInt(4, antrian.getJumlah());
            ps.setInt(5, antrian.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM antrian WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Antrian map(ResultSet rs) throws SQLException {
        Antrian a = new Antrian();
        a.setId(rs.getInt("id"));
        int transaksiId = rs.getInt("transaksi_id");
        a.setTransaksiId(rs.wasNull() ? null : transaksiId);
        int layananId = rs.getInt("layanan_id");
        a.setLayananId(rs.wasNull() ? null : layananId);
        a.setLayananNama(rs.getString("layanan_nama"));
        a.setNamaPelanggan(rs.getString("nama_pelanggan"));
        a.setDeskripsi(rs.getString("deskripsi"));
        a.setJumlah(rs.getInt("jumlah"));
        a.setStatus(StatusAntrian.fromDb(rs.getString("status")));
        Timestamp masuk = rs.getTimestamp("waktu_masuk");
        a.setWaktuMasuk(masuk == null ? null : masuk.toLocalDateTime());
        Timestamp selesai = rs.getTimestamp("waktu_selesai");
        a.setWaktuSelesai(selesai == null ? null : selesai.toLocalDateTime());
        int userId = rs.getInt("user_id");
        a.setUserId(rs.wasNull() ? null : userId);
        a.setUserName(rs.getString("user_nama"));
        return a;
    }
}
