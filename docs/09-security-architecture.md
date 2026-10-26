# 09 — Security Architecture

Security is a product feature for MoneyWise — our users are high-value fraud targets with low margin for loss. The architecture follows **Zero Trust**, is designed to **OWASP ASVS Level 2** (L3 for the most sensitive flows), mitigates the **OWASP Top 10**, and aligns controls to **ISO/IEC 27001:2022** Annex A.

## Guiding principles
- **Never trust, always verify** — authenticate and authorise every request, service-to-service included.
- **Least privilege & segmentation** — minimal IAM, network micro-segmentation, per-service identities.
- **Assume breach** — encrypt everything, minimise blast radius, log immutably, detect fast.
- **Privacy by design** — minimise PII; what we don't hold can't leak.

---

## Zero Trust model

```mermaid
flowchart TB
    U[User device\n+ device attestation] -->|TLS 1.3| FD[Front Door + WAF]
    FD --> APIM[API Management\nOAuth2/OIDC verify, rate limit]
    APIM -->|mTLS + JWT| MESH[Service Mesh\n(mTLS, authZ policy)]
    subgraph Trust_Zone["No implicit trust — every hop verified"]
        MESH --> S1[Budget] 
        MESH --> S2[Savings]
        MESH --> S3[Scam]
        S1 -->|managed identity| KV[Key Vault]
        S2 -->|managed identity| DB[(Encrypted DB)]
    end
    SENT[Microsoft Sentinel\nSIEM/SOAR] -.-> FD
    SENT -.-> APIM
    SENT -.-> MESH
```

Pillars: verify identity (user + workload), validate device posture, enforce least-privilege per request, micro-segment the network, inspect & log continuously.

---

## Authentication
- **Users:** phone + OTP (OIDC); step-up MFA for sensitive actions (large withdrawals, consent changes). Optional biometric unlock on device. Refresh-token rotation with reuse detection; short-lived (15 min) access JWTs.
- **Workloads:** Azure Managed Identities / workload identity federation — **no secrets in code**. Service-to-service via **mTLS** in the mesh + signed JWT with audience scoping.
- **USSD/SMS:** MSISDN binding + session OTP for sensitive operations; network-level verification with telco.
- **Brute-force/enumeration defences:** OTP attempt lockout, device throttling, no user-existence disclosure.

