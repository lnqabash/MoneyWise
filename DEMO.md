# MoneyWise — Live Demo Script (~5 min)

A tight walkthrough of the working app. The hero is **Thandi**, a gig driver with irregular income. Everything below is live against the running backend — no slides needed.

---

## Setup (before you present)

1. Start the backend:
   ```
   cd backend
   mvn spring-boot:run
   ```
   Wait for `Started MoneyMateApplication` (~15s).
2. Open **http://localhost:8080/** in a browser (Chrome/Edge for the voice feature).
3. Leave the sign-in screen showing. The demo account (Thandi) is pre-seeded.

> One-liner if asked what it is: *"MoneyWise is an AI financial confidence assistant for people with irregular incomes — it budgets, saves, teaches, and protects them from scams, in their language, even offline."*

---

## The walkthrough

### 1. Sign in (15s)
- Phone is pre-filled. Tap **Send me a code** → the code auto-fills (demo mode) → **Sign in**.
- **Say:** "Thandi signs in with just her phone number — no email, no paperwork. That matters for people outside the formal banking system."

### 2. Dashboard — the hero number (45s)
- Point to **"You can safely spend today"**.
- Tap **Why this number?**
- **Say:** "Instead of a rigid monthly budget — useless when your income swings daily — MoneyWise gives one honest number: what's safe to spend today. And it explains itself in plain language. That transparency is how we build trust with users who've been burned before."

### 3. Financial Health Score (30s)
- Point to the score ring → tap **See the 4 pillars**.
- **Say:** "This is our alternative to a credit score. It rewards healthy behaviour — saving, resilience, stability — not debt history. So someone with no bank record can still build, and see, their financial standing."

### 4. Add an expense — AI auto-categorisation (30s)
- In **Add an expense**, type `MTN airtime` and an amount → **Add expense**.
- **Say:** "She just describes it — 'MTN airtime' — and the AI files it automatically. No menus, no forms. The dashboard updates instantly."

### 5. Scam Shield — the showstopper (60s)
- Go to **🛡️ Shield** tab. A scam message is pre-filled. Tap **Check this message**.
- Point to the red verdict and the **red flags**.
- **Say:** "This is the part users love. A suspicious message gets checked in seconds, and crucially we show *why* it's dangerous — asks for your PIN, fake urgency, hidden link. We don't just block it, we teach her to spot the next one. Our users are the most targeted by fraud and have the least to lose."

### 6. Savings Circles — the differentiator (60s)
- Go to **👥 Circles** tab → **Create** (auto-adds two members and starts it) → open the circle.
- Point out the **locked payout** message.
- Tap **"I'll pay my R200"** (the pot fills as all members pay).
- Now tap **Pay out the pot**.
- **Say:** "This digitises the stokvel — the savings groups millions already trust. Everyone sees the pot. And the payout is locked until everyone has paid in — no organiser can run off with the money. We keep the social trust, and remove the risk."

### 7. Voice assistant — inclusion (30s)
- Tap the **🎤** button → tap the example **"What is my health score?"** (or speak it).
- **Say:** "For users who can't read well or type easily, voice is the whole app. She just asks, and MoneyWise answers out loud — in her language."

### 8. Close (15s)
- **Say:** "Budgeting, saving, learning, scam protection, community savings — built to work on a cheap phone, in any language, even offline. MoneyWise: money that makes sense, confidence that lasts."

---

## If asked / backup

- **"Is the AI real?"** — "The scam detection and budgeting run on explainable on-device logic that works offline; the architecture plugs into Azure OpenAI for richer coaching when online."
- **"Does it really work offline / on a basic phone?"** — "Yes — the design is offline-first with a USSD/SMS channel for feature phones. Core actions never need a live connection."
- **"How do you make money?"** — "Free core forever; premium power-users, B2B2C licensing, and ethical partner products — never by exploiting low-income users."
- **If the voice mic fails** (browser/mic issues): use the example buttons instead — they run the same thing without a microphone.
- **If the network drops mid-demo**: the sign-in and data are local; keep going.

## Fast reset between demos
- Sign out (top-right avatar) and sign back in. To fully reset data, restart the backend (H2 is in-memory, so it re-seeds fresh).
