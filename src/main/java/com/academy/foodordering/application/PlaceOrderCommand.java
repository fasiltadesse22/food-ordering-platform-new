package com.academy.foodordering.application;

import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(UUID customerId, String restaurantId,
                                List<LineRequest> items) {
    public record LineRequest(String menuItemCode, int quantity) {}
}
