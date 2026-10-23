package giriatk.model;

public enum Role {
    OWNER,
    KASIR;

    public static Role fromDb(String value) {
        if (value == null) {
            return KASIR;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return KASIR;
        }
    }
}
