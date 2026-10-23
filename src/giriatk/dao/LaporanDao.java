package giriatk.dao;

import giriatk.config.Db;
import giriatk.model.BarisLaporan;
import giriatk.model.ItemTerlaris;
import giriatk.model.Ringkasan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LaporanDao {

    public Ringkasan ringkasanHariIni() throws SQLException {
        LocalDate today = LocalDate.now();
        Ringkasan ringkasan = new Ringkasan();
        BarisLaporan rekap = rekap(today, today);
        ringkasan.setOmzet(rekap.getOmzet());
        ringkasan.setLaba(rekap.getLaba());
        ringkasan.setJumlahTransaksi(rekap.getJumlahTransaksi());

        try (Connection conn = Db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM antrian WHERE status IN ('MENUNGGU','PROSES')");
                    ResultSet rs = ps.executeQuery()) {
                ringkasan.setAntrianAktif(rs.next() ? rs.getInt(1) : 0);
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM barang WHERE aktif = 1 AND stok <= stok_minimum");
                    ResultSet rs = ps.executeQuery()) {
                ringkasan.setStokMenipis(rs.next() ? rs.getInt(1) : 0);
            }
        }
        return ringkasan;
    }

    public BarisLaporan rekap(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM transaksi WHERE tanggal >= ? AND tanggal < ?) AS jml, "
                + "(SELECT COALESCE(SUM(total),0) FROM transaksi WHERE tanggal >= ? AND tanggal < ?) AS omzet, "
                + "(SELECT COALESCE(SUM(i.subtotal - i.harga_beli * i.qty),0) FROM transaksi_item i "
                + " JOIN transaksi t ON t.id = i.transaksi_id WHERE t.tanggal >= ? AND t.tanggal < ?) AS laba";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp start = start(from);
            Timestamp end = start(to.plusDays(1));
            int i = 1;
            for (int n = 0; n < 3; n++) {
                ps.setTimestamp(i++, start);
                ps.setTimestamp(i++, end);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BarisLaporan("Rekap", rs.getInt("jml"), nvl(rs.getBigDecimal("omzet")), nvl(rs.getBigDecimal("laba")));
                }
            }
        }
        return new BarisLaporan("Rekap", 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public List<BarisLaporan> omzetPerHari(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT d.hari, d.jml, d.omzet, COALESCE(l.laba,0) AS laba FROM "
                + "(SELECT DATE(tanggal) AS hari, COUNT(*) AS jml, COALESCE(SUM(total),0) AS omzet FROM transaksi "
                + " WHERE tanggal >= ? AND tanggal < ? GROUP BY DATE(tanggal)) d "
                + "LEFT JOIN "
                + "(SELECT DATE(t.tanggal) AS hari, COALESCE(SUM(i.subtotal - i.harga_beli * i.qty),0) AS laba "
                + " FROM transaksi_item i JOIN transaksi t ON t.id = i.transaksi_id "
                + " WHERE t.tanggal >= ? AND t.tanggal < ? GROUP BY DATE(t.tanggal)) l ON l.hari = d.hari "
                + "ORDER BY d.hari";
        List<BarisLaporan> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, start(to.plusDays(1)));
            ps.setTimestamp(3, start(from));
            ps.setTimestamp(4, start(to.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date hari = rs.getDate("hari");
                    list.add(new BarisLaporan(hari == null ? "-" : hari.toLocalDate().toString(),
                            rs.getInt("jml"), nvl(rs.getBigDecimal("omzet")), nvl(rs.getBigDecimal("laba"))));
                }
            }
        }
        return list;
    }

    public List<BarisLaporan> omzetPerHariLengkap(LocalDate from, LocalDate to) throws SQLException {
        List<BarisLaporan> isi = omzetPerHari(from, to);
        java.util.Map<LocalDate, BarisLaporan> map = new java.util.HashMap<>();
        for (BarisLaporan b : isi) {
            try {
                map.put(LocalDate.parse(b.getLabel()), b);
            } catch (Exception ignored) {
            }
        }
        List<BarisLaporan> hasil = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            BarisLaporan b = map.get(d);
            hasil.add(b != null ? b : new BarisLaporan(d.toString(), 0, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        return hasil;
    }

    public List<ItemTerlaris> itemTerlaris(LocalDate from, LocalDate to, int limit) throws SQLException {
        String sql = "SELECT i.nama_item, i.tipe, SUM(i.qty) AS qty, COALESCE(SUM(i.subtotal),0) AS omzet, "
                + "COALESCE(SUM(i.subtotal - i.harga_beli * i.qty),0) AS laba "
                + "FROM transaksi_item i JOIN transaksi t ON t.id = i.transaksi_id "
                + "WHERE t.tanggal >= ? AND t.tanggal < ? "
                + "GROUP BY i.nama_item, i.tipe ORDER BY qty DESC LIMIT " + Math.max(1, limit);
        List<ItemTerlaris> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, start(to.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ItemTerlaris(rs.getString("nama_item"), rs.getString("tipe"),
                            rs.getInt("qty"), nvl(rs.getBigDecimal("omzet")), nvl(rs.getBigDecimal("laba"))));
                }
            }
        }
        return list;
    }

    public List<BarisLaporan> rekapPerTipe(LocalDate from, LocalDate to) throws SQLException {
        return rekapKelompok(from, to,
                "i.tipe", "WHERE t.tanggal >= ? AND t.tanggal < ? AND i.tipe IN ('BARANG','JASA')");
    }

    public List<BarisLaporan> rekapPerKategori(LocalDate from, LocalDate to) throws SQLException {
        return rekapKelompok(from, to,
                "COALESCE(k.nama, 'Tanpa Kategori')",
                "LEFT JOIN barang b ON b.id = i.barang_id LEFT JOIN kategori k ON k.id = b.kategori_id "
                + "WHERE t.tanggal >= ? AND t.tanggal < ? AND i.tipe = 'BARANG'");
    }

    private List<BarisLaporan> rekapKelompok(LocalDate from, LocalDate to, String groupExpr, String whereAndJoin)
            throws SQLException {
        String sql = "SELECT " + groupExpr + " AS label, COUNT(DISTINCT t.id) AS jml, "
                + "COALESCE(SUM(i.subtotal),0) AS omzet, COALESCE(SUM(i.subtotal - i.harga_beli * i.qty),0) AS laba "
                + "FROM transaksi_item i JOIN transaksi t ON t.id = i.transaksi_id " + whereAndJoin + " "
                + "GROUP BY " + groupExpr + " ORDER BY omzet DESC";
        List<BarisLaporan> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, start(from));
            ps.setTimestamp(2, start(to.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BarisLaporan(rs.getString("label"), rs.getInt("jml"),
                            nvl(rs.getBigDecimal("omzet")), nvl(rs.getBigDecimal("laba"))));
                }
            }
        }
        return list;
    }

    private Timestamp start(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