## Authorization
- **RBAC + ABAC:** roles (user, agent, support, admin) plus attributes (ownership, consent scope, region). Enforced at gateway *and* service (defence in depth).
- **Ownership checks** on every resource (`user_id` must match token subject) — prevents IDOR / BOLA (OWASP API #1).
- **Policy-as-code** (OPA/Rego) for consistent, testable authZ decisions.
- **Consent-scoped data/AI access** — AI features only run where `consent_data_ai = true`.

## Encryption
| State | Control |
|---|---|
| In transit | TLS 1.3 external; mTLS internal; HSTS; cert rotation |
| At rest | AES-256 TDE on all stores; column-level encryption for PII |
| Keys | Azure Key Vault (HSM-backed), envelope encryption, automatic rotation, BYOK option |
| Secrets | Key Vault + Managed Identity; no secrets in repos/images; secret scanning in CI |
| Tokens | Signed (RS256) + short TTL; refresh rotation; revocation list in Redis |
| Crypto-shredding | Per-user PII keys enable right-to-erasure without ledger deletion |

## Monitoring & detection
- **SIEM/SOAR:** Microsoft Sentinel — correlation rules for auth anomalies, impossible travel, mass-enumeration, privilege escalation; automated SOAR playbooks (isolate token, block IP, page on-call).
- **Telemetry:** Azure Monitor + App Insights (traces/metrics/logs), structured logs with correlation/trace IDs.
- **Defender for Cloud** for posture & workload protection; container image scanning.
- **Anomaly detection** on transaction patterns (velocity, unusual withdrawal) feeding fraud rules.

## Audit trails
- **Immutable, append-only** audit store (write-once, TTL per retention policy) in Cosmos + export to WORM Blob.
- Logged events: auth, consent changes, money movements, scam actions, admin/support access, data exports/erasure, AI decisions (model version + inputs' hash).
- **Tamper-evidence:** hash-chaining of audit records; access to audit store is itself audited.
- Supports ISO 27001 evidence, incident forensics, and regulatory reporting.

## Fraud detection
- Real-time rules (velocity, geo/device anomaly, mule-pattern) at transaction time.
- ML risk scoring on money movements; step-up auth or hold on high-risk.
- **Scam Shield** (see [AI Design](08-ai-module-design.md#3-scam-detection-engine)) protects users from *external* fraud; this layer protects the *platform* from account-takeover and transactional fraud.
- Shared signals: crowd scam reports + fraud ML improve both.

---

## OWASP Top 10 (2021) mitigations

| Risk | Mitigation |
|---|---|
| A01 Broken Access Control | Ownership checks, OPA policies, deny-by-default, gateway+service enforcement |
| A02 Cryptographic Failures | TLS 1.3, AES-256, Key Vault HSM, no weak ciphers, column encryption |
| A03 Injection | Parameterised queries/ORM, input validation, output encoding, prompt-injection guardrails for AI |
| A04 Insecure Design | Threat modelling (STRIDE) in SSDLC, secure patterns, abuse cases |
| A05 Security Misconfiguration | Hardened images, IaC scanning, CIS benchmarks, no defaults |
| A06 Vulnerable Components | SCA (Dependabot/Snyk), SBOM, pinned deps, patch SLAs |
| A07 Auth Failures | OTP lockout, MFA step-up, token rotation+reuse detection, session mgmt |
| A08 Integrity Failures | Signed artifacts, image signing (Cosign), verified CI/CD, dependency pinning |
| A09 Logging/Monitoring Failures | Centralised logging, Sentinel alerting, immutable audit, trace IDs |
| A10 SSRF | Egress allow-lists, metadata-endpoint protection, URL validation in Scam Shield fetches |

### OWASP API Security notable
- **API1 BOLA** → per-object ownership checks; **API2** → strong OTP/JWT; **API4** → rate limiting + quotas; **API8** → config hardening; **API10** → safe third-party (telco/bank) consumption behind adapters.

## OWASP ASVS alignment (selected L2 controls)
- V2 Auth: MFA step-up, OTP rate-limits, secure recovery.
- V3 Session: short TTL, rotation, secure/HttpOnly where web, revocation.
- V4 Access Control: deny-by-default, ownership enforcement.
- V6 Crypto: approved algorithms, KMS, key rotation.
- V7 Logging: no sensitive data in logs, time sync, integrity.
- V9 Data Protection: PII minimisation, encryption, retention.
- V14 Config: hardening, secrets management.

## ISO/IEC 27001:2022 control mapping (sample)
| Annex A control | Implementation |
|---|---|
| A.5 Information security policies | ISMS, security steering, policy set |
| A.6 People / A.6.3 awareness | Secure-coding & phishing training |
| A.8 Asset & access management | RBAC/ABAC, IAM reviews, data classification |
| A.8.24 Cryptography | Key Vault, encryption standards |
| A.8.16 Monitoring | Sentinel, Azure Monitor |
| A.8.15 Logging | Immutable audit trail |
| A.5.7 Threat intelligence | Scam feeds, Sentinel TI |
| A.5.24–.28 Incident management | IR runbooks, SOAR, forensics |
| A.5.30 ICT readiness/continuity | Multi-zone, DR, backups |
| A.8.25–.29 Secure development | SSDLC, SAST/DAST, pentest |

## Compliance posture
- Data-protection regimes (GDPR / POPIA / local): lawful basis, consent management, DSR (access/erasure), DPIA for AI features, data residency in-region.
- Payments/fintech regulation handled per market (licensing, KYC/AML scaling with product phase; MVP uses KYC-lite + partner rails to limit regulatory surface).

---

**Related:** [SSDLC Framework](10-ssdlc-framework.md) · [Cloud Architecture](11-cloud-architecture.md) · [DevOps Pipeline](13-devops-pipeline.md)
