package com.academy.foodordering.application;

import com.academy.foodordering.domain.GuardedRestaurantDecision;
import com.academy.foodordering.domain.RestaurantDecisionLifecycle;
import java.util.Objects;

/**
 * Side-effect-free application boundary for GUARD EVALUATION ONLY.
 * It deliberately has no repository, Spring endpoint, merchant gateway,
 * event publisher, or real authority to accept/reject orders.
 */
public final class PreviewRestaurantDecisionService {
    public sealed interface Outcome permits Eligible, Rejected {}
    public record Eligible(GuardedRestaurantDecision.Evaluation evaluation) implements Outcome {
        public Eligible {
            Objects.requireNonNull(evaluation);
            if (!evaluation.accepted()) throw new IllegalArgumentException("Expected eligible candidate");
        }
    }
    public record Rejected(GuardedRestaurantDecision.Evaluation evaluation) implements Outcome {
        public Rejected {
            Objects.requireNonNull(evaluation);
            if (evaluation.accepted()) throw new IllegalArgumentException("Expected rejection");
        }
    }
    public Outcome evaluate(RestaurantDecisionLifecycle current,
                            RestaurantDecisionLifecycle.Action action,
                            GuardedRestaurantDecision.Context context) {
        var result = GuardedRestaurantDecision.evaluate(current, action, context);
        return result.accepted() ? new Eligible(result) : new Rejected(result);
    }
}
