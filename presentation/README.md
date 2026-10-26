# MoneyWise — Investor Pitch Deck

A self-contained, 20-slide HTML presentation. No build step, no internet, no dependencies — open and present.

## Run it
Open `presentation/index.html` in any modern browser (Chrome/Edge recommended).

## Controls
| Key | Action |
|---|---|
| → / Space / Page Down | Next slide |
| ← / Page Up | Previous slide |
| Home / End | First / last slide |
| 1–9 | Jump to that slide |
| **N** | Toggle **speaker notes** panel |
| **F** | Fullscreen (presentation mode) |
| swipe | Next/previous (touch devices) |

On-screen: the nav pill (bottom-right) has prev/next, a slide counter, and a **Notes** button. Progress bar runs along the top.

## Speaker notes
Every slide has executive-level speaker notes plus source/citation references. Press **N** (or the Notes button) to show them below the slide — handy for rehearsal or a presenter screen.

## Export to PDF
`Ctrl/Cmd + P` → **Save as PDF**. The print stylesheet renders one slide per page and includes the speaker notes. Set layout to **Landscape** for best results.

## Design
- Microsoft Fluent-inspired, blue/teal/white palette.
- All infographics are drawn in CSS (fishbone, funnel, flow, score ring, charts, matrix, ecosystem) — no image files, so nothing can break or fail to load.
- Accessibility: semantic landmarks, `aria` labels, keyboard navigation, focus management, high-contrast text, and `prefers-reduced-motion` support.

## A note on figures
Market sizes (TAM/SAM/SOM) and the revenue/cost numbers are **illustrative models for the competition**, grounded where possible in public sources (World Bank Findex, GSMA). The speaker notes flag the assumptions — be ready to discuss them if asked.

## 20 slides
1. Title · 2. Problem · 3. Why it matters · 4. Personas · 5. Root-cause (fishbone) · 6. Market opportunity · 7. Introducing MoneyWise · 8. How it works · 9. Core features · 10. AI differentiation · 11. Security &amp; SSDLC · 12. Architecture · 13. Business model · 14. Revenue · 15. Costs · 16. Impact &amp; KPIs · 17. Roadmap · 18. Competitive advantage · 19. Future vision · 20. Closing.
