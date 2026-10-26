# ADR-0004 — Offline-first & feature-phone reach

**Status:** Accepted · **Date:** 2026-10-25 · **Deciders:** Architecture, Product, UX

## Context
Target users face low connectivity and expensive data; many use feature phones. A standard always-online smartphone app would exclude a large share of the intended audience — directly undermining the financial-inclusion mission. The persona coverage matrix mandates that no core journey depend on a smartphone or live connection.

## Options considered
1. **Online-only smartphone app** — simplest to build; excludes Grace/Maria/Sipho.
2. **Online app + thin SMS notifications** — partial; still excludes feature-phone-only users from core actions.
3. **Offline-first app + full USSD/SMS parity + on-device models** — maximum reach, higher complexity.

## Decision
Build **offline-first**: local store + sync queue, on-device models (budgeting heuristic + scam-lite classifier), low-data/delta sync, and **full USSD/SMS parity** for MVP money actions. Core journeys degrade gracefully offline.

## Consequences
- **Positive:** genuine inclusion; resilience to poor networks; lower user data cost; differentiator.
- **Negative:** sync/conflict complexity (last-writer-wins for user entities, server authority for balances); two client surfaces (app + USSD); on-device model lifecycle to manage.
- Drives event-driven, idempotent APIs and a clear online/offline capability contract.

## Related
[System Architecture](../05-system-architecture.md) · [UI/UX](../12-ui-ux-design.md) · [AI Module Design](../08-ai-module-design.md)
