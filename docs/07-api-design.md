# 07 — API Design

RESTful HTTP/JSON over TLS 1.3, versioned at `/v1`. Resource-oriented, idempotent writes, cursor pagination, and a consistent error envelope. An OpenAPI 3.1 skeleton is included at the end. gRPC is used internally (service↔AI Engine); REST is the public/client contract.

**Base URL:** `https://api.moneymate.app/v1`

## Conventions

- **Auth:** OAuth2 + OIDC; short-lived JWT access tokens (15 min) + rotating refresh tokens; `Authorization: Bearer <jwt>`.
- **Idempotency:** mutating POSTs accept `Idempotency-Key` header (stored in `transactions.idempotency_key`).
- **Localisation:** `Accept-Language` header drives response copy and AI explanation language.
- **Low-data:** supports `Prefer: return=minimal`, gzip/brotli, and `fields=` sparse fieldsets.
- **Pagination:** `?limit=&cursor=`; responses carry `nextCursor`.
- **Money:** `{ "amountMinor": 42000, "currency": "ZAR" }` (integer minor units).
- **Rate limiting:** per-user & per-IP (gateway); `429` with `Retry-After`.

### Standard error envelope
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "amountMinor must be a positive integer",
    "traceId": "0af7651916cd43dd8448eb211c80319c",
    "details": [{ "field": "amountMinor", "issue": "must_be_positive" }]
  }
}
```

### Error codes
| HTTP | code | When |
|---|---|---|
| 400 | VALIDATION_ERROR | Malformed/invalid payload |
| 401 | UNAUTHENTICATED | Missing/expired token |
| 403 | FORBIDDEN | Authn OK, not authorised (policy) |
| 404 | NOT_FOUND | Resource missing / not owned |
| 409 | CONFLICT / IDEMPOTENT_REPLAY | Duplicate, version conflict |
| 422 | BUSINESS_RULE | e.g., withdrawal would overdraw |
| 429 | RATE_LIMITED | Throttled |
| 500 | INTERNAL | Unexpected |
| 503 | DEPENDENCY_UNAVAILABLE | AI/partner down → degraded mode |

---

## 1. Authentication APIs

Phone-first (OTP) auth suited to users without email.

### `POST /auth/otp/request`
```json
// request
{ "phone": "+27831234567", "channel": "sms" }
// 202 Accepted
{ "challengeId": "chg_8f2...", "expiresIn": 300 }
```

### `POST /auth/otp/verify`
```json
// request
{ "challengeId": "chg_8f2...", "code": "481923", "device": { "id": "dev_123", "platform": "android" } }
// 200 OK
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "rt_9a...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "userId": "usr_01H...",
  "isNewUser": true
}
// 401 { "error": { "code": "UNAUTHENTICATED", "message": "Invalid or expired code" } }
```

### `POST /auth/token/refresh`
```json
{ "refreshToken": "rt_9a..." }  // -> new access+refresh (rotation); old refresh revoked
```

### `POST /auth/logout`  → `204 No Content` (revokes refresh token / device).

---

## 2. Profile APIs

### `GET /profile`
```json
// 200
{
  "userId": "usr_01H...",
  "displayName": "Thandi",
  "preferredLanguage": "zu",
  "preferredChannel": "app",
  "currency": "ZAR",
  "incomePattern": "daily",
  "personaSegment": "gig_worker",
  "consent": { "dataForAi": true, "marketing": false }
}
```

### `PATCH /profile`
```json
// request (partial)
{ "preferredLanguage": "zu", "consent": { "dataForAi": true } }
// 200 -> updated profile   | 400 VALIDATION_ERROR
```

### `GET /profile/consents` · `PUT /profile/consents`
Manage granular data/AI/marketing consents (GDPR/POPIA-aligned). `PUT` returns `200` with an immutable audit entry id.

### `DELETE /profile`  (right to erasure)
`202 Accepted` → async crypto-shred; returns `{ "requestId": "...", "status": "scheduled" }`.

---

## 3. Budget APIs

### `POST /income`  (record income → triggers re-forecast)
Headers: `Idempotency-Key`
```json
{ "amountMinor": 42000, "currency": "ZAR", "source": "gig", "capture": "voice", "isBusiness": false, "receivedOn": "2026-10-25" }
// 201
{ "incomeId": "inc_...", "reforecastQueued": true }
```

### `POST /expenses`  (record spend)
```json
{ "amountMinor": 8500, "currency": "ZAR", "category": "transport", "spentOn": "2026-10-25" }
// 201 { "expenseId": "exp_...", "envelopeId": "env_transport", "remainingMinor": 15500 }
// 422 { "error": { "code": "BUSINESS_RULE", "message": "This exceeds your transport envelope" } }
```

### `GET /budget/current`
```json
// 200
{
  "budgetId": "bud_...",
  "strategy": "dynamic_envelope",
  "safeToSpendTodayMinor": 12000,
  "currency": "ZAR",
  "forecastAt": "2026-10-25T06:00:00Z",
  "confidence": 0.82,
  "envelopes": [
    { "name": "bills", "allocatedMinor": 60000, "spentMinor": 20000 },
    { "name": "food", "allocatedMinor": 40000, "spentMinor": 18000 },
    { "name": "goals", "allocatedMinor": 15000, "spentMinor": 15000 }
  ],
  "explanation": {
    "summary": "You can safely spend R120 today.",
    "reasons": [
      "Your income this week is 15% below your 8-week average",
      "R600 of bills are due in the next 5 days",
      "You're on track for your Car Repair goal"
    ],
    "modelVersion": "budget-fc-2.3.1"
  }
}
```

### `GET /budget/forecast?horizonDays=14`
Returns predicted income/expense and lean-period warnings (Smart Spending Predictions).
```json
{ "horizonDays": 14, "predictedIncomeMinor": 180000, "predictedExpenseMinor": 165000,
  "leanPeriods": [{ "from": "2026-11-02", "to": "2026-11-06", "shortfallMinor": 22000 }],
  "confidence": 0.74 }
