# Current architecture and capabilities — through Lesson 1.1.7

**Purpose:** Concise current capability index, **not** a new ADR, checkpoint or specification. Reviewed against the Java main implementation baseline `46b02d28f0acd4555d663e4361581f07b66c575d` (2026-11-20). Latest *business-code* implementation commit: `8c5e5144b40adbd8679f4fe3f8046020b0279d2b`; later governance/documentation commits do not imply a changed runtime. Revalidate against `main` at every new lesson; this line is historical evidence, not a moving SHA.

## Executable architecture

One Java 21 / Spring Boot 4.1.2 / Maven deployable. Currently one real business use case: **UC-001 record an order request** using a static server-owned synthetic restaurant menu, safe integer quotation calculations, immutable `Order` snapshot and `ConcurrentHashMap` process-local create-only repository.

Current HTTP API in `api/OrderController.java`:
- `GET /api/restaurants/{id}/menu` — synthetic fixture menu.
- `POST /api/orders` — validate, quote, record intake; 201 + Location for a new request, not merchant acceptance.
- `GET /api/orders/{id}` — same-process lookup or 404.
- Example error: 400 for bad request shape, 422 for item not available.

On success: `REQUESTED_AWAITING_RESTAURANT`, merchant decision `PENDING`, payment `NOT_STARTED`. `X-Request-ID` is a correlation ID, **not** a deduplication key. Each repeated valid POST may allocate a new OrderId. Storage disappears on process restart.

## Ownership and current dependencies

| Java package | Actual responsibility |
|---|---|
| `api/` | Spring MVC request/response mapping, validation and exception translation |
| `application/` | `PlaceOrderService`, command/outcome, `OrderRepository` and `MenuCatalog` ports; pure `PreviewRestaurantDecisionService` |
| `domain/` | Immutable IDs, `Order`, `OrderRequestRecorded`, business rejection and isolated pure lifecycle/guard models |
| `infrastructure/` | In-memory menu/repository adapters, both process-local |
| bootstrap `FoodOrderingApplication` | One executable Spring Boot composition |

Future target: progressively establish owner-aligned business boundaries *within the single deployable* when a lesson justifies them. No independent order/merchant/payment/fulfillment microservices or independent databases exist today. No service-discovery or Saga coordination exists.

## Evidence classes — do not conflate

| Category | Source | What it can establish |
|---|---|---|
| **Real local effect** | `PlaceOrderService.handle` calls `OrderRepository.save` and only then constructs `OrderRequestRecorded` | Local process recorded request if save returned normally; **not durable** |
| **Atomic process-local create** | `InMemoryOrderRepository.save` uses `putIfAbsent` by typed OrderId | One JVM's no-replacement check, **not multi-instance consistency** |
| **Pure Order lifecycle view** | `OrderLifecycle`, `Order.intakeLifecycle()` | A modeled NEW→REQUESTED transition and truthful view of existing intake |
| **Illustrative merchant/payment** | `RestaurantDecisionLifecycle`, `PaymentAttemptLifecycle`, `LifecycleSnapshot` | Hypothetical independent state results; no external participants contacted |
| **Candidate guards** | `GuardedRestaurantDecision`, `PreparationGuardProbe`, `PreviewRestaurantDecisionService` | Pure predicates and candidate outcomes under **assumed** actor/merchant/policy evidence; no commit |
| **Learning simulations** | Separate React academy | Conceptual explanations, not proof of Java runtime behavior |

No production code may promote `CandidateIllustrative...` or a pure provider state to an authenticated external fact. All business policy decisions POL-01–POL-10 remain **UNRESOLVED**; source authority: [academy decision register](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/domain/CLUSTER_1_1_LESSON_1_1_5_DECISION_BACKLOG.md).

## Explicitly NOT implemented / NOT proved

No restart durability, durable database, idempotency, retry-safe exactly-once customer intent, verified customer identity, merchant authentication/acceptance, provider authorization/capture/refund/settlement, kitchen work, delivery, versioned order update, transactional outbox, message delivery, concurrent merchant state commit, distributed failure isolation, cross-instance state, distributed transaction, linearizable agreement or consensus. Local tests do not change these facts.

## Verify and navigate

- [API contract](../usecases/UC-001-place-order.md), [ADR 0001](../adr/0001-single-deployable-intake.md).
- [Latest implemented guard contract](../engineering/LESSON_1_1_7_GUARDED_TRANSITION_CONTRACT.md), [historical Lesson 1.1.7 checkpoint](../../checkpoints/LESSON_1_1_7.md).
- [Failure evidence matrix](../verification/FAILURE_EVIDENCE_MATRIX.md), [session preflight](../governance/SESSION_STARTUP.md), [document ownership](../governance/DOCUMENT_OWNERSHIP.md).
- [Java implementation verification](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44745491071): 59/59 JUnit, Maven package, live HTTP 201/422. Always inspect latest `main` CI independently.
- [Academy contract](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/contracts/DUAL_REPOSITORY_EXECUTION_CONTRACT_V1.md); no synchronized Git histories.
