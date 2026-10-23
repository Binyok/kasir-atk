package giriatk.model;

import java.time.LocalDateTime;

public class Antrian {

    private int id;
    private Integer transaksiId;
    private Integer layananId;
    private String layananNama;
    private String namaPelanggan;
    private String deskripsi;
    private int jumlah = 1;
    private StatusAntrian status = StatusAntrian.MENUNGGU;
    private LocalDateTime waktuMasuk;
    private LocalDateTime waktuSelesai;
    private Integer userId;
    private String userName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getTransaksiId() {
        return transaksiId;
    }

    public void setTransaksiId(Integer transaksiId) {
        this.transaksiId = transaksiId;
    }

    public Integer getLayananId() {
        return layananId;
    }

    public void setLayananId(Integer layananId) {
        this.layananId = layananId;
    }

    public String getLayananNama() {
        return layananNama;
    }

    public void setLayananNama(String layananNama) {
        this.layananNama = layananNama;
    }

    public String getNamaPelanggan() {
        return namaPelanggan;
    }

    public void setNamaPelanggan(String namaPelanggan) {
        this.namaPelanggan = namaPelanggan;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public int getJumlah() {
        return jumlah;
    }

    public void setJumlah(int jumlah) {
        this.jumlah = jumlah;
    }

    public StatusAntrian getStatus() {
        return status;
    }

    public void setStatus(StatusAntrian status) {
        this.status = status;
    }

    public LocalDateTime getWaktuMasuk() {
        return waktuMasuk;
    }

    public void setWaktuMasuk(LocalDateTime waktuMasuk) {
        this.waktuMasuk = waktuMasuk;
    }

    public LocalDateTime getWaktuSelesai() {
        return waktuSelesai;
    }

    public void setWaktuSelesai(LocalDateTime waktuSelesai) {
        this.waktuSelesai = waktuSelesai;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
