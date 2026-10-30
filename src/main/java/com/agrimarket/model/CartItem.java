package com.agrimarket.model;

import java.math.BigDecimal;

/** An item in the buyer's cart (kept in memory until the order is placed). */
public record CartItem(Product product, BigDecimal quantityKg) {

    public BigDecimal subtotal() {
        return product.pricePerKg().multiply(quantityKg);
    }
}