```

---

## 4. Savings APIs

### `POST /goals`
```json
{ "name": "School fees", "goalType": "school", "targetMinor": 200000, "currency": "ZAR",
  "targetDate": "2027-01-15", "rule": { "type": "good_day", "amountMinor": 2000 } }
// 201 { "goalId": "goal_...", "status": "active", "weeklyTargetMinor": 15000 }
```

### `GET /goals` · `GET /goals/{goalId}`
```json
{ "goalId": "goal_...", "name": "School fees", "targetMinor": 200000, "savedMinor": 46000,
  "progress": 0.23, "streakWeeks": 4, "status": "active", "softLocked": true }
```

### `PATCH /goals/{goalId}`  (edit rule/target/pause)
### `POST /goals/{goalId}/contributions`  (manual save)
```json
{ "amountMinor": 2000, "source": "manual" }  // Idempotency-Key
// 201 { "txnId": "txn_...", "savedMinor": 48000 }
```

### `POST /goals/{goalId}/withdrawals`  (friction + guard)
```json
{ "amountMinor": 10000, "reason": "emergency", "confirmSoftLock": true }
// 200 { "txnId": "txn_...", "savedMinor": 38000, "warning": "This sets back your goal by ~3 weeks" }
// 422 { "error": { "code": "BUSINESS_RULE", "message": "Confirm soft-lock override to proceed" } }
```

### Emergency fund
### `GET /emergency-fund`
```json
{ "goalId": "goal_ef", "savedMinor": 90000, "monthlyExpensesMinor": 85000, "coverageMonths": 1.06,
  "status": "building", "recommendation": "Aim for 3 months (R2,550) coverage." }
