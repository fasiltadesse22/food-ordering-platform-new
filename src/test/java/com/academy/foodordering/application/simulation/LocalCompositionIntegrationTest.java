package com.academy.foodordering.application.simulation;

import com.academy.foodordering.application.PlaceOrderCommand;
import com.academy.foodordering.application.PlaceOrderService;
import com.academy.foodordering.domain.OrderId;
import com.academy.foodordering.domain.PaymentAttemptLifecycle;
import com.academy.foodordering.domain.PreparationGuardProbe;
import com.academy.foodordering.domain.RestaurantDecisionLifecycle;
import com.academy.foodordering.infrastructure.DeterministicLocalPaymentSimulator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static com.academy.foodordering.application.simulation.LocalCompositionSimulation.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the actual Spring application plus the isolated in-memory teaching workflow. */
@SpringBootTest
class LocalCompositionIntegrationTest {
    @Autowired PlaceOrderService intake;
    @Autowired LocalCompositionSimulation scenarios;
    private static final TeachingPolicy EITHER = new TeachingPolicy(PaymentSequence.EITHER_FOR_EXPERIMENT,
        CancellationWindow.BEFORE_PREPARATION,true,true);

    private OrderId order() {
        var placed=intake.place(new PlaceOrderCommand(UUID.randomUUID(),"cedar-kitchen",
            List.of(new PlaceOrderCommand.LineRequest("lentil-bowl",1))));
        assertEquals("PENDING",placed.restaurantDecision());
        assertEquals("NOT_STARTED",placed.paymentStatus());
        return placed.orderIdentity();
    }
    private static LocalCompositionSimulation.Applied applied(Outcome outcome) {
        assertInstanceOf(Applied.class,outcome);
        return (Applied)outcome;
    }
    private static LocalCompositionSimulation.Rejected rejected(Outcome outcome,String code) {
        assertInstanceOf(Rejected.class,outcome);
        var x=(Rejected)outcome;
        assertEquals(code,x.code());
        return x;
    }
    private static Command merchant(Action a) { return Command.assumed(a,Actor.MERCHANT_OPERATOR); }
    private static Command kitchen(Action a) { return Command.assumed(a,Actor.KITCHEN_OPERATOR); }
    private static Command platform(Action a) { return Command.assumed(a,Actor.PLATFORM); }
    private static Command captured() { return Command.provider(Action.OBSERVE_CAPTURE,SimulatedPaymentPort.Preset.CONFIRMED); }
    private static Command lostCapture() { return Command.provider(Action.OBSERVE_CAPTURE,SimulatedPaymentPort.Preset.RESPONSE_LOST); }
    private void pay(OrderId id) {
        applied(scenarios.advance(id,Command.of(Action.SUBMIT_PAYMENT,Actor.CUSTOMER)));
        applied(scenarios.advance(id,captured()));
    }

