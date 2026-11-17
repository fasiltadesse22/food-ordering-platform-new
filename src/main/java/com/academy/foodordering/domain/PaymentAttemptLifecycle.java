package com.academy.foodordering.domain;

import java.util.Objects;

/**
 * Provider-like outcomes are modeled for education only. No capture, refund,
 * bank account movement, provider callback, settlement, or external authority
 * is implemented by this class.
 */
public record PaymentAttemptLifecycle(PaymentAttemptId attemptId, OrderId orderId, State state) {
    public enum State { NOT_STARTED, SUBMITTED_UNKNOWN, AUTHORIZED, DECLINED, CAPTURED, REFUND_PENDING, REFUNDED }
    public enum Action { SUBMIT, CONFIRM_AUTHORIZED, CONFIRM_DECLINED, CONFIRM_CAPTURED, REQUEST_REFUND, CONFIRM_REFUNDED }
    public record Decision(PaymentAttemptLifecycle next, boolean accepted, String reason, String illustrativeFact) {
        public Decision { Objects.requireNonNull(next); Objects.requireNonNull(reason); }
    }
    public PaymentAttemptLifecycle {
        Objects.requireNonNull(attemptId); Objects.requireNonNull(orderId); Objects.requireNonNull(state);
    }
    public static PaymentAttemptLifecycle newAttempt(PaymentAttemptId attemptId, OrderId orderId) {
        return new PaymentAttemptLifecycle(attemptId, orderId, State.NOT_STARTED);
    }
    public Decision decide(Action action) {
        Objects.requireNonNull(action);
        State next = switch(state) {
            case NOT_STARTED -> action == Action.SUBMIT ? State.SUBMITTED_UNKNOWN : null;
            case SUBMITTED_UNKNOWN -> switch(action) {
                case CONFIRM_AUTHORIZED -> State.AUTHORIZED;
                case CONFIRM_DECLINED -> State.DECLINED;
                case CONFIRM_CAPTURED -> State.CAPTURED;
                default -> null;
            };
            case AUTHORIZED -> action == Action.CONFIRM_CAPTURED ? State.CAPTURED : null;
            case CAPTURED -> action == Action.REQUEST_REFUND ? State.REFUND_PENDING : null;
            case REFUND_PENDING -> action == Action.CONFIRM_REFUNDED ? State.REFUNDED : null;
            case DECLINED, REFUNDED -> null;
        };
        if (next == null) return new Decision(this, false,
            "Illegal local teaching-model transition; outcome unchanged and no fact emitted", null);
        return new Decision(new PaymentAttemptLifecycle(attemptId,orderId,next), true,
            "Hypothetical evidence-assumption transition only; no provider was contacted",
            "IllustrativePaymentAttempt"+next.name());
    }
}
