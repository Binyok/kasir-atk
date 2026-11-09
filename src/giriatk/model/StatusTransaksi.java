package giriatk.model;

public enum StatusTransaksi {
    SUKSES,
    VOID;

    public static StatusTransaksi fromDb(String value) {
        if (value == null) {
            return SUKSES;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SUKSES;
        }
    }
}
