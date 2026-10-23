package giriatk.model;

import java.math.BigDecimal;

public class BarisLaporan {

    private String label;
    private int jumlahTransaksi;
    private BigDecimal omzet = BigDecimal.ZERO;
    private BigDecimal laba = BigDecimal.ZERO;

    public BarisLaporan() {
    }

    public BarisLaporan(String label, int jumlahTransaksi, BigDecimal omzet, BigDecimal laba) {
        this.label = label;
        this.jumlahTransaksi = jumlahTransaksi;
        this.omzet = omzet;
        this.laba = laba;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getJumlahTransaksi() {
        return jumlahTransaksi;
    }

    public void setJumlahTransaksi(int jumlahTransaksi) {
        this.jumlahTransaksi = jumlahTransaksi;
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
