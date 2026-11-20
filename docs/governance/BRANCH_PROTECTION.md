# Branch protection — verified status and owner setup

**Observed on 2026-11-20:** GitHub `branches/main` API reported `protected=false` and `rulesets` API returned an empty array for **both** `fasiltadesse22/food-ordering-platform-new` and `fasiltadesse22/distributed-systems-engineering-academy`. These are live observed results, not historical assumptions.

**Not applied:** The connected GitHub tool exposes repository/ruleset reads and code/PR writes, **not repository administration or branch protection mutation**. No protection was enabled by this governance exercise. The owner must configure the rule through GitHub UI (or an admin-authorized tool) and verify it afterward.

## Required owner action (each repository independently)

Open repository **Settings → Rules → Rulesets** (or **Settings → Branches → Add branch protection rule**, depending on UI). Create an active branch ruleset targeting `main`:

- Require pull request before merging; restrict force pushes and branch deletion.
- Require successful status checks before merging, and require branch up-to-date where practical.
- For Java repo select the GitHub Actions job check from **Verify Food Ordering Platform / java** (check exact provider-reported check name when configuring).
- For academy repo select **Academy V0.1 verification / Content, TypeScript, Simulation, Production build** and **Academy browser smoke / browser** (check exact reported check names).
- Decide explicitly whether administrators can bypass; prefer no bypass for application code, if repository permissions support it.
- Avoid requiring approvals you cannot fulfill if a solo-maintainer project would be deadlocked; configure practical review policy deliberately.

**Verification after owner action:** Re-query GitHub `GET /repos/{owner}/{repo}/branches/main` and `GET /repos/{owner}/{repo}/rulesets`; inspect actual required checks. Until then, merge discipline remains **manual**, and no completion report may claim enforced branch protection. Governance CI runs and PR usage are not equivalent to enforcement.

## Separate repositories, separate gates

Do not require Java CI to fetch mutable academy `main`, nor require both repositories to share commit state. Pin immutable implementation SHAs as *evidence*, not an artificial synchronized transaction. New work begins with [mandatory session preflight](SESSION_STARTUP.md).
