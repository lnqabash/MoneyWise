# ADR-0003 — Polyglot persistence

**Status:** Accepted · **Date:** 2026-10-25 · **Deciders:** Architecture, Data

## Context
Different data has different shapes and consistency needs: financial ledgers require ACID and strong integrity; scam reports and learning content are flexible, high-volume documents; sessions and safe-to-spend need low-latency caching; media and ML artifacts need object storage.

## Options considered
1. **Single relational DB for everything** — simple ops, but awkward for flexible docs and high-write audit/scam data; scaling pressure on one store.
2. **Single document DB** — flexible, but weaker fit for strict financial ACID and relational reporting.
3. **Polyglot persistence** — right tool per job.

## Decision
Use **polyglot persistence**: Azure SQL/PostgreSQL for financial/relational core (Users, Income, Expenses, Budgets, Goals, Transactions, Notifications); Cosmos DB for documents (ScamReports, LearningProgress/content, audit); Redis for cache/sessions; Blob/ADLS for media, model artifacts, and the analytics lake.

## Consequences
- **Positive:** ACID where money lives; flexible, horizontally scalable storage for documents/audit; fast cache; cheap object/lake storage.
- **Negative:** more operational surface and skills; cross-store consistency handled via events + sagas (eventual consistency outside the relational core). Clear ownership: each service owns its store.
- Monetary values stored as integer minor units everywhere to avoid float errors.

## Related
[Database Design](../06-database-design.md)
