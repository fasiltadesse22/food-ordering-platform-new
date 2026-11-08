# Lesson 1.1.2 — First runnable Food Ordering Platform checkpoint

**Classification:** Type A — persistent application evolution.  
**Date:** 2026-11-08.  
**Engineering question:** Which customer-visible business outcome can the initial platform actually guarantee?

## Repository and exact evidence

- **Planning baseline SHA:** 2258cb1b8fc788a2ca8e35a2419e031fddbcb0eb.
- **Verified Java implementation SHA:** [68c2cebb96dfcb213167ed35c78c1e9cca87f455](https://github.com/fasiltadesse22/food-ordering-platform-new/commit/68c2cebb96dfcb213167ed35c78c1e9cca87f455).
- **Successful Java CI run:** [42514135184](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/42514135184), Ubuntu/Temurin Java 21, Spring Boot 4.1.2.
- **Academy lesson source:** https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/content/lessons/1.1.2.md.
- **Authority/source contract:** https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/domain/CLUSTER_1_1_LESSON_1_1_1_DOMAIN_DISCOVERY.md.

The verification evidence is tied to the implementation SHA above. This checkpoint Markdown document is a *subsequent documentation commit*, so do not say it was included in that earlier CI test.

## Implemented scope

- One independently runnable Java 21 Spring Boot 4.1.2 deployable with a Maven root build.
- **UC-001 Submit Order Request:** a customer's requested item codes and quantities are validated against a fictional, static, server-side restaurant fixture.
- Application creates immutable intake record with unique UUID, timestamp, price snapshot and synthetic quote; stores in **process-local, volatile** ConcurrentHashMap.
- REST routes: POST /api/orders, GET /api/orders/{id}, GET /api/restaurants/{id}/menu.
- HTTP adapter distinguishes malformed input **400**, business rejections **422**, missing resource read **404**, and accepted request-intake **201 Created** with Location.
- 201 means only **REQUESTED_AWAITING_RESTAURANT**; restaurantDecision is **PENDING**; paymentStatus is **NOT_STARTED**.
- X-Request-ID response/log correlation (not business idempotency).
- Explicitly scoped intake/rejection logs without recording customer UUIDs in the use-case log statements.

## Actual verification

Executed by GitHub Actions at implementation commit 68c2cebb:

1. mvn -B -ntp verify — **success**.
2. 7 application unit tests — **passed**.
3. 1 in-memory repository test — **passed**.
4. 5 Spring Boot + MockMvc HTTP integration tests — **passed**.
5. Runnable jar packaged and file existence confirmed — **passed**.
6. Started jar on CI at port 18080, waited for menu GET, POSTed valid intake and checked fields — **success**.
7. POSTed unavailable fixture item against running jar and asserted 422 ITEM_UNAVAILABLE — **success**.

Observed CI messages: LIVE_HTTP_INTAKE_OK and LIVE_HTTP_BUSINESS_REJECTION_OK. The generated order ID in the live CI run is not a fixture guarantee; it is random for each request.

**No other runtime experiment is claimed.** No production deployment or load test. No actual merchant acceptance or money processing.

## Domain acceptance matrix

| ID | Situation | Expected and observed test coverage |
|---|---|---|
| AC-01 | Available demo item, valid quantity | 201 Created, Location, PENDING merchant / NOT_STARTED payment; MVC integration and CI live HTTP |
| AC-02 | Retrieve created intake in running process | 200, consistent intake representation; MVC integration |
| AC-03 | Malformed JSON or quantity zero | 400 INVALID_REQUEST; MVC integration |
| AC-04 | Unavailable menu item / unknown restaurant | 422 business rejection; unit + MVC; live HTTP for unavailable item |
| AC-05 | Repeat identical PlaceOrder calls | Two distinct intake IDs; unit test documenting **lack** of idempotency |
| AC-06 | Two Injera Combos | 3600 synthetic minor units, from server fixture; unit test |
| AC-07 | GET nonexistent UUID | 404; MVC integration |

For full request/response conditions see [UC-001](../docs/usecases/UC-001-place-order.md).

## Implementation and design trade-offs

HTTP adapter: OrderController + ApiExceptionHandler + RequestCorrelationFilter.  
Use case: PlaceOrderService with PlaceOrderCommand and small MenuCatalog/OrderRepository ports.  
Domain: immutable Order request and BusinessRejection.  
Adapters: InMemoryMenuCatalog (fictional) and InMemoryOrderRepository (volatile).

[ADR 0001](../docs/adr/0001-single-deployable-intake.md) chooses one deployable. Alternatives rejected at this milestone: prematurely split Order/Restaurant/Payment services, Kafka/Saga, fake provider, PostgreSQL without a measured persistence experiment, and client-owned pricing.

## Known limits and deferred mechanisms

No authenticated/authorized customer, live restaurant availability or fulfillment acceptance, payment authorization/capture/refund, tax/discount/legal pricing, delivery, reliable notifications, durable persistence, read-your-writes across replicas, safe retry/idempotency contract, transactional business invariants, load/capacity claims, fault recovery, multi-instance consistency or production security. A process restart loses all intakes. Repeated POST may create duplicates. These are explicit baseline forces for later lessons, not hidden features.

## Forward dependency

Lesson 1.1.3 will develop command/decision/fact language around the genuine intake baseline, without assuming a payment event or distributed infrastructure exists. Persistence, lifecycle guards, idempotency and concurrency deserve their own later reproducible checkpoints.

**Checkpoint status:** verified against the cited implementation SHA and CI run; business policies remain educational assumptions, not merchant-approved production contracts.
