# Documentation ownership and evidence hierarchy

**Status:** Repository governance rule, effective before Lesson 1.1.8. This document does not approve any business policy.

| File family | Canonical responsibility | What must NOT live here | Update trigger |
|---|---|---|---|
| `README.md` | Short current entry point, run commands, links | Full history, lesson journal | User-facing behavior, setup or linked index changes |
| `docs/architecture/CURRENT_STATE.md` | Current **descriptive** capabilities and explicit non-guarantees | Repeated historical transcripts or hidden new decisions | Any actual runtime capability/ownership change |
| `docs/adr/` | Accepted architecture choices and reconsideration triggers | Transient lesson log | Meaningful architecture decision; supersede rather than overwrite old ADR |
| `docs/engineering/` | Mechanics, invariants and authority/effect contract | Duplicate checkpoint/CI transcript | Mechanism or invariant changes |
| `docs/usecases/` | Public use-case/API and acceptance contract | Claims inferred from simulations | Public API/business use-case changes |
| `docs/verification/` | Tested failure guarantees, **negative** evidence and missing experiments | Generic assertions that “tests prove reliability” | New guarantee, fault harness or test results |
| `checkpoints/` | **Historical** lesson execution and immutable evidence | Duplicated full mechanism contracts | Lesson freeze; later correction by dated addendum with original evidence |
| Academy `docs/progress/LEARNING_PROGRESS.md` | One active status per lesson + clearly labeled historical chronology | Java runtime source of truth | Each lesson completion / correction |
| Academy `docs/domain/...DECISION_BACKLOG.md` | Sole authoritative POL-01–POL-10 candidate/approval status | Implicitly approved policy in Java docs | Legitimate stakeholder approval, evidence and version |

**Authority order is purpose-specific, not “the newest document wins.”** Java source/tests and executed CI prove *implemented behavior*. Approved ADRs constrain permissible *architecture*. Legitimately approved business policy governs *commercial rules*. The roadmap/blueprint governs *lesson scope*. A checkpoint proves what happened at a specified historical commit. `CURRENT_STATE.md` summarizes these; it does not override them.

On contradiction: identify both sources, reproduce actual code or official decision evidence, reconcile the current index/ledger, and record corrections with immutable old and new refs. **Never silently rewrite history to appear newer**.

### Before every future lesson checkpoint

1. Run the [session protocol](SESSION_STARTUP.md) on fresh repository SHAs.
2. Update current-state index **only when reality changed**; update README only if setup or public behavior changed.
3. Update mechanism contract only for changed mechanism; write new ADR only for meaningful decision.
4. Freeze short checkpoint with baseline SHA, merged SHA, commands, results, relevant failure injection and limits.
5. Update the academy's one authoritative active lesson status; label older candidate commentary `HISTORICAL`.
6. Run governance validator, Java suite, packaged HTTP smoke, academy tests if changed, and inspect post-merge CI before completion claims.

**Do not copy engineering documents into checkpoints.** Use exact relative links to canonical contracts. The historical original of the duplicated Lesson 1.1.7 checkpoint is preserved at [the pre-governance Java SHA](https://github.com/fasiltadesse22/food-ordering-platform-new/blob/46b02d28f0acd4555d663e4361581f07b66c575d/checkpoints/LESSON_1_1_7.md).
