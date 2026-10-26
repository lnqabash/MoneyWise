// Thin fetch wrapper for the MoneyMate API. Token is held in memory and
// mirrored to sessionStorage so a refresh keeps you signed in during a session.

import type {
  Budget,
  ContributionResponse,
  Goal,
  HealthScore,
  OtpRequestResponse,
  Profile,
  ScanResult,
  TokenResponse,
} from "./types";

const TOKEN_KEY = "mm_token";

let token: string | null = sessionStorage.getItem(TOKEN_KEY);

export function setToken(t: string | null) {
  token = t;
  if (t) sessionStorage.setItem(TOKEN_KEY, t);
  else sessionStorage.removeItem(TOKEN_KEY);
}

export function getToken() {
  return token;
}

async function request<T>(
  path: string,
  options: { method?: string; body?: unknown; auth?: boolean } = {},
): Promise<T> {
  const { method = "GET", body, auth = true } = options;
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (auth && token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`/v1${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const message = data?.error?.message ?? `Request failed (${res.status})`;
    throw new Error(message);
  }
  return data as T;
}

export const api = {
  // Auth
  requestOtp: (phone: string) =>
    request<OtpRequestResponse>("/auth/otp/request", {
      method: "POST",
      auth: false,
      body: { phone, channel: "sms" },
    }),
  verifyOtp: (challengeId: string, code: string) =>
    request<TokenResponse>("/auth/otp/verify", {
      method: "POST",
      auth: false,
      body: { challengeId, code },
    }),

  // Profile
  getProfile: () => request<Profile>("/profile"),

  // Budget
  getBudget: () => request<Budget>("/budget/current"),
  recordIncome: (amountMinor: number, currency: string) =>
    request("/income", {
      method: "POST",
      body: {
        amountMinor,
        currency,
        source: "gig",
        capture: "manual",
        isBusiness: false,
        receivedOn: new Date().toISOString().slice(0, 10),
      },
    }),

  // Savings
  getGoals: () => request<Goal[]>("/goals"),
  contribute: (goalId: string, amountMinor: number) =>
    request<ContributionResponse>(`/goals/${goalId}/contributions`, {
      method: "POST",
      body: { amountMinor, source: "good_day" },
    }),

  // Scam Shield
  scan: (content: string) =>
    request<ScanResult>("/scam/scan", {
      method: "POST",
      body: { channel: "whatsapp", content, language: "en" },
    }),
  recordScamAction: (reportId: string, action: string) =>
    request(`/scam/${reportId}/action`, { method: "POST", body: { action } }),

  // Health Score
  getHealthScore: () => request<HealthScore>("/health-score"),
};

export function formatMoney(minor: number, currency: string): string {
  const symbols: Record<string, string> = { ZAR: "R", KES: "KSh ", USD: "$" };
  const sym = symbols[currency] ?? `${currency} `;
  return `${sym}${(minor / 100).toFixed(2)}`;
}
