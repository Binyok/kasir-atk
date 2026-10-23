package giriatk.model;

import java.math.BigDecimal;

public class ItemTerlaris {

    private String nama;
    private String tipe;
    private int qty;
    private BigDecimal omzet = BigDecimal.ZERO;
    private BigDecimal laba = BigDecimal.ZERO;

    public ItemTerlaris() {
    }

    public ItemTerlaris(String nama, String tipe, int qty, BigDecimal omzet, BigDecimal laba) {
        this.nama = nama;
        this.tipe = tipe;
        this.qty = qty;
        this.omzet = omzet;
        this.laba = laba;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getTipe() {
        return tipe;
    }

    public void setTipe(String tipe) {
        this.tipe = tipe;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public BigDecimal getOmzet() {
        return omzet;
    }

    public void setOmzet(BigDecimal omzet) {
        this.omzet = omzet;
    }

    public BigDecimal getLaba() {
        return laba;
    }

    public void setLaba(BigDecimal laba) {
        this.laba = laba;
    }
}
