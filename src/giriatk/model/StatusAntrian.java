package giriatk.model;

public enum StatusAntrian {
    MENUNGGU,
    PROSES,
    SELESAI,
    DIAMBIL;

    public static StatusAntrian fromDb(String value) {
        if (value == null) {
            return MENUNGGU;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MENUNGGU;
        }
    }

    public boolean isSelesai() {
        return this == SELESAI || this == DIAMBIL;
    }
}
