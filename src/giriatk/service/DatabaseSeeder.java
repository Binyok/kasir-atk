package giriatk.service;

import giriatk.dao.BarangDao;
import giriatk.dao.KategoriDao;
import giriatk.dao.LayananDao;
import giriatk.dao.UserDao;
import giriatk.model.Barang;
import giriatk.model.Kategori;
import giriatk.model.Layanan;
import giriatk.model.Role;
import giriatk.model.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class DatabaseSeeder {

    private final UserDao userDao = new UserDao();
    private final KategoriDao kategoriDao = new KategoriDao();
    private final LayananDao layananDao = new LayananDao();
    private final BarangDao barangDao = new BarangDao();

    public void seed() throws SQLException {
        seedUsers();
        seedKategori();
        seedLayanan();
        seedBarang();
    }

    private void seedUsers() throws SQLException {
        if (!userDao.findAll().isEmpty()) {
            return;
        }
        User owner = new User();
        owner.setUsername("owner");
        owner.setNamaLengkap("Pemilik Toko");
        owner.setRole(Role.OWNER);
        owner.setAktif(true);
        userDao.insert(owner, "owner123");

        User kasir = new User();
        kasir.setUsername("kasir");
        kasir.setNamaLengkap("Kasir");
        kasir.setRole(Role.KASIR);
        kasir.setAktif(true);
        userDao.insert(kasir, "kasir123");
    }

    private void seedKategori() throws SQLException {
        if (!kategoriDao.findAll().isEmpty()) {
            return;
        }
        addKategori("Alat Tulis", "Pulpen, pensil, penghapus, dan sejenisnya");
        addKategori("Kertas", "HVS, folio, buku, dan kertas lainnya");
        addKategori("Perlengkapan Kantor", "Map, ordner, stapler, dan lainnya");
    }

    private void addKategori(String nama, String keterangan) throws SQLException {
        Kategori k = new Kategori();
        k.setNama(nama);
        k.setKeterangan(keterangan);
        kategoriDao.insert(k);
    }

    private void seedLayanan() throws SQLException {
        if (!layananDao.findAll().isEmpty()) {
            return;
        }
        addLayanan("Print Hitam Putih", "lembar", 200, "Cetak dokumen hitam putih");
        addLayanan("Print Warna", "lembar", 1000, "Cetak dokumen berwarna");
        addLayanan("Fotokopi", "lembar", 250, "Fotokopi dokumen");
        addLayanan("Laminating", "lembar", 3000, "Laminating dokumen");
        addLayanan("Jilid Spiral", "buku", 5000, "Jilid spiral dokumen");
    }

    private void addLayanan(String nama, String satuan, double harga, String keterangan) throws SQLException {
        Layanan l = new Layanan();
        l.setNama(nama);
        l.setSatuan(satuan);
        l.setHargaSatuan(BigDecimal.valueOf(harga));
        l.setKeterangan(keterangan);
        l.setAktif(true);
        layananDao.insert(l);
    }

    private void seedBarang() throws SQLException {
        if (!barangDao.findAll().isEmpty()) {
            return;
        }
        List<Kategori> kategori = kategoriDao.findAll();
        Integer alatTulis = idByName(kategori, "Alat Tulis");
        Integer kertas = idByName(kategori, "Kertas");
        Integer kantor = idByName(kategori, "Perlengkapan Kantor");

        addBarang("Pulpen Standard AE7", alatTulis, "pcs", 2000, 3500, 40, 10);
        addBarang("Pensil 2B", alatTulis, "pcs", 1500, 3000, 36, 10);
        addBarang("Penghapus Karet", alatTulis, "pcs", 1000, 2500, 25, 8);
        addBarang("Kertas HVS A4 70gr", kertas, "rim", 45000, 55000, 12, 4);
        addBarang("Kertas HVS F4 70gr", kertas, "rim", 48000, 58000, 8, 4);
        addBarang("Buku Tulis 38 Lembar", kertas, "pcs", 3000, 5000, 30, 10);
        addBarang("Map Plastik", kantor, "pcs", 1500, 3000, 50, 10);
        addBarang("Stapler Kecil", kantor, "pcs", 12000, 18000, 6, 3);
        addBarang("Isi Staples", kantor, "box", 2000, 4000, 20, 5);
    }

    private Integer idByName(List<Kategori> list, String nama) {
        for (Kategori k : list) {
            if (k.getNama().equalsIgnoreCase(nama)) {
                return k.getId();
            }
        }
        return null;
    }

    private void addBarang(String nama, Integer kategoriId, String satuan,
            double hargaBeli, double hargaJual, int stok, int stokMinimum) throws SQLException {
        Barang b = new Barang();
        b.setKode(barangDao.kodeOtomatis());
        b.setNama(nama);
        b.setKategoriId(kategoriId);
        b.setSatuan(satuan);
        b.setHargaBeli(BigDecimal.valueOf(hargaBeli));
        b.setHargaJual(BigDecimal.valueOf(hargaJual));
        b.setStok(stok);
        b.setStokMinimum(stokMinimum);
        b.setAktif(true);
        barangDao.insert(b);
    }
}
