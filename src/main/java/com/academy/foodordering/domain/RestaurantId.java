package com.academy.foodordering.domain;

import java.util.Objects;

/**
 * Catalog identity within this teaching application's namespace.
 * The display name and any future external merchant ID are NOT this identity.
 */
public record RestaurantId(String value) {
    public RestaurantId {
        Objects.requireNonNull(value, "RestaurantId value");
        if (value.isBlank()) throw new IllegalArgumentException("RestaurantId must not be blank");
    }
    @Override public String toString() { return value; }
}
