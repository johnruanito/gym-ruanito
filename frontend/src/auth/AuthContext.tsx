import { createContext, use, useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, setSessaoEncerradaHandler } from "../api/client";
import type { Perfil, SessaoResponse } from "../api/types";

interface AuthState {
  /** undefined = ainda verificando se há sessão (ao abrir/recarregar a página). */
  session: Perfil | null | undefined;
  isAdmin: boolean;
  /** Pode alterar dados (cadastro de alunos, catraca). O instrutor só consulta. */
  podeOperar: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Limpa o estado local sem chamar a API (sessão já encerrada no servidor). */
  encerrarLocalmente: (aviso?: string) => void;
  /** Mensagem para a tela de login (ex.: "sua sessão expirou"). */
  aviso: string | null;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Perfil | null | undefined>(undefined);
  const [aviso, setAviso] = useState<string | null>(null);
  const qc = useQueryClient();

  const encerrarLocalmente = useCallback(
    (mensagem?: string) => {
      setAviso(mensagem ?? null);
      setSession(null);
      qc.clear(); // nada do usuário anterior fica em cache na tela
    },
    [qc],
  );

  useEffect(
    () => setSessaoEncerradaHandler(() => encerrarLocalmente("Sua sessão foi encerrada. Entre novamente.")),
    [encerrarLocalmente],
  );

  // Ao abrir a página: o cookie pode existir (sessão de antes do F5). Pergunta à API quem somos.
  // Se o access token venceu, o cliente renova sozinho; se não houver sessão, cai no login.
  useEffect(() => {
    api<Perfil>("/auth/me")
      .then(setSession)
      .catch(() => {
        setAviso(null); // primeira visita / sem sessão: não é "sessão encerrada", é só login normal
        setSession(null);
      });
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const res = await api<SessaoResponse>("/auth/login", { method: "POST", body: { username, password } });
    setAviso(null);
    setSession(res.perfil);
  }, []);

  const logout = useCallback(async () => {
    try {
      await api<void>("/auth/logout", { method: "POST" }); // revoga no servidor e apaga os cookies
    } finally {
      encerrarLocalmente();
    }
  }, [encerrarLocalmente]);

  const value = useMemo(
    () => ({
      session,
      isAdmin: session?.papel === "ADMIN",
      podeOperar: session?.papel === "ADMIN" || session?.papel === "RECEPCAO",
      login,
      logout,
      encerrarLocalmente,
      aviso,
    }),
    [session, login, logout, encerrarLocalmente, aviso],
  );
  return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth() {
  const ctx = use(AuthContext);
  if (!ctx) throw new Error("useAuth fora do AuthProvider");
  return ctx;
}
