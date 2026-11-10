# Lesson 1.1.4 — Stable Business Identity and Authoritative Local State

**Engineering question:** Which identities are stable and which component is entitled to determine a fact?  
**Classification:** A — verified real-platform evolution.  
**Prior baseline:** [225db684a24080f2c4ad1ae6e2f5e259edf0aa7d](https://github.com/fasiltadesse22/food-ordering-platform-new/tree/225db684a24080f2c4ad1ae6e2f5e259edf0aa7d).  
**Contract scope:** order-request intake in one Java/Spring Boot process; no live merchant/payment authority.

## 1. Current identity contract

| Identity | Type in Java | Meaning / legitimate source | Limit |
|---|---|---|---|
| Order | \`OrderId(UUID)\` | Generated for *one newly created order intake* by \`PlaceOrderService\`. Stable for every subsequent lookup of the same intake. | Repeating PlaceOrder remains a *second create*, with a different OrderId. No idempotency contract. |
| Customer reference | \`CustomerId(UUID)\` | Taken from request via the application command; validated as nonnull by domain constructors. | The client is NOT authenticated as this customer. |
| Restaurant reference | \`RestaurantId(String)\` | Resolved against the server-owned fictional catalog, keyed by its stable \`code\`. | Display name is not the ID; live merchant identity/availability is not established. |
| HTTP request/correlation | Existing \`X-Request-ID\` transport value | Identifies/correlates an HTTP occurrence. | Does not prove authorization, business-operation uniqueness, nor safe retry. |
| Payment attempt / provider reference | **Not implemented** | Conceptual future external/business identity. | No payment integration, provider callback, capture, refund, or authoritative outcome exists. |

**Typed domain model:** \`Order\` now stores \`OrderId orderIdentity\`, \`CustomerId customerIdentity\`, and \`RestaurantId restaurantIdentity\`. The existing raw \`id()/customerId()/restaurantId()\` accessors and raw-compatible constructor remain explicit adapters, so \`OrderController.OrderResponse\` continues to produce the exact same UUID/String JSON fields. \`PlaceOrderCommand\` remains a transport/application command with raw UUID and String; only after its conditions and catalog resolution are validated does \`PlaceOrderService\` construct typed identifiers for the domain. This is an intentional anti-corruption boundary, not a claim that a raw string is universally safe.

**Post-save fact:** \`OrderRequestRecorded\` contains typed identities internally and raw compatibility accessors. \`PlaceOrderOutcome.Recorded\` validates that OrderId, CustomerId and RestaurantId all match between the stored Order and its returned local fact; no fact is reported for a known business rejection. This does not create a new persisted event ID or event-sourced ledger.

## 2. State ownership and the meaning of a version

- \`Order\` is an immutable intake *snapshot*. No authorized subsequent update/revision of order status or payment state exists in this lesson.
- \`InMemoryOrderRepository\` alone owns the mutable \`ConcurrentHashMap<OrderId,Order>\`. The map is process-local and disappears after a restart.
- \`save(Order)\` is deliberately **create-only** and implemented using atomic \`putIfAbsent\`. An attempt to reuse an already-stored OrderId throws rather than silently overwriting a valid snapshot, including if the replacement has identical fields.
- A typed ID is stable as a business reference. It is not an authorization credential, optimistic lock, version number, idempotency key, history position or guarantee of durable identity across a crash.
- **No artificial version counter** or fake "update state" operation has been introduced. When a real update use case with business guards is defined in later lessons, the resulting new snapshot should keep OrderId stable while a tested revision protocol and legitimate state transition are introduced as warranted.
- \`putIfAbsent\` protects *one identical key within this one process*. It does not deduplicate different UUID IDs, does not provide multi-instance persistence and does not replace database uniqueness/locking.

A timestamp is not a state version. \`OrderRequestRecorded.observedAfterSaveAt\` records an observation using a wall clock, not a guaranteed monotonic ordering across clock corrections. It does not prove the repository write will survive a restart.

## 3. Mechanism and negative paths

\`\`\`text
HTTP PlaceOrderRequest
  → PlaceOrderCommand (raw customer UUID / restaurant code)
  → PlaceOrderService fixture validation and business decision
       ├─ reject: no Order / no OrderRequestRecorded
       └─ eligible: generate new OrderId
           → Order(typed OrderId, CustomerId, RestaurantId)
           → InMemoryOrderRepository.save() / putIfAbsent
             ├─ duplicate identity: exception; existing object unchanged
             ├─ unexpected failure after write: effect may be unknown
             └─ save returned normally: OrderRequestRecorded (typed identities)
               → OrderResponse (same JSON UUID and String fields)
               → GET /api/orders/{id} through the same OrderId
\`\`\`

For the normal HTTP path, a second identical POST is an independent create: the server generates a new OrderId before a separate \`putIfAbsent\`. That is **not** an accidental replacement of an existing entity. An endpoint acting on an *already identified* OrderId in a future approved update must preserve that ID rather than generating a replacement. There is no implemented idempotency protocol today.

## 4. Authority, external facts and projected status

- Local platform intake: the Java application can assert a process-local order record after normal return from its adapter.
- Restaurant acceptance: **PENDING**. The static restaurant fixture is not a merchant operating system and cannot assert acceptance.
- Payment: **NOT_STARTED**. PaymentAttemptId, provider operation ID, authorization, capture and settlement are unimplemented.
- A hypothetical payment status in a React model is an *educational projection*, not authority to say a provider settled funds. A provider receipt would have to be correlated to the correct provider operation and attempt, and its claim (authorization/capture/settlement) carefully interpreted.
- Names and aliases are not primary keys. Two real merchants with the same display name would need separate legitimate merchant identity and trust mapping; that future integration is not present.

## 5. Executable evidence and limits

New tests cover distinct ID values and types, validation, preservation of raw transport IDs, immutable snapshots, fact-reference consistency, create-only semantics, competing concurrent creates of a single ID, stable lookups, and two independent intake identities despite identical attributes. Existing tests continue covering 201/422, fixed-price provenance, no merchant/payment success, rejection, and duplicated POST without idempotency.

The repository's CI workflow runs \`mvn -B -ntp verify\`, checks the packaged jar and executes HTTP 201/422 smoke. A green CI run is evidence only for that immutable SHA and those input conditions. It cannot prove crash durability, provider identities or browser projection correctness.

## 6. Alternatives considered and triggers to revisit

| Choice | Why rejected/deferred | Reconsideration trigger |
|---|---|---|
| Use only raw UUID and String throughout | Allows accidental semantic ID swapping in domain logic | Typed domain identifiers introduced now |
| Convert every HTTP/application field to a new custom JSON type | Risks a broad breaking refactor without business value | After clients and serialization contracts justify it |
| Add a version field incremented on fabricated updates | A fake business transition would mislead learners about correctness | First approved update use case with concurrent decision requirements |
| Introduce payment attempt IDs in actual Java now | No payment attempt workflow/provider contract | Payment design approved with authority and legal state semantics |
| Use REST idempotency keys automatically | Current POST intentionally creates a distinct intake for each eligible invocation | Explicit deduplication semantics and tested retry requirements |
| Store authoritative facts in a local event list | Second uncoordinated mutable store adds false durability claims | Proven business demand for durable history and transactional consistency |

## 7. Next dependency

Lesson 1.1.5 is a **concept/analysis** workflow-story lesson and should use this identity map to describe partial ordering/restaurant/payment journeys without claiming those domains already execute. The Java Order state-machine, merchant authority, provider integration, and durable history are not part of Lesson 1.1.4.
