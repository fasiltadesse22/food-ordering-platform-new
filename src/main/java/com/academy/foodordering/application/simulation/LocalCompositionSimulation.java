package com.academy.foodordering.application.simulation;

import com.academy.foodordering.application.OrderRepository;
import com.academy.foodordering.domain.GuardedRestaurantDecision;
import com.academy.foodordering.domain.OrderId;
import com.academy.foodordering.domain.PaymentAttemptId;
import com.academy.foodordering.domain.PaymentAttemptLifecycle;
import com.academy.foodordering.domain.PreparationGuardProbe;
import com.academy.foodordering.domain.RestaurantDecisionLifecycle;
import com.academy.foodordering.domain.RestaurantId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

/**
 * Application-facing, process-local composition of THREE independent learning
 * lifecycles anchored to a real recorded OrderId. No production Order update,
 * actual merchant instruction, payment provider, refund, kitchen effect, or
 * remotely authenticated fact is performed. This is not an operational API.
 *
 * Atomic compute is solely one-JVM simulation-session consistency. It does not
 * imply durable audit, real merchant authority, provider idempotency or
 * cross-service atomicity.
 */
@Service
public final class LocalCompositionSimulation {
    public enum PaymentSequence { UNRESOLVED, PAYMENT_FIRST, MERCHANT_FIRST, EITHER_FOR_EXPERIMENT }
    public enum CancellationWindow { UNRESOLVED, BEFORE_ACCEPTANCE, BEFORE_PREPARATION }
    public enum Journey { ACTIVE, CANCELLATION_REQUESTED, CANCELLED_SIMULATED, COMPLETED_SIMULATED }
    public enum Actor { CUSTOMER, MERCHANT_OPERATOR, KITCHEN_OPERATOR, PLATFORM, PAYMENT_SIMULATOR }
    public enum Action {
        SUBMIT_PAYMENT, OBSERVE_CAPTURE, MERCHANT_ACCEPT, MERCHANT_REJECT,
        REQUEST_CANCELLATION, APPROVE_CANCELLATION, DENY_CANCELLATION,
        START_PREPARATION, FINISH_PREPARATION, REQUEST_REFUND,
        OBSERVE_REFUND, COMPLETE_JOURNEY
    }

    /** Every non-UNRESOLVED option is a CLASSROOM ASSUMPTION, never approved POL-01–10. */
    public record TeachingPolicy(PaymentSequence sequencing, CancellationWindow cancellationWindow,
                                 boolean refundEligibilityAssumed, boolean captureBeforePreparationAssumed) {
        public TeachingPolicy {
            Objects.requireNonNull(sequencing); Objects.requireNonNull(cancellationWindow);
        }
        public static TeachingPolicy unresolved() {
            return new TeachingPolicy(PaymentSequence.UNRESOLVED,CancellationWindow.UNRESOLVED,false,false);
        }
    }

    /**
     * Caller-controlled source evidence is a teaching fixture, not authentication.
     * A payment result is supplied only to the deterministic simulator port.
     */
    public record Command(Action action, Actor actor, boolean sourceEvidenceAssumed,
                          SimulatedPaymentPort.Preset providerPreset) {
        public Command {
            Objects.requireNonNull(action); Objects.requireNonNull(actor);
        }
        public static Command of(Action action, Actor actor) {
            return new Command(action,actor,false,null);
        }
        public static Command assumed(Action action, Actor actor) {
            return new Command(action,actor,true,null);
        }
        public static Command provider(Action action, SimulatedPaymentPort.Preset preset) {
            return new Command(action,Actor.PAYMENT_SIMULATOR,true,preset);
        }
    }
    public record AssumedFact(int ordinal, String label, String authorityLevel) {
        public AssumedFact {
            if (ordinal < 1 || label == null || !label.startsWith("Simulated"))
                throw new IllegalArgumentException("Only explicitly simulated observations may be logged");
            if (!"CLASSROOM_ASSUMPTION_NOT_EXTERNAL_FACT".equals(authorityLevel))
                throw new IllegalArgumentException("Cannot forge external fact authority");
        }
    }

