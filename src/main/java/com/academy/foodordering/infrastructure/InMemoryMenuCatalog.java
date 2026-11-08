package com.academy.foodordering.infrastructure;

import com.academy.foodordering.application.MenuCatalog;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Static, synthetic merchant fixture; NOT inventory management or live pricing. */
@Component
public final class InMemoryMenuCatalog implements MenuCatalog {
    private final RestaurantMenu demo = new RestaurantMenu(
            "cedar-kitchen", "Cedar Kitchen (fictional demo)",
            List.of(new MenuItem("lentil-bowl", "Lentil Bowl", 1250, true),
                    new MenuItem("injera-combo", "Injera Combo", 1800, true),
                    new MenuItem("seasonal-special", "Seasonal Special", 2100, false)));

    @Override
    public Optional<RestaurantMenu> findRestaurant(String code) {
        return demo.code().equals(code) ? Optional.of(demo) : Optional.empty();
    }
}
