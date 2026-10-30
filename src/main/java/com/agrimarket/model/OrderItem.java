package com.agrimarket.model;

import java.math.BigDecimal;

public record OrderItem(int productId, String productName, BigDecimal quantityKg, BigDecimal unitPrice) {

    public BigDecimal subtotal() {
        return unitPrice.multiply(quantityKg);
    }
}