```

---

## 5. Scam Scanner APIs

### `POST /scam/scan`  (text / URL)
```json
{ "channel": "whatsapp", "content": "You won R5000! Click http://b1t.ly/claim to verify your bank PIN", "language": "en" }
// 200
{
  "reportId": "scam_...",
  "riskLevel": "high",
  "riskScore": 0.96,
  "redFlags": [
    { "code": "URGENCY", "explanation": "Pressures you to act immediately" },
    { "code": "CREDENTIAL_REQUEST", "explanation": "Asks for your bank PIN — banks never do this" },
    { "code": "SUSPICIOUS_URL", "explanation": "Shortened link hides the real destination" }
  ],
  "advice": { "summary": "Do not click. This is almost certainly a scam.",
    "actions": ["block_sender", "report_to_telco", "warn_contacts"] },
  "modelVersion": "scam-cls-1.8.0"
}
```

### `POST /scam/scan-image`  (screenshot OCR, multipart/form-data)
Field `image` (jpg/png, <5MB) → same verdict shape; runs OCR then classifier.

### `POST /scam/{reportId}/action`
```json
{ "action": "reported" }  // records user_action -> feeds analytics/retraining
// 200 { "status": "recorded" }
```

### `GET /scam/reports?limit=20&cursor=`  → user's past checks (paginated).

---

## 6. Education APIs

### `GET /learning/path`  (personalised)
```json
{ "pathId": "path_core_01", "language": "sw",
  "modules": [
    { "moduleId": "m_budget_basics", "title": "Budgeting on uneven income", "durationSec": 150,
      "status": "completed", "mediaUrl": ".../audio/sw/m1.mp3", "offlineAvailable": true },
    { "moduleId": "m_spot_scam", "title": "Spotting a scam", "durationSec": 120,
      "status": "in_progress", "trigger": "scam_event" }
  ] }
```

### `POST /learning/{moduleId}/complete`
```json
{ "score": 90, "actionTaken": "created_goal" }
// 200 { "status": "completed", "nextModuleId": "m_emergency_fund", "streak": 3 }
```

### `GET /learning/assessment` · `POST /learning/assessment`
Baseline/periodic adaptive quiz; voice-answerable (`answers` accept text or transcribed speech).

---

## Voice & USSD endpoints

### `POST /voice/intent`
```json
{ "audioRef": "blob://.../utt123.ogg", "language": "zu" }
// 200
{ "intent": "budget.safe_to_spend", "entities": {}, "spokenResponse": "Namuhla ungasebenzisa u-R120 ngokuphepha.",
  "ssmlUrl": ".../tts/zu/resp123.mp3", "navigateTo": "/budget/current" }
```

### `POST /ussd/session`  (telco webhook)
```json
{ "sessionId": "ussd_...", "msisdn": "+27831234567", "input": "1*2", "serviceCode": "*134#" }
// 200 { "message": "Savings\n1. Save R20\n2. Check balance\n0. Back", "continue": true }
```

---

## Cross-cutting

- **Health/observability:** `GET /health` (liveness), `GET /ready` (readiness), `GET /metrics` (Prometheus, internal).
- **Webhooks (partners):** signed (HMAC) inbound for mobile-money events; verified at gateway.
- **Degraded mode:** when AI Engine is unavailable, budget/scam endpoints return `503 DEPENDENCY_UNAVAILABLE` with a cached/heuristic fallback payload and `"degraded": true`.

## OpenAPI 3.1 skeleton

```yaml
openapi: 3.1.0
info: { title: MoneyMate API, version: "1.0.0" }
servers: [{ url: https://api.moneymate.app/v1 }]
security: [{ bearerAuth: [] }]
components:
  securitySchemes:
    bearerAuth: { type: http, scheme: bearer, bearerFormat: JWT }
  schemas:
    Money:
      type: object
      required: [amountMinor, currency]
      properties:
        amountMinor: { type: integer, minimum: 0 }
        currency: { type: string, minLength: 3, maxLength: 3 }
    Error:
      type: object
      properties:
        error:
          type: object
          properties:
            code: { type: string }
            message: { type: string }
            traceId: { type: string }
paths:
  /auth/otp/request: { post: { summary: Request OTP } }
  /auth/otp/verify: { post: { summary: Verify OTP, returns tokens } }
  /income: { post: { summary: Record income } }
  /budget/current: { get: { summary: Current budget & safe-to-spend } }
  /goals: { get: { summary: List goals }, post: { summary: Create goal } }
  /scam/scan: { post: { summary: Scan text/URL for scams } }
  /learning/path: { get: { summary: Personalised learning path } }
```

---

**Related:** [System Architecture](05-system-architecture.md) · [Database Design](06-database-design.md) · [Security](09-security-architecture.md)
