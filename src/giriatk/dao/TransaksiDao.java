package giriatk.dao;

import giriatk.config.Db;
import giriatk.model.StatusTransaksi;
import giriatk.model.TipeItem;
import giriatk.model.TipeTransaksi;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransaksiDao {

    public String kodeOtomatis(Connection conn) throws SQLException {
        int next = 1;
        String sqlLast = "SELECT id FROM transaksi ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlLast);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                next = rs.getInt("id") + 1;
            }
        }
        String candidate = String.format("TRX-%04d", next);
        String sqlCheck = "SELECT 1 FROM transaksi WHERE kode = ?";
        while (true) {
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setString(1, candidate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return candidate;
                    }
                }
            }
            next++;
            candidate = String.format("TRX-%04d", next);
        }
    }

    public int insert(Connection conn, Transaksi t) throws SQLException {
        String sql = "INSERT INTO transaksi (kode, tipe, subtotal, diskon, total, bayar, kembali, metode_bayar, payment_ref, catatan, user_id, tanggal) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getKode());
            ps.setString(2, t.getTipe().name());
            ps.setBigDecimal(3, t.getSubtotal());
            ps.setBigDecimal(4, t.getDiskon());
            ps.setBigDecimal(5, t.getTotal());
            ps.setBigDecimal(6, t.getBayar());
            ps.setBigDecimal(7, t.getKembali());
            ps.setString(8, t.getMetodeBayar() == null ? "TUNAI" : t.getMetodeBayar());
            ps.setString(9, t.getPaymentRef());
            ps.setString(10, t.getCatatan());
            if (t.getUserId() == null) {
                ps.setNull(11, java.sql.Types.INTEGER);
            } else {
                ps.setInt(11, t.getUserId());
            }
            ps.setTimestamp(12, Timestamp.valueOf(t.getTanggal() == null ? LocalDateTime.now() : t.getTanggal()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setId(keys.getInt(1));
                }
            }
        }
        return t.getId();
    }

    public void insertItem(Connection conn, int transaksiId, TransaksiItem item) throws SQLException {
        String sql = "INSERT INTO transaksi_item (transaksi_id, tipe, barang_id, layanan_id, nama_item, qty, harga, harga_beli, subtotal) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, transaksiId);
            ps.setString(2, item.getTipe().name());
            if (item.getBarangId() == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, item.getBarangId());
            }
            if (item.getLayananId() == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, item.getLayananId());
            }
            ps.setString(5, item.getNamaItem());
            ps.setInt(6, item.getQty());
            ps.setBigDecimal(7, item.getHarga());
            ps.setBigDecimal(8, item.getHargaBeli());
            ps.setBigDecimal(9, item.getSubtotal());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
        }
    }

    public Transaksi findById(int id) throws SQLException {
        String sql = "SELECT t.id, t.kode, t.tipe, t.subtotal, t.diskon, t.total, t.bayar, t.kembali, t.metode_bayar, t.payment_ref, t.catatan, "
                + "t.user_id, u.nama_lengkap AS user_nama, t.tanggal, t.status, t.void_alasan, t.void_at, t.void_by, "
                + "uv.nama_lengkap AS void_user_nama "
                + "FROM transaksi t "
                + "LEFT JOIN users u ON u.id = t.user_id "
                + "LEFT JOIN users uv ON uv.id = t.void_by "
                + "WHERE t.id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Transaksi t = map(rs);
                t.getItems().addAll(findItems(conn, id));
                return t;
            }
        }
    }

    public List<TransaksiItem> findItems(Connection conn, int transaksiId) throws SQLException {
        String sql = "SELECT id, transaksi_id, tipe, barang_id, layanan_id, nama_item, qty, harga, harga_beli, subtotal "
                + "FROM transaksi_item WHERE transaksi_id = ? ORDER BY id";
        List<TransaksiItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transaksiId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransaksiItem item = new TransaksiItem();
                    item.setId(rs.getInt("id"));
                    item.setTransaksiId(rs.getInt("transaksi_id"));
                    item.setTipe(TipeItem.fromDb(rs.getString("tipe")));
                    int barangId = rs.getInt("barang_id");
                    item.setBarangId(rs.wasNull() ? null : barangId);
                    int layananId = rs.getInt("layanan_id");
                    item.setLayananId(rs.wasNull() ? null : layananId);
                    item.setNamaItem(rs.getString("nama_item"));
                    item.setQty(rs.getInt("qty"));
                    item.setHarga(nvl(rs.getBigDecimal("harga")));
                    item.setHargaBeli(nvl(rs.getBigDecimal("harga_beli")));
                    item.setSubtotal(nvl(rs.getBigDecimal("subtotal")));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<Transaksi> findRange(LocalDate from, LocalDate to, int limit) throws SQLException {
        String sql = "SELECT t.id, t.kode, t.tipe, t.subtotal, t.diskon, t.total, t.bayar, t.kembali, t.metode_bayar, t.payment_ref, t.catatan, "
                + "t.user_id, u.nama_lengkap AS user_nama, t.tanggal, t.status, t.void_alasan, t.void_at, t.void_by, "
                + "uv.nama_lengkap AS void_user_nama "
                + "FROM transaksi t "
                + "LEFT JOIN users u ON u.id = t.user_id "
                + "LEFT JOIN users uv ON uv.id = t.void_by "
                + "WHERE t.tanggal >= ? AND t.tanggal < ? ORDER BY t.tanggal DESC, t.id DESC LIMIT " + Math.max(1, limit);
        List<Transaksi> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public void voidTransaksi(int id, String alasan, Integer userId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlCheck = "SELECT status FROM transaksi WHERE id = ? FOR UPDATE";
                try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Transaksi tidak ditemukan.");
                        }
                        if ("VOID".equalsIgnoreCase(rs.getString("status"))) {
                            throw new SQLException("Transaksi sudah pernah di-void sebelumnya.");
                        }
                    }
                }

                String sqlUpdate = "UPDATE transaksi SET status = 'VOID', void_alasan = ?, void_at = ?, void_by = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setString(1, alasan);
                    ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    if (userId == null) {
                        ps.setNull(3, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(3, userId);
                    }
                    ps.setInt(4, id);
                    ps.executeUpdate();
                }

                List<TransaksiItem> items = findItems(conn, id);
                BarangDao barangDao = new BarangDao();
                for (TransaksiItem item : items) {
                    if (item.getTipe() == TipeItem.BARANG && item.getBarangId() != null) {
                        barangDao.tambahStok(conn, item.getBarangId(), item.getQty());
                    }
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private Transaksi map(ResultSet rs) throws SQLException {
        Transaksi t = new Transaksi();
        t.setId(rs.getInt("id"));
        t.setKode(rs.getString("kode"));
        t.setTipe(TipeTransaksi.fromDb(rs.getString("tipe")));
        t.setSubtotal(nvl(rs.getBigDecimal("subtotal")));
        t.setDiskon(nvl(rs.getBigDecimal("diskon")));
        t.setTotal(nvl(rs.getBigDecimal("total")));
        t.setBayar(nvl(rs.getBigDecimal("bayar")));
        t.setKembali(nvl(rs.getBigDecimal("kembali")));
        t.setMetodeBayar(rs.getString("metode_bayar"));
        t.setPaymentRef(rs.getString("payment_ref"));
        t.setCatatan(rs.getString("catatan"));
        int userId = rs.getInt("user_id");
        t.setUserId(rs.wasNull() ? null : userId);
        t.setUserName(rs.getString("user_nama"));
        Timestamp ts = rs.getTimestamp("tanggal");
        t.setTanggal(ts == null ? null : ts.toLocalDateTime());
        t.setStatus(StatusTransaksi.fromDb(rs.getString("status")));
        t.setVoidAlasan(rs.getString("void_alasan"));
        Timestamp vts = rs.getTimestamp("void_at");
        t.setVoidAt(vts == null ? null : vts.toLocalDateTime());
        int vby = rs.getInt("void_by");
        t.setVoidBy(rs.wasNull() ? null : vby);
        t.setVoidByName(rs.getString("void_user_nama"));
        return t;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
