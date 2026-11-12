import { createContext, use, useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, loadSession, saveSession, setUnauthorizedHandler } from "../api/client";
import type { Token } from "../api/types";

interface AuthState {
  session: Token | null;
  isAdmin: boolean;
  /** Pode alterar dados (cadastro de alunos, catraca). O instrutor só consulta. */
  podeOperar: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Token | null>(loadSession);
  const qc = useQueryClient();

  const logout = useCallback(() => {
    saveSession(null);
    setSession(null);
    qc.clear();
  }, [qc]);

  useEffect(() => setUnauthorizedHandler(logout), [logout]);

  const login = useCallback(async (username: string, password: string) => {
    const token = await api<Token>("/auth/login", { method: "POST", body: { username, password } });
    const sessao = { ...token, username: username.trim().toLowerCase() };
    saveSession(sessao);
    setSession(sessao);
  }, []);

  const value = useMemo(
    () => ({
      session,
      isAdmin: session?.papel === "ADMIN",
      podeOperar: session?.papel === "ADMIN" || session?.papel === "RECEPCAO",
      login,
      logout,
    }),
    [session, login, logout],
  );
  return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth() {
  const ctx = use(AuthContext);
  if (!ctx) throw new Error("useAuth fora do AuthProvider");
  return ctx;
}
