import { useEffect, useState } from "react";
import { api, formatMoney } from "../api/client";
import type { Goal } from "../api/types";
import "./Savings.css";

export default function Savings() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [amount, setAmount] = useState(20);
  const [toast, setToast] = useState("");
  const [busy, setBusy] = useState(false);

  async function load() {
    const g = await api.getGoals();
    setGoals(g);
  }

  useEffect(() => {
    load().catch(() => {});
  }, []);

  async function contribute() {
    if (!goals.length || amount <= 0) return;
    setBusy(true);
    try {
      const r = await api.contribute(goals[0].goalId, amount * 100);
      setToast(`✓ Saved! New balance ${formatMoney(r.savedMinor, goals[0].currency)}.`);
      await load();
      setTimeout(() => setToast(""), 4000);
    } catch (e) {
      setToast((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h3 className="section-title">Savings Centre</h3>

      {goals.map((g) => {
        const pct = Math.round((g.progress ?? 0) * 100);
        return (
          <div className="goal-item" key={g.goalId}>
            <div className="goal-head">
              <strong>🎯 {g.name}</strong>
              <span className="goal-pct">{pct}%</span>
            </div>
            <div className="goal-bar">
              <div className="goal-fill" style={{ width: `${pct}%` }} />
            </div>
            <div className="goal-sub">
              <span>
                {formatMoney(g.savedMinor, g.currency)} of {formatMoney(g.targetMinor, g.currency)}
              </span>
              <span className="streak">{g.status === "reached" ? "reached 🎉" : g.goalType}</span>
            </div>
          </div>
        );
      })}

      <div className="card">
        <h4>Add to your goal</h4>
        <p className="hint">Good day? Pop a little into your jar.</p>
        <div className="row">
          <input
            type="number"
            min={1}
            value={amount}
            onChange={(e) => setAmount(parseInt(e.target.value, 10) || 0)}
          />
          <button className="btn btn-primary" onClick={contribute} disabled={busy}>
            {busy ? <span className="spinner" /> : `Save R${amount}`}
          </button>
        </div>
        {toast && <p className="toast">{toast}</p>}
      </div>
    </>
  );
}
