import { useState } from "react";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import "./SignIn.css";

export default function SignIn() {
  const { signIn } = useAuth();
  const [phone, setPhone] = useState("+27830000001");
  const [challengeId, setChallengeId] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const [devHint, setDevHint] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleRequest() {
    setError("");
    setBusy(true);
    try {
      const r = await api.requestOtp(phone.trim());
      setChallengeId(r.challengeId);
      if (r.devCode) {
        setDevHint(`Demo mode: your code is ${r.devCode}`);
        setCode(r.devCode);
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function handleVerify() {
    setError("");
    setBusy(true);
    try {
      const r = await api.verifyOtp(challengeId!, code.trim());
      signIn(r.accessToken);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="phone">
      <div className="screen auth">
        <div className="brand">
          <div className="brand-logo">🪙</div>
          <h1>MoneyMate</h1>
          <p className="tagline">
            Money that makes sense.
            <br />
            Confidence that lasts.
          </p>
        </div>

        <div className="promises">
          <div>
            <span>📊</span>Plan your money
          </div>
          <div>
            <span>🎯</span>Save a little
          </div>
          <div>
            <span>🛡️</span>Stay safe from scams
          </div>
        </div>

        <div className="card">
          <label htmlFor="phone">Your phone number</label>
          <input
            id="phone"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            inputMode="tel"
          />
          {!challengeId && (
            <button className="btn btn-primary" onClick={handleRequest} disabled={busy}>
              {busy ? <span className="spinner" /> : "Send me a code"}
            </button>
          )}

          {challengeId && (
            <>
              <label htmlFor="code">Enter the 6-digit code</label>
              <input
                id="code"
                type="text"
                inputMode="numeric"
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="••••••"
              />
              {devHint && <p className="hint">{devHint}</p>}
              <button className="btn btn-primary" onClick={handleVerify} disabled={busy}>
                {busy ? <span className="spinner" /> : "Sign in"}
              </button>
            </>
          )}
          <p className="error">{error}</p>
        </div>

        <p className="demo-note">
          Demo account pre-seeded as <strong>Thandi</strong>, a gig driver.
        </p>
      </div>
    </div>
  );
}
