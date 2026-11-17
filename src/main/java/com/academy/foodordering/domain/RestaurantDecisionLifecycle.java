package com.academy.foodordering.domain;

import java.util.Objects;

/**
 * A pure LOCAL model of a merchant decision; decisions are hypothetical, not
 * authoritative external merchant responses. Caller must NOT project the
 * returned fact as a real restaurant event.
 */
public record RestaurantDecisionLifecycle(OrderId orderId, RestaurantId restaurantId, State state) {
    public enum State { PENDING, ACCEPTED, REJECTED }
    public enum Action { ACCEPT, REJECT }
    public record Decision(RestaurantDecisionLifecycle next, boolean accepted, String reason, String illustrativeFact) {
        public Decision { Objects.requireNonNull(next); Objects.requireNonNull(reason); }
    }
    public RestaurantDecisionLifecycle {
        Objects.requireNonNull(orderId); Objects.requireNonNull(restaurantId); Objects.requireNonNull(state);
    }
    public static RestaurantDecisionLifecycle pending(OrderId id, RestaurantId merchant) {
        return new RestaurantDecisionLifecycle(id, merchant, State.PENDING);
    }
    public Decision decide(Action action) {
        Objects.requireNonNull(action);
        if (state != State.PENDING)
            return new Decision(this, false, "Decision is terminal in this teaching model; no revision policy approved", null);
        State nextState = action == Action.ACCEPT ? State.ACCEPTED : State.REJECTED;
        String fact = action == Action.ACCEPT ? "IllustrativeRestaurantAccepted" : "IllustrativeRestaurantRejected";
        return new Decision(new RestaurantDecisionLifecycle(orderId, restaurantId, nextState), true,
                "Local pure-model transition; NOT a real merchant decision", fact);
    }
}
