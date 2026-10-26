# 02 — User Personas

Five personas spanning the target segments. Each represents a cluster of real behaviours and constraints that drive product and architecture decisions. Technology comfort uses a 1–5 scale (1 = feature phone only / low digital literacy, 5 = power smartphone user).

---

## Persona 1 — Thandi, the Gig Driver

| | |
|---|---|
| **Name** | Thandi Nkosi |
| **Occupation** | Ride-hailing & delivery driver (multi-app: Bolt, Uber, local courier) |
| **Age** | 29 |
| **Location** | Peri-urban township, Johannesburg |
| **Income pattern** | Daily cash + digital payouts; highly variable (R150–R900/day); peaks on weekends, drops when car needs repairs |
| **Devices** | Mid-range Android (3-year-old), prepaid data, often <500MB left by month-end |
| **Tech comfort** | **4/5** — app-savvy, lives in ride-hailing apps, but distrusts "banking" apps |

**Pain points**
- Income swings wildly day to day; never sure how much she can spend or save.
- Fuel, data, and airtime eat earnings before she notices.
- Got burned by a fake "driver bonus" WhatsApp link last year.
- Data runs out; apps that need constant connection are useless by the 25th.

**Goals**
- Smooth out the "good day / bad day" rollercoaster.
- Build a cushion for car repairs (her #1 income risk).
- Stop losing money to small invisible leaks.

**How MoneyMate helps:** Daily-income budgeting, "safe-to-spend today" number, auto round-up micro-savings into a Car Repair goal, Scam Shield on WhatsApp links, offline mode for month-end.

---

## Persona 2 — Carlos, the Freelance Creative

| | |
|---|---|
| **Name** | Carlos Mendes |
| **Occupation** | Freelance graphic designer & videographer |
| **Age** | 34 |
| **Location** | Mid-size city, works remotely for international clients |
| **Income pattern** | Project-based; large irregular lump sums (sometimes nothing for 6 weeks, then a $2,000 invoice); multi-currency |
| **Devices** | Modern smartphone + laptop; good home Wi-Fi, mobile data when out |
| **Tech comfort** | **5/5** — early adopter, comfortable with fintech, skeptical of AI "black boxes" |

**Pain points**
- Feast-or-famine cash flow; blows through lump sums, then scrambles.
- No idea how much to set aside for tax and lean months.
- Generic budgeting apps assume a monthly salary and break on his reality.
- Wants to understand *why* the app recommends something, not just be told.

**Goals**
- Convert lumpy income into a stable "personal salary."
- Automatically provision for tax and quiet periods.
- Grow an emergency fund and start investing.

**How MoneyMate helps:** Lump-sum smoothing (AI allocates a windfall across weeks/goals), tax & emergency-fund envelopes, explainable AI showing the reasoning, spending predictions for lean months.

---

## Persona 3 — Grace, the Informal Trader

| | |
|---|---|
| **Name** | Grace Achieng |
| **Occupation** | Market stall vendor (vegetables & household goods) |
| **Age** | 46 |
| **Location** | Open-air market, Nairobi |
| **Income pattern** | Daily cash takings; blends business and personal money; seasonal (festivals = boom) |
| **Devices** | Entry-level Android *and* an old feature phone she trusts more; uses mobile money (M-Pesa) heavily |
| **Tech comfort** | **2/5** — confident with mobile money & calls, nervous with apps and typing |

**Pain points**
- Business and household money are one pot — no idea if the stall is profitable.
- Literacy is limited; English-only apps are a wall.
- Targeted by "double your money" savings-group scams.
- Connectivity at the market is patchy; data is a real cost.

**Goals**
- Separate business cash from household spending.
- Save for stock during the slow season.
- Understand money without having to read a lot.

**How MoneyMate helps:** Voice Assistant in Swahili, picture/voice expense capture, business-vs-personal tagging, offline-first with SMS/USSD, Scam Shield for savings-group fraud, audio literacy lessons.

---

## Persona 4 — Maria, the Domestic Worker

| | |
|---|---|
| **Name** | Maria Santos |
| **Occupation** | Domestic worker / house cleaner (multiple households) |
| **Age** | 52 |
| **Location** | Suburban, commutes from informal settlement |
| **Income pattern** | Semi-regular — several employers pay weekly/monthly cash; occasional missed or delayed payment |
| **Devices** | Basic smartphone given by daughter; shared family data plan |
| **Tech comfort** | **2/5** — uses WhatsApp and calls; intimidated by forms and numbers |

**Pain points**
- Sends money to family; little left and no savings.
- Doesn't trust banks; keeps cash at home (risky).
- Reading small text and English instructions is hard.
- Afraid of "doing something wrong" in an app and losing money.

**Goals**
- Put a little aside safely each week for her grandchildren's school fees.
- Simple, reassuring guidance she can trust.
- Avoid scams that target older, less tech-savvy users.

**How MoneyMate helps:** Large-type accessible UI, voice-first flows in Portuguese/local language, tiny automatic micro-saves, School Fees goal with gentle reminders, strong reassurance & undo, Scam Shield.

---

## Persona 5 — Sipho, the Seasonal & Low-Income Household Earner

| | |
|---|---|
| **Name** | Sipho Dlamini |
| **Occupation** | Seasonal farm worker + odd construction jobs in off-season |
| **Age** | 38 |
| **Location** | Rural district, travels for work |
| **Income pattern** | Highly seasonal — strong harvest-season income, near-zero off-season; supports a 5-person household |
| **Devices** | Feature phone primarily; borrows a smartphone occasionally; very weak rural signal |
| **Tech comfort** | **1/5** — comfortable with calls, SMS, basic USSD menus only |

**Pain points**
- Earns in bursts for a few months, then nothing — must stretch savings over the off-season.
- Lives where data and signal barely reach.
- Can't use smartphone apps reliably; needs USSD/SMS.
- No buffer for emergencies (medical, funerals) — one shock is a crisis.

**Goals**
- Make harvest income last through the dry months.
- Build any emergency buffer at all.
- Access everything without a smartphone or data.

**How MoneyMate helps:** Full USSD/SMS experience, seasonal-income smoothing plan, automated off-season drawdown schedule, Emergency Fund Tracking, SMS-based literacy tips and scam warnings.

---

## Persona coverage matrix

This ensures the product serves the full spectrum, not just the easy (high-tech) users.

| Capability | Thandi | Carlos | Grace | Maria | Sipho |
|---|:--:|:--:|:--:|:--:|:--:|
| Smartphone app | ● | ● | ◐ | ◐ | ○ |
| USSD / SMS | ◐ | ○ | ● | ◐ | ● |
| Voice-first | ◐ | ○ | ● | ● | ◐ |
| Multilingual | ◐ | ◐ | ● | ● | ● |
| Offline mode | ● | ◐ | ● | ◐ | ● |
| Explainable AI | ◐ | ● | ◐ | ◐ | ○ |
| Scam protection | ● | ◐ | ● | ● | ● |

● primary need  ◐ secondary  ○ minimal

The matrix drives a hard requirement: **no core journey may depend on a smartphone or an online connection.**

---

**Related:** [User Journeys](03-user-journeys.md) · [UI/UX Design](12-ui-ux-design.md)