    public record Snapshot(OrderId orderId, RestaurantId restaurantId, PaymentAttemptId attemptId,
                           RestaurantDecisionLifecycle.State merchant,
                           PaymentAttemptLifecycle.State payment, PreparationGuardProbe.Stage preparation,
                           Journey journey, TeachingPolicy policy, List<AssumedFact> history) {
        public Snapshot {
            Objects.requireNonNull(orderId); Objects.requireNonNull(restaurantId);
            Objects.requireNonNull(attemptId); Objects.requireNonNull(merchant);
            Objects.requireNonNull(payment); Objects.requireNonNull(preparation);
            Objects.requireNonNull(journey); Objects.requireNonNull(policy);
            history = List.copyOf(history);
        }
        public boolean hasUnresolvedFinancialPosition() {
            return (merchant == RestaurantDecisionLifecycle.State.REJECTED
                    || journey == Journey.CANCELLED_SIMULATED)
                && (payment == PaymentAttemptLifecycle.State.CAPTURED
                    || payment == PaymentAttemptLifecycle.State.REFUND_PENDING);
        }
        public String evidenceScope() {
            return "PROCESS_LOCAL_CLASSROOM_ONLY_NO_EXTERNAL_MERCHANT_OR_PAYMENT_EFFECT";
        }
    }

    public sealed interface Outcome permits Applied, Rejected {}
    public record Applied(Snapshot next, AssumedFact simulatedFact) implements Outcome {
        public Applied { Objects.requireNonNull(next); Objects.requireNonNull(simulatedFact); }
    }
    public record Rejected(Snapshot unchanged, String code, String reason) implements Outcome {
        public Rejected { Objects.requireNonNull(unchanged); Objects.requireNonNull(code); Objects.requireNonNull(reason); }
    }

    private final OrderRepository intake;
    private final SimulatedPaymentPort simulatedProvider;
    private final ConcurrentHashMap<OrderId,Snapshot> sessions = new ConcurrentHashMap<>();

    public LocalCompositionSimulation(OrderRepository intake, SimulatedPaymentPort simulatedProvider) {
        this.intake = Objects.requireNonNull(intake);
        this.simulatedProvider = Objects.requireNonNull(simulatedProvider);
    }

    public Snapshot begin(OrderId id, TeachingPolicy policy) {
        Objects.requireNonNull(id); Objects.requireNonNull(policy);
        var order = intake.findById(id).orElseThrow(() ->
            new IllegalArgumentException("Simulation requires a recorded, locally accessible intake"));
        var proposed = new Snapshot(order.orderIdentity(), order.restaurantIdentity(),PaymentAttemptId.newId(),
            RestaurantDecisionLifecycle.State.PENDING, PaymentAttemptLifecycle.State.NOT_STARTED,
            PreparationGuardProbe.Stage.NOT_STARTED, Journey.ACTIVE, policy,List.of());
        var existing = sessions.putIfAbsent(id,proposed);
        if (existing != null && !existing.policy().equals(policy))
            throw new IllegalStateException("Scenario policy is frozen; cannot silently change assumptions");
        return existing == null ? proposed : existing;
    }

    public Snapshot inspect(OrderId id) {
        var snapshot = sessions.get(Objects.requireNonNull(id));
        if (snapshot == null) throw new IllegalArgumentException("No classroom session for OrderId");
        return snapshot;
    }

    /** Atomic within one JVM; rejected action must preserve EXACT snapshot reference. */
    public Outcome advance(OrderId id, Command command) {
        Objects.requireNonNull(id); Objects.requireNonNull(command);
        var answer = new AtomicReference<Outcome>();
        sessions.compute(id,(key,before)->{
            if (before == null) throw new IllegalArgumentException("No classroom session for OrderId");
            Outcome decision = decide(before,command);
            answer.set(decision);
            return decision instanceof Applied applied ? applied.next() : before;
        });
        return answer.get();
    }

