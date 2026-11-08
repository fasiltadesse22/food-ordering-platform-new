package com.academy.foodordering.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {
    @Autowired MockMvc mvc;
    private static final String VALID = """
      {"customerId":"6f1595f8-02bc-4484-9800-810a98b611ee",
       "restaurantId":"cedar-kitchen",
       "items":[{"menuItemCode":"lentil-bowl","quantity":2}]}
      """;

    @Test void acceptedIntakeIs201ButNotMerchantAcceptedOrPaid() throws Exception {
        String location = mvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON).content(VALID).header("X-Request-ID","example-123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Request-ID","example-123"))
                .andExpect(header().string("Location",startsWith("/api/orders/")))
                .andExpect(jsonPath("$.intakeStatus").value("REQUESTED_AWAITING_RESTAURANT"))
                .andExpect(jsonPath("$.restaurantDecision").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_STARTED"))
                .andExpect(jsonPath("$.quotedTotalCents").value(2500))
                .andExpect(jsonPath("$.meaning").value(containsString("restaurant has not accepted")))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("NOT_STARTED"));
    }

    @Test void invalidInputIs400() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("\"quantity\":2", "\"quantity\":0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test void unavailableMenuItemIs422() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("lentil-bowl", "seasonal-special")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ITEM_UNAVAILABLE"));
    }

    @Test void missingRestaurantIs422AndOrderReadMissingIs404() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("cedar-kitchen", "unknown-kitchen")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RESTAURANT_NOT_FOUND"));
        mvc.perform(get("/api/orders/01234567-89ab-4cde-8f00-112233445566"))
                .andExpect(status().isNotFound());
    }

    @Test void exposesOnlyFixtureMenuNotLiveMerchantInventory() throws Exception {
        mvc.perform(get("/api/restaurants/cedar-kitchen/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(containsString("fictional demo")))
                .andExpect(jsonPath("$.items",hasSize(3)));
        mvc.perform(get("/api/restaurants/unknown/menu")).andExpect(status().isNotFound());
    }
}
