package com.academy.foodordering.domain;

import java.util.Objects;

/**
 * Pure model of PLATFORM-owned order-intake lifecycle. NOT an integration with
 * restaurant, kitchen or provider. Full cancellation guards are deliberately
 * deferred until business policy and cross-lifecycle concurrency are defined.
 */
public record OrderLifecycle(OrderId orderId, State state) {
    public enum State { NEW, REQUESTED }
    public enum Action { RECORD_INTAKE, CLOSE_UNFULFILLABLE }
    public record Decision(OrderLifecycle next, boolean accepted, String reason, String fact) {
        public Decision {
            Objects.requireNonNull(next); Objects.requireNonNull(reason);
        }
    }
    public OrderLifecycle {
        Objects.requireNonNull(orderId); Objects.requireNonNull(state);
    }
    public static OrderLifecycle newRequest(OrderId id) { return new OrderLifecycle(id, State.NEW); }
    public static OrderLifecycle recorded(OrderId id) { return new OrderLifecycle(id, State.REQUESTED); }

    public Decision decide(Action action) {
        Objects.requireNonNull(action);
        // Intake has already occurred before this lifecycle instance exists.
        // Policy-dependent closure cannot be approved just because a command arrived.
        return switch(action) {
            case RECORD_INTAKE -> state == State.NEW
                ? new Decision(recorded(orderId), true, "Pure intake transition; persistence is a separate effect", "IllustrativeOrderIntakeRecorded")
                : new Decision(this, false, "Intake already recorded for this OrderId", null);
            case CLOSE_UNFULFILLABLE -> new Decision(this, false,
                "Requires authoritative merchant rejection and approved closure policy (POL-02)", null);
        };
    }
}
