# Lesson 1.1.7 — Guarded Decision, Preconditions and Postconditions Contract

**Roadmap:** Master Roadmap V1 · Cluster 1.1 · Chapter 2 · Lesson 1.1.7 (Type A).  
**Baseline Java:** `79024c3c36fe1a8cc6f71b79505601894d927507`.  
**Status:** Feature candidate; no tests may be claimed until CI executes.

## Engineering question

What makes a requested state transition legally executable at a defined authority and decision boundary? A valid command shape is not a permitted decision, a permitted **pure local** decision is not a database commit, and a simulated merchant/provider outcome is not authentic external evidence.

## Changes and ownership

- `GuardCheck` makes each guard's rule identifier, truth result and explanation inspectable.
- `GuardedRestaurantDecision` performs an **exhaustive, deterministic** 11-check local evaluation over the existing immutable RestaurantDecisionLifecycle and its Action. Checks include requested order/merchant identity, actor role and scope, *separate* technical authentication and business authorization assumptions, expected current state, one-shot PENDING rule, basket/price terms, source evidence assumption and explicit hypothetical POL-02 policy. This is a teaching evaluator, not an endpoint or merchant authority.
- `PreviewRestaurantDecisionService` makes application outcomes a sealed `Eligible` / `Rejected` distinction. **Eligible means a candidate**, not an accepted merchant request and not a persistence side effect.
- `PreparationGuardProbe` evaluates hypothetical START_PREPARING/COMPLETE_PREPARATION preconditions without creating a kitchen lifecycle module, persistence, payment dependency, HTTP endpoint or real preparation. Its six independent checks include matching OrderId, kitchen authority, accepted merchant decision, source evidence, current stage and explicit hypothetical POL-04. The classroom model's preparation gate is **not approved policy**.
- Existing `OrderLifecycle`, `RestaurantDecisionLifecycle`, `PaymentAttemptLifecycle`, `Order`, `PlaceOrderService`, `OrderController` and transport contracts remain unchanged; POL-01–POL-10 remain unresolved.

## Formal contract and guard order

`evaluate(current, action, context) → Evaluation(before, next, guards, accepted, reason, illustrativeCandidateFact)`.

For merchant decision the eleven checks run deterministically in this published order:

1. IDENTITY_ORDER.
2. IDENTITY_RESTAURANT.
3. ACTOR_ROLE.
4. ACTOR_SCOPE.
5. AUTHENTICATED_ASSUMED.
6. BUSINESS_AUTHORITY_ASSUMED.
7. EXPECTED_STATE.
8. STATE_PENDING.
9. BASKET_TERMS_ASSUMED.
10. MERCHANT_EVIDENCE_ASSUMED.
11. POL_02_EDUCATIONAL_ONLY.

All checks are reported, so the reader can see multiple failed conditions. The **first failure** in this stable order determines the explanatory rejection reason. This is an educational precedence rule, not a universal security/error disclosure policy. In production, authentication should occur at a trusted boundary and error responses must not expose confidential evidence about other actors.

**Accepted (educational assumption only):** all checks pass; delegate the final pure state transition to the existing RestaurantDecisionLifecycle; preserve OrderId and RestaurantId; candidate fact name starts with `CandidateIllustrative...`; no repository write, external call or historical business fact is established. The correct postcondition is *eligible candidate*, not merchant commitment.

**Rejected:** accepted=false, `next == before` (same immutable object), no candidate fact; guard failure is explicit and inspectable. Defensive constructors reject fabricated success labels for rejected decisions.

## What the blueprint's PLACED example means here

The roadmap's `AcceptOrder(current=PLACED)` is **a hypothetical commercial workflow phase**, not an implemented `OrderLifecycle.State`. The actual Java platform stores an Order REQUESTED intake snapshot. The separate `RestaurantDecisionLifecycle.State.PENDING` is the *relevant local guard state for merchant decisions*. Thus the example maps conceptually `Order REQUESTED + RestaurantDecision PENDING → merchant ACCEPTED candidate`; the platform never renames REQUESTED to PLACED or claims that merchant acceptance happened. The model's authorization/evidence assumptions are explicitly supplied by the learning exercise; no real source attests them.

## Required illustrative edge cases

| Case | Guard outcome | Next state | Successful fact |
|---|---|---|---|
| Authenticated merchant operator, matching IDs/terms, PENDING, POL-02 classroom assumption | Eligible candidate | ACCEPTED or REJECTED | `CandidateIllustrative...` only |
| CUSTOMER or PLATFORM attempts merchant ACCEPT | ACTOR_ROLE fails | Exact original PENDING object | None |
| Actor authenticated but not authorized for merchant | BUSINESS_AUTHORITY_ASSUMED fails | Same object | None |
| Wrong OrderId or merchant identity | IDENTITY_ORDER/RESTAURANT fails | Same object | None |
| Expected state differs from observed | EXPECTED_STATE fails; cannot prove compare-and-swap | Same object | None |
| Restaurant already REJECTED, attempts ACCEPT | STATE_PENDING fails | Same object | None |
| POL-02 unresolved mode | POL_02_EDUCATIONAL_ONLY fails | Same object | None |
| START_PREPARING before merchant ACCEPTED | MERCHANT_ACCEPTED fails | Same kitchen phase | None |
| COMPLETE_PREPARATION while NOT_STARTED | CURRENT_STAGE fails | Same kitchen phase | None |
| Preparation POL-04 not assumed | POL_04_EDUCATIONAL_ONLY fails | Same kitchen phase | None |

## Commit and effects boundary

The evaluator *does not have* a persistence commit point. A decision result is a proposed pure next state, with no authoritative write. Actual use-case execution in the existing `PlaceOrderService` validates a request, saves the immutable intake snapshot in a process-local repository, and only **then** returns OrderRequestRecorded. The post-save fact supports a local intake effect; HTTP 201 is not merchant acceptance, durable storage, or payment. Future real merchant transitions would need authenticated external source evidence and an actual compare-and-set/versioned persistence boundary if concurrency is possible. A guard passing against stale state does not guarantee the write will remain legal at commit time.

## Table-derived verification

`GuardedTransitionsTest` parameterizes successful merchant accept/reject; enumerates independent failures across eleven context conditions; checks terminal merchant states; exercises denied kitchen start and completion; verifies hypothetical positive probes; validates defensive result invariants and pure precommit limitations. Existing Java 1.1.6 lifecycle and 1.1.2 intake regression tests remain in the same Maven workflow. No new HTTP endpoint or production actor simulation is exposed.

## Scope/deferred decisions

No real merchant, kitchen, delivery, payment, refund, transaction isolation, distributed messaging, storage semantics or new microservices. POL-01–POL-10 are still open. Lesson 1.1.8 (effect reversibility) and 1.1.9 (multi-lifecycle composition) remain future work; deeper concurrent state races and stale reads remain later milestones. Keep both repositories separate.

## Acceptance evidence

**Pending:** CI run, exact Java feature SHA and merged SHA, Maven test count, packaging and live 201/422 smoke results. Update only after reading actual GitHub Actions output.
