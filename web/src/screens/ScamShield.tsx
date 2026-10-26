import { useState } from "react";
import { api } from "../api/client";
import type { ScanResult } from "../api/types";
import "./ScamShield.css";

const ACTION_LABELS: Record<string, string> = {
  block_sender: "Block sender",
  report_to_telco: "Report",
  warn_contacts: "Warn contacts",
  verify_sender: "Verify sender",
  do_not_share_details: "Don't share details",
  stay_alert: "Stay alert",
};

const ICONS = { high: "🚫", medium: "⚠️", low: "✅" } as const;
const LABELS = { high: "High risk", medium: "Be careful", low: "Looks okay" } as const;

export default function ScamShield() {
  const [content, setContent] = useState(
    "You won R5000! Click http://b1t.ly/claim to verify your bank PIN immediately",
  );
  const [result, setResult] = useState<ScanResult | null>(null);
  const [done, setDone] = useState<Set<string>>(new Set());
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function scan() {
    if (!content.trim()) return;
    setBusy(true);
    setError("");
    setDone(new Set());
    try {
      const r = await api.scan(content.trim());
      setResult(r);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function act(action: string) {
    if (!result) return;
    try {
      await api.recordScamAction(result.reportId, action);
      setDone((prev) => new Set(prev).add(action));
    } catch {
      /* ignore in demo */
    }
  }

  return (
    <>
      <h3 className="section-title">🛡️ Scam Shield</h3>
      <div className="card">
        <p className="hint">
          Got a suspicious message? Paste it here and we'll check it together — no judgement.
        </p>
        <textarea
          rows={4}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Paste the message or link here…"
        />
        <button className="btn btn-primary" onClick={scan} disabled={busy}>
          {busy ? <span className="spinner" /> : "Check this message"}
        </button>
        {error && <p className="error">{error}</p>}
      </div>

      {result && (
        <>
          <div className={`verdict ${result.riskLevel}`}>
            <div className="verdict-badge">
              {ICONS[result.riskLevel]} {LABELS[result.riskLevel]} · score{" "}
              {Math.round(result.riskScore * 100)}%
            </div>
            <div className="verdict-summary">{result.advice.summary}</div>
          </div>

          {result.redFlags.length > 0 && (
            <div className="flags">
              {result.redFlags.map((f) => (
                <div className="flag" key={f.code}>
                  <div className={`flag-code ${result.riskLevel}`}>{f.code}</div>
                  {f.explanation}
                </div>
              ))}
            </div>
          )}

          {result.advice.actions.length > 0 && (
            <div className="actions">
              {result.advice.actions.map((a) => (
                <button
                  key={a}
                  className={`action-btn ${done.has(a) ? "done" : ""}`}
                  onClick={() => act(a)}
                >
                  {done.has(a) ? "✓ " : ""}
                  {ACTION_LABELS[a] ?? a}
                </button>
              ))}
            </div>
          )}
          <p className="model-tag" style={{ marginTop: 8 }}>
            Model: {result.modelVersion}
          </p>
        </>
      )}
    </>
  );
}