    @Test void springApplicationActuallyComposesAcceptedButUnpaidWithStableIdentity() {
        var id=order();var policy=new TeachingPolicy(PaymentSequence.MERCHANT_FIRST,
            CancellationWindow.BEFORE_ACCEPTANCE,false,true);
        var first=scenarios.begin(id,policy);
        var m=applied(scenarios.advance(id,merchant(Action.MERCHANT_ACCEPT)));
        assertEquals(id,first.orderId());
        assertEquals(first.attemptId(),m.next().attemptId());
        assertEquals(RestaurantDecisionLifecycle.State.ACCEPTED,m.next().merchant());
        assertEquals(PaymentAttemptLifecycle.State.NOT_STARTED,m.next().payment());
        assertEquals(Journey.ACTIVE,m.next().journey());
        assertTrue(m.simulatedFact().label().startsWith("Simulated"));
        assertEquals("CLASSROOM_ASSUMPTION_NOT_EXTERNAL_FACT",m.simulatedFact().authorityLevel());
        assertEquals(1,m.next().history().size());
        assertEquals("PROCESS_LOCAL_CLASSROOM_ONLY_NO_EXTERNAL_MERCHANT_OR_PAYMENT_EFFECT",m.next().evidenceScope());
    }
    @Test void paidButMerchantRejectedPreservesOutstandingMoneyUntilSimulatedRefundConfirmed() {
        var id=order();scenarios.begin(id,EITHER);pay(id);
        var rejected=applied(scenarios.advance(id,merchant(Action.MERCHANT_REJECT)));
        assertTrue(rejected.next().hasUnresolvedFinancialPosition());
        assertEquals(PaymentAttemptLifecycle.State.CAPTURED,rejected.next().payment());
        var request=applied(scenarios.advance(id,platform(Action.REQUEST_REFUND)));
        assertEquals(PaymentAttemptLifecycle.State.REFUND_PENDING,request.next().payment());
        assertTrue(request.next().hasUnresolvedFinancialPosition());
        var lost=applied(scenarios.advance(id,Command.provider(Action.OBSERVE_REFUND,SimulatedPaymentPort.Preset.RESPONSE_LOST)));
        assertEquals(PaymentAttemptLifecycle.State.REFUND_PENDING,lost.next().payment());
        assertTrue(lost.next().hasUnresolvedFinancialPosition());
        var confirmed=applied(scenarios.advance(id,Command.provider(Action.OBSERVE_REFUND,SimulatedPaymentPort.Preset.CONFIRMED)));
        assertEquals(PaymentAttemptLifecycle.State.REFUNDED,confirmed.next().payment());
        assertFalse(confirmed.next().hasUnresolvedFinancialPosition());
        assertEquals(6,confirmed.next().history().size());
        assertTrue(confirmed.simulatedFact().label().contains("NotBankSettlement"));
    }
    @Test void paymentAfterCancellationRequestRejectedWithoutAnyMutationOrSuccessFact() {
        var id=order();scenarios.begin(id,EITHER);
        applied(scenarios.advance(id,Command.of(Action.REQUEST_CANCELLATION,Actor.CUSTOMER)));
        var before=scenarios.inspect(id);
        var denial=rejected(scenarios.advance(id,Command.of(Action.SUBMIT_PAYMENT,Actor.CUSTOMER)),"CANCEL_RACE");
        assertSame(before,denial.unchanged());
        assertSame(before,scenarios.inspect(id));
        assertEquals(1,scenarios.inspect(id).history().size());
        applied(scenarios.advance(id,platform(Action.APPROVE_CANCELLATION)));
        rejected(scenarios.advance(id,Command.of(Action.SUBMIT_PAYMENT,Actor.CUSTOMER)),"TERMINAL_JOURNEY");
    }
    @Test void racingCancelAndPreparationRequiresExplicitSourceAndCutoffAssumptions() {
        var id=order();scenarios.begin(id,EITHER);pay(id);
        applied(scenarios.advance(id,merchant(Action.MERCHANT_ACCEPT)));
        applied(scenarios.advance(id,kitchen(Action.START_PREPARATION)));
        applied(scenarios.advance(id,Command.of(Action.REQUEST_CANCELLATION,Actor.CUSTOMER)));
        var before=scenarios.inspect(id);
        rejected(scenarios.advance(id,platform(Action.APPROVE_CANCELLATION)),"POL_04_CUTOFF");
        assertSame(before,scenarios.inspect(id));
        applied(scenarios.advance(id,platform(Action.DENY_CANCELLATION)));
        assertEquals(Journey.ACTIVE,scenarios.inspect(id).journey());
        assertEquals(PreparationGuardProbe.Stage.PREPARING,scenarios.inspect(id).preparation());
    }
    @Test void unresolvedPoliciesFailClosedInsteadOfInventingCommercialRules() {
        var id=order();scenarios.begin(id,TeachingPolicy.unresolved());
        rejected(scenarios.advance(id,merchant(Action.MERCHANT_ACCEPT)),"POL_01_UNRESOLVED");
        rejected(scenarios.advance(id,Command.of(Action.SUBMIT_PAYMENT,Actor.CUSTOMER)),"POL_01_UNRESOLVED");
        applied(scenarios.advance(id,Command.of(Action.REQUEST_CANCELLATION,Actor.CUSTOMER)));
        rejected(scenarios.advance(id,platform(Action.APPROVE_CANCELLATION)),"POL_03_UNRESOLVED");
        assertEquals(Journey.CANCELLATION_REQUESTED,scenarios.inspect(id).journey());
        assertEquals(1,scenarios.inspect(id).history().size());
    }
    @Test void missingMerchantKitchenOrProviderEvidenceIsNeverPromotedToCommittedSourceFact() {
        var id=order();scenarios.begin(id,EITHER);
        rejected(scenarios.advance(id,Command.of(Action.MERCHANT_ACCEPT,Actor.MERCHANT_OPERATOR)),"MERCHANT_EVIDENCE");
        rejected(scenarios.advance(id,Command.assumed(Action.MERCHANT_ACCEPT,Actor.CUSTOMER)),"MERCHANT_EVIDENCE");
        rejected(scenarios.advance(id,kitchen(Action.START_PREPARATION)),"CAPTURE_BEFORE_PREP");
        applied(scenarios.advance(id,Command.of(Action.SUBMIT_PAYMENT,Actor.CUSTOMER)));
        rejected(scenarios.advance(id,new Command(Action.OBSERVE_CAPTURE,Actor.PAYMENT_SIMULATOR,false,
            SimulatedPaymentPort.Preset.CONFIRMED)),"SIMULATOR_EVIDENCE");
        assertEquals(PaymentAttemptLifecycle.State.SUBMITTED_UNKNOWN,scenarios.inspect(id).payment());
    }
    @Test void lostProviderResultStaysUnknownAndCannotBeConvertedToAutomaticRefund() {
        var id=order();scenarios.begin(id,EITHER);
        applied(scenarios.advance(id,Command.of(Action.SUBMIT_PAYMENT,Actor.CUSTOMER)));
        var lost=applied(scenarios.advance(id,lostCapture()));
        assertEquals(PaymentAttemptLifecycle.State.SUBMITTED_UNKNOWN,lost.next().payment());
        applied(scenarios.advance(id,merchant(Action.MERCHANT_REJECT)));
        rejected(scenarios.advance(id,platform(Action.REQUEST_REFUND)),"PAYMENT_STATE");
        assertFalse(scenarios.inspect(id).hasUnresolvedFinancialPosition());
        assertTrue(scenarios.inspect(id).history().stream().anyMatch(f->f.label().contains("Unknown")));
        // A future provider reconciliation with the SAME attempt identity may resolve this local model.
        applied(scenarios.advance(id,captured()));
        assertTrue(scenarios.inspect(id).hasUnresolvedFinancialPosition());
    }
    @Test void refundLiabilityMustBeExplicitlyAssumedEvenWhenCapturedAndRejected() {
        var id=order();var policy=new TeachingPolicy(PaymentSequence.EITHER_FOR_EXPERIMENT,
            CancellationWindow.BEFORE_PREPARATION,false,true);
        scenarios.begin(id,policy);pay(id);
        applied(scenarios.advance(id,merchant(Action.MERCHANT_REJECT)));
        var before=scenarios.inspect(id);
        rejected(scenarios.advance(id,platform(Action.REQUEST_REFUND)),"POL_05_UNRESOLVED");
        assertSame(before,scenarios.inspect(id));
    }
    @Test void normalTeachingJourneyRequiresIndependentCaptureMerchantAndPreparationEvidence() {
        var id=order();scenarios.begin(id,EITHER);
        rejected(scenarios.advance(id,platform(Action.COMPLETE_JOURNEY)),"COMPLETION_GUARD");
        rejected(scenarios.advance(id,kitchen(Action.START_PREPARATION)),"CAPTURE_BEFORE_PREP");
        pay(id);
        rejected(scenarios.advance(id,kitchen(Action.START_PREPARATION)),"PREPARATION_GUARD");
        applied(scenarios.advance(id,merchant(Action.MERCHANT_ACCEPT)));
        applied(scenarios.advance(id,kitchen(Action.START_PREPARATION)));
        rejected(scenarios.advance(id,platform(Action.COMPLETE_JOURNEY)),"COMPLETION_GUARD");
        applied(scenarios.advance(id,kitchen(Action.FINISH_PREPARATION)));
        var end=applied(scenarios.advance(id,platform(Action.COMPLETE_JOURNEY)));
        assertEquals(Journey.COMPLETED_SIMULATED,end.next().journey());
        assertEquals(PreparationGuardProbe.Stage.READY,end.next().preparation());
        assertTrue(end.simulatedFact().label().contains("NotDelivered"));
        rejected(scenarios.advance(id,merchant(Action.MERCHANT_REJECT)),"TERMINAL_JOURNEY");
    }
    @Test void policyChoiceChangesValidSequenceWithoutChangingUnderlyingOrderIntake() {
        var id=order();
        var policy=new TeachingPolicy(PaymentSequence.PAYMENT_FIRST,
            CancellationWindow.BEFORE_ACCEPTANCE,true,true);
        scenarios.begin(id,policy);
        rejected(scenarios.advance(id,merchant(Action.MERCHANT_ACCEPT)),"SEQUENCE");
        applied(scenarios.advance(id,Command.of(Action.SUBMIT_PAYMENT,Actor.CUSTOMER)));
        rejected(scenarios.advance(id,merchant(Action.MERCHANT_ACCEPT)),"SEQUENCE");
        applied(scenarios.advance(id,captured()));
        applied(scenarios.advance(id,merchant(Action.MERCHANT_ACCEPT)));
        assertEquals(PaymentAttemptLifecycle.State.CAPTURED,scenarios.inspect(id).payment());
        assertThrows(IllegalStateException.class,()->scenarios.begin(id,TeachingPolicy.unresolved()));
    }
    @Test void duplicateAndCompetingMerchantDecisionsHaveSingleProcessLocalWinner() throws Exception {
        var id=order();scenarios.begin(id,EITHER);
        var gate=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->{gate.await();return scenarios.advance(id,merchant(Action.MERCHANT_ACCEPT));});
            var b=executor.submit(()->{gate.await();return scenarios.advance(id,merchant(Action.MERCHANT_REJECT));});
            gate.countDown();
            var x=a.get(10,TimeUnit.SECONDS);
            var y=b.get(10,TimeUnit.SECONDS);
            assertEquals(1,(x instanceof Applied?1:0)+(y instanceof Applied?1:0));
            assertEquals(1,scenarios.inspect(id).history().size());
            assertNotEquals(RestaurantDecisionLifecycle.State.PENDING,scenarios.inspect(id).merchant());
            assertEquals(1,(x instanceof Rejected?1:0)+(y instanceof Rejected?1:0));
        }
    }
    @Test void starterRequiresActualRecordedOrderRatherThanFabricatingIntake() {
        assertThrows(IllegalArgumentException.class,
            ()->scenarios.begin(OrderId.newId(),EITHER));
    }
    @Test void paymentSimulationPortIsExplicitlyDeterministicAndNonExternal() {
        var port=new DeterministicLocalPaymentSimulator();
        for(var effect:SimulatedPaymentPort.Effect.values()){
            assertEquals(SimulatedPaymentPort.Observation.SIMULATED_CONFIRMED,
                port.observe(effect,SimulatedPaymentPort.Preset.CONFIRMED));
            assertEquals(SimulatedPaymentPort.Observation.SIMULATED_DECLINED,
                port.observe(effect,SimulatedPaymentPort.Preset.DECLINED));
            assertEquals(SimulatedPaymentPort.Observation.UNKNOWN,
                port.observe(effect,SimulatedPaymentPort.Preset.RESPONSE_LOST));
        }
    }
}
