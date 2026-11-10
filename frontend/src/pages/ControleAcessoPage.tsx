import { useRef, useState, type FormEvent } from "react";
import { ApiError } from "../api/client";
import { useRegistrarAcesso } from "../api/hooks";
import type { RegistroAcesso } from "../api/types";
import { Alert, Button, PageHeader } from "../components/ui";
import { fmtHora } from "../lib/format";

type Modo = "matricula" | "rg";

/**
 * Tela da recepção/catraca: um campo, Enter registra. Compatível com leitor de código de barras
 * (que "digita" a matrícula e envia Enter). Entrada ou saída é decidida pelo servidor.
 */
export function ControleAcessoPage() {
  const [modo, setModo] = useState<Modo>("matricula");
  const [ultimo, setUltimo] = useState<RegistroAcesso | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const registrar = useRegistrarAcesso();

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const valor = inputRef.current?.value.trim();
    if (!valor) return;
    registrar.mutate(modo === "matricula" ? { matricula: valor } : { rg: valor }, {
      onSuccess: (r) => setUltimo(r),
      onSettled: () => {
        if (inputRef.current) inputRef.current.value = "";
        inputRef.current?.focus();
      },
    });
  }

  const erro = registrar.error instanceof ApiError ? registrar.error.message : registrar.error && "Falha ao registrar.";

  return (
    <div className="mx-auto max-w-xl">
      <PageHeader title="Controle de acesso" subtitle="Informe a matrícula ou o RG — entrada e saída são alternadas automaticamente" />

      <form onSubmit={onSubmit} className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-zinc-200">
        <div className="mb-4 inline-flex rounded-lg bg-zinc-100 p-1 text-sm" role="tablist">
          {(["matricula", "rg"] as const).map((m) => (
            <button
              key={m}
              type="button"
              role="tab"
              aria-selected={modo === m}
              onClick={() => {
                setModo(m);
                inputRef.current?.focus();
              }}
              className={`rounded-md px-4 py-1.5 font-medium ${modo === m ? "bg-white shadow-sm" : "text-zinc-500"}`}
            >
              {m === "matricula" ? "Matrícula" : "RG"}
            </button>
          ))}
        </div>
        <div className="flex gap-3">
          <input
            ref={inputRef}
            autoFocus
            inputMode={modo === "matricula" ? "numeric" : "text"}
            aria-label={modo === "matricula" ? "Matrícula" : "RG"}
            placeholder={modo === "matricula" ? "Ex.: 20260042" : "Número do RG"}
            className="flex-1 rounded-xl border-0 px-4 py-3 text-2xl font-medium tracking-wider ring-1 ring-zinc-300 tabular-nums focus:ring-2 focus:ring-rose-500 focus:outline-none"
          />
          <Button type="submit" className="px-6 text-base" disabled={registrar.isPending}>
            Registrar
          </Button>
        </div>
      </form>

      <div className="mt-6" aria-live="polite">
        {erro ? (
          <Alert>{erro}</Alert>
        ) : (
          ultimo && (
            <div
              className={`rounded-2xl p-6 text-white ${ultimo.tipo === "ENTRADA" ? "bg-emerald-600" : "bg-sky-700"}`}
            >
              <p className="text-sm font-medium tracking-widest uppercase opacity-80">
                {ultimo.tipo === "ENTRADA" ? "Entrada registrada" : "Saída registrada"}
              </p>
              <p className="mt-1 text-2xl font-semibold">{ultimo.nome}</p>
              <p className="mt-1 text-sm opacity-80 tabular-nums">
                Matrícula {ultimo.matricula} · {fmtHora(ultimo.horario)}
              </p>
            </div>
          )
        )}
      </div>
    </div>
  );
}
