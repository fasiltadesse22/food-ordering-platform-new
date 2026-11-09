# Lesson 1.1.3 — Command, Decision and Fact Checkpoint

**Date:** 2026-11-09
**Classification:** Type A · real Java evolution
**Engineering question:** What separates an intention to change state from proof that it changed?

## Immutable continuity

- Baseline 1.1.2 Java SHA: 08b33714431a53f6b1cb1e1e09718b0281da1ce8
- Verified implementation SHA: 28921380dc9e762fc7cf40fb829c0bacc0186ce7
- Verified CI: https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/42589276744 — SUCCESS
- This document is a later documentation-only commit. Inspect its own CI before claiming that commit has passed.

## What actually changed

PlaceOrderService.handle(PlaceOrderCommand) produces typed PlaceOrderOutcome.Recorded or PlaceOrderOutcome.Rejected. It constructs OrderRequestRecorded only after the existing process-local repository save returns normally. The local fact matches the accepted intake order and has an observation timestamp after normal save return. It is NOT a durable database commit event, a published message, merchant approval or payment.

Expected business rejection returns code and message without a successful fact or stored order. An unexpected infrastructure failure propagates because a repository could write and then throw; the caller has an unknown outcome, not a proven failure or business rejection.

The existing place() method adapts the typed outcome to its historical Order return or BusinessRejection throw interface. Original UC-001 HTTP semantics remain unchanged: 201 + Location for local intake only, merchant PENDING, payment NOT_STARTED, existing 400/422 rejection and synthetic prices. Identical POSTs remain non-idempotent.

## Executed verification

The [GitHub Actions run](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/42589276744) executed Maven root verify successfully: **18/18 tests passed** (five Spring MVC integration tests, seven existing application tests, five new decision/fact tests and one repository test). Packaged-jar confirmation passed. CI started the runnable Java application on port 18080: real HTTP POST returned **201** with pending merchant and payment-not-started fields; an unavailable fixture item returned **422 ITEM_UNAVAILABLE**. The CI log includes LIVE_HTTP_INTAKE_OK and LIVE_HTTP_BUSINESS_REJECTION_OK.

New tests cover: matching local fact/order after successful save; six rejected-command scenarios with no successful fact and no write; two identical commands creating different intake IDs; a custom repository that writes before throwing (unknown effect); backwards compatibility with the existing place() contract.

## Architecture and limitations

One Spring Boot deployable remains appropriate. No separate event store, durable database, message broker, idempotency protocol, Kafka, Outbox, Event Sourcing, authorized merchant acceptance, payment or refund code was added. The local fact is an in-process value returned to the handler's caller; it is not independently persisted, queryable or published. Even its timestamp does not establish durable commit.

Lesson 1.1.4 may expand business identity and authoritative state later. Do not reinterpret this fact as a provider-certified or merchant-approved outcome.
