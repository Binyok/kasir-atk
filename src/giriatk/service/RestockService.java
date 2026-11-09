package giriatk.service;

import giriatk.config.Db;
import giriatk.dao.BarangDao;
import giriatk.dao.RestockDao;
import giriatk.model.Barang;
import giriatk.model.Restock;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class RestockService {

    private final RestockDao restockDao = new RestockDao();
    private final BarangDao barangDao = new BarangDao();

    public Restock simpan(Restock restock) throws SQLException {
        if (restock.getJumlah() <= 0) {
            throw new SQLException("Jumlah restock harus lebih dari 0.");
        }
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Barang barang = barangDao.findById(restock.getBarangId());
                if (barang == null) {
                    throw new SQLException("Barang tidak ditemukan.");
                }
                restockDao.insert(conn, restock);
                barangDao.tambahStok(conn, restock.getBarangId(), restock.getJumlah());
                if (restock.getHargaBeli() != null && restock.getHargaBeli().compareTo(BigDecimal.ZERO) > 0) {
                    barangDao.updateHargaBeli(conn, restock.getBarangId(), restock.getHargaBeli());
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return restock;
    }

    public Restock catatKoreksi(int barangId, int jumlahKoreksi, String alasan, Integer userId) throws SQLException {
        if (jumlahKoreksi == 0) {
            throw new SQLException("Jumlah koreksi tidak boleh 0.");
        }
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Barang barang = barangDao.findById(barangId);
                if (barang == null) {
                    throw new SQLException("Barang tidak ditemukan.");
                }
                if (jumlahKoreksi < 0 && (barang.getStok() + jumlahKoreksi) < 0) {
                    throw new SQLException("Stok tidak mencukupi untuk dikurangi (stok saat ini: " + barang.getStok() + ").");
                }
                Restock r = new Restock();
                r.setBarangId(barangId);
                r.setJumlah(jumlahKoreksi);
                r.setHargaBeli(barang.getHargaBeli());
                r.setCatatan("[KOREKSI STOK] " + (alasan == null ? "" : alasan.trim()));
                r.setUserId(userId);
                restockDao.insert(conn, r);
                barangDao.tambahStok(conn, barangId, jumlahKoreksi);
                conn.commit();
                return r;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
