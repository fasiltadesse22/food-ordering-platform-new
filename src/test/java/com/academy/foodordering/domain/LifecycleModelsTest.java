package com.academy.foodordering.domain;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Table-based tests exercising pure models, not merchant or provider execution. */
class LifecycleModelsTest {
    private final OrderId order = new OrderId(UUID.fromString("8b4ef6e5-ed20-4230-9f83-ae3c4e458715"));
    private final RestaurantId restaurant = new RestaurantId("cedar-kitchen");
    private final PaymentAttemptId attempt = new PaymentAttemptId(UUID.fromString("27ea2c7b-e3e7-4c59-b633-8b2e7334fe81"));

    @Test void merchantAlternativesAndTerminalRejectionAreExplicit() {
        for(var action:RestaurantDecisionLifecycle.Action.values()) {
            var start = RestaurantDecisionLifecycle.pending(order,restaurant);
            var result = start.decide(action);
            assertTrue(result.accepted());
            assertEquals(order, result.next().orderId());
            assertEquals(restaurant, result.next().restaurantId());
            assertNotNull(result.illustrativeFact());
            assertEquals(action == RestaurantDecisionLifecycle.Action.ACCEPT
                    ? RestaurantDecisionLifecycle.State.ACCEPTED : RestaurantDecisionLifecycle.State.REJECTED,
                    result.next().state());
            for(var again:RestaurantDecisionLifecycle.Action.values()) {
                var illegal = result.next().decide(again);
                assertFalse(illegal.accepted());
                assertSame(result.next(), illegal.next());
                assertNull(illegal.illustrativeFact());
            }
        }
    }

    @Test void paymentTransitionTableAcceptsOnlyExplicitPathsAndDoesNotChangeInput() {
        record Rule(PaymentAttemptLifecycle.State from, PaymentAttemptLifecycle.Action action,
                    PaymentAttemptLifecycle.State to) {}
        var valid = java.util.List.of(
            new Rule(PaymentAttemptLifecycle.State.NOT_STARTED, PaymentAttemptLifecycle.Action.SUBMIT, PaymentAttemptLifecycle.State.SUBMITTED_UNKNOWN),
            new Rule(PaymentAttemptLifecycle.State.SUBMITTED_UNKNOWN, PaymentAttemptLifecycle.Action.CONFIRM_AUTHORIZED, PaymentAttemptLifecycle.State.AUTHORIZED),
            new Rule(PaymentAttemptLifecycle.State.SUBMITTED_UNKNOWN, PaymentAttemptLifecycle.Action.CONFIRM_DECLINED, PaymentAttemptLifecycle.State.DECLINED),
            new Rule(PaymentAttemptLifecycle.State.SUBMITTED_UNKNOWN, PaymentAttemptLifecycle.Action.CONFIRM_CAPTURED, PaymentAttemptLifecycle.State.CAPTURED),
            new Rule(PaymentAttemptLifecycle.State.AUTHORIZED, PaymentAttemptLifecycle.Action.CONFIRM_CAPTURED, PaymentAttemptLifecycle.State.CAPTURED),
            new Rule(PaymentAttemptLifecycle.State.CAPTURED, PaymentAttemptLifecycle.Action.REQUEST_REFUND, PaymentAttemptLifecycle.State.REFUND_PENDING),
            new Rule(PaymentAttemptLifecycle.State.REFUND_PENDING, PaymentAttemptLifecycle.Action.CONFIRM_REFUNDED, PaymentAttemptLifecycle.State.REFUNDED)
        );
        int accepted=0, rejected=0;
        for(var state: PaymentAttemptLifecycle.State.values()) {
            for(var action: PaymentAttemptLifecycle.Action.values()) {
                var original = new PaymentAttemptLifecycle(attempt,order,state);
                var decision = original.decide(action);
                var match = valid.stream().filter(r -> r.from()==state && r.action()==action).findFirst();
                if(match.isPresent()) {
                    accepted++;
                    assertTrue(decision.accepted(), state + " / " + action);
                    assertEquals(match.get().to(), decision.next().state());
                    assertNotNull(decision.illustrativeFact());
                    assertNotSame(original, decision.next());
                    assertEquals(attempt,decision.next().attemptId());
                    assertEquals(order,decision.next().orderId());
                } else {
                    rejected++;
                    assertFalse(decision.accepted(), state + " / " + action);
                    assertSame(original, decision.next());
                    assertNull(decision.illustrativeFact());
                }
                assertEquals(state,original.state());
            }
        }
        assertEquals(7,accepted);
        assertEquals(35,rejected);
    }

    @Test void orderModelDoesNotApprovePolicyDependentClosure() {
        var recorded = OrderLifecycle.recorded(order);
        for(var action: OrderLifecycle.Action.values()) {
            var result=recorded.decide(action);
            assertFalse(result.accepted());
            assertSame(recorded,result.next());
            assertNull(result.fact());
        }
    }

    @Test void threeIndependentIdentitiesAndPartialStateComposition() {
        var o=OrderLifecycle.recorded(order);
        var merchant=RestaurantDecisionLifecycle.pending(order,restaurant);
        var payment=PaymentAttemptLifecycle.newAttempt(attempt,order);
        payment=payment.decide(PaymentAttemptLifecycle.Action.SUBMIT).next();
        payment=payment.decide(PaymentAttemptLifecycle.Action.CONFIRM_AUTHORIZED).next();
        var partial=new LifecycleSnapshot(o,merchant,payment);
        assertTrue(partial.awaitingMerchantDespiteAuthorization());
        assertEquals(OrderLifecycle.State.REQUESTED,partial.order().state());
        assertEquals(RestaurantDecisionLifecycle.State.PENDING,partial.merchant().state());
        assertEquals(PaymentAttemptLifecycle.State.AUTHORIZED,partial.payment().state());
        merchant=merchant.decide(RestaurantDecisionLifecycle.Action.REJECT).next();
        payment=payment.decide(PaymentAttemptLifecycle.Action.CONFIRM_CAPTURED).next();
        payment=payment.decide(PaymentAttemptLifecycle.Action.REQUEST_REFUND).next();
        var reconciliation=new LifecycleSnapshot(o,merchant,payment);
        assertTrue(reconciliation.merchantRejectedButFinancialRemediationPending());
        assertEquals(OrderLifecycle.State.REQUESTED,reconciliation.order().state());
        assertFalse(reconciliation.awaitingMerchantDespiteAuthorization());
        assertThrows(IllegalArgumentException.class,()->new LifecycleSnapshot(o,
            RestaurantDecisionLifecycle.pending(OrderId.newId(),restaurant),
            PaymentAttemptLifecycle.newAttempt(attempt,order)));
    }
}
