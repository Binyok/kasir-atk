package giriatk.model;

public enum TipeItem {
    BARANG,
    JASA;

    public static TipeItem fromDb(String value) {
        if (value == null) {
            return BARANG;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BARANG;
        }
    }

    public boolean isBarang() {
        return this == BARANG;
    }
}
