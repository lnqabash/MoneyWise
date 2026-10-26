# MoneyMate Backend — thin vertical slice

A runnable Spring Boot (Java 17) **modular monolith** implementing the first end-to-end slice of the [MoneyMate design](../docs): phone+OTP auth, dynamic budgeting with an explainable safe-to-spend, savings goals, and the explainable Scam Shield.

Architecture follows [ADR-0002](../docs/adr/0002-microservices-vs-monolith.md): one deployable, strict module boundaries (`profile`, `budget`, `savings`, `scam`) that can later be extracted into microservices.

## What's implemented

| Module | Endpoints | Notes |
|---|---|---|
| `profile` | `POST /v1/auth/otp/request`, `POST /v1/auth/otp/verify`, `GET/PATCH /v1/profile` | Phone+OTP, JWT issuance |
| `budget` | `POST /v1/income`, `POST /v1/expenses`, `GET /v1/budget/current` | Volatility-aware, explainable safe-to-spend |
| `savings` | `POST/GET /v1/goals`, `GET /v1/goals/{id}`, `POST /v1/goals/{id}/contributions` | Goals, weekly targets, contributions |
| `scam` | `POST /v1/scam/scan`, `POST /v1/scam/{id}/action` | Rule-based explainable detection; raw content hashed, never stored |
| `health` | `GET /v1/health-score` | ⭐ AI Financial Health Score — 4 explainable pillars (see [Differentiators](../docs/16-differentiators.md)) |
| `ledger` | `GET /v1/transactions` | Immutable, idempotent money ledger — single source of truth for all movements |
| `circles` | `POST /v1/circles`, `/{id}/members`, `/{id}/start`, `/{id}/contributions`, `/{id}/payout`, `GET /v1/circles` | ⭐ Community Savings Circles (ROSCA stokvels) with transparent ledger + guarded payout |

**More Phase 2/3 logic features (built + verified):**
- **Smart Spending Predictions** — `GET /v1/budget/forecast?horizonDays=14` projects income/expense and flags lean weeks, with explanations.
- **Lump-Sum Smoothing** — `POST /v1/budget/smooth` turns a windfall into a steady weekly "personal salary" (slices to emergency + goals).
- **Expense auto-categorisation** — `POST /v1/expenses` infers a category from a free-text `note` when `category` is omitted/"auto".
- **Credit-Readiness** — `GET /v1/credit-readiness` derives a consent-based readiness tier from behaviour (health score + ledger activity). Explicitly a guide, not a credit decision.

