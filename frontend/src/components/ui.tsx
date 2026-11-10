import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";
import type { Situacao } from "../api/types";

const cx = (...c: (string | false | null | undefined)[]) => c.filter(Boolean).join(" ");

type Variant = "primary" | "secondary" | "danger" | "ghost";

const variants: Record<Variant, string> = {
  primary: "bg-zinc-900 text-white hover:bg-zinc-700",
  secondary: "bg-white text-zinc-900 ring-1 ring-zinc-300 hover:bg-zinc-50",
  danger: "bg-white text-rose-700 ring-1 ring-rose-200 hover:bg-rose-50",
  ghost: "text-zinc-600 hover:bg-zinc-100",
};

export function Button({
  variant = "primary",
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      className={cx(
        "inline-flex items-center justify-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium transition-colors",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500 disabled:cursor-not-allowed disabled:opacity-40",
        variants[variant],
        className,
      )}
      {...props}
    />
  );
}

const fieldBase =
  "block w-full rounded-lg border-0 bg-white px-3 py-2 text-sm text-zinc-900 ring-1 ring-zinc-300 placeholder:text-zinc-400 " +
  "focus:ring-2 focus:ring-rose-500 focus:outline-none aria-invalid:ring-rose-500";

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx(fieldBase, className)} {...props} />;
}

export function Select({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={cx(fieldBase, "pr-8", className)} {...props} />;
}

export function Field({
  label,
  error,
  children,
  className,
}: {
  label: string;
  error?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <label className={cx("block", className)}>
      <span className="mb-1 block text-xs font-medium tracking-wide text-zinc-600 uppercase">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-rose-600">{error}</span>}
    </label>
  );
}

export function Card({ title, actions, children }: { title?: string; actions?: ReactNode; children: ReactNode }) {
  return (
    <section className="rounded-xl bg-white p-5 shadow-sm ring-1 ring-zinc-200">
      {(title || actions) && (
        <header className="mb-4 flex items-center justify-between gap-4">
          {title && <h2 className="text-sm font-semibold text-zinc-900">{title}</h2>}
          {actions}
        </header>
      )}
      {children}
    </section>
  );
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-zinc-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-zinc-500">{subtitle}</p>}
      </div>
      {actions}
    </div>
  );
}

const situacaoStyle: Record<Situacao, string> = {
  ATIVO: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  INATIVO: "bg-zinc-100 text-zinc-600 ring-zinc-200",
  PENDENTE: "bg-amber-50 text-amber-700 ring-amber-200",
};

export function SituacaoBadge({ situacao }: { situacao: Situacao }) {
  return (
    <span className={cx("inline-flex rounded-full px-2 py-0.5 text-xs font-medium ring-1", situacaoStyle[situacao])}>
      {situacao.charAt(0) + situacao.slice(1).toLowerCase()}
    </span>
  );
}

export function Alert({ tone = "error", children }: { tone?: "error" | "success" | "info"; children: ReactNode }) {
  const tones = {
    error: "bg-rose-50 text-rose-800 ring-rose-200",
    success: "bg-emerald-50 text-emerald-800 ring-emerald-200",
    info: "bg-sky-50 text-sky-800 ring-sky-200",
  };
  return (
    <div role={tone === "error" ? "alert" : "status"} className={cx("rounded-lg px-4 py-3 text-sm ring-1", tones[tone])}>
      {children}
    </div>
  );
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}) {
  if (totalElements === 0) return null;
  return (
    <div className="mt-4 flex items-center justify-between text-sm text-zinc-500">
      <span>
        {totalElements} registro{totalElements === 1 ? "" : "s"}
      </span>
      <div className="flex items-center gap-2">
        <Button variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>
          Anterior
        </Button>
        <span className="tabular-nums">
          {page + 1} / {Math.max(totalPages, 1)}
        </span>
        <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
          Próxima
        </Button>
      </div>
    </div>
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <p className="py-10 text-center text-sm text-zinc-500">{children}</p>;
}
