package giriatk.model;

import java.math.BigDecimal;

public class Ringkasan {

    private BigDecimal omzet = BigDecimal.ZERO;
    private BigDecimal laba = BigDecimal.ZERO;
    private int jumlahTransaksi;
    private int antrianAktif;
    private int stokMenipis;

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

    public int getJumlahTransaksi() {
        return jumlahTransaksi;
    }

    public void setJumlahTransaksi(int jumlahTransaksi) {
        this.jumlahTransaksi = jumlahTransaksi;
    }

    public int getAntrianAktif() {
        return antrianAktif;
    }

    public void setAntrianAktif(int antrianAktif) {
        this.antrianAktif = antrianAktif;
    }

    public int getStokMenipis() {
        return stokMenipis;
    }

    public void setStokMenipis(int stokMenipis) {
        this.stokMenipis = stokMenipis;
    }

    public BigDecimal getRataRata() {
        if (jumlahTransaksi == 0) {
            return BigDecimal.ZERO;
        }
        return omzet.divide(BigDecimal.valueOf(jumlahTransaksi), 0, java.math.RoundingMode.HALF_UP);
    }
}
