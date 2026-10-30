package com.agrimarket.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Input checks shared by the services. Each returns the cleaned value or throws ValidationException. */
public final class Validate {

    private Validate() {
    }

    public static String required(String value, String field, int maxLength) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        if (v.length() > maxLength) {
            throw new ValidationException(field + " must be at most " + maxLength + " characters.");
        }
        return v;
    }

    public static String optional(String value, int maxLength, String field) {
        String v = value == null ? "" : value.trim();
        if (v.length() > maxLength) {
            throw new ValidationException(field + " must be at most " + maxLength + " characters.");
        }
        return v.isEmpty() ? null : v;
    }

    /** Indian mobile number: 10 digits starting 6-9 (spaces, dashes and +91 are ignored). */
    public static String phone(String value) {
        String digits = value == null ? "" : value.replaceAll("[\\s-]", "");
        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        }
        if (!digits.matches("[6-9]\\d{9}")) {
            throw new ValidationException("Enter a valid 10-digit mobile number.");
        }
        return digits;
    }

    public static void password(String password, String confirm) {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        if (!password.equals(confirm)) {
            throw new ValidationException("Passwords do not match.");
        }
    }

    public static BigDecimal positiveAmount(String value, String field) {
        BigDecimal v = number(value, field);
        if (v.signum() <= 0) {
            throw new ValidationException(field + " must be greater than zero.");
        }
        return v;
    }

    public static BigDecimal nonNegativeAmount(String value, String field) {
        BigDecimal v = number(value, field);
        if (v.signum() < 0) {
            throw new ValidationException(field + " cannot be negative.");
        }
        return v;
    }

    private static BigDecimal number(String value, String field) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        try {
            BigDecimal d = new BigDecimal(v);
            if (d.scale() > 2) {
                d = d.setScale(2, RoundingMode.HALF_UP);
            }
            if (d.compareTo(new BigDecimal("99999999")) > 0) {
                throw new ValidationException(field + " is too large.");
            }
            return d;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number, for example 25 or 12.5.");
        }
    }
}
