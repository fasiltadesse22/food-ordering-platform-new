package com.academy.foodordering.domain;

import com.academy.foodordering.application.PreviewRestaurantDecisionService;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** Pure transition and guard tests. These do not attest real merchant or kitchen effects. */
class GuardedTransitionsTest {
    private static final OrderId O = new OrderId(UUID.fromString("9719ab10-d7fa-444f-9592-3a9a4c42c253"));
    private static final RestaurantId R = new RestaurantId("cedar-kitchen");
    private static final RestaurantId OTHER = new RestaurantId("other-kitchen");

    private static RestaurantDecisionLifecycle pending() {
        return RestaurantDecisionLifecycle.pending(O,R);
    }
    private static GuardedRestaurantDecision.Context valid() {
        return new GuardedRestaurantDecision.Context(O,R,R,
            GuardedRestaurantDecision.ActorRole.MERCHANT_OPERATOR,
            true,true,true,true,RestaurantDecisionLifecycle.State.PENDING,
            GuardedRestaurantDecision.TeachingPolicy.EDUCATIONAL_ONE_SHOT);
    }
    private enum InvalidGuard {
        WRONG_ORDER, WRONG_RESTAURANT, CUSTOMER_ACTOR, PLATFORM_ACTOR,
        WRONG_ACTOR_MERCHANT, UNAUTHENTICATED, NO_BUSINESS_AUTHORITY,
        STALE_STATE, BASKET_MISMATCH, NO_ASSUMED_SOURCE_EVIDENCE, POLICY_UNRESOLVED
    }
    private static GuardedRestaurantDecision.Context invalid(InvalidGuard flag) {
        var v=valid();
        return new GuardedRestaurantDecision.Context(
            flag==InvalidGuard.WRONG_ORDER?OrderId.newId():v.requestedOrderId(),
            flag==InvalidGuard.WRONG_RESTAURANT?OTHER:v.requestedRestaurantId(),
            flag==InvalidGuard.WRONG_ACTOR_MERCHANT?OTHER:v.actorMerchantId(),
            flag==InvalidGuard.CUSTOMER_ACTOR?GuardedRestaurantDecision.ActorRole.CUSTOMER:
                flag==InvalidGuard.PLATFORM_ACTOR?GuardedRestaurantDecision.ActorRole.PLATFORM:v.actor(),
            flag!=InvalidGuard.UNAUTHENTICATED,
            flag!=InvalidGuard.NO_BUSINESS_AUTHORITY,
            flag!=InvalidGuard.BASKET_MISMATCH,
            flag!=InvalidGuard.NO_ASSUMED_SOURCE_EVIDENCE,
            flag==InvalidGuard.STALE_STATE?RestaurantDecisionLifecycle.State.ACCEPTED:v.expectedState(),
            flag==InvalidGuard.POLICY_UNRESOLVED?
                GuardedRestaurantDecision.TeachingPolicy.UNRESOLVED:v.teachingPolicy());
    }

    @ParameterizedTest
    @EnumSource(RestaurantDecisionLifecycle.Action.class)
    void eligibleCandidateIsPureAndCarriesAllCheckedPostconditions(RestaurantDecisionLifecycle.Action action) {
        var before=pending();
        var evaluation=GuardedRestaurantDecision.evaluate(before,action,valid());
        assertTrue(evaluation.accepted());
        assertSame(before,evaluation.before());
        assertNotSame(before,evaluation.next());
        assertEquals(O,evaluation.next().orderId());
        assertEquals(R,evaluation.next().restaurantId());
        assertEquals(RestaurantDecisionLifecycle.State.PENDING,before.state());
        assertEquals(action==RestaurantDecisionLifecycle.Action.ACCEPT?
            RestaurantDecisionLifecycle.State.ACCEPTED:RestaurantDecisionLifecycle.State.REJECTED,
            evaluation.next().state());
        assertTrue(evaluation.illustrativeCandidateFact().startsWith("CandidateIllustrative"));
        assertTrue(evaluation.guards().stream().allMatch(GuardCheck::passed));
        assertEquals(11,evaluation.guards().size());
        assertTrue(new PreviewRestaurantDecisionService().evaluate(before,action,valid())
            instanceof PreviewRestaurantDecisionService.Eligible);
    }

    @ParameterizedTest
    @EnumSource(InvalidGuard.class)
    void eachFailedGuardPreservesOriginalAndSuppressesCandidateFact(InvalidGuard failure) {
        var before=pending();
        var result=GuardedRestaurantDecision.evaluate(before,
                RestaurantDecisionLifecycle.Action.ACCEPT,invalid(failure));
        assertFalse(result.accepted(),failure.name());
        assertSame(before,result.next());
        assertEquals(RestaurantDecisionLifecycle.State.PENDING,before.state());
        assertNull(result.illustrativeCandidateFact());
        assertTrue(result.guards().stream().anyMatch(c->!c.passed()),failure.name());
        assertTrue(new PreviewRestaurantDecisionService().evaluate(before,
            RestaurantDecisionLifecycle.Action.ACCEPT,invalid(failure))
            instanceof PreviewRestaurantDecisionService.Rejected);
    }

    @ParameterizedTest
    @EnumSource(RestaurantDecisionLifecycle.State.class)
    void onlyPendingStateAllowsDecision(RestaurantDecisionLifecycle.State state) {
        var before=new RestaurantDecisionLifecycle(O,R,state);
        var assumed=valid();
        var context=new GuardedRestaurantDecision.Context(O,R,R,assumed.actor(),
            true,true,true,true,state,assumed.teachingPolicy());
        var result=GuardedRestaurantDecision.evaluate(before,
            RestaurantDecisionLifecycle.Action.ACCEPT,context);
        assertEquals(state==RestaurantDecisionLifecycle.State.PENDING,result.accepted());
        if(state!=RestaurantDecisionLifecycle.State.PENDING) {
            assertSame(before,result.next());
            assertNull(result.illustrativeCandidateFact());
            assertTrue(result.reason().contains("STATE_PENDING"));
        }
    }

