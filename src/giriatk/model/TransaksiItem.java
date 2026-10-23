package giriatk.model;

import java.math.BigDecimal;

public class TransaksiItem {

    private int id;
    private int transaksiId;
    private TipeItem tipe;
    private Integer barangId;
    private Integer layananId;
    private String namaItem;
    private int qty;
    private BigDecimal harga = BigDecimal.ZERO;
    private BigDecimal hargaBeli = BigDecimal.ZERO;
    private BigDecimal subtotal = BigDecimal.ZERO;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTransaksiId() {
        return transaksiId;
    }

    public void setTransaksiId(int transaksiId) {
        this.transaksiId = transaksiId;
    }

    public TipeItem getTipe() {
        return tipe;
    }

    public void setTipe(TipeItem tipe) {
        this.tipe = tipe;
    }

    public Integer getBarangId() {
        return barangId;
    }

    public void setBarangId(Integer barangId) {
        this.barangId = barangId;
    }

    public Integer getLayananId() {
        return layananId;
    }

    public void setLayananId(Integer layananId) {
        this.layananId = layananId;
    }

    public String getNamaItem() {
        return namaItem;
    }

    public void setNamaItem(String namaItem) {
        this.namaItem = namaItem;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public BigDecimal getHarga() {
        return harga;
    }

    public void setHarga(BigDecimal harga) {
        this.harga = harga;
    }

    public BigDecimal getHargaBeli() {
        return hargaBeli;
    }

    public void setHargaBeli(BigDecimal hargaBeli) {
        this.hargaBeli = hargaBeli;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getLaba() {
        return subtotal.subtract(hargaBeli.multiply(BigDecimal.valueOf(qty)));
    }
}
