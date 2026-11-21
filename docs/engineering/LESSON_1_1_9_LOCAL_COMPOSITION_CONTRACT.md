# Lesson 1.1.9 — Local cross-lifecycle composition contract

**Classification:** Type A verified local application evolution. **NOT** a merchant/payment integration.  
**Starting Java SHA:** `081d1f1a877772e41d7a30ce0052e9609c71f6b5`.  
**Companion academy:** Lessons 1.1.6–1.1.8; new Lesson 1.1.9.  
**Authority:** the [academy policy register](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/domain/CLUSTER_1_1_LESSON_1_1_5_DECISION_BACKLOG.md); POL-01–POL-10 are **UNRESOLVED**.

## What is now executable

The `LocalCompositionSimulation` Spring application service attaches an opt-in, volatile teaching session to an **already recorded** `OrderId`. It reads the genuine `OrderRepository` intake, preserves `OrderId` and `RestaurantId`, allocates a separately scoped `PaymentAttemptId`, and composes independently evolving `RestaurantDecisionLifecycle`, `PaymentAttemptLifecycle`, `PreparationGuardProbe.Stage` and `Journey`. It does **not** mutate the canonical `Order` resource. Previously shipped `POST /api/orders` and `GET /api/orders/{id}` keep their original semantics and responses.

Session construction is explicit: `begin(orderId, teachingPolicy)`. Duplicate begin with the same teaching policy returns the original session; changing policy under the same identity is rejected. No implicit or default approved commercial policy is added. The `SimulatedPaymentPort` supports only deterministic `CONFIRMED`, `DECLINED` and `RESPONSE_LOST` teaching presets, resolved locally by `DeterministicLocalPaymentSimulator` with **no network, external money, merchant system or callback**. All recorded labels begin `Simulated` and have `CLASSROOM_ASSUMPTION_NOT_EXTERNAL_FACT`. This strict label/class guards against accidentally calling the local observations provider facts.

A session's snapshot contains independent merchant, payment, preparation and journey states, a frozen policy, the typed identities and an immutable copy of a process-local list of *illustrative observations*. There is no production payment attempt, merchant commitment, refund, kitchen effect, durable audit or customer authorization. The memory store is a classroom notebook, not an authoritative entity repository.

## Independent lifecycles and cross-boundary eligibility

| Teaching command | Local prerequisites / positive path | Rejection / unknown result |
|---|---|---|
| SUBMIT_PAYMENT | Customer/platform actor, ACTIVE journey, explicit assumed ordering; if MERCHANT_FIRST, prior simulated acceptance | No action after cancellation request or terminal closure; unresolved POL-01 blocks |
| OBSERVE_CAPTURE | Simulator actor, assumed source signal, submitted attempt; local CONFIRMED / DECLINED preset | Response lost means SUBMITTED_UNKNOWN **stays unknown**; no refund from unknown |
| MERCHANT_ACCEPT / MERCHANT_REJECT | Merchant teaching actor, assumed evidence, actual matching IDs, existing 1.1.7 pure guard, PENDING; PAYMENT_FIRST requires simulated capture before accept | Repeated/competing terminal merchant decisions rejected with unchanged snapshot; no authentication |
| REQUEST_CANCELLATION | Customer actor while journey ACTIVE | This records only a *request*, not an approved cancellation |
| APPROVE_CANCELLATION | Explicit hypothetical PLATFORM authority and window rule BEFORE_ACCEPTANCE or BEFORE_PREPARATION | Unresolved POL-03 or exceeded POL-03/04 teaching cutoff rejects; no rollback of captured payment |
| DENY_CANCELLATION | Hypothetical PLATFORM authority with outstanding request | Returns to ACTIVE; records separate illustrative denial |
| START_PREPARATION / FINISH_PREPARATION | Simulated kitchen actor, assumed source evidence, merchant ACCEPTED, matching stage; hypothetical optional capture-before-prep | Pending cancellation and missing assumed authority block; no kitchen command |
| REQUEST_REFUND | PLATFORM teaching actor, explicit policy assumption, simulated CAPTURED and rejection or cancellation | Without assumed POL-05 entitlement, no refund attempt; cannot infer bank refund from order status |
| OBSERVE_REFUND | Simulator result, refund PENDING | RESPONSE_LOST or DECLINED retains refund pending and unresolved financial position; even CONFIRMED is local, not bank settlement |
| COMPLETE_JOURNEY | Explicit platform teaching evidence, ACTIVE, merchant accepted, simulated captured, prep READY | Pure classroom completion; *not delivered*, no fulfillment integration |

