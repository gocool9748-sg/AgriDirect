package com.agrimarket.model;

/** Order lifecycle: PLACED -> CONFIRMED -> SHIPPED -> DELIVERED, or CANCELLED before shipping. */
public enum OrderStatus {
    PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;

    /** Moves a farmer may make on an order they received. */
    public boolean farmerCanMoveTo(OrderStatus next) {
        return switch (this) {
            case PLACED -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            default -> false;
        };
    }

    /** A buyer may cancel only before the order is shipped. */
    public boolean buyerCanCancel() {
        return this == PLACED || this == CONFIRMED;
    }

    public boolean isOpen() {
        return this == PLACED || this == CONFIRMED || this == SHIPPED;
    }
}
