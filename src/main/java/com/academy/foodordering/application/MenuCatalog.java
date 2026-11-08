package com.academy.foodordering.application;

import java.util.List;
import java.util.Optional;

/** Teaching catalog snapshot, not authoritative live merchant inventory or price quotation. */
public interface MenuCatalog {
    Optional<RestaurantMenu> findRestaurant(String code);

    record RestaurantMenu(String code, String name, List<MenuItem> items) {
        public RestaurantMenu { items = List.copyOf(items); }
    }
    record MenuItem(String code, String name, long priceCents, boolean available) {}
}
