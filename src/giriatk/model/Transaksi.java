package giriatk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Transaksi {

    private int id;
    private String kode;
    private TipeTransaksi tipe = TipeTransaksi.BARANG;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal diskon = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal bayar = BigDecimal.ZERO;
    private BigDecimal kembali = BigDecimal.ZERO;
    private String metodeBayar = "TUNAI";
    private String paymentRef;
    private String catatan;
    private Integer userId;
    private String userName;
    private LocalDateTime tanggal;
    private boolean buatAntrian = true;
    private String namaPelanggan;
    private StatusTransaksi status = StatusTransaksi.SUKSES;
    private String voidAlasan;
    private LocalDateTime voidAt;
    private Integer voidBy;
    private String voidByName;
    private final List<TransaksiItem> items = new ArrayList<>();

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

    public TipeTransaksi getTipe() {
        return tipe;
    }

    public void setTipe(TipeTransaksi tipe) {
        this.tipe = tipe;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiskon() {
        return diskon;
    }

    public void setDiskon(BigDecimal diskon) {
        this.diskon = diskon;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getBayar() {
        return bayar;
    }

    public void setBayar(BigDecimal bayar) {
        this.bayar = bayar;
    }

    public BigDecimal getKembali() {
        return kembali;
    }

    public void setKembali(BigDecimal kembali) {
        this.kembali = kembali;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    public String getMetodeBayar() {
        return metodeBayar;
    }

    public void setMetodeBayar(String metodeBayar) {
        this.metodeBayar = metodeBayar == null ? "TUNAI" : metodeBayar;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
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

    public LocalDateTime getTanggal() {
        return tanggal;
    }

    public void setTanggal(LocalDateTime tanggal) {
        this.tanggal = tanggal;
    }

    public List<TransaksiItem> getItems() {
        return items;
    }

    public boolean isBuatAntrian() {
        return buatAntrian;
    }

    public void setBuatAntrian(boolean buatAntrian) {
        this.buatAntrian = buatAntrian;
    }

    public String getNamaPelanggan() {
        return namaPelanggan;
    }

    public void setNamaPelanggan(String namaPelanggan) {
        this.namaPelanggan = namaPelanggan;
    }

    public StatusTransaksi getStatus() {
        return status;
    }

    public void setStatus(StatusTransaksi status) {
        this.status = status;
    }

    public String getVoidAlasan() {
        return voidAlasan;
    }

    public void setVoidAlasan(String voidAlasan) {
        this.voidAlasan = voidAlasan;
    }

    public LocalDateTime getVoidAt() {
        return voidAt;
    }

    public void setVoidAt(LocalDateTime voidAt) {
        this.voidAt = voidAt;
    }

    public Integer getVoidBy() {
        return voidBy;
    }

    public void setVoidBy(Integer voidBy) {
        this.voidBy = voidBy;
    }

    public String getVoidByName() {
        return voidByName;
    }

    public void setVoidByName(String voidByName) {
        this.voidByName = voidByName;
    }
}
