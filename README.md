# Food Ordering Platform — Current Java Baseline (through Lesson 1.1.9)

Java 21 · Spring Boot 4.1.2 · Maven · **one deployable, intentionally limited order-request intake**. This is an evolving engineering exercise, **not** a production food-delivery system.

The platform accepts a customer-supplied basket for the synthetic restaurant fixture, verifies fixture items, calculates a demo quotation, and records an immutable order-request snapshot in **volatile, process-local memory**. A successful HTTP 201 means **request recorded**, not merchant acceptance, durable storage, authorization, payment capture, or delivery.

**Current system and ownership:** [docs/architecture/CURRENT_STATE.md](docs/architecture/CURRENT_STATE.md)  
**Architectural decision:** [ADR 0001](docs/adr/0001-single-deployable-intake.md)  
**API contract:** [UC-001](docs/usecases/UC-001-place-order.md)  
**Future-lesson startup contract:** [docs/governance/SESSION_STARTUP.md](docs/governance/SESSION_STARTUP.md)  
**Documentation ownership:** [docs/governance/DOCUMENT_OWNERSHIP.md](docs/governance/DOCUMENT_OWNERSHIP.md)  
**Reliability evidence:** [docs/verification/FAILURE_EVIDENCE_MATRIX.md](docs/verification/FAILURE_EVIDENCE_MATRIX.md)

## Run locally

Requirements: Java 21 and Maven (prefer 3.9+).

```bash
mvn -B -ntp verify
mvn spring-boot:run
```

In a second terminal:

```bash
curl http://localhost:8080/api/restaurants/cedar-kitchen/menu
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -H 'X-Request-ID: walkthrough-001' \
  -d '{"customerId":"6f1595f8-02bc-4484-9800-810a98b611ee","restaurantId":"cedar-kitchen","items":[{"menuItemCode":"lentil-bowl","quantity":2}]}'
```

A valid request returns **201 Created**, Location `/api/orders/{orderId}`, `intakeStatus=REQUESTED_AWAITING_RESTAURANT`, `restaurantDecision=PENDING`, `paymentStatus=NOT_STARTED`, and `quotedTotalCents=2500` in synthetic USD fixture units. GET the Location while the same process is running.

Try `seasonal-special`: **422 ITEM_UNAVAILABLE**. Malformed input/quantity zero: **400**. `X-Request-ID` is correlation metadata, **not an idempotency key**. Repeating POST may create another distinct order.

## New in Lesson 1.1.9: executable local composition, not external fulfillment

A Spring `LocalCompositionSimulation` service can attach a separately identified process-local **classroom scenario** to an existing order intake. It composes independent merchant, payment, preparation and journey states, with explicit hypothetical policy assumptions and a deterministic `SimulatedPaymentPort` test double. It records only `Simulated*` observations. There are no new public order/merchant/payment endpoints; the underlying Order remains unchanged. The [mechanism contract](docs/engineering/LESSON_1_1_9_LOCAL_COMPOSITION_CONTRACT.md) explains guards, source/authority limits, failure cases and the runnable Spring integration tests. None of these scenarios represents live charges, refunds, merchant decisions, kitchen activity or production business approval.

## What is modeled, not executed

Later lessons introduced typed identities, explicit Order, RestaurantDecision and PaymentAttempt lifecycle models, guarded merchant decision **candidates**, and a preparation **probe**. These classes are *pure educational models*; there is no merchant API, provider integration, kitchen execution, real refund, authenticated actor, committed merchant transition, or external business fact. See [Lesson 1.1.7 engineering contract](docs/engineering/LESSON_1_1_7_GUARDED_TRANSITION_CONTRACT.md).

There is **no restart durability, request idempotency, cross-instance consistency, distributed transaction, message delivery protocol, or consensus**. Real limitations, tested negative behaviors, and missing failure experiments are tracked in [the evidence matrix](docs/verification/FAILURE_EVIDENCE_MATRIX.md). POL-01–POL-10 remain unresolved in the [academy policy backlog](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/domain/CLUSTER_1_1_LESSON_1_1_5_DECISION_BACKLOG.md).

## Verification and lesson continuity

CI runs Maven verification, packages and starts the application, and smoke-checks HTTP 201/422. Governance checks validate local documentation/architecture boundaries; neither CI nor the simulators prove remote guarantees. The previous guard checkpoint was [Lesson 1.1.7, Java PR #4](https://github.com/fasiltadesse22/food-ordering-platform-new/pull/4), merged at `8c5e5144b40adbd8679f4fe3f8046020b0279d2b`, with [59-test post-merge verification](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44745491071). Its [historical checkpoint](checkpoints/LESSON_1_1_7.md) remains valid. **Latest implemented Type A application evolution:** [Lesson 1.1.9 Java PR #7](https://github.com/fasiltadesse22/food-ordering-platform-new/pull/7), merged at `c7d3e2d741883b1aba5fcd1e51f4275ef48344ea`; [post-merge 72-test/HTTP CI](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44915338911). See the [frozen 1.1.9 checkpoint](checkpoints/LESSON_1_1_9.md).

The [React/TypeScript academy](https://github.com/fasiltadesse22/distributed-systems-engineering-academy) is a **separate Git repository** that owns the roadmap, deep lesson manuscripts, policy backlog, simulations and progress. No monorepo or synchronized commit assumption. The future target is a **progressively modular monolith** before any evidence-justified remote extraction; do not create fake service boundaries for lessons.
