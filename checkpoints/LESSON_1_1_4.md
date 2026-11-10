# Java Lesson 1.1.4 — Typed Identity and Authoritative Local State

**Classification:** Type A, real Java platform evolution.  
**Question:** Which identities are stable and which component is entitled to determine a fact?  
**Baseline main:** \`225db684a24080f2c4ad1ae6e2f5e259edf0aa7d\`.  
**Branch:** \`feat/lesson-114-stable-identity\`; [pull request](https://github.com/fasiltadesse22/food-ordering-platform-new/pull/2).

## Scope

- Three typed immutable identity values \`OrderId\`, \`CustomerId\`, and \`RestaurantId\`, used as the internal domain identity/reference values.
- \`Order\` and \`OrderRequestRecorded\` maintain raw boundary adapter accessors for the existing HTTP contract; post-save facts must refer to the exact order/customer/restaurant identities.
- Process-local repository map is keyed by \`OrderId\`; \`save\` is atomic create-only and rejects silent ID replacement. State ownership remains local to this single application process.
- No fake status mutation or revision version, no new payment-attempt entity, merchant callback, provider integration, event journal, Kafka, durable store, idempotency protocol, CQRS or microservice.
- Existing non-idempotent PlaceOrder behavior is unchanged. Two eligible repeated POSTs still create two different OrderIds.

## Invariants

1. Every locally recorded order has a stable, typed OrderId and the corresponding customer/restaurant references.
2. The returned local fact cannot attach to a different order/customer/restaurant.
3. The repository cannot silently overwrite an existing OrderId; a duplicate save fails even if fields match.
4. Lookup via raw UUID or typed OrderId returns the same original immutable snapshot within the current process.
5. HTTP JSON identifiers retain their original UUID/string formats and receipt meaning.
6. Rejected commands create no success fact; unexpected storage effects are not rewritten as rejection.

## Implementation / evidence

See [engineering contract](../docs/engineering/LESSON_1_1_4_IDENTITY_AUTHORITY_CONTRACT.md) and [Java PR #2](https://github.com/fasiltadesse22/food-ordering-platform-new/pull/2). Verify actual success on final branch commit and record immutable CI run/commit SHA before merging. This document intentionally does not assert an unobserved test result.

Existing Java 21/Spring Boot one-deployable system retains \`GET /api/orders/{id}\`, \`POST /api/orders\`, and \`GET /api/restaurants/{id}/menu\`. The in-memory catalog is synthetic. The API does not authenticate the customer. The provider is not present.

## Deferred

Versioned updates with optimistic concurrency, store durability, business state machines, authentication, payment attempt handling, callbacks, real refunds, multi-tenant merchant reference maps and durable event history. Revisit only when the roadmap provides authoritative business policies and testable forces.
