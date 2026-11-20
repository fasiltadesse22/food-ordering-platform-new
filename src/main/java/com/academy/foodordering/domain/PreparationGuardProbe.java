package com.academy.foodordering.domain;

import java.util.List;
import java.util.Objects;

/**
 * A hypothetical guard PROBE, not a new Fulfillment domain lifecycle.
 * Demonstrates two required illegal paths without introducing kitchen storage
 * or approving POL-01/POL-04. No preparation or completion actually happens.
 */
public final class PreparationGuardProbe {
    private PreparationGuardProbe() {}
    public enum Stage { NOT_STARTED, PREPARING, READY }
    public enum Command { START_PREPARING, COMPLETE_PREPARATION }
    public enum Actor { CUSTOMER, PLATFORM, KITCHEN_OPERATOR }

    public record Input(OrderId orderId, RestaurantDecisionLifecycle merchant,
                        Stage stage, Command command, Actor actor,
                        boolean merchantAcceptanceEvidenceAssumed,
                        boolean educationalPreparationPolicy) {
        public Input {
            Objects.requireNonNull(orderId);
            Objects.requireNonNull(merchant);
            Objects.requireNonNull(stage);
            Objects.requireNonNull(command);
            Objects.requireNonNull(actor);
        }
    }
    public record Result(Stage before, Stage next, boolean eligible,
                         List<GuardCheck> guards, String reason, String illustrativeCandidateFact) {
        public Result {
            Objects.requireNonNull(before);
            Objects.requireNonNull(next);
            Objects.requireNonNull(guards);
            Objects.requireNonNull(reason);
            guards = List.copyOf(guards);
            if (!eligible && (before != next || illustrativeCandidateFact != null))
                throw new IllegalArgumentException("A rejected probe must preserve phase and emit no fact");
        }
    }
    public static Result inspect(Input input) {
        Objects.requireNonNull(input);
        var expected = input.command() == Command.START_PREPARING ? Stage.NOT_STARTED : Stage.PREPARING;
        List<GuardCheck> checks = List.of(
            new GuardCheck("ORDER_MATCH", input.orderId().equals(input.merchant().orderId()),
                    "Kitchen milestone must refer to this merchant decision's OrderId"),
            new GuardCheck("KITCHEN_AUTHORITY", input.actor() == Actor.KITCHEN_OPERATOR,
                    "Only an illustrative kitchen operator may propose work progress"),
            new GuardCheck("MERCHANT_ACCEPTED", input.merchant().state() == RestaurantDecisionLifecycle.State.ACCEPTED,
                    "Restaurant must have accepted before preparation may proceed in this illustrative policy"),
            new GuardCheck("MERCHANT_EVIDENCE_ASSUMED", input.merchantAcceptanceEvidenceAssumed(),
                    "A modelled ACCEPTED state is not proof of real merchant acceptance"),
            new GuardCheck("CURRENT_STAGE", input.stage() == expected,
                    "Required current preparation stage is " + expected),
            new GuardCheck("POL_04_EDUCATIONAL_ONLY", input.educationalPreparationPolicy(),
                    "Preparation timing and compensation POL-04 are unresolved")
        );
        var failed = checks.stream().filter(c -> !c.passed()).findFirst();
        if (failed.isPresent())
            return new Result(input.stage(), input.stage(), false, checks,
                    failed.get().code() + ": " + failed.get().explanation(), null);
        var next = input.command() == Command.START_PREPARING ? Stage.PREPARING : Stage.READY;
        return new Result(input.stage(), next, true, checks,
                "Only an eligible classroom probe; NO kitchen command, state write or success fact",
                input.command() == Command.START_PREPARING
                    ? "CandidateIllustrativePreparationStarted" : "CandidateIllustrativePreparationCompleted");
    }
}
