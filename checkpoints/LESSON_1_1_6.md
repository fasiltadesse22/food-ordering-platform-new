# Lesson 1.1.6 — Independent Domain Lifecycles

This checkpoint implements **pure local models**, preserving the existing Spring Boot single-deployable UC-001 intake API. No restaurant, bank, payment processor or courier is contacted. All model merchant and payment facts are named `Illustrative...` and are not publishable provider/restaurant evidence.

## Model contracts

| Identity and model | Owner | States | Actual executable authority |
|---|---|---|---|
| `OrderLifecycle(OrderId)` | Platform intake | NEW → REQUESTED | Local record intake; existing Order snapshot provides REQUESTED view |
| `RestaurantDecisionLifecycle(OrderId,RestaurantId)` | Hypothetical merchant | PENDING → ACCEPTED or REJECTED | Pure educational decision; no external merchant is connected |
| `PaymentAttemptLifecycle(PaymentAttemptId,OrderId)` | Hypothetical financial authority | NOT_STARTED → SUBMITTED_UNKNOWN → AUTHORIZED/DECLINED/CAPTURED → REFUND_PENDING → REFUNDED | Pure educational evidence-assumption model; no money effect occurs |
| `LifecycleSnapshot` | Analytical composition | Separate product state | Referential consistency only, **not an atomic transaction** |

### Transition rules

- Order NEW + RECORD_INTAKE → REQUESTED (pure decision); repeated RECORD_INTAKE rejected and no new fact. CLOSE_UNFULFILLABLE rejected pending business policy POL-02.
- Restaurant PENDING + ACCEPT or REJECT → terminal model result. Repeated or conflicting decision is rejected and returns the same immutable snapshot without emitting a fact. This does **not** approve a real merchant policy.
- Payment NOT_STARTED + SUBMIT → SUBMITTED_UNKNOWN. This does not prove provider submission in production.
- Payment SUBMITTED_UNKNOWN + illustrative confirmed outcome → AUTHORIZED, DECLINED or CAPTURED. AUTHORIZED + confirmed capture → CAPTURED. CAPTURED + refund request → REFUND_PENDING, and only illustrative provider confirmation moves to REFUNDED.
- All other payment state/action pairs reject without changing identity, state or asserting facts. The model tests all 42 combinations, 7 allowed and 35 rejected.

These states encode one **illustrative teaching policy** for the isolated model. Actual permitted ordering, cancellation, refund liability and preparation rules remain unresolved POL-01–POL-10 and are not implemented.

## Implemented evidence and constraints

`Order.intakeLifecycle()` returns a truthful strongly typed local intake view of the existing immutable Order. All previous POST/GET routes and response fields are unchanged. No persistent update operation, provider adapter, merchant API, callback, durable message, distributed coordinator or idempotency guarantee exists. Independent states may show payment AUTHORIZED while merchant PENDING, or merchant REJECTED while refund PENDING. This illustrates partial business completion, not actual financial operations.

## Verification

- Pre-change baseline: `ea17135cdc1974d89d78ce78931bb63cc5e9544e`.
- Run `./mvnw test` (or `mvn test` if wrapper absent), `./mvnw package`, and packaged-jar HTTP 201/422 intake smoke. Record exact CI and immutable revision before marking this checkpoint complete.
- **Verification:** [GitHub Actions run 44052994889](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44052994889) passed on immutable feature revision `7c89a3e9ece643b486d6132613d12e1424312cb6`: **34/34 JUnit tests**, Maven package succeeded, runnable jar verified, live HTTP 201 intake and 422 business rejection smoke passed. This documentation commit and any merge need their own CI verification.
- Next lesson 1.1.7 may add guards after current acceptance; do not implement future policy here.
