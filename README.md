# MoneyMate — AI Financial Confidence Assistant

> A secure, scalable, cloud-native fintech platform that improves financial inclusion and financial confidence for people with irregular incomes, limited financial knowledge, or limited access to formal financial services.

**Tagline:** *Money that makes sense. Confidence that lasts.*

---

## How this design is organised

This repository is a complete, developer-ready solution design. Each document is self-contained but cross-referenced. Read in order for a full picture, or jump to the area you need.

| # | Document | What's inside |
|---|----------|---------------|
| 01 | [Product Vision](docs/01-product-vision.md) | Mission, vision, problem statement, success metrics |
| 02 | [User Personas](docs/02-user-personas.md) | 5 detailed personas across the target segments |
| 03 | [User Journeys](docs/03-user-journeys.md) | Budgeting, Saving, Scam Detection, Financial Learning |
| 04 | [Feature List & Roadmap](docs/04-feature-list.md) | Full feature catalogue, MVP / Phase 2 / Phase 3 |
| 05 | [System Architecture](docs/05-system-architecture.md) | C4 Context / Container / Component diagrams (Mermaid) |
| 06 | [Database Design](docs/06-database-design.md) | Full schema, PKs, FKs, indexes, ERD |
| 07 | [API Design](docs/07-api-design.md) | REST spec, payloads, responses, error handling |
| 08 | [AI Module Design](docs/08-ai-module-design.md) | Budget, education, scam models + MLOps + explainability |
| 09 | [Security Architecture](docs/09-security-architecture.md) | OWASP ASVS, Top 10, Zero Trust, ISO 27001 |
| 10 | [SSDLC Framework](docs/10-ssdlc-framework.md) | Secure SDLC with STRIDE threat modelling |
| 11 | [Cloud Architecture](docs/11-cloud-architecture.md) | Azure deployment architecture |
| 12 | [UI/UX Design](docs/12-ui-ux-design.md) | Design system, component library, screens |
| 13 | [DevOps Pipeline](docs/13-devops-pipeline.md) | CI/CD, GitHub Actions, Docker, Kubernetes |
| 14 | [Business Model](docs/14-business-model.md) | Lean Canvas, revenue, scaling, partnerships |
| 15 | [Hackathon Demo Kit](docs/15-hackathon-demo.md) | Demo script, flow, pitches (3/5/10 min) |
| 16 | [Flagship Differentiators](docs/16-differentiators.md) | AI Health Score, Voice-First, Community Savings Circles |
| — | [Architecture Decision Records](docs/adr/) | Key decisions with rationale and trade-offs |

---

## At a glance

- **Architecture style:** Cloud-native microservices on Azure (AKS + App Service), event-driven via Azure Service Bus.
- **Clients:** Offline-first mobile app (Flutter) + USSD/SMS fallback for feature phones.
- **AI:** Azure OpenAI for coaching/explanations, custom ML for budget forecasting & scam detection, on-device models for offline/low-data use.
- **Security:** Zero Trust, OWASP ASVS L2, encryption everywhere, full audit trail, ISO 27001-aligned controls.
- **Inclusion-first:** Multilingual (voice + text), low-data, offline mode, feature-phone reach.

## Design principles

1. **Inclusion before features** — every capability must work on a cheap phone, a slow connection, and in the user's language.
2. **Confidence over complexity** — plain-language guidance, no jargon, explainable AI.
3. **Safety by default** — the platform actively protects users from scams and bad decisions.
4. **Trust is earned** — transparent data use, user-controlled sharing, strong security.
5. **Cost-aware** — minimise user data cost and platform infra cost without sacrificing safety.

## Status

This is a solution design / blueprint intended to be realistically implementable by a fintech startup. It contains specifications, diagrams, and ADRs — not yet application source code.
