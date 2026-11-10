import type { FormEvent } from "react";
import { Link, useSearchParams } from "react-router";
import { api } from "../api/client";
import { useAcessos, type FiltroAcessos } from "../api/hooks";
import type { Acesso, Page } from "../api/types";
import { Alert, Button, Card, EmptyState, Field, Input, PageHeader, Pagination } from "../components/ui";
import { fmtDataHora, fmtDuracao, isoDateLocal } from "../lib/format";

/** Relatório de entradas e saídas. Período padrão: últimos 30 dias (definido no servidor). */
export function RelatorioAcessosPage() {
  const [params, setParams] = useSearchParams();
  const filtro: FiltroAcessos = {
    matricula: params.get("matricula") ?? "",
    de: params.get("de") ?? "",
    ate: params.get("ate") ?? "",
    page: Number(params.get("page") ?? 0),
  };
  const { data, isLoading, isFetching, error } = useAcessos(filtro);

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const next = new URLSearchParams();
    for (const k of ["matricula", "de", "ate"]) {
      const v = String(fd.get(k) ?? "").trim();
      if (v) next.set(k, v);
    }
    setParams(next);
  }

  /** Exporta todas as páginas do filtro atual em CSV (abre no Excel com acentuação correta). */
  async function exportarCsv() {
    const linhas: Acesso[] = [];
    for (let page = 0; ; page++) {
      const p = await api<Page<Acesso>>("/acessos", { query: { ...filtro, page, size: 200 } });
      linhas.push(...p.content);
      if (page + 1 >= p.totalPages) break;
    }
    const csv = [
      "Matrícula;Nome;Entrada;Saída;Duração",
      ...linhas.map((a) =>
        [a.matricula, `"${a.nome.replace(/"/g, '""')}"`, fmtDataHora(a.entrada), fmtDataHora(a.saida), fmtDuracao(a.duracaoMinutos)].join(";"),
      ),
    ].join("\r\n");
    const url = URL.createObjectURL(new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8" }));
    const link = Object.assign(document.createElement("a"), { href: url, download: `acessos-${isoDateLocal(new Date())}.csv` });
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <>
      <PageHeader
        title="Entradas e saídas"
        subtitle="Filtra pela data de entrada; sem período informado, mostra os últimos 30 dias"
        actions={
          <Button variant="secondary" onClick={exportarCsv} disabled={!data?.totalElements}>
            Exportar CSV
          </Button>
        }
      />

      <Card>
        <form onSubmit={onSubmit} key={params.toString()} className="grid gap-4 sm:grid-cols-4">
          <Field label="Matrícula">
            <Input name="matricula" defaultValue={filtro.matricula} inputMode="numeric" placeholder="Todos os alunos" />
          </Field>
          <Field label="De">
            <Input name="de" type="date" defaultValue={filtro.de} />
          </Field>
          <Field label="Até">
            <Input name="ate" type="date" defaultValue={filtro.ate} />
          </Field>
          <div className="flex items-end gap-2">
            <Button type="button" variant="ghost" onClick={() => setParams(new URLSearchParams())}>
              Limpar
            </Button>
            <Button type="submit" className="flex-1">
              Gerar
            </Button>
          </div>
        </form>
      </Card>

      <div className="mt-6 space-y-4">
        {error && <Alert>{(error as Error).message}</Alert>}
        <Card>
          {data?.content.length ? (
            <div className={`overflow-x-auto transition-opacity ${isFetching ? "opacity-60" : ""}`}>
              <table className="w-full text-left text-sm">
                <thead className="text-xs tracking-wide text-zinc-500 uppercase">
                  <tr className="border-b border-zinc-200">
                    <th className="py-2 pr-4 font-medium">Aluno</th>
                    <th className="py-2 pr-4 font-medium">Entrada</th>
                    <th className="py-2 pr-4 font-medium">Saída</th>
                    <th className="py-2 text-right font-medium">Duração</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100">
                  {data.content.map((a) => (
                    <tr key={a.id}>
                      <td className="py-2.5 pr-4">
                        <Link to={`/alunos/${a.matricula}`} className="font-medium text-zinc-900 hover:underline">
                          {a.nome}
                        </Link>
                        <span className="ml-2 text-xs text-zinc-500 tabular-nums">{a.matricula}</span>
                      </td>
                      <td className="py-2.5 pr-4 tabular-nums">{fmtDataHora(a.entrada)}</td>
                      <td className="py-2.5 pr-4 tabular-nums">
                        {a.saida ? fmtDataHora(a.saida) : <span className="text-amber-600">sem saída</span>}
                      </td>
                      <td className="py-2.5 text-right tabular-nums">{fmtDuracao(a.duracaoMinutos)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState>{isLoading ? "Carregando…" : "Nenhum acesso no período."}</EmptyState>
          )}
          {data && (
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onChange={(page) => {
                const next = new URLSearchParams(params);
                next.set("page", String(page));
                setParams(next);
              }}
            />
          )}
        </Card>
      </div>
    </>
  );
}
