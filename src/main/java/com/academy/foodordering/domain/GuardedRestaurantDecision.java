package com.academy.foodordering.domain;

import java.util.List;
import java.util.Objects;

/**
 * Pure guarded EVALUATION of a hypothetical merchant decision.
 *
 * This type has no authenticated merchant integration, store, commit or publication.
 * Every positive result is an illustrative CANDIDATE, NOT merchant acceptance.
 * POL-02 remains unresolved; ONLY an explicitly chosen EDUCATIONAL_ONE_SHOT
 * assumption permits the isolated model to propose a transition.
 */
public final class GuardedRestaurantDecision {
    private GuardedRestaurantDecision() {}

    public enum ActorRole { CUSTOMER, PLATFORM, MERCHANT_OPERATOR }
    public enum TeachingPolicy { UNRESOLVED, EDUCATIONAL_ONE_SHOT }

    /**
     * Caller's input is not verified credential/proof. Booleans are deliberately
     * explicit classroom assumptions, not authenticators or a production policy.
     * Expected state is a LOCAL stale-read illustration, not an atomic version.
     */
    public record Context(
            OrderId requestedOrderId,
            RestaurantId requestedRestaurantId,
            RestaurantId actorMerchantId,
            ActorRole actor,
            boolean technicalAuthenticationAssumed,
            boolean merchantAuthorizationAssumed,
            boolean basketTermsMatchAssumed,
            boolean merchantEvidenceAssumed,
            RestaurantDecisionLifecycle.State expectedState,
            TeachingPolicy teachingPolicy) {
        public Context {
            Objects.requireNonNull(requestedOrderId);
            Objects.requireNonNull(requestedRestaurantId);
            Objects.requireNonNull(actorMerchantId);
            Objects.requireNonNull(actor);
            Objects.requireNonNull(expectedState);
            Objects.requireNonNull(teachingPolicy);
        }
    }

    public record Evaluation(
            RestaurantDecisionLifecycle before,
            RestaurantDecisionLifecycle next,
            RestaurantDecisionLifecycle.Action action,
            boolean accepted,
            List<GuardCheck> guards,
            String reason,
            String illustrativeCandidateFact) {
        public Evaluation {
            Objects.requireNonNull(before);
            Objects.requireNonNull(next);
            Objects.requireNonNull(action);
            Objects.requireNonNull(guards);
            Objects.requireNonNull(reason);
            guards = List.copyOf(guards);
            if (!before.orderId().equals(next.orderId())
                    || !before.restaurantId().equals(next.restaurantId()))
                throw new IllegalArgumentException("A transition cannot change decision identity");
            if (!accepted && (next != before || illustrativeCandidateFact != null))
                throw new IllegalArgumentException("Rejected decisions preserve the same state object and emit no fact");
            if (accepted && (guards.stream().anyMatch(g -> !g.passed())
                    || illustrativeCandidateFact == null || illustrativeCandidateFact.isBlank()))
                throw new IllegalArgumentException("Accepted teaching decisions need all guards and an illustrative label");
        }
    }

    public static Evaluation evaluate(RestaurantDecisionLifecycle current,
                                       RestaurantDecisionLifecycle.Action action,
                                       Context context) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(context, "context");

        List<GuardCheck> checks = List.of(
            check("IDENTITY_ORDER", current.orderId().equals(context.requestedOrderId()),
                    "Request must reference the identical OrderId"),
            check("IDENTITY_RESTAURANT", current.restaurantId().equals(context.requestedRestaurantId()),
                    "Request must reference the decision's RestaurantId"),
            check("ACTOR_ROLE", context.actor() == ActorRole.MERCHANT_OPERATOR,
                    "Only an illustrative merchant operator role may propose merchant decisions"),
            check("ACTOR_SCOPE", current.restaurantId().equals(context.actorMerchantId()),
                    "Actor's asserted merchant identity must match the decision owner"),
            check("AUTHENTICATED_ASSUMED", context.technicalAuthenticationAssumed(),
                    "An identity claim alone is not technical authentication"),
            check("BUSINESS_AUTHORITY_ASSUMED", context.merchantAuthorizationAssumed(),
                    "Authentication does not grant merchant decision authority"),
            check("EXPECTED_STATE", current.state() == context.expectedState(),
                    "Locally observed expected state must match; this is not compare-and-swap"),
            check("STATE_PENDING", current.state() == RestaurantDecisionLifecycle.State.PENDING,
                    "The illustrative one-shot decision can be made only from PENDING"),
            check("BASKET_TERMS_ASSUMED", context.basketTermsMatchAssumed(),
                    "An acceptance/rejection must concern the identical offered basket/price terms"),
            check("MERCHANT_EVIDENCE_ASSUMED", context.merchantEvidenceAssumed(),
                    "Real merchant evidence is unavailable; a classroom assumption is required"),
            check("POL_02_EDUCATIONAL_ONLY", context.teachingPolicy() == TeachingPolicy.EDUCATIONAL_ONE_SHOT,
                    "POL-02 is UNRESOLVED; only explicit classroom policy permits simulation")
        );

        var failed = checks.stream().filter(g -> !g.passed()).findFirst();
        if (failed.isPresent())
            return new Evaluation(current, current, action, false, checks,
                    failed.get().code() + ": " + failed.get().explanation(), null);

        var raw = current.decide(action);
        if (!raw.accepted()) // Defensive: never convert underlying rejection into success.
            return new Evaluation(current, current, action, false, checks, raw.reason(), null);

        return new Evaluation(current, raw.next(), action, true, checks,
                "Eligible for an ILLUSTRATIVE one-shot merchant transition only; NO COMMIT",
                "Candidate" + raw.illustrativeFact());
    }

    private static GuardCheck check(String code, boolean passed, String explanation) {
        return new GuardCheck(code, passed, explanation);
    }
}
