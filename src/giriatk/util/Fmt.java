package giriatk.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Fmt {

    private static final Locale LOCALE = Locale.of("id", "ID");
    private static final DecimalFormat CURRENCY = new DecimalFormat("#,##0", new DecimalFormatSymbols(LOCALE));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", LOCALE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", LOCALE);
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private Fmt() {
    }

    public static String rupiah(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return "Rp " + CURRENCY.format(v);
    }

    public static String rupiah(double value) {
        return rupiah(BigDecimal.valueOf(value));
    }

    public static String angka(BigDecimal value) {
        return CURRENCY.format(value == null ? BigDecimal.ZERO : value);
    }

    public static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    public static String tanggal(LocalDate value) {
        return value == null ? "-" : DATE.format(value);
    }

    public static String tanggalWaktu(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    public static String jam(LocalDateTime value) {
        return value == null ? "-" : TIME.format(value);
    }

    public static String capWaktu() {
        return FILE_STAMP.format(LocalDateTime.now());
    }

    public static BigDecimal parseAngka(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseBulat(String text) {
        BigDecimal value = parseAngka(text);
        return value == null ? null : value.intValue();
    }
}
