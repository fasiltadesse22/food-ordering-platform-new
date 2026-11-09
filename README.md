# Food Ordering Platform — Lessons 1.1.2 and 1.1.3

**Source of truth:** [Academy roadmap](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/roadmap/MASTER_ROADMAP_V1.md) and [Lesson 1.1.1 domain discovery](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/domain/CLUSTER_1_1_LESSON_1_1_1_DOMAIN_DISCOVERY.md).

## First genuine workflow: create an order *request*

This is a **single runnable Spring Boot application** on Java 21 and Spring Boot 4.1.2. It validates an incoming Customer's requested basket against a synthetic restaurant fixture, computes a quoted demo total, and stores a new order request in volatile process memory. **No merchant accepts anything and no payment occurs.**

## Requirements

- Java 21
- Maven 3.6.3+ (prefer Maven 3.9+)

Run:

```bash
mvn -B verify
mvn spring-boot:run
```

Open another terminal:

```bash
curl http://localhost:8080/api/restaurants/cedar-kitchen/menu
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' -H 'X-Request-ID: walkthrough-001' \
  -d '{"customerId":"6f1595f8-02bc-4484-9800-810a98b611ee","restaurantId":"cedar-kitchen","items":[{"menuItemCode":"lentil-bowl","quantity":2}]}'
```

A successful response: **HTTP 201**, Location `/api/orders/{orderId}`, `intakeStatus=REQUESTED_AWAITING_RESTAURANT`, `restaurantDecision=PENDING`, `paymentStatus=NOT_STARTED`, `quotedTotalCents=2500` in **synthetic USD fixture units**. Follow the Location with GET within the same running process.

Try an unavailable item by replacing `lentil-bowl` with `seasonal-special`: **HTTP 422**, `ITEM_UNAVAILABLE`. Try `quantity=0`: **HTTP 400**. The correlation header `X-Request-ID` helps log tracing; it is **not** an idempotency key.

## Architecture

```text
HTTP request
   → OrderController [input contract and 201/4xx mapping]
   → PlaceOrderService [application use case + business decisions]
      → MenuCatalog [synthetic server-owned demo menu adapter]
      → Order [immutable domain intake fact]
      → OrderRepository [ConcurrentHashMap adapter, volatile]
   → 201 Created [request recorded; merchant PENDING; payment NOT_STARTED]
```

See [UC-001 specification](docs/usecases/UC-001-place-order.md) and [ADR 0001](docs/adr/0001-single-deployable-intake.md).

## Explicit missing guarantees

No persistent database, process-restart durability, checkout authentication/authorization, idempotency/retry safety, synchronized multi-instance state, race-control guarantees, live merchant inventory, price contracts, actual restaurant acceptance, payment provider, refund, delivery, tax or production deployment. This is **real runnable use-case code** under a narrow and honest contract, not production-ready commerce.

## Verification

`mvn -B -ntp verify` runs JUnit domain/application/HTTP integration tests; [GitHub Actions](https://github.com/fasiltadesse22/food-ordering-platform-new/actions) independently reports actual outcome. Do not claim a passing test unless inspected.

## Lesson 1.1.3 — explicit command decisions and local facts

A typed \`PlaceOrderOutcome\` now distinguishes successful local request intake from business rejection. \`OrderRequestRecorded\` is constructed only after the volatile repository's save returns normally. Infrastructure errors remain uncertain; rejected commands never generate a success fact. The existing \`place()\` and HTTP response behavior remain compatible.

Read [the Lesson 1.1.3 contract](docs/engineering/LESSON_1_1_3_COMMAND_FACT_CONTRACT.md). This is not event publication, durable storage, merchant acceptance, payment processing or idempotency.

## Execution continuity

Each academy lesson must reference an immutable platform SHA, relevant tests and the implementation's actual limits. Lesson 1.1.3 will add command/fact semantics without pretending the current intake is merchant approval or payment.
