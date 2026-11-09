# Lesson 1.1.3 — Command, Decision and Local Fact Semantics

## Business meaning and exact source contract

**Command:** existing \`PlaceOrderCommand\` expresses customer intent with a supplied but unauthenticated customer reference. It is not proof of state mutation.

**Decision:** \`PlaceOrderService.handle(command)\` returns either \`PlaceOrderOutcome.Recorded(order, fact)\` after a normal process-local repository save, or \`PlaceOrderOutcome.Rejected(code, message)\` for an anticipated business rejection.

**Established local claim:** \`OrderRequestRecorded\` is created only after the in-process \`OrderRepository.save\` call returns normally. It describes **request intake**, not restaurant acceptance, card payment, external messaging, crash-durable commit or a published integration event. \`observedAfterSaveAt\` records local program-time observation, not a database commit timestamp.

**Unknown effect:** storage infrastructure failures are propagated, not rewritten as ordinary business rejection. A repository may save and then throw; an unseen or failed acknowledgement does not prove that a record was absent.

**Compatibility:** existing \`place(command)\` adapts the decision to the original \`Order\` return / \`BusinessRejection\` throw interface, preserving UC-001 HTTP 201, Location, 400/422, merchant PENDING, payment NOT_STARTED, server-owned demo pricing and repeat-POST non-idempotency.

## Local causal sequence

1. Accept one client command; structural HTTP validation remains at the controller.
2. Validate intent and synthetic business conditions in the application.
3. Construct immutable order-request intake with a fresh UUID.
4. Invoke in-memory repository save.
5. Only after normal return, construct a typed \`OrderRequestRecorded\` local fact and return \`Recorded\`.
6. Rejected commands return \`Rejected\` and do not construct a success fact.
7. If the adapter fails after any side effect, propagate the infrastructure exception; do not assert rejection, absence or success.
8. HTTP continues using \`place\`, preserving the public response's truthful narrow scope.

No second event journal is added: it would create a separate, non-atomic mutable store and false guarantees. The typed fact is an educationally meaningful local representation available to the handler's caller, not a durable record with an independent lookup API. Even the existing log line is not proof of persistent historical truth.

## Evidence plan

\`PlaceOrderDecisionTest\` checks fact/order matching, rejection cases without successful writes, duplicate commands, adapter-write-then-throw uncertainty, and compatibility. Existing \`PlaceOrderServiceTest\`, repository tests and HTTP integration tests remain. Maven verify, packaged-jar smoke and actual workflow results must be inspected before this checkpoint can be marked verified.

## Deferred business and architecture decisions

No \`AcceptOrder\`, \`OrderAccepted\`, provider payment, refund, cancellation, Kafka, Outbox, Event Sourcing, durable DB, customer authorization, transactional event store or idempotency protocol is introduced. Those belong to later approved contracts and evidence. The single Spring Boot process remains the intentionally minimal architecture.