    static Stream<Arguments> forbiddenPreparation() {
        return Stream.of(
            arguments(PreparationGuardProbe.Command.START_PREPARING,PreparationGuardProbe.Stage.NOT_STARTED,
                RestaurantDecisionLifecycle.State.PENDING,"MERCHANT_ACCEPTED"),
            arguments(PreparationGuardProbe.Command.START_PREPARING,PreparationGuardProbe.Stage.NOT_STARTED,
                RestaurantDecisionLifecycle.State.REJECTED,"MERCHANT_ACCEPTED"),
            arguments(PreparationGuardProbe.Command.COMPLETE_PREPARATION,PreparationGuardProbe.Stage.NOT_STARTED,
                RestaurantDecisionLifecycle.State.ACCEPTED,"CURRENT_STAGE"),
            arguments(PreparationGuardProbe.Command.COMPLETE_PREPARATION,PreparationGuardProbe.Stage.READY,
                RestaurantDecisionLifecycle.State.ACCEPTED,"CURRENT_STAGE")
        );
    }
    @ParameterizedTest
    @MethodSource("forbiddenPreparation")
    void preparingBeforeAcceptanceAndCompletionBeforePreparationRemainNoOps(
            PreparationGuardProbe.Command action,PreparationGuardProbe.Stage stage,
            RestaurantDecisionLifecycle.State merchant,String failingCode) {
        var decision=new RestaurantDecisionLifecycle(O,R,merchant);
        var result=PreparationGuardProbe.inspect(new PreparationGuardProbe.Input(
            O,decision,stage,action,PreparationGuardProbe.Actor.KITCHEN_OPERATOR,true,true));
        assertFalse(result.eligible());
        assertEquals(stage,result.next());
        assertNull(result.illustrativeCandidateFact());
        assertTrue(result.reason().contains(failingCode));
    }

    @ParameterizedTest
    @EnumSource(PreparationGuardProbe.Command.class)
    void validClassroomPreparationAssumptionIsExplicitlyNonEffectful(PreparationGuardProbe.Command action) {
        var stage=action==PreparationGuardProbe.Command.START_PREPARING?
            PreparationGuardProbe.Stage.NOT_STARTED:PreparationGuardProbe.Stage.PREPARING;
        var decision=new RestaurantDecisionLifecycle(O,R,RestaurantDecisionLifecycle.State.ACCEPTED);
        var result=PreparationGuardProbe.inspect(new PreparationGuardProbe.Input(
            O,decision,stage,action,PreparationGuardProbe.Actor.KITCHEN_OPERATOR,true,true));
        assertTrue(result.eligible());
        assertTrue(result.guards().stream().allMatch(GuardCheck::passed));
        assertEquals(stage,result.before());
        assertNotEquals(stage,result.next());
        assertTrue(result.illustrativeCandidateFact().startsWith("CandidateIllustrative"));
        assertTrue(result.reason().contains("NO kitchen command"));
    }

    @Test void missingPolicyOrActorAuthorityBlocksPreparationEvenAfterAcceptance() {
        var decision=new RestaurantDecisionLifecycle(O,R,RestaurantDecisionLifecycle.State.ACCEPTED);
        for(var actor:PreparationGuardProbe.Actor.values()) {
            var result=PreparationGuardProbe.inspect(new PreparationGuardProbe.Input(O,decision,
                PreparationGuardProbe.Stage.NOT_STARTED,PreparationGuardProbe.Command.START_PREPARING,
                actor,true,false));
            assertFalse(result.eligible());
            assertEquals(PreparationGuardProbe.Stage.NOT_STARTED,result.next());
            assertNull(result.illustrativeCandidateFact());
        }
    }
    @Test void defensiveOutcomeContractsRejectFabricatedSuccessOnRejection() {
        var before=pending();
        assertThrows(IllegalArgumentException.class,()->new GuardedRestaurantDecision.Evaluation(
            before,before,RestaurantDecisionLifecycle.Action.ACCEPT,false,
            java.util.List.of(),"Rejected","CandidateFake"));
        assertThrows(IllegalArgumentException.class,()->new GuardedRestaurantDecision.Evaluation(
            before,before,RestaurantDecisionLifecycle.Action.ACCEPT,true,
            java.util.List.of(new GuardCheck("POLICY",false,"Not approved")),"Accepted","CandidateFake"));
    }

    @Test void localPureGuardCannotSupplyCommitEvidenceOrResolveStaleWrite() {
        var before=pending();
        var result=GuardedRestaurantDecision.evaluate(before,
                RestaurantDecisionLifecycle.Action.ACCEPT,valid());
        assertTrue(result.accepted());
        // The recorded input remains PENDING: no repository write or real merchant event.
        assertEquals(RestaurantDecisionLifecycle.State.PENDING,before.state());
        assertTrue(result.reason().contains("NO COMMIT"));
        var stale=new RestaurantDecisionLifecycle(O,R,RestaurantDecisionLifecycle.State.REJECTED);
        var rejected=GuardedRestaurantDecision.evaluate(stale,
                RestaurantDecisionLifecycle.Action.ACCEPT,valid());
        assertFalse(rejected.accepted());
        assertSame(stale,rejected.next());
        assertNull(rejected.illustrativeCandidateFact());
    }
}
