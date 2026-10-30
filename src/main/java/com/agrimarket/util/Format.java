package com.agrimarket.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Display helpers for money, weights and dates. */
public final class Format {

    private static final NumberFormat RUPEES = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private Format() {
    }

    public static String money(BigDecimal amount) {
        return RUPEES.format(amount == null ? BigDecimal.ZERO : amount);
    }

    public static String kg(BigDecimal quantity) {
        if (quantity == null) {
            return "0 kg";
        }
        return quantity.stripTrailingZeros().toPlainString() + " kg";
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }
}
