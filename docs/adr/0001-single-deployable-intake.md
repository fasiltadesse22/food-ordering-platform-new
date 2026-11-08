# ADR 0001 — One deployable, bounded order-request intake

**Status:** Accepted for Lesson 1.1.2 baseline; revisit with evidence.  
**Date:** 2026-11-08

## Decision forces

Customer intent can be received and recorded before restaurant decision. Neither merchant acceptance nor payment authorization has been implemented. Source contract requires an early runnable baseline and honest API semantics. No measured load, independent release or failure-isolation pressure justifies network service boundaries.

## Decision

One Java 21/Spring Boot 4.1.2 process. Domain `Order` records platform intake only; application `PlaceOrderService` checks a synthetic, static, server-owned menu and creates an intake record; HTTP adapter is not the domain. `OrderRepository` and `MenuCatalog` ports allow deterministic tests without framework plumbing. Process-local ConcurrentHashMap provides intentionally ephemeral storage.

Return HTTP 201 + Location for **creation of an order-request intake resource only**. Response explicitly says `REQUESTED_AWAITING_RESTAURANT`, `restaurantDecision: PENDING`, `paymentStatus: NOT_STARTED`; no payment provider is called.

## Alternatives not chosen

- Multiple microservices: inject unnecessary network partial-failure and operational cost into the first use case.
- Database: durability/concurrency/invariant enforcement must be motivated and tested in later lessons; a fake durable claim would be worse than an explicit volatile adapter.
- Client prices: untrusted; fixture catalog supplies a **teaching quotation**, but live restaurant pricing is not available.
- Merchant acceptance/payment simulation in POST: would conflate separate authorities and mask unresolved business policy.

## Limitations and revisit triggers

No authenticated customer identity, tenant authorization, production-ready pricing, merchant acceptance/availability, idempotency, HTTP retry safety, durable storage, cross-process consistency, live money, checkout taxes, discounts, currency conversion, delivery contract or operations controls. **Repeated POST creates separate intake resources.** If restarting service loses orders, that is expected baseline fragility. Use business, load and failure evidence to decide when to add mechanisms.
