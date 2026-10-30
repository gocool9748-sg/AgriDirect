package com.agrimarket.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Order(
        int id,
        int buyerId,
        String buyerName,
        String buyerPhone,
        int farmerId,
        String farmerName,
        String farmerPhone,
        LocalDateTime orderDate,
        BigDecimal totalAmount,
        OrderStatus status,
        String paymentMode,
        String deliveryAddress,
        String itemsSummary) {

    public String paymentLabel() {
        return "UPI_ON_DELIVERY".equals(paymentMode) ? "UPI on delivery" : "Cash on delivery";
    }
}
