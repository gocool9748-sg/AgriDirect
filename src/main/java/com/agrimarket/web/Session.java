package com.agrimarket.web;

import com.agrimarket.model.Admin;
import com.agrimarket.model.Buyer;
import com.agrimarket.model.CartItem;
import com.agrimarket.model.Farmer;
import java.util.LinkedHashMap;
import java.util.Map;

/** One browser's login state, kept in server memory and found by the AGRISESSION cookie. */
final class Session {

    enum Role { FARMER, BUYER, ADMIN }

    final String id;
    /** Hidden in every form; a POST without it is refused (stops cross-site form posts). */
    final String csrf;
    volatile long lastSeen = System.currentTimeMillis();

    Role role;
    Farmer farmer;
    Buyer buyer;
    Admin admin;

    /** The buyer's cart, by product id (kept in memory until the order is placed, like the desktop app). */
    final Map<Integer, CartItem> cart = new LinkedHashMap<>();

    /** One-time messages shown on the next page. */
    String flashOk;
    String flashError;

    /** Pre-fills the login form after a failed login or a new registration. */
    String loginRole = "farmer";
    String loginId = "";

    Session(String id, String csrf) {
        this.id = id;
        this.csrf = csrf;
    }

    boolean loggedIn() {
        return role != null;
    }

    String homePath() {
        if (role == null) {
            return "/";
        }
        return switch (role) {
            case FARMER -> "/farmer";
            case BUYER -> "/buyer";
            case ADMIN -> "/admin";
        };
    }

    void ok(String message) {
        flashOk = message;
    }

    void error(String message) {
        flashError = message;
    }
}