    private Outcome decide(Snapshot s, Command c) {
        if (s.journey()==Journey.COMPLETED_SIMULATED || s.journey()==Journey.CANCELLED_SIMULATED) {
            if(c.action()!=Action.REQUEST_REFUND && c.action()!=Action.OBSERVE_REFUND)
                return reject(s,"TERMINAL_JOURNEY","A terminal teaching journey may retain a separate financial obligation");
        }
        var m=s.merchant();
        var p=s.payment();
        var k=s.preparation();
        var j=s.journey();
        String label;
        switch(c.action()) {
            case SUBMIT_PAYMENT -> {
                if (c.actor()!=Actor.CUSTOMER && c.actor()!=Actor.PLATFORM)
                    return reject(s,"ACTOR","Only customer/platform may initiate simulated payment");
                if(j!=Journey.ACTIVE) return reject(s,"CANCEL_RACE","Payment after cancellation request is blocked in this teaching example");
                if(s.policy().sequencing()==PaymentSequence.UNRESOLVED)
                    return reject(s,"POL_01_UNRESOLVED","Select an explicit hypothetical ordering before simulating");
                if(s.policy().sequencing()==PaymentSequence.MERCHANT_FIRST
                        && m!=RestaurantDecisionLifecycle.State.ACCEPTED)
                    return reject(s,"SEQUENCE","Merchant-first teaching hypothesis requires accepted before payment");
                var decision=payment(s).decide(PaymentAttemptLifecycle.Action.SUBMIT);
                if(!decision.accepted())return reject(s,"PAYMENT_STATE",decision.reason());
                p=decision.next().state();
                label="SimulatedPaymentSubmittedUnknown";
            }
            case OBSERVE_CAPTURE -> {
                if(c.actor()!=Actor.PAYMENT_SIMULATOR || !c.sourceEvidenceAssumed() || c.providerPreset()==null)
                    return reject(s,"SIMULATOR_EVIDENCE","A deliberate local simulator observation is required");
                if(p!=PaymentAttemptLifecycle.State.SUBMITTED_UNKNOWN)
                    return reject(s,"PAYMENT_STATE","Only a submitted teaching attempt can be resolved");
                var observation=simulatedProvider.observe(SimulatedPaymentPort.Effect.CAPTURE,c.providerPreset());
                if(observation==SimulatedPaymentPort.Observation.UNKNOWN)
                    label="SimulatedCaptureResponseLostUnknown";
                else {
                    var decision=payment(s).decide(observation==SimulatedPaymentPort.Observation.SIMULATED_CONFIRMED
                        ?PaymentAttemptLifecycle.Action.CONFIRM_CAPTURED
                        :PaymentAttemptLifecycle.Action.CONFIRM_DECLINED);
                    if(!decision.accepted())return reject(s,"PAYMENT_STATE",decision.reason());
                    p=decision.next().state();
                    label=observation==SimulatedPaymentPort.Observation.SIMULATED_CONFIRMED
                        ?"SimulatedCaptureConfirmedNotReal":"SimulatedPaymentDeclinedNotReal";
                }
            }
            case MERCHANT_ACCEPT, MERCHANT_REJECT -> {
                if(c.actor()!=Actor.MERCHANT_OPERATOR || !c.sourceEvidenceAssumed())
                    return reject(s,"MERCHANT_EVIDENCE","Merchant identity and source evidence are assumed only by the explicit training fixture");
                if(j!=Journey.ACTIVE)return reject(s,"CANCEL_RACE","Merchant decision during cancellation request is blocked in this teaching example");
                if(s.policy().sequencing()==PaymentSequence.UNRESOLVED)
                    return reject(s,"POL_01_UNRESOLVED","Payment/merchant ordering policy is unresolved");
                if(c.action()==Action.MERCHANT_ACCEPT
                    && s.policy().sequencing()==PaymentSequence.PAYMENT_FIRST
                    && p!=PaymentAttemptLifecycle.State.CAPTURED)
                    return reject(s,"SEQUENCE","Payment-first teaching scenario requires simulated captured state");
                var current=new RestaurantDecisionLifecycle(s.orderId(),s.restaurantId(),m);
                var context=new GuardedRestaurantDecision.Context(s.orderId(),s.restaurantId(),s.restaurantId(),
                    GuardedRestaurantDecision.ActorRole.MERCHANT_OPERATOR,true,true,true,true,m,
                    GuardedRestaurantDecision.TeachingPolicy.EDUCATIONAL_ONE_SHOT);
                var result=GuardedRestaurantDecision.evaluate(current,
                    c.action()==Action.MERCHANT_ACCEPT
                       ?RestaurantDecisionLifecycle.Action.ACCEPT:RestaurantDecisionLifecycle.Action.REJECT,context);
                if(!result.accepted())return reject(s,"MERCHANT_STATE",result.reason());
                m=result.next().state();
                label=m==RestaurantDecisionLifecycle.State.ACCEPTED
                    ?"SimulatedMerchantAcceptedNotReal":"SimulatedMerchantRejectedNotReal";
            }
            case REQUEST_CANCELLATION -> {
                if(c.actor()!=Actor.CUSTOMER)return reject(s,"ACTOR","Customer-only teaching request");
                if(j!=Journey.ACTIVE)return reject(s,"CANCELLATION_STATE","Cancellation already requested or closed");
                j=Journey.CANCELLATION_REQUESTED;
                label="SimulatedCancellationRequestedNotApproved";
            }
            case APPROVE_CANCELLATION -> {
                if(c.actor()!=Actor.PLATFORM || !c.sourceEvidenceAssumed())
                    return reject(s,"POLICY_AUTHORITY","Simulated decision authority must be assumed explicitly");
                if(j!=Journey.CANCELLATION_REQUESTED)
                    return reject(s,"CANCELLATION_STATE","No outstanding cancellation request");
                var window=s.policy().cancellationWindow();
                if(window==CancellationWindow.UNRESOLVED)
                    return reject(s,"POL_03_UNRESOLVED","No production cancellation cutoff approved");
                if(window==CancellationWindow.BEFORE_ACCEPTANCE
                    && m==RestaurantDecisionLifecycle.State.ACCEPTED)
                    return reject(s,"POL_03_CUTOFF","Teaching cutoff is before acceptance");
                if(window==CancellationWindow.BEFORE_PREPARATION
                    && k!=PreparationGuardProbe.Stage.NOT_STARTED)
                    return reject(s,"POL_04_CUTOFF","Simulated preparation started; cannot claim automatic cancellation");
                j=Journey.CANCELLED_SIMULATED;
                label="SimulatedCancellationApprovedNotReal";
            }
            case DENY_CANCELLATION -> {
                if(c.actor()!=Actor.PLATFORM || !c.sourceEvidenceAssumed())
                    return reject(s,"POLICY_AUTHORITY","Explicit classroom decision authority required");
                if(j!=Journey.CANCELLATION_REQUESTED)
                    return reject(s,"CANCELLATION_STATE","No pending cancellation to deny");
                j=Journey.ACTIVE;
                label="SimulatedCancellationDeniedNotReal";
            }
            case START_PREPARATION, FINISH_PREPARATION -> {
                if(c.actor()!=Actor.KITCHEN_OPERATOR || !c.sourceEvidenceAssumed())
                    return reject(s,"KITCHEN_EVIDENCE","No real kitchen actor or source proof exists");
                if(j!=Journey.ACTIVE)return reject(s,"CANCEL_RACE","Preparation cannot proceed in a pending cancellation scenario");
                if(s.policy().captureBeforePreparationAssumed()
                    && p!=PaymentAttemptLifecycle.State.CAPTURED)
                    return reject(s,"CAPTURE_BEFORE_PREP","Hypothetical policy requires simulated capture");
                var probe=PreparationGuardProbe.inspect(new PreparationGuardProbe.Input(s.orderId(),
                    new RestaurantDecisionLifecycle(s.orderId(),s.restaurantId(),m),k,
                    c.action()==Action.START_PREPARATION
                      ?PreparationGuardProbe.Command.START_PREPARING
                      :PreparationGuardProbe.Command.COMPLETE_PREPARATION,
                    PreparationGuardProbe.Actor.KITCHEN_OPERATOR,true,true));
                if(!probe.eligible())return reject(s,"PREPARATION_GUARD",probe.reason());
                k=probe.next();
                label=k==PreparationGuardProbe.Stage.PREPARING
                    ?"SimulatedKitchenWorkStartedNotReal":"SimulatedKitchenReadyNotReal";
            }
            case REQUEST_REFUND -> {
                if(c.actor()!=Actor.PLATFORM || !c.sourceEvidenceAssumed())
                    return reject(s,"REFUND_AUTHORITY","Explicit assumed classroom authorization required");
                if(!s.policy().refundEligibilityAssumed())
                    return reject(s,"POL_05_UNRESOLVED","No approved refund liability or entitlement");
                if(m!=RestaurantDecisionLifecycle.State.REJECTED && j!=Journey.CANCELLED_SIMULATED)
                    return reject(s,"NO_REMEDIATION_CAUSE","Refund teaching case requires rejection or simulated cancellation");
                var d=payment(s).decide(PaymentAttemptLifecycle.Action.REQUEST_REFUND);
                if(!d.accepted())return reject(s,"PAYMENT_STATE","Only simulated captured funds can generate refund candidate");
                p=d.next().state();
                label="SimulatedRefundRequestedNotCompleted";
            }
            case OBSERVE_REFUND -> {
                if(c.actor()!=Actor.PAYMENT_SIMULATOR || !c.sourceEvidenceAssumed() || c.providerPreset()==null)
                    return reject(s,"SIMULATOR_EVIDENCE","Explicit hypothetical refund observation required");
                if(p!=PaymentAttemptLifecycle.State.REFUND_PENDING)
                    return reject(s,"REFUND_STATE","No pending simulated refund");
                var observation=simulatedProvider.observe(SimulatedPaymentPort.Effect.REFUND,c.providerPreset());
                if(observation==SimulatedPaymentPort.Observation.SIMULATED_CONFIRMED) {
                    var d=payment(s).decide(PaymentAttemptLifecycle.Action.CONFIRM_REFUNDED);
                    p=d.next().state();
                    label="SimulatedRefundConfirmedNotBankSettlement";
                } else if(observation==SimulatedPaymentPort.Observation.UNKNOWN)
                    label="SimulatedRefundResponseLostUnknown";
                else label="SimulatedRefundDeclinedObligationStillOpen";
            }
            case COMPLETE_JOURNEY -> {
                if(c.actor()!=Actor.PLATFORM || !c.sourceEvidenceAssumed())
                    return reject(s,"COMPLETION_EVIDENCE","Only explicitly assumed classroom completion evidence is allowed");
                if(j!=Journey.ACTIVE || m!=RestaurantDecisionLifecycle.State.ACCEPTED
                    || p!=PaymentAttemptLifecycle.State.CAPTURED
                    || k!=PreparationGuardProbe.Stage.READY)
                    return reject(s,"COMPLETION_GUARD","Simulated completion requires accepted, captured and ready; no delivery occurred");
                j=Journey.COMPLETED_SIMULATED;
                label="SimulatedJourneyCompleteNotDelivered";
            }
            default -> throw new IllegalStateException("Unsupported classroom action");
        }
        var history=new ArrayList<>(s.history());
        var fact=new AssumedFact(history.size()+1,label,"CLASSROOM_ASSUMPTION_NOT_EXTERNAL_FACT");
        history.add(fact);
        return new Applied(new Snapshot(s.orderId(),s.restaurantId(),s.attemptId(),m,p,k,j,s.policy(),history),fact);
    }

    private static PaymentAttemptLifecycle payment(Snapshot s) {
        return new PaymentAttemptLifecycle(s.attemptId(),s.orderId(),s.payment());
    }
    private static Rejected reject(Snapshot s,String code,String explanation) {
        return new Rejected(s,code,explanation);
    }
}
