# ADR-0001 — Cloud provider: Azure

**Status:** Accepted · **Date:** 2026-10-25 · **Deciders:** Architecture, Cloud, Security

## Context
We need a cloud platform offering managed Kubernetes, strong GenAI capabilities, enterprise security/compliance, and good multi-region data-residency support for market-by-market expansion. The brief states Azure is preferred.

## Options considered
1. **Azure** — AKS, Azure OpenAI, AI Vision/Speech/Translator, Sentinel, strong compliance, Entra ID.
2. **AWS** — mature, broad, but GenAI (Bedrock) and the integrated SIEM/identity story differ.
3. **GCP** — strong data/ML, smaller enterprise-compliance footprint in some target markets.

## Decision
Adopt **Azure** as the primary cloud. It directly satisfies the required services (App Service, Azure SQL, Azure OpenAI, AI Vision, Key Vault, Azure Monitor, Sentinel), offers integrated identity (Entra ID + Managed Identities) that underpins our Zero-Trust design, and has the compliance certifications fintech requires.

## Consequences
- **Positive:** cohesive security/identity story; first-class GenAI; integrated SIEM (Sentinel); managed data services with geo-replication for DR and residency.
- **Negative / risk:** cloud-provider lock-in. **Mitigation:** containerised workloads on Kubernetes, vendor adapters ([ADR-0005](0005-vendor-abstraction.md)), IaC, and open standards (OCI, OpenTelemetry) keep portability reasonable.
- Team needs Azure expertise; standardise on Bicep/Terraform.

## Related
[Cloud Architecture](../11-cloud-architecture.md) · [Security](../09-security-architecture.md)
