# ADR-0006 — Explainable AI as a product requirement

**Status:** Accepted · **Date:** 2026-10-25 · **Deciders:** Architecture, AI, Product, Security

## Context
Our users have low financial literacy, low trust in institutions, and high scam exposure. An AI that says "spend R120" or "blocked" with no reason will not be trusted — and for a scam verdict, understanding *why* is itself the protective lesson. Regulators also increasingly expect explainability for automated financial decisions.

## Options considered
1. **Black-box models, score-only outputs** — simplest, highest accuracy ceiling, lowest trust.
2. **Post-hoc explanations only when asked** — better, but trust must be built proactively.
3. **Explainability as a first-class, always-present output** — every AI decision carries plain-language reasons in the user's language.

## Decision
Treat **explainability as a functional requirement**, not an add-on. Budget and scam models emit structured reasons (SHAP feature importances, rule-flag hits) that an explainability layer renders into plain-language, localised explanations via the LLM. Allocation logic uses an auditable constrained-optimisation policy rather than a black box. Guidance carries disclaimers; regulated-advice edge cases hand off to humans.

## Consequences
- **Positive:** user trust, better behaviour change, regulatory alignment, a demo/product differentiator.
- **Negative:** constrains model choices toward explainable/auditable designs; added latency/cost for explanation generation (mitigated by caching + templates); templates must be maintained and localised.
- Makes "why?" a shippable feature across Budget Planner and Scam Shield.

## Related
[AI Module Design](../08-ai-module-design.md) · [Security](../09-security-architecture.md) · [UI/UX](../12-ui-ux-design.md)
