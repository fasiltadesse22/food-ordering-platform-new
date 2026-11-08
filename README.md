# Food Ordering Platform — Engineering Evolution Project

A **fresh-start, production-oriented Java/Spring Boot application** that evolves through the [Distributed Systems Engineering Academy](https://github.com/fasiltadesse22/distributed-systems-engineering-academy) curriculum.

## Governing roadmap

[Master Roadmap V1.0](https://github.com/fasiltadesse22/distributed-systems-engineering-academy/blob/main/docs/roadmap/MASTER_ROADMAP_V1.md) is authoritative. The two repositories are separate and independently versioned; lessons must point to immutable platform commits and executed evidence, not just branch names.

## Engineering principles

- Begin with a real, runnable Food Ordering application, a minimal coherent business workflow, tests, and CI, **not** pre-created microservices.
- Model Order, Restaurant, Payment and related actors/invariants; do not let the learning focus collapse to only one domain.
- Introduce PostgreSQL, distributed service boundaries, Kafka, Redis, resilience, outbox/inbox, CQRS or Saga only when an appropriate requirement, failure window or experiment justifies them.
- Preserve fragile states, predict before implementing, compare simpler alternatives, measure outcomes, and record ADRs and removal triggers.
- Only describe tests and performance as *verified/measured* when they actually ran in a known environment.

## Status

**Repository planning baseline only.** Java application code, build, CI and experiments will be added from scratch when the first relevant lessons and application checkpoint are executed. This README is not evidence that a runnable application already exists.