### Hypothetical alternatives

`PaymentSequence`: UNRESOLVED (fail closed), PAYMENT_FIRST, MERCHANT_FIRST, EITHER_FOR_EXPERIMENT. These are competing policy hypotheses, not defaults for a marketplace. `CancellationWindow`: UNRESOLVED, BEFORE_ACCEPTANCE, BEFORE_PREPARATION. The last two are hypothetical POL-03/04 gates, not consumer rights or regulations. `refundEligibilityAssumed` and `captureBeforePreparationAssumed` also belong solely to classroom assumptions. Actual entitlement, timing, costs, carrier handoff and source evidence remain for policy owners.

An **accepted-but-unpaid** teaching state is legal under MERCHANT_FIRST: merchant ACCEPTED, payment NOT_STARTED, order intake still REQUESTED. A **rejected-but-paid** state is legal under EITHER: merchant REJECTED with payment CAPTURED; the session must signal unresolved financial position until a separate simulated refund progresses to REFUNDED. An order `CANCELLED_SIMULATED` with captured funds also remains financially unresolved. No global order status is allowed to collapse those independent observations.

### Execution / local atomicity

`ConcurrentHashMap.compute` serializes decisions for a **single session key in one JVM**. It supports exactly one winning **simulated** terminal merchant decision when two local threads race. Each `Rejected` points at the exact same immutable `Snapshot` object, the history length stays constant and no success label is produced. Each `Applied` creates a new immutable snapshot and appends a distinct local simulated observation, never overwriting the original.

The simulated gateway runs inside that local decision function. Its `RESPONSE_LOST` preset models an ambiguous observation: without a provider, one cannot demonstrate that real funds moved. A local preset is not a remote side effect or an exactly-once guarantee. Repeating commands can add repeated observations; this teaching session does not implement deduplication, stable customer intent or a bank operation identity. Do not generalize `compute` into a transactional merchant/DB/provider commit.

### Diagnosing partial state

For the synthetic complaint “customer says paid; merchant rejected”: inspect the genuine intake `OrderId` first; inspect this *separate* teaching session; identify whether `CAPTURED` is an assumed simulator result or `SUBMITTED_UNKNOWN` is an observation gap; record REJECTED independently; check whether an authorized refund simulation is only requested or hypothetically observed. **Never tell the customer a real payment or refund occurred based on this simulation.** Real provider reconciliation requires a provider-specific contract and trusted evidence before it can be implemented.

## Executable evidence and missing guarantees

`LocalCompositionIntegrationTest` loads the actual Spring context and creates real local intake records before beginning sessions. It tests accepted-but-unpaid, rejected-but-paid and refund pending, payment-after-cancel prohibition, cancellation-versus-preparation cutoff, no-policy fail-closed behavior, missing assumed actor/evidence, simulated response loss and recovery, forbidden refund, happy-path preparation/completion, competing simulated merchant decisions, and stable identity / genuine intake prerequisites. The pre-existing MVC integration tests still assert unchanged HTTP 201/422 and no actual merchant or payment effects.

The production jar and HTTP contract remain identical. This class intentionally exposes **no operational HTTP endpoints**; allowing clients to send arbitrary “merchant accepted” or “provider capture confirmed” signals without authentication would falsely manufacture authority. Real operator and provider adapters, persisted/versioned mutations, remote failure experiments, payment settlement, refunds, customer security and cross-instance correctness are deferred. When real effects are justified later, design transaction and idempotency boundaries and inject actual failures *before* making reliability claims.

## Architecture decision and reconsideration

Stay within ADR 0001's single deployable and existing ports. A teaching session is separate from the authoritative intake aggregate and can be discarded without changing business records. Avoid microservices, distributed Sagas, outbox/inbox, external gateways or storage before their failure forces are observed. Reconsider public APIs/real adapters only when actual identities, actor authorization, POL-01–POL-10 approvals, source evidence and operational failure models exist. This is the required local stepping-stone for subsequent work, not a contract to prematurely distribute the application.
