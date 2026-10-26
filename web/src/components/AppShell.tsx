import { useState, type ReactNode } from "react";
import { NavLink } from "react-router-dom";
import VoiceAssistant from "./VoiceAssistant";
import "./AppShell.css";

export default function AppShell({ children }: { children: ReactNode }) {
  const [offline, setOffline] = useState(false);

  return (
    <div className="phone">
      <div className="statusbar">
        <span>9:41</span>
        <button
          className={`conn ${offline ? "offline" : ""}`}
          onClick={() => setOffline((o) => !o)}
          title="Toggle to show the offline-first story"
        >
          ● {offline ? "offline" : "online"}
        </button>
      </div>

      <div className="screen">{children}</div>

      <VoiceAssistant />

      <nav className="bottom-nav">
        <NavLink to="/" end className="nav-item">
          <span>🏠</span>Home
        </NavLink>
        <NavLink to="/save" className="nav-item">
          <span>🎯</span>Save
        </NavLink>
        <NavLink to="/shield" className="nav-item">
          <span>🛡️</span>Shield
        </NavLink>
        <NavLink to="/learn" className="nav-item">
          <span>📚</span>Learn
        </NavLink>
      </nav>
    </div>
  );
}
