export type User = {
  id: string;
  name: string;
  email: string;
  role: string;
  mfa_enabled: boolean;
  mfa_required: boolean;
};
export type Analysis = {
  id: string;
  domain: string;
  url: string;
  score: number;
  risk: "LOW" | "MEDIUM" | "HIGH";
  coverage: string;
  createdAt: string;
  rulesVersion: string;
  notice: string;
  indicators: {
    id: string;
    title: string;
    explanation: string;
    weight: number;
  }[];
  sources: {
    name: string;
    status: string;
    detail: string;
    checkedAt: string;
  }[];
};
export type Row = {
  id: string;
  domain: string;
  url: string;
  score: number;
  risk: Analysis["risk"];
  created_at: number;
};
export type Dashboard = {
  total: number;
  risks: Record<string, number>;
  trend: { date: string; total: number; high: number }[];
  scope: string;
  updatedAt: string;
};
export const riskLabel = {
  LOW: "Baixo risco",
  MEDIUM: "Risco médio",
  HIGH: "Alto risco",
};
export const sourceLabel: Record<string, string> = {
  FOUND: "Correspondência encontrada",
  NOT_FOUND: "Sem correspondência",
  NOT_CONFIGURED: "Não configurada",
  UNAVAILABLE: "Indisponível",
};
export class ApiFailure extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}
let csrf = "";
export async function api<T = Record<string, unknown>>(
  path: string,
  method = "GET",
  data?: unknown,
): Promise<T> {
  if (method !== "GET") {
    const r = await fetch("/api/v1/csrf", {
      credentials: "include",
      cache: "no-store",
    });
    if (!r.ok) throw new Error("Não foi possível iniciar a conexão segura.");
    csrf = (await r.json()).token;
  }
  const response = await fetch("/api/v1" + path, {
    method,
    credentials: "include",
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
      ...(method !== "GET" ? { "X-XSRF-TOKEN": csrf } : {}),
    },
    ...(data !== undefined ? { body: JSON.stringify(data) } : {}),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 403) csrf = "";
    if (response.status === 401 && typeof window !== "undefined")
      window.dispatchEvent(new Event("sentinela:unauthorized"));
    throw new ApiFailure(
      response.status,
      body.message || "Não foi possível concluir. Tente novamente.",
    );
  }
  return body;
}
export function date(value: number | string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function analysisQuery(
  scope: string,
  filters: { page: number; risk: string; search: string; days: string },
) {
  return scope === "overview"
    ? "/analyses?page=0&size=5&days=30"
    : `/analyses?page=${filters.page}&size=10&risk=${filters.risk}&q=${encodeURIComponent(filters.search)}&days=${filters.days}`;
}
