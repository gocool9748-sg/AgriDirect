package com.agrimarket.model;

import java.math.BigDecimal;

public record Product(
        int id,
        int farmerId,
        String farmerName,
        String farmerVillage,
        String farmerDistrict,
        int categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal pricePerKg,
        BigDecimal qtyAvailable,
        boolean active) {

    public static final BigDecimal LOW_STOCK_KG = new BigDecimal("10");

    /** Status label shown to farmers and the admin. */
    public String stockStatus() {
        if (!active) {
            return "Removed";
        }
        if (qtyAvailable.signum() <= 0) {
            return "Out of stock";
        }
        if (qtyAvailable.compareTo(LOW_STOCK_KG) < 0) {
            return "Low stock";
        }
        return "Active";
    }
}
