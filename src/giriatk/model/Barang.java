package giriatk.model;

import java.math.BigDecimal;

public class Barang {

    private int id;
    private String kode;
    private String nama;
    private String merk;
    private String jenis;
    private String warna;
    private String ukuran;
    private String gambar;
    private Integer kategoriId;
    private String kategoriNama;
    private String satuan = "pcs";
    private BigDecimal hargaBeli = BigDecimal.ZERO;
    private BigDecimal hargaJual = BigDecimal.ZERO;
    private int stok;
    private int stokMinimum;
    private boolean aktif = true;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public Integer getKategoriId() {
        return kategoriId;
    }

    public void setKategoriId(Integer kategoriId) {
        this.kategoriId = kategoriId;
    }

    public String getKategoriNama() {
        return kategoriNama;
    }

    public void setKategoriNama(String kategoriNama) {
        this.kategoriNama = kategoriNama;
    }

    public String getSatuan() {
        return satuan;
    }

    public void setSatuan(String satuan) {
        this.satuan = satuan;
    }

    public BigDecimal getHargaBeli() {
        return hargaBeli;
    }

    public void setHargaBeli(BigDecimal hargaBeli) {
        this.hargaBeli = hargaBeli;
    }

    public BigDecimal getHargaJual() {
        return hargaJual;
    }

    public void setHargaJual(BigDecimal hargaJual) {
        this.hargaJual = hargaJual;
    }

    public int getStok() {
        return stok;
    }

    public void setStok(int stok) {
        this.stok = stok;
    }

    public int getStokMinimum() {
        return stokMinimum;
    }

    public void setStokMinimum(int stokMinimum) {
        this.stokMinimum = stokMinimum;
    }

    public boolean isAktif() {
        return aktif;
    }

    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    public String getMerk() {
        return merk;
    }

    public void setMerk(String merk) {
        this.merk = merk;
    }

    public String getJenis() {
        return jenis;
    }

    public void setJenis(String jenis) {
        this.jenis = jenis;
    }

    public String getWarna() {
        return warna;
    }

    public void setWarna(String warna) {
        this.warna = warna;
    }

    public String getUkuran() {
        return ukuran;
    }

    public void setUkuran(String ukuran) {
        this.ukuran = ukuran;
    }

    public String getGambar() {
        return gambar;
    }

    public void setGambar(String gambar) {
        this.gambar = gambar;
    }

    public String getLabelLengkap() {
        StringBuilder sb = new StringBuilder(nama);
        if (merk != null && !merk.isEmpty()) {
            sb.append(" · ").append(merk);
        }
        if (jenis != null && !jenis.isEmpty()) {
            sb.append(" · ").append(jenis);
        }
        if (warna != null && !warna.isEmpty()) {
            sb.append(" · ").append(warna);
        }
        if (ukuran != null && !ukuran.isEmpty()) {
            sb.append(" ").append(ukuran);
        }
        return sb.toString();
    }

    public boolean isHabis() {
        return stok <= 0;
    }

    public boolean isMenipis() {
        return stok > 0 && stok <= stokMinimum;
    }

    public String getStatusStok() {
        if (isHabis()) {
            return "HABIS";
        }
        if (isMenipis()) {
            return "MENIPIS";
        }
        return "AMAN";
    }

    @Override
    public String toString() {
        return nama;
    }
}
