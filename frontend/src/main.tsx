import { lazy, StrictMode, type ComponentType, type ReactNode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, Navigate, RouterProvider, useLocation } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ApiError } from "./api/client";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import { Layout } from "./components/Layout";
import { LoginPage } from "./pages/LoginPage";
import "./index.css";

// Code splitting por rota: o formulário (Zod + React Hook Form) e os relatórios
// só são baixados quando o usuário abre a tela correspondente.
const page = <K extends string>(load: () => Promise<Record<K, ComponentType>>, name: K) =>
  lazy(() => load().then((m) => ({ default: m[name] })));

const PainelPage = page(() => import("./pages/PainelPage"), "PainelPage");
const ControleAcessoPage = page(() => import("./pages/ControleAcessoPage"), "ControleAcessoPage");
const AlunosPage = page(() => import("./pages/AlunosPage"), "AlunosPage");
const AlunoFormPage = page(() => import("./pages/AlunoFormPage"), "AlunoFormPage");
const RelatorioAcessosPage = page(() => import("./pages/RelatorioAcessosPage"), "RelatorioAcessosPage");
const UsuariosPage = page(() => import("./pages/UsuariosPage"), "UsuariosPage");
const MinhaSenhaPage = page(() => import("./pages/MinhaSenhaPage"), "MinhaSenhaPage");

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      // Não insiste em erros de cliente (4xx): só repete falhas de rede/servidor
      retry: (count, err) => !(err instanceof ApiError && err.status < 500) && count < 2,
    },
  },
});

function RequireAuth({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  const location = useLocation();
  if (session === undefined) return <p className="p-8 text-sm text-zinc-500">Carregando…</p>;
  return session ? children : <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
}

const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: (
      <RequireAuth>
        <Layout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <PainelPage /> },
      { path: "acesso", element: <ControleAcessoPage /> },
      { path: "alunos", element: <AlunosPage /> },
      { path: "alunos/novo", element: <AlunoFormPage /> },
      { path: "alunos/:matricula", element: <AlunoFormPage /> },
      { path: "relatorios/acessos", element: <RelatorioAcessosPage /> },
      { path: "equipe", element: <UsuariosPage /> },
      { path: "minha-senha", element: <MinhaSenhaPage /> },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  },
]);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
);
