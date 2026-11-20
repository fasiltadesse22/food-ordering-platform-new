# Lesson 1.1.7 — Guarded transition engineering contract

**Purpose:** The durable *mechanism* contract. For historical execution/CI evidence see [the lesson checkpoint](../../checkpoints/LESSON_1_1_7.md). The pre-governance file was an exact duplicate of the checkpoint; its full original bytes remain at [the immutable pre-correction SHA](https://github.com/fasiltadesse22/food-ordering-platform-new/blob/46b02d28f0acd4555d663e4361581f07b66c575d/docs/engineering/LESSON_1_1_7_GUARDED_TRANSITION_CONTRACT.md).

## Scope and authority

The Java application is one Spring Boot deployable with **real local order-request intake only**. RestaurantDecision and PaymentAttempt lifecycles are pure illustrative domain models; no external merchant, payment provider or kitchen authority. This lesson introduces **candidate guard evaluation** and no external effect. POL-01–POL-10 are [unresolved business-policy decisions](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/domain/CLUSTER_1_1_LESSON_1_1_5_DECISION_BACKLOG.md). The blueprint's `AcceptOrder(current=PLACED)` maps conceptually to Order REQUESTED *intake* plus the independent RestaurantDecision PENDING, **not** a newly implemented `OrderLifecycle.State.PLACED`.

## Formal domain operation

`GuardedRestaurantDecision.evaluate(current, action, context) → Evaluation(before, next, action, accepted, guards, reason, illustrativeCandidateFact)`.

**Guard order and meaning** (all are evaluated; first failing code selects diagnostic explanation in this *internal teaching function*):

| Guard | Predicate meaning |
|---|---|
| IDENTITY_ORDER | Target OrderId matches modeled merchant decision |
| IDENTITY_RESTAURANT | Target RestaurantId matches decision owner |
| ACTOR_ROLE | Model-supplied actor role is MERCHANT_OPERATOR |
| ACTOR_SCOPE | Actor's claimed merchant matches target owner |
| AUTHENTICATED_ASSUMED | Classroom assumes technical principal verified |
| BUSINESS_AUTHORITY_ASSUMED | Classroom assumes decision authorization separately verified |
| EXPECTED_STATE | Supplied observed state matches locally expected state (not database CAS) |
| STATE_PENDING | One-shot illustrative merchant decision only from PENDING |
| BASKET_TERMS_ASSUMED | Classroom assumes correct offered basket/price version |
| MERCHANT_EVIDENCE_ASSUMED | Classroom assumes authoritative external merchant evidence |
| POL_02_EDUCATIONAL_ONLY | Explicit teaching assumption selected; unresolved policy is blocked |

**Eligible:** all guards true, underlying pure RestaurantDecisionLifecycle action accepted; new immutable candidate with unchanged OrderId/RestaurantId, `CandidateIllustrativeRestaurantAccepted` or `CandidateIllustrativeRestaurantRejected`. The `PreviewRestaurantDecisionService.Eligible` wrapper means **eligible for teaching**, not actual merchant commitment.

**Rejected:** `accepted=false`, **same object** `next == before`, named failing rule, **null candidate fact**. Defensive Evaluation constructor prevents a rejected result from carrying a success label, an accepted result with failed guards, and identity alteration. First-failure ordering is not a security permission protocol; public HTTP adapters must not expose private resource existence or sensitive guard details.

### State-only total decision table

| Previous merchant state | ACCEPT | REJECT |
|---|---|---|
| PENDING | Candidate ACCEPTED if every assumption passes | Candidate REJECTED if every assumption passes |
| ACCEPTED | Reject, unchanged, no fact | Reject, unchanged, no fact |
| REJECTED | Reject, unchanged, no fact | Reject, unchanged, no fact |

No physical merchant request is sent, merchant signature verified, state persisted, payment processed, or business fact published. Terminality is an **educational one-shot policy assumption**, not commercial approval. Real subsequent changes would require actor authentication, binding offer identity, policy owner approval and source evidence.

## PreparationGuardProbe — explicitly non-fulfillment

This pure evaluator has `Stage.NOT_STARTED/PREPARING/READY`, `START_PREPARING/COMPLETE_PREPARATION` and guards ORDER_MATCH, KITCHEN_AUTHORITY, MERCHANT_ACCEPTED, MERCHANT_EVIDENCE_ASSUMED, CURRENT_STAGE, POL_04_EDUCATIONAL_ONLY. The six state/action pairs are:

| Stage | START_PREPARING | COMPLETE_PREPARATION |
|---|---|---|
| NOT_STARTED | Candidate PREPARING only under all assumptions | Rejected / unchanged |
| PREPARING | Rejected / unchanged | Candidate READY only under all assumptions |
| READY | Rejected / unchanged | Rejected / unchanged |

On rejection the before phase and next phase match and no candidate fact exists. Even an eligible result is an educational model that does **not** start or complete kitchen work. No kitchen module/storage, HTTP endpoint, or genuine merchant acknowledgement was introduced. POL-04 remains unresolved.

## Application and effect boundary

`PreviewRestaurantDecisionService.evaluate` returns explicit sealed `Eligible` / `Rejected` **preview outcomes**. No repository, external adapter, Spring endpoint, event publisher, or write occurs. `EXPECTED_STATE` is not versioned transactional compare-and-swap: concurrent actors can evaluate stale PENDING inputs. Cross-lifecycle composition, source authentication, transaction commits, distributed retries and full policy gates belong to later justified changes.

Actual separate `PlaceOrderService.handle` returns `OrderRequestRecorded` **only after** the volatile OrderRepository save returns normally; that fact describes local intake, not merchant acceptance or durability. The existing HTTP 201/422 contract remains unchanged.

## Verification obligations and future changes

The [historical checkpoint](../../checkpoints/LESSON_1_1_7.md) pins Java source/CI and table-derived tests. Current [failure evidence](../verification/FAILURE_EVIDENCE_MATRIX.md) separates pure-model proof from real fault injection. For any future modification of these guards: recheck authority and policy source; update total transition table and positive/negative JUnit tests; preserve identical-identity/no-success-fact rejection; explain commit/version limitations. No new business policy may be inferred from local model labels. See [current architecture](../architecture/CURRENT_STATE.md).
