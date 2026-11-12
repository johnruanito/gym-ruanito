import { useState, type FormEvent } from "react";
import { Navigate, useLocation, useNavigate } from "react-router";
import { ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Logo } from "../components/Layout";
import { Alert, Button, Field, Input } from "../components/ui";

export function LoginPage() {
  const { session, login, aviso } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const destino = (location.state as { from?: string } | null)?.from ?? "/";
  if (session === undefined) return null; // ainda verificando se já existe sessão
  if (session) return <Navigate to={destino} replace />;

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    setErro(null);
    setEnviando(true);
    try {
      await login(String(data.get("username")), String(data.get("password")));
      navigate(destino, { replace: true });
    } catch (err) {
      setErro(err instanceof ApiError && err.status === 401 ? "Usuário ou senha inválidos." : "Falha ao conectar ao servidor.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="grid min-h-dvh place-items-center bg-zinc-950 px-4">
      <div className="w-full max-w-sm">
        <Logo className="mb-8 text-xl text-white" />
        <form onSubmit={onSubmit} className="space-y-4 rounded-2xl bg-white p-6 shadow-xl">
          <div>
            <h1 className="text-lg font-semibold text-zinc-900">Entrar</h1>
            <p className="text-sm text-zinc-500">Acesso da equipe da recepção</p>
          </div>
          {aviso && !erro && <Alert tone="info">{aviso}</Alert>}
          {erro && <Alert>{erro}</Alert>}
          <Field label="Usuário">
            <Input name="username" autoComplete="username" required autoFocus />
          </Field>
          <Field label="Senha">
            <Input name="password" type="password" autoComplete="current-password" required />
          </Field>
          <Button type="submit" className="w-full" disabled={enviando}>
            {enviando ? "Entrando…" : "Entrar"}
          </Button>
        </form>
      </div>
    </div>
  );
}
