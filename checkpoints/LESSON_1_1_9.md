# Lesson 1.1.9 — Historical Java Application Composition Checkpoint

**Record status:** MERGED, independently post-merge verified.  
**Roadmap / scope:** Course 2, Master Roadmap V1; Cluster 1.1, Chapter 2, Lesson 1.1.9, execution class A.  
**Authority:** [Local composition engineering contract](../docs/engineering/LESSON_1_1_9_LOCAL_COMPOSITION_CONTRACT.md) and [current architecture](../docs/architecture/CURRENT_STATE.md). This checkpoint is immutable evidence of a specific implementation, not a replacement architecture specification.

## Baselines, branch and verified merge

- Academy baseline before execution: \`05ab302d1e2059d07c0f7585f0fd30e73211fce6\` (Lesson 1.1.8 frozen).
- Java baseline: \`081d1f1a877772e41d7a30ce0052e9609c71f6b5\` (last accepted single-deployable intake/guard governance revision).
- Feature branch: \`lesson/1.1.9-local-composition\`; feature tip \`0d9680fee97e00548e26af9fd6a9270df1f91f10\`.
- [Java PR #7](https://github.com/fasiltadesse22/food-ordering-platform-new/pull/7), squash-merged business-implementation SHA **\`c7d3e2d741883b1aba5fcd1e51f4275ef48344ea\`**.
- [Post-merge GitHub Actions CI 44915338911](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44915338911): **SUCCESS**, exact head \`c7d3e2d741883b1aba5fcd1e51f4275ef48344ea\`.

## Mechanism and scope

Added:
- \`application/simulation/LocalCompositionSimulation.java\`: a Spring application service anchored to already recorded OrderId and RestaurantId with a separately scoped PaymentAttemptId; independent merchant, payment, preparation, journey snapshots; explicit hypothetical policy and source evidence requirements; rejected/no-op behavior; volatile one-JVM \`ConcurrentHashMap.compute\` session update; append-only *local simulated* observation list.
- \`application/simulation/SimulatedPaymentPort.java\` and \`infrastructure/DeterministicLocalPaymentSimulator.java\`: deterministic CONFIRMED, DECLINED and RESPONSE_LOST presets for local simulation of capture/refund observations; **no actual payment provider or money movement**.
- \`LocalCompositionIntegrationTest.java\`: **13 new Spring integration tests** showing genuine UC-001 intake as a prerequisite, accepted-but-unpaid and captured-but-rejected composition, refund request/pending/unknown/confirmed teaching states, payment-after-cancellation rejection, cancellation-versus-kitchen work cutoff, unapproved policies fail closed, missing actor/source evidence, happy-path preparation/completion, repeated terminal decision and two-thread competing merchant decision.
- The [mechanism contract](../docs/engineering/LESSON_1_1_9_LOCAL_COMPOSITION_CONTRACT.md), current-state index and failure-evidence matrix updated to distinguish local execution from real-world effects.

No change to \`Order\`, \`OrderRepository\`, \`PlaceOrderService\`, \`OrderController\`, the synthetic menu, the existing 201/422 HTTP contract, or the originally approved single deployable. No public endpoint accepts forged merchant/provider evidence. POL-01–POL-10 remain UNRESOLVED; the selected TeachingPolicy is only a scoped classroom hypothesis, not an approved marketplace rule.

## Observed positive, negative and failure evidence

Pre-merge initial [CI 44915009919](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44915009919) ran 72 tests but **failed two new assertions**: an initial preparation command with capture-before-preparation selected failed the earlier CAPTURE_BEFORE_PREP guard, while tests incorrectly expected the later PREPARATION_GUARD. The correction preserved both guards and strengthened test ordering: assert capture prerequisite first, then after simulated capture assert merchant acceptance prerequisite. The corrected [feature CI 44915270300](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44915270300) passed.

The **post-merge** [CI 44915338911](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44915338911), Ubuntu/Temurin Java 21/Maven:
- \`python3 -m unittest discover -s scripts -p 'test_*.py' -v\`: passed governance fixture tests.
- \`python3 scripts/check_repository_governance.py\`: passed canonical links, documentation boundaries and limited import checks.
- \`mvn -B -ntp verify\`: **72 JUnit tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
- Packaged executable \`target/food-ordering-platform-0.1.0-SNAPSHOT.jar\` verified.
- Ran packaged Spring application on \`localhost:18080\`; live GET menu, POST order HTTP **201** with \`REQUESTED_AWAITING_RESTAURANT\`, \`PENDING\`, \`NOT_STARTED\` and synthetic quote 2500, followed by HTTP **422** for unavailable fixture item, all passed.

The fault response-loss preset proves only that the **local simulation** keeps SUBMITTED_UNKNOWN or REFUND_PENDING instead of falsely recording success. Two-thread merchant conflict proves only one winning process-local simulated decision for a single map key. The exercise did **not** inject a real provider timeout, crash after external effects, retry across processes, distributed race or durable recovery. The HTTP smoke proves genuine local intake, **not** a merchant/payment endpoint.

## Business invariants and deferred risks

Retained: strong identity ownership; create-only local intake; validation; quote from server-owned fixture; no false fact before successful save; request correlation not deduplication; rejected simulation preserving exact immutable prior snapshot without a success fact. Created a *separate* local learning state: it is not source-authoritative Order status, merchant commitment or real payment position.

Deferred: customer/merchant authentication, business policy approval, real provider interface, capture/refund financial identities, settlement, merchant/kitchen integrations, durable storage/versioned transitions, idempotency, cross-instance consistency, outbox/Saga, remote partial-failure evidence and delivery proof. [Failure evidence matrix](../docs/verification/FAILURE_EVIDENCE_MATRIX.md) explicitly classifies absent guarantees.

## Reconsideration and next dependency

The next lesson, 1.1.10, may analyze ownership and application module boundaries under its own blueprint, **but no part of 1.1.10 was implemented here**. This local, single-deployable composition supports later comparisons by making policy and source boundaries explicit without prematurely creating microservices.

**Freeze:** \`c7d3e2d741883b1aba5fcd1e51f4275ef48344ea\` is the immutable Java implementation checkpoint. A separate documentation-only PR records final evidence; its merge SHA and CI must be verified independently.
