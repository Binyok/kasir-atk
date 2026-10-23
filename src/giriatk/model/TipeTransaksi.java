package giriatk.model;

public enum TipeTransaksi {
    BARANG,
    JASA,
    CAMPURAN;

    public static TipeTransaksi fromDb(String value) {
        if (value == null) {
            return BARANG;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BARANG;
        }
    }

    public static TipeTransaksi dariItem(boolean adaBarang, boolean adaJasa) {
        if (adaBarang && adaJasa) {
            return CAMPURAN;
        }
        if (adaJasa) {
            return JASA;
        }
        return BARANG;
    }
}
