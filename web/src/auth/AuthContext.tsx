import { createContext, useContext, useState, type ReactNode } from "react";
import { getToken, setToken } from "../api/client";

interface AuthState {
  isAuthenticated: boolean;
  signIn: (accessToken: string) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authed, setAuthed] = useState<boolean>(!!getToken());

  const signIn = (accessToken: string) => {
    setToken(accessToken);
    setAuthed(true);
  };
  const signOut = () => {
    setToken(null);
    setAuthed(false);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated: authed, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
