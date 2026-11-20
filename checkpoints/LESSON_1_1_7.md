# Lesson 1.1.7 — Guarded transitions: historical execution checkpoint

**Record status:** Completed, merged and independently verified on 2026-11-20. **This file records history**, not necessarily the newest Java `main` state. This checkpoint replaces an accidental full duplication of the technical contract, *without changing lesson execution facts*. Its original complete historical version remains at [pre-correction commit `46b02d28...`](https://github.com/fasiltadesse22/food-ordering-platform-new/blob/46b02d28f0acd4555d663e4361581f07b66c575d/checkpoints/LESSON_1_1_7.md).

| Field | Frozen value |
|---|---|
| Course/lesson | Master Roadmap V1 · Cluster 1.1 · Chapter 2 · **1.1.7** |
| Classification | **Type A** Java domain evolution paired with **Type D** academy |
| Engineering question | What makes a requested state transition legal in a particular state? |
| Java baseline | `79024c3c36fe1a8cc6f71b79505601894d927507` |
| Feature branch proof | `048b2322456951e6fc01b54889e851f96b8820d9`, [CI 44743299092](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44743299092) |
| Java PR and implementation merge | [PR #4](https://github.com/fasiltadesse22/food-ordering-platform-new/pull/4) · `8c5e5144b40adbd8679f4fe3f8046020b0279d2b` |
| Java post-merge acceptance | [CI 44745491071](https://github.com/fasiltadesse22/food-ordering-platform-new/actions/runs/44745491071) |
| Academy implementation and merge | [PR #7](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/pull/7) · `1708a94cf4a78c05b582cab9d86d1948ca62571f` |
| Academy post-merge proof | [unit/build CI 44746432638](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/actions/runs/44746432638), [Chromium CI 44746432701](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/actions/runs/44746432701) |
| Next dependency | Lesson **1.1.8**, **not started** at this freeze |

## Actual changes

Added pure `GuardCheck`, `GuardedRestaurantDecision` with eleven named checks, `PreparationGuardProbe` with six checks, and `PreviewRestaurantDecisionService` with explicit `Eligible`/`Rejected` candidate outcomes. Added `GuardedTransitionsTest` parameterized positive/negative tests. Kept `OrderController`, `PlaceOrderService`, existing HTTP contracts and process-local storage unchanged. The [technical contract](../docs/engineering/LESSON_1_1_7_GUARDED_TRANSITION_CONTRACT.md) is the canonical source for guard ordering, allowed/illegal transition tables and authority semantics.

## Executed verification

- Java GitHub Actions Ubuntu/Temurin 21, `mvn -B -ntp verify`: **59/59 JUnit tests** (including **25** new guarded-transition invocations); Maven BUILD SUCCESS; packaged runnable `target/food-ordering-platform-0.1.0-SNAPSHOT.jar`.
- `java -jar ... --server.port=18080`, then `curl`: HTTP **201** with `REQUESTED_AWAITING_RESTAURANT`, `PENDING`, `NOT_STARTED`, quoted cents 2500; HTTP **422** with `ITEM_UNAVAILABLE`.
- Academy: **69/69 unit/component tests**, 26-topic manuscript integrity, TypeScript, production build; **48/48 desktop Chromium** browser tests. No mobile-specific new acceptance.
- Source: [immutable Java implementation tree](https://github.com/fasiltadesse22/food-ordering-platform-new/tree/8c5e5144b40adbd8679f4fe3f8046020b0279d2b); [academy applied walkthrough](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/1708a94cf4a78c05b582cab9d86d1948ca62571f/docs/engineering/LESSON_1_1_7_APPLIED_WALKTHROUGH.md).

## Invariants, limitations and deferred work

A rejected candidate preserves the exact original state and emits no candidate success fact. Accepted result preserves typed identity under **explicit classroom assumptions**. No real merchant identity/authentication, kitchen effect, provider operation, database update, versioned compare-and-set, distributed guarantee or approved business-policy guard exists. POL-01–POL-10 remain **unresolved**, with the [academy register](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/domain/CLUSTER_1_1_LESSON_1_1_5_DECISION_BACKLOG.md) authoritative. Future scope includes effect reversibility, race/commit mechanisms and cross-lifecycle composition, only when its roadmap lesson requires them.

**Correction provenance:** This governance refactoring separated an identical pair of documents into one concise historical checkpoint and one technical contract. Neither change supplies new runtime behavior, modifies earlier commit evidence, or redefines what the 59 tests proved.
