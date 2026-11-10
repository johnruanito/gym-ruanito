import type { Problem, Token } from "./types";

const SESSION_KEY = "academia.session";

export function loadSession(): Token | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const token = JSON.parse(raw) as Token;
    return new Date(token.expiresAt) > new Date() ? token : null;
  } catch {
    return null;
  }
}

export function saveSession(token: Token | null) {
  try {
    if (token) sessionStorage.setItem(SESSION_KEY, JSON.stringify(token));
    else sessionStorage.removeItem(SESSION_KEY);
  } catch {
    /* armazenamento indisponível: a sessão vale só enquanto a página estiver aberta */
  }
}

export class ApiError extends Error {
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title ?? `Erro ${problem.status}`);
    this.problem = problem;
  }

  get status() {
    return this.problem.status;
  }
}

let onUnauthorized: () => void = () => {};

/** Registrado pelo AuthProvider: token expirado/inválido → volta ao login. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

type Query = Record<string, string | number | null | undefined>;

export async function api<T>(
  path: string,
  options: { method?: string; body?: unknown; query?: Query } = {},
): Promise<T> {
  const url = new URL(`/api${path}`, window.location.origin);
  for (const [k, v] of Object.entries(options.query ?? {})) {
    if (v !== undefined && v !== null && v !== "") url.searchParams.set(k, String(v));
  }

  const headers: Record<string, string> = { Accept: "application/json" };
  const session = loadSession();
  if (session) headers.Authorization = `Bearer ${session.accessToken}`;
  if (options.body !== undefined) headers["Content-Type"] = "application/json";

  const res = await fetch(url, {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (res.status === 401 && path !== "/auth/login") {
    saveSession(null);
    onUnauthorized();
  }
  if (!res.ok) {
    let problem: Problem = { status: res.status, title: res.statusText };
    try {
      problem = { ...problem, ...(await res.json()) };
    } catch {
      /* corpo vazio ou não-JSON */
    }
    throw new ApiError(problem);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}
