package giriatk.service;

import giriatk.config.Db;
import giriatk.dao.AntrianDao;
import giriatk.dao.BarangDao;
import giriatk.dao.TransaksiDao;
import giriatk.model.Antrian;
import giriatk.model.Barang;
import giriatk.model.StatusAntrian;
import giriatk.model.TipeItem;
import giriatk.model.TipeTransaksi;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class TransaksiService {

    private final TransaksiDao transaksiDao = new TransaksiDao();
    private final BarangDao barangDao = new BarangDao();
    private final AntrianDao antrianDao = new AntrianDao();

    public Transaksi simpan(Transaksi transaksi) throws SQLException {
        if (transaksi.getItems().isEmpty()) {
            throw new SQLException("Keranjang masih kosong.");
        }
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                hitung(transaksi);
                validasiStok(conn, transaksi);
                transaksi.setKode(transaksiDao.kodeOtomatis(conn));
                transaksiDao.insert(conn, transaksi);
                for (TransaksiItem item : transaksi.getItems()) {
                    transaksiDao.insertItem(conn, transaksi.getId(), item);
                    if (item.getTipe() == TipeItem.BARANG && item.getBarangId() != null) {
                        barangDao.kurangiStok(conn, item.getBarangId(), item.getQty());
                    }
                }
                if (transaksi.isBuatAntrian()) {
                    buatAntrian(conn, transaksi);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return transaksi;
    }

    private void hitung(Transaksi transaksi) {
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean adaBarang = false;
        boolean adaJasa = false;
        for (TransaksiItem item : transaksi.getItems()) {
            BigDecimal harga = item.getHarga() == null ? BigDecimal.ZERO : item.getHarga();
            item.setSubtotal(harga.multiply(BigDecimal.valueOf(item.getQty())));
            subtotal = subtotal.add(item.getSubtotal());
            if (item.getTipe() == TipeItem.BARANG) {
                adaBarang = true;
            } else {
                adaJasa = true;
            }
        }
        transaksi.setSubtotal(subtotal);
        transaksi.setTipe(TipeTransaksi.dariItem(adaBarang, adaJasa));
        BigDecimal diskon = transaksi.getDiskon() == null ? BigDecimal.ZERO : transaksi.getDiskon();
        BigDecimal total = subtotal.subtract(diskon).max(BigDecimal.ZERO);
        transaksi.setTotal(total);
        BigDecimal bayar = transaksi.getBayar() == null ? BigDecimal.ZERO : transaksi.getBayar();
        transaksi.setKembali(bayar.subtract(total).max(BigDecimal.ZERO));
    }

    private void validasiStok(Connection conn, Transaksi transaksi) throws SQLException {
        for (TransaksiItem item : transaksi.getItems()) {
            if (item.getTipe() != TipeItem.BARANG || item.getBarangId() == null) {
                continue;
            }
            Barang barang = barangDao.findById(item.getBarangId());
            if (barang == null) {
                throw new SQLException("Barang \"" + item.getNamaItem() + "\" tidak ditemukan.");
            }
            if (barang.getStok() < item.getQty()) {
                throw new SQLException("Stok \"" + barang.getNama() + "\" tidak cukup. Tersisa " + barang.getStok() + ".");
            }
        }
    }

    private void buatAntrian(Connection conn, Transaksi transaksi) throws SQLException {
        for (TransaksiItem item : transaksi.getItems()) {
            if (item.getTipe() != TipeItem.JASA) {
                continue;
            }
            Antrian antrian = new Antrian();
            antrian.setTransaksiId(transaksi.getId());
            antrian.setLayananId(item.getLayananId());
            antrian.setNamaPelanggan(transaksi.getNamaPelanggan());
            antrian.setDeskripsi(item.getNamaItem());
            antrian.setJumlah(item.getQty());
            antrian.setStatus(StatusAntrian.MENUNGGU);
            antrian.setUserId(transaksi.getUserId());
            antrianDao.insert(conn, antrian);
        }
    }
}
