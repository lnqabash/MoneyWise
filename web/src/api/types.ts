// Shared API types mirroring the backend DTOs (docs/07-api-design.md).

export interface OtpRequestResponse {
  challengeId: string;
  expiresIn: number;
  devCode?: string | null;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  isNewUser: boolean;
}

export interface Profile {
  userId: string;
  displayName: string | null;
  preferredLanguage: string;
  preferredChannel: string;
  currency: string;
  incomePattern: string | null;
  personaSegment: string | null;
  consent: Record<string, boolean>;
}

export interface Envelope {
  name: string;
  allocatedMinor: number;
  spentMinor: number;
}

export interface Explanation {
  summary: string;
  reasons: string[];
  modelVersion: string;
}

export interface Budget {
  strategy: string;
  safeToSpendTodayMinor: number;
  currency: string;
  confidence: number;
  envelopes: Envelope[];
  explanation: Explanation;
}

export interface Goal {
  goalId: string;
  name: string;
  goalType: string;
  targetMinor: number;
  savedMinor: number;
  currency: string;
  progress: number;
  status: string;
  softLocked: boolean;
  weeklyTargetMinor: number | null;
}

export interface ContributionResponse {
  txnId: string;
  savedMinor: number;
  status: string;
}

export interface RedFlag {
  code: string;
  explanation: string;
}

export interface ScamAdvice {
  summary: string;
  actions: string[];
}

export interface ScanResult {
  reportId: string;
  riskLevel: "low" | "medium" | "high";
  riskScore: number;
  redFlags: RedFlag[];
  advice: ScamAdvice;
  modelVersion: string;
}

export interface HealthPillar {
  code: string;
  name: string;
  score: number;
  reason: string;
}

export interface HealthScore {
  overallScore: number;
  band: string;
  pillars: HealthPillar[];
  summary: string;
  modelVersion: string;
}

export interface ApiError {
  error: { code: string; message: string; traceId?: string };
}
