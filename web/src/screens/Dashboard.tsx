import { useEffect, useState } from "react";
import { api, formatMoney } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import type { Budget, HealthScore, Profile } from "../api/types";
import "./Dashboard.css";

function ringColour(score: number) {
  if (score >= 80) return "var(--mm-safe)";
  if (score >= 60) return "var(--mm-primary)";
  if (score >= 40) return "var(--mm-warn)";
  return "var(--mm-danger)";
}
function pillarColour(score: number) {
  if (score >= 70) return "var(--mm-safe)";
  if (score >= 40) return "var(--mm-warn)";
  return "var(--mm-danger)";
}

export default function Dashboard() {
  const { signOut } = useAuth();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [budget, setBudget] = useState<Budget | null>(null);
  const [health, setHealth] = useState<HealthScore | null>(null);
  const [showWhy, setShowWhy] = useState(false);
  const [showPillars, setShowPillars] = useState(false);

  useEffect(() => {
    api.getProfile().then(setProfile).catch(() => {});
    api.getBudget().then(setBudget).catch(() => {});
    api.getHealthScore().then(setHealth).catch(() => {});
  }, []);

  const name = profile?.displayName ?? "there";
  const greeting = profile?.preferredLanguage === "zu" ? "Sawubona" : "Hello";
  const today = new Date().toLocaleDateString(undefined, {
    weekday: "long",
    day: "numeric",
    month: "short",
  });

  return (
    <>
      <div className="appbar">
        <div>
          <h2>
            {greeting}, {name} 👋
          </h2>
          <span className="date">{today}</span>
        </div>
        <button className="avatar" onClick={signOut} title="Sign out">
          {name.charAt(0).toUpperCase()}
        </button>
      </div>

      {/* Hero safe-to-spend */}
      <div className="hero">
        <span className="hero-label">You can safely spend today</span>
        <div className="hero-amount">
          {budget ? formatMoney(budget.safeToSpendTodayMinor, budget.currency) : "…"}
        </div>
        <button className="chip" onClick={() => setShowWhy((v) => !v)}>
          Why this number?
        </button>
        {budget && (
          <span className="hero-conf">confidence {Math.round(budget.confidence * 100)}%</span>
        )}
      </div>

      {showWhy && budget && (
        <div className="why">
          <h4>Here's how we worked it out</h4>
          <ul>
            {budget.explanation.reasons.map((r, i) => (
              <li key={i}>{r}</li>
            ))}
          </ul>
          <span className="model-tag">Model: {budget.explanation.modelVersion}</span>
        </div>
      )}

      {/* Financial Health Score */}
      <h3 className="section-title">Your Financial Health Score</h3>
      {health && (
        <>
          <div className="health-card">
            <div
              className="health-ring"
              style={{
                background: `conic-gradient(${ringColour(health.overallScore)} ${Math.round(
                  (health.overallScore / 100) * 360,
                )}deg, var(--mm-line) 0deg)`,
              }}
            >
              <span className="health-num">{health.overallScore}</span>
            </div>
            <div style={{ flex: 1 }}>
              <span className="health-band">{health.band}</span>
              <p className="health-summary">{health.summary}</p>
              <button className="chip chip-ghost" onClick={() => setShowPillars((v) => !v)}>
                See the 4 pillars
              </button>
            </div>
          </div>

          {showPillars && (
            <div className="pillars">
              {health.pillars.map((p) => (
                <div className="pillar" key={p.code}>
                  <div className="pillar-top">
                    <span>{p.name}</span>
                    <span style={{ color: pillarColour(p.score), fontWeight: 800 }}>
                      {p.score}
                    </span>
                  </div>
                  <div className="pillar-bar">
                    <div
                      className="pillar-fill"
                      style={{ width: `${p.score}%`, background: pillarColour(p.score) }}
                    />
                  </div>
                  <div className="pillar-reason">{p.reason}</div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* Spending envelopes */}
      <h3 className="section-title">Your spending</h3>
      <div className="envelopes">
        {budget && budget.envelopes.length === 0 && (
          <p className="hint">No spending recorded yet.</p>
        )}
        {budget?.envelopes.map((e) => {
          const pct = e.allocatedMinor
            ? Math.min(100, (e.spentMinor / e.allocatedMinor) * 100)
            : 0;
          return (
            <div className="env" key={e.name}>
              <div className="env-top">
                <span>{e.name.charAt(0).toUpperCase() + e.name.slice(1)}</span>
                <span>
                  {formatMoney(e.spentMinor, budget.currency)} /{" "}
                  {formatMoney(e.allocatedMinor, budget.currency)}
                </span>
              </div>
              <div className="env-bar">
                <div className="env-fill" style={{ width: `${pct}%` }} />
              </div>
            </div>
          );
        })}
      </div>

      <div className="tip">
        <span>💡</span>
        <div>
          <strong>Tip of the day</strong>
          <br />
          Saving on good days beats forcing it on slow ones.
        </div>
      </div>
    </>
  );
}
