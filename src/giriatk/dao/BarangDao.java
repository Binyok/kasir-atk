package giriatk.dao;

import giriatk.config.Db;
import giriatk.model.Barang;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BarangDao {

    private static final String BASE_SELECT =
            "SELECT b.id, b.kode, b.nama, b.merk, b.jenis, b.warna, b.ukuran, b.gambar, b.kategori_id, k.nama AS kategori_nama, b.satuan, "
            + "b.harga_beli, b.harga_jual, b.stok, b.stok_minimum, b.aktif "
            + "FROM barang b LEFT JOIN kategori k ON k.id = b.kategori_id ";

    public List<Barang> findAll() throws SQLException {
        return query(BASE_SELECT + "ORDER BY b.nama");
    }

    public List<Barang> findAktif() throws SQLException {
        return query(BASE_SELECT + "WHERE b.aktif = 1 ORDER BY b.nama");
    }

    public List<Barang> cari(String keyword, boolean hanyaAktif) throws SQLException {
        String sql = BASE_SELECT
                + "WHERE (b.nama LIKE ? OR b.kode LIKE ? OR b.merk LIKE ? OR b.jenis LIKE ? OR b.warna LIKE ?) "
                + (hanyaAktif ? "AND b.aktif = 1 " : "")
                + "ORDER BY b.nama";
        List<Barang> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<Barang> findMenipis() throws SQLException {
        return query(BASE_SELECT + "WHERE b.aktif = 1 AND b.stok <= b.stok_minimum ORDER BY b.stok, b.nama");
    }

    public int countMenipis() throws SQLException {
        String sql = "SELECT COUNT(*) FROM barang WHERE aktif = 1 AND stok <= stok_minimum";
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private List<Barang> query(String sql) throws SQLException {
        List<Barang> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Barang findById(int id) throws SQLException {
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(BASE_SELECT + "WHERE b.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Barang barang) throws SQLException {
        String sql = "INSERT INTO barang (kode, nama, merk, jenis, warna, ukuran, gambar, kategori_id, satuan, harga_beli, harga_jual, stok, stok_minimum, aktif) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, barang);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    barang.setId(keys.getInt(1));
                }
            }
        }
        return barang.getId();
    }

    public void update(Barang barang) throws SQLException {
        String sql = "UPDATE barang SET kode = ?, nama = ?, merk = ?, jenis = ?, warna = ?, ukuran = ?, gambar = ?, kategori_id = ?, satuan = ?, harga_beli = ?, "
                + "harga_jual = ?, stok = ?, stok_minimum = ?, aktif = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, barang);
            ps.setInt(15, barang.getId());
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Barang barang) throws SQLException {
        ps.setString(1, barang.getKode());
        ps.setString(2, barang.getNama());
        ps.setString(3, barang.getMerk());
        ps.setString(4, barang.getJenis());
        ps.setString(5, barang.getWarna());
        ps.setString(6, barang.getUkuran());
        ps.setString(7, barang.getGambar());
        if (barang.getKategoriId() == null) {
            ps.setNull(8, java.sql.Types.INTEGER);
        } else {
            ps.setInt(8, barang.getKategoriId());
        }
        ps.setString(9, barang.getSatuan());
        ps.setBigDecimal(10, barang.getHargaBeli());
        ps.setBigDecimal(11, barang.getHargaJual());
        ps.setInt(12, barang.getStok());
        ps.setInt(13, barang.getStokMinimum());
        ps.setBoolean(14, barang.isAktif());
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM barang WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean kodeTaken(String kode, int exceptId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM barang WHERE kode = ? AND id <> ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.setInt(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void tambahStok(Connection conn, int barangId, int jumlah) throws SQLException {
        String sql = "UPDATE barang SET stok = stok + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jumlah);
            ps.setInt(2, barangId);
            ps.executeUpdate();
        }
    }

    public void updateHargaBeli(Connection conn, int barangId, BigDecimal harga) throws SQLException {
        String sql = "UPDATE barang SET harga_beli = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, harga);
            ps.setInt(2, barangId);
            ps.executeUpdate();
        }
    }

    public void kurangiStok(Connection conn, int barangId, int jumlah) throws SQLException {
        String sql = "UPDATE barang SET stok = stok - ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jumlah);
            ps.setInt(2, barangId);
            ps.executeUpdate();
        }
    }

    public String kodeOtomatis() throws SQLException {
        String sql = "SELECT kode FROM barang WHERE kode LIKE 'BRG%' ORDER BY kode DESC LIMIT 1";
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            int next = 1;
            if (rs.next()) {
                String last = rs.getString("kode");
                try {
                    next = Integer.parseInt(last.substring(3)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
            return String.format("BRG%04d", next);
        }
    }

    private Barang map(ResultSet rs) throws SQLException {
        Barang b = new Barang();
        b.setId(rs.getInt("id"));
        b.setKode(rs.getString("kode"));
        b.setNama(rs.getString("nama"));
        b.setMerk(rs.getString("merk"));
        b.setJenis(rs.getString("jenis"));
        b.setWarna(rs.getString("warna"));
        b.setUkuran(rs.getString("ukuran"));
        b.setGambar(rs.getString("gambar"));
        int kategoriId = rs.getInt("kategori_id");
        b.setKategoriId(rs.wasNull() ? null : kategoriId);
        b.setKategoriNama(rs.getString("kategori_nama"));
        b.setSatuan(rs.getString("satuan"));
        b.setHargaBeli(nvl(rs.getBigDecimal("harga_beli")));
        b.setHargaJual(nvl(rs.getBigDecimal("harga_jual")));
        b.setStok(rs.getInt("stok"));
        b.setStokMinimum(rs.getInt("stok_minimum"));
        b.setAktif(rs.getBoolean("aktif"));
        return b;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
