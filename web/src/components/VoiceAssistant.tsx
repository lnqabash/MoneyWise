import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, formatMoney } from "../api/client";
import "./VoiceAssistant.css";

// Minimal typing for the browser Web Speech API (not in TS DOM lib by default).
type SpeechRecognitionLike = {
  lang: string;
  interimResults: boolean;
  maxAlternatives: number;
  start: () => void;
  stop: () => void;
  onresult: ((ev: { results: Array<Array<{ transcript: string }>> }) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
};

const EXAMPLES = [
  "How much can I spend today?",
  "Check a scam",
  "What is my health score?",
];

export default function VoiceAssistant() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [listening, setListening] = useState(false);
  const [status, setStatus] = useState("Tap the mic and ask me something");
  const [heard, setHeard] = useState("");
  const [reply, setReply] = useState("");
  const recognizerRef = useRef<SpeechRecognitionLike | null>(null);

  function getRecognizer(): SpeechRecognitionLike | null {
    const w = window as unknown as {
      SpeechRecognition?: new () => SpeechRecognitionLike;
      webkitSpeechRecognition?: new () => SpeechRecognitionLike;
    };
    const Ctor = w.SpeechRecognition ?? w.webkitSpeechRecognition;
    return Ctor ? new Ctor() : null;
  }

  function speak(text: string) {
    if (!("speechSynthesis" in window)) return;
    const u = new SpeechSynthesisUtterance(text);
    u.lang = "en-ZA";
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(u);
  }

  function stopListening() {
    if (recognizerRef.current) {
      try {
        recognizerRef.current.stop();
      } catch {
        /* noop */
      }
      recognizerRef.current = null;
    }
    setListening(false);
  }

  function startListening() {
    const rec = getRecognizer();
    if (!rec) {
      setStatus("This browser doesn't support voice input. Tap an example below instead.");
      return;
    }
    rec.lang = "en-ZA";
    rec.interimResults = false;
    rec.maxAlternatives = 1;
    rec.onresult = (ev) => handleUtterance(ev.results[0][0].transcript);
    rec.onerror = () => {
      setStatus("I didn't catch that. Try again or tap an example.");
      stopListening();
    };
    rec.onend = () => stopListening();
    recognizerRef.current = rec;
    setListening(true);
    setStatus("Listening… ask me about your money");
    setHeard("");
    setReply("");
    rec.start();
  }

  async function handleUtterance(text: string) {
    stopListening();
    setHeard(`You said: \u201C${text}\u201D`);
    const t = text.toLowerCase();
    let answer = "";

    try {
      if (/(spend|afford|safe to spend|how much)/.test(t)) {
        const b = await api.getBudget();
        answer = `${b.explanation.summary} ${b.explanation.reasons[0] ?? ""}`;
      } else if (/(health|score|how am i doing)/.test(t)) {
        const h = await api.getHealthScore();
        answer = `Your financial health score is ${h.overallScore} out of 100 — ${h.band}. ${h.summary}`;
      } else if (/(scam|suspicious|fraud|check)/.test(t)) {
        answer = "Sure — opening the Shield so you can paste the message.";
        navigate("/shield");
      } else if (/(save|saving|goal|jar)/.test(t)) {
        const goals = await api.getGoals();
        if (goals.length) {
          const g = goals[0];
          answer = `You've saved ${formatMoney(g.savedMinor, g.currency)} of ${formatMoney(
            g.targetMinor,
            g.currency,
          )} towards ${g.name}. You're doing well!`;
        } else {
          answer = "You don't have a savings goal yet. Want to set one up?";
        }
        navigate("/save");
      } else {
        answer =
          "I can help with your budget, savings, scam checks, and your health score. Try asking how much you can spend today.";
      }
    } catch {
      answer = "Sorry, I couldn't reach your data just now.";
    }

    setReply(answer);
    speak(answer);
  }

  return (
    <>
      <button className="voice-fab" onClick={() => setOpen(true)} title="Ask MoneyMate">
        🎤
      </button>

      {open && (
        <div className="voice-panel">
          <div className="voice-sheet">
            <button
              className="voice-close"
              onClick={() => {
                setOpen(false);
                stopListening();
              }}
            >
              ×
            </button>
            <div className={`voice-orb ${listening ? "listening" : ""}`}>🎤</div>
            <p className="voice-status">{status}</p>
            {heard && <p className="voice-heard">{heard}</p>}
            {reply && <p className="voice-reply">{reply}</p>}
            <div className="voice-examples">
              <span>Try:</span>
              {EXAMPLES.map((e) => (
                <button key={e} className="voice-eg" onClick={() => handleUtterance(e)}>
                  {e}
                </button>
              ))}
            </div>
            <button className="btn btn-primary" onClick={startListening}>
              Start talking
            </button>
          </div>
        </div>
      )}
    </>
  );
}
