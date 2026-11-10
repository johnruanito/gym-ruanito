import { Suspense } from "react";
import { NavLink, Outlet } from "react-router";
import { useAuth } from "../auth/AuthContext";

const links = [
  { to: "/", label: "Painel", end: true },
  { to: "/acesso", label: "Controle de acesso" },
  { to: "/alunos", label: "Alunos" },
  { to: "/relatorios/acessos", label: "Entradas e saídas" },
];

export function Logo({ className = "" }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 font-semibold tracking-tight ${className}`}>
      <svg viewBox="0 0 32 32" className="size-7" aria-hidden>
        <rect width="32" height="32" rx="7" fill="#18181b" />
        <g fill="#f43f5e">
          <rect x="5" y="10" width="4" height="12" rx="1.5" />
          <rect x="23" y="10" width="4" height="12" rx="1.5" />
          <rect x="9" y="12.5" width="2.5" height="7" rx="1" />
          <rect x="20.5" y="12.5" width="2.5" height="7" rx="1" />
          <rect x="11.5" y="15" width="9" height="2" rx="1" />
        </g>
      </svg>
      Academia
    </span>
  );
}

export function Layout() {
  const { session, logout } = useAuth();
  return (
    <div className="min-h-dvh bg-zinc-50">
      <header className="sticky top-0 z-10 border-b border-zinc-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4">
          <Logo className="text-zinc-900" />
          <nav className="flex flex-1 gap-1 overflow-x-auto">
            {links.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end={l.end}
                className={({ isActive }) =>
                  `rounded-md px-3 py-1.5 text-sm whitespace-nowrap ${
                    isActive ? "bg-zinc-900 text-white" : "text-zinc-600 hover:bg-zinc-100"
                  }`
                }
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-3 text-sm">
            <span className="hidden text-zinc-500 sm:inline">{session?.nome}</span>
            <button onClick={logout} className="text-zinc-600 hover:text-zinc-900">
              Sair
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Suspense fallback={<p className="text-sm text-zinc-500">Carregando…</p>}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  );
}
