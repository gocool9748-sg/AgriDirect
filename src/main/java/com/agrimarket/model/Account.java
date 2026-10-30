package com.agrimarket.model;

/** A user together with the stored password hash, used only during login. */
public record Account<T>(T user, String passwordHash) {
}