**External-service adapters (ports, per [ADR-0005](../docs/adr/0005-vendor-abstraction.md)):**
The core depends on ports with offline stub implementations, so the app runs fully without external accounts; dropping in a real provider is a bean/config change, not new core code.
- `adapters/messaging` — `MessagingPort` (SMS/WhatsApp). OTP now flows through it (stub logs; real adapter = Twilio/Africa's Talking/telco).
- `adapters/ai` — `AiCompletionPort` (LLM generation/translation). Stub returns grounded text; real adapter = Azure OpenAI.
- `adapters/mobilemoney` — `MobileMoneyPort` (M-Pesa/bank). Stub reports "not wired" so money is never falsely claimed to move externally.

**Phase 2 — Community Savings Circles:** digitised stokvels/chamas. An organiser creates a circle; members join (each gets a rotation position); the circle starts; everyone contributes a fixed amount per cycle; the pot pays out to the scheduled rotation member. **Safety:** payout is server-guarded — it's blocked until every active member has contributed, and it always pays the scheduled recipient, so no single party (even the organiser) can divert funds. All money moves through the central ledger. Three demo members are seeded (Thandi +27830000001, Grace +27830000002, Maria +27830000003).

**Tier 1 (fully-functional) additions:**
- **Transactions ledger** — income, expenses, save sweeps, and withdrawals all post real, idempotent ledger rows (`app.moneymate.ledger`).
- **Life-events** — recording income publishes `IncomeRecordedEvent`, which triggers a budget re-forecast (in-process stand-in for Azure Service Bus).
- **Savings rules engine** — `POST /v1/goals/auto-save/run` (and a weekly scheduler) run affordability-aware auto-saves: skip/reduce on lean weeks, full on good weeks.
- **Withdrawals + emergency fund** — `POST /v1/goals/{id}/withdrawals` (soft-lock friction), `GET /v1/emergency-fund` (coverage months).
- **Auth hardening** — refresh tokens with rotation + reuse detection (`POST /v1/auth/token/refresh`, `POST /v1/auth/logout`) and OTP request rate limiting.

The **web demo** (served at `/`) also includes a ⭐ **voice-first assistant** (browser Web Speech API) that answers "how much can I spend?", "what's my health score?", and routes scam checks by voice.

The budget and scam engines are the **on-device / fallback models** from [AI Module Design](../docs/08-ai-module-design.md) — they run with no LLM or trained model, so the slice works offline and demos anywhere. The cloud ML models plug in behind the same contracts later.

## Run it (no Docker needed)

Requires JDK 17 + Maven (both already on this machine). Uses an embedded H2 database and seeds the "Thandi" demo persona.

```bash
cd backend
mvn spring-boot:run
# or: mvn -DskipTests package && java -jar target/moneymate-backend.jar
```

- API docs (Swagger UI): http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:mem:moneymate`, user `sa`, no password)
- Health: http://localhost:8080/actuator/health

## Run with PostgreSQL (Docker)

```bash
cd backend
docker compose up --build
```

This runs the `docker` profile against PostgreSQL 16. Same Flyway migrations, same API.

## Try the full demo flow

The OTP is returned in the response (`devCode`) only because `moneymate.otp.expose-otp=true` in dev — in production it's delivered by SMS via the telco adapter.

```bash
# 1. Request OTP for the seeded demo user
curl -s -X POST localhost:8080/v1/auth/otp/request \
  -H 'Content-Type: application/json' -d '{"phone":"+27830000001"}'
# -> { "challengeId": "...", "expiresIn": 300, "devCode": "123456" }

# 2. Verify to get a JWT
curl -s -X POST localhost:8080/v1/auth/otp/verify \
  -H 'Content-Type: application/json' \
  -d '{"challengeId":"<id>","code":"<devCode>"}'
# -> { "accessToken": "<JWT>", ... }

# 3. Safe-to-spend with explanation
curl -s localhost:8080/v1/budget/current -H 'Authorization: Bearer <JWT>'

# 4. Scam Shield (the showstopper)
curl -s -X POST localhost:8080/v1/scam/scan -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"channel":"whatsapp","content":"You won R5000! Click http://b1t.ly/claim to verify your bank PIN","language":"en"}'
# -> high risk + explainable red flags + advice
```

## Design notes / deliberate scope of the slice

- **Money** is stored as integer **minor units** (cents) everywhere — no floats.
- **Privacy:** scam message content is **hashed (SHA-256), never persisted** ([Security](../docs/09-security-architecture.md)).
- **Security:** stateless JWT, per-request ownership via `CurrentUser.id()`. Refresh-token rotation, MFA step-up, and mTLS between services are specified in the docs and come in the next iteration.
- **Not yet built (next up):** event bus (life-events), refresh tokens, the transactions ledger table writes, USSD/SMS channel, the real AI/LLM integration, and the Flutter client. These are stubbed at the seams (see `// Real impl:` comments).

## Tests

```bash
mvn test
```

Covers the scam detection engine (phishing → high, investment scam → flagged, benign → low).

## Project layout

```
src/main/java/app/moneymate
├── MoneyMateApplication.java      # entry point
├── DemoDataSeeder.java            # "Thandi" persona seed (profile: seed)
├── common/                        # error envelope, exceptions
│   └── security/                  # JWT, filter, security config, CurrentUser
├── profile/                       # users, OTP auth, profile
├── budget/                        # income, expenses, safe-to-spend calculator
├── savings/                       # goals + contributions
├── scam/                          # explainable rule-based Scam Shield
├── health/                        # AI Financial Health Score (4 pillars)
├── ledger/                        # immutable transactions ledger
├── circles/                       # Community Savings Circles (ROSCA)
└── common/events/                 # in-process life-events (income.recorded)
src/main/resources
├── static/                        # web demo (index.html, app.js, styles.css)
├── application.yml                # default (H2 + seed)
├── application-docker.yml         # PostgreSQL profile
└── db/migration/V1__initial_schema.sql
```
