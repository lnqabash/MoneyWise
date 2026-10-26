# ADR-0005 — Abstract external vendors behind adapters

**Status:** Accepted · **Date:** 2026-10-25 · **Deciders:** Architecture

## Context
MoneyWise depends on external providers that vary by market and over time: telcos/USSD gateways, mobile-money and bank APIs, LLM provider, threat-intel feeds, notification channels. Expanding market-by-market means swapping providers without rewriting core logic. There's also cloud lock-in risk from [ADR-0001](0001-cloud-provider-azure.md).

## Decision
Isolate every external dependency behind a **port/adapter (hexagonal)** boundary with a stable internal interface. Core services depend on the port, not the concrete vendor. Provide per-market/per-vendor adapter implementations selected via configuration.

Examples: `AiCompletionPort` (Azure OpenAI adapter today), `MessagingPort` (telco A/B adapters), `MobileMoneyPort` (M-Pesa, bank adapters), `ThreatIntelPort`.

## Consequences
- **Positive:** market expansion and vendor swaps without core changes; testability (mock adapters); reduced lock-in; parallel per-market rollout.
- **Negative:** small upfront abstraction cost; must avoid leaking vendor-specific concepts through the port.
- Enables the vendor-neutral claims in [ADR-0001](0001-cloud-provider-azure.md)'s lock-in mitigation.

## Related
[System Architecture](../05-system-architecture.md) · [Cloud Architecture](../11-cloud-architecture.md)
