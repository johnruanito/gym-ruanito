import type { Problem } from "./types";

/**
 * Cliente HTTP da API.
 *
 * Não existe token aqui: a sessão vive em cookies HttpOnly, que o navegador envia sozinho e que
 * nenhum script consegue ler. Este arquivo só precisa:
 *  1. enviar os cookies (credentials: "same-origin");
 *  2. quando a API responder 401 (access token de 15 min venceu), renovar UMA vez e repetir a chamada.
 */

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

let onSessaoEncerrada: () => void = () => {};

/** Registrado pelo AuthProvider: refresh falhou → volta para o login. */
export function setSessaoEncerradaHandler(handler: () => void) {
  onSessaoEncerrada = handler;
}

/** Rotas que não devem disparar renovação automática (evita laço infinito). */
const SEM_RENOVACAO = ["/auth/login", "/auth/refresh", "/auth/logout"];

/**
 * Renovação "single-flight": se 5 requisições derem 401 ao mesmo tempo, só UM refresh é feito e
 * todas esperam por ele. Sem isso, os 5 refresh concorrentes usariam o mesmo token e o servidor
 * entenderia como reuso (roubo) e derrubaria a sessão.
 */
let renovacaoEmAndamento: Promise<boolean> | null = null;

export function renovarSessao(): Promise<boolean> {
  renovacaoEmAndamento ??= fetch("/api/auth/refresh", { method: "POST", credentials: "same-origin" })
    .then((r) => r.ok)
    .catch(() => false)
    .finally(() => {
      renovacaoEmAndamento = null;
    });
  return renovacaoEmAndamento;
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
  if (options.body !== undefined) headers["Content-Type"] = "application/json";

  const enviar = () =>
    fetch(url, {
      method: options.method ?? "GET",
      headers,
      credentials: "same-origin",
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });

  let res = await enviar();

  if (res.status === 401 && !SEM_RENOVACAO.includes(path)) {
    if (await renovarSessao()) {
      res = await enviar(); // mesma requisição, agora com o access token novo no cookie
    } else {
      onSessaoEncerrada();
    }
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
