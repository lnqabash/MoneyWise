# ADR-0002 — Microservices (with modular-monolith MVP)

**Status:** Accepted · **Date:** 2026-10-25 · **Deciders:** Architecture

## Context
The brief specifies a microservices architecture (Budget, Savings, Learning, Scam Detection, Notification, Analytics, AI Engine, etc.). We also need to ship an MVP quickly with a small team and avoid premature operational complexity.

## Options considered
1. **Full microservices from day one** — matches the target state; high early ops overhead.
2. **Modular monolith first, split later (strangler)** — fast MVP, clean module boundaries, extract services as scale/teams grow.
3. **Monolith** — fastest, but doesn't meet independent-scaling needs (AI/scam workloads scale differently).

## Decision
Target-state is **microservices, event-driven** via Service Bus. For MVP we implement a **modular monolith with strict module boundaries** aligned to the target services, deployed behind the API gateway, then **extract the high-divergence services first** (AI Engine, Scam Detection — different language/scaling/GPU needs) using the strangler pattern.

## Consequences
- **Positive:** fast MVP, lower early ops cost, clear path to microservices without rework; independent scaling where it matters most (AI/scam) early.
- **Negative:** discipline required to keep module boundaries clean so extraction is cheap; ArchUnit-style tests and clear domain boundaries enforced.
- Event-driven contracts defined up front so extraction doesn't change integration semantics.

## Related
[System Architecture](../05-system-architecture.md)
