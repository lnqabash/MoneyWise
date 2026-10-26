import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import SignIn from "./screens/SignIn";
import AppShell from "./components/AppShell";
import Dashboard from "./screens/Dashboard";
import Savings from "./screens/Savings";
import ScamShield from "./screens/ScamShield";
import Learn from "./screens/Learn";

export default function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="*" element={<SignIn />} />
      </Routes>
    );
  }

  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/save" element={<Savings />} />
        <Route path="/shield" element={<ScamShield />} />
        <Route path="/learn" element={<Learn />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}
