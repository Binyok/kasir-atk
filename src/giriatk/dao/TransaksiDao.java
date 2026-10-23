package giriatk.dao;

import giriatk.config.Db;
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

    private static final DateTimeFormatter KODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String kodeOtomatis(Connection conn) throws SQLException {
        String prefix = "TRX-" + LocalDate.now().format(KODE_DATE) + "-";
        String sql = "SELECT COUNT(*) FROM transaksi WHERE kode LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int next = rs.next() ? rs.getInt(1) + 1 : 1;
                return prefix + String.format("%04d", next);
            }
        }
    }

    public int insert(Connection conn, Transaksi t) throws SQLException {
        String sql = "INSERT INTO transaksi (kode, tipe, subtotal, diskon, total, bayar, kembali, catatan, user_id, tanggal) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getKode());
            ps.setString(2, t.getTipe().name());
            ps.setBigDecimal(3, t.getSubtotal());
            ps.setBigDecimal(4, t.getDiskon());
            ps.setBigDecimal(5, t.getTotal());
            ps.setBigDecimal(6, t.getBayar());
            ps.setBigDecimal(7, t.getKembali());
            ps.setString(8, t.getCatatan());
            if (t.getUserId() == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, t.getUserId());
            }
            ps.setTimestamp(10, Timestamp.valueOf(t.getTanggal() == null ? LocalDateTime.now() : t.getTanggal()));
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
        String sql = "SELECT t.id, t.kode, t.tipe, t.subtotal, t.diskon, t.total, t.bayar, t.kembali, t.catatan, "
                + "t.user_id, u.nama_lengkap AS user_nama, t.tanggal "
                + "FROM transaksi t LEFT JOIN users u ON u.id = t.user_id WHERE t.id = ?";
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
        String sql = "SELECT t.id, t.kode, t.tipe, t.subtotal, t.diskon, t.total, t.bayar, t.kembali, t.catatan, "
                + "t.user_id, u.nama_lengkap AS user_nama, t.tanggal "
                + "FROM transaksi t LEFT JOIN users u ON u.id = t.user_id "
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
        t.setCatatan(rs.getString("catatan"));
        int userId = rs.getInt("user_id");
        t.setUserId(rs.wasNull() ? null : userId);
        t.setUserName(rs.getString("user_nama"));
        Timestamp ts = rs.getTimestamp("tanggal");
        t.setTanggal(ts == null ? null : ts.toLocalDateTime());
        return t;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
