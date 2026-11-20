# Mandatory repository-first preflight — starting with Lesson 1.1.8

**This is an execution contract, not an invocation of future work.** Stop and report missing evidence; do not infer current runtime from a previous chat. Each repository is independently versioned.

## Required startup reading order

1. **Both repositories:** fetch latest `main` SHA, full tree, relevant open PRs and latest CI results; record dirty/active branches and potential conflicts. Do not assume SHA from previous sessions.
2. **Academy scope:** read `docs/roadmap/MASTER_ROADMAP_V1.md` (relevant phase/cluster/lesson), `docs/blueprints/CLUSTER_1_1_BLUEPRINT_V0_1.md`, `content/cluster-1.1/blueprint.json`, and `docs/contracts/DUAL_REPOSITORY_EXECUTION_CONTRACT_V1.md`.
3. **Business authority:** read academy `docs/domain/CLUSTER_1_1_LESSON_1_1_5_DECISION_BACKLOG.md`; POL-01–POL-10 unresolved until legitimately approved. Read `docs/progress/LEARNING_PROGRESS.md` for active lesson status, not historical candidates.
4. **Java architecture:** read `docs/architecture/CURRENT_STATE.md`, `README.md`, all currently applicable accepted `docs/adr/`, and relevant `docs/usecases/`.
5. **Continuity:** read immediate predecessor `checkpoints/LESSON_*.md` and relevant `docs/engineering/`, including the Java source behind any claim.
6. **Code and proof:** inspect affected `src/main/java/` and `src/test/java/`, `pom.xml`, `.github/workflows/verify.yml`, `docs/verification/FAILURE_EVIDENCE_MATRIX.md`, and latest immutable CI results. For academy changes inspect its own content audit, Vite/Playwright configuration and tests.
7. **Confirm prohibited work:** no distributed guarantees, merchant/payment/kitchen effects, new endpoints or policy approvals may be inferred from illustrative classes; no forced microservices and no premature future-lesson scope.

## Pre-implementation report — mandatory for every Java-changing lesson

| Field | Must state explicitly |
|---|---|
| Baselines | Fresh SHA of **both** main branches; reference lesson contract |
| Current implementation | Actual classes, endpoints, persistence and verified capabilities |
| Architecture | Applicable ADRs, owner boundary, dependency/transaction/deployment scope |
| Policy | Unresolved decision IDs and credible authority/evidence for any proposed approval |
| Files and scope | Concrete planned changes and expressly excluded future lessons |
| Invariants | Current API, identity, no-op rejection, local post-save fact and any new rule |
| Failure test plan | Normal path, negative path, relevant actual fault injection, recovery assertion and missing experiments |
| Readiness | Prior lessons/CI green? Any blocker, drift or unfrozen checkpoint? |

Before coding, stop if an essential policy is unresolved and the only route is inventing approval; choose an isolated simulation or defer. Do not promote Java `CandidateIllustrative...` labels to external effects.

## Definition of completed lesson

Changes go through **separate** PRs for each altered repo. Use the Java governance validator/unit tests/Maven suite and executable HTTP smoke, and academy TypeScript/unit/content/desktop Chromium if changed. Record exact PRs, commits, commands, outcomes and post-merge CI in concise historical checkpoint and one current status row. Treat “unknown / not implemented / simulated” as valid truthful results; never claim a distributed guarantee without a corresponding executed failure experiment.

See [document ownership](DOCUMENT_OWNERSHIP.md), [current state](../architecture/CURRENT_STATE.md), and [failure evidence](../verification/FAILURE_EVIDENCE_MATRIX.md).
