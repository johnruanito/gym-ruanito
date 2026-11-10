import type { FormEvent } from "react";
import { Link, useSearchParams } from "react-router";
import { ApiError } from "../api/client";
import { useAlunos, useExcluirAluno, type FiltroAlunos } from "../api/hooks";
import { SITUACOES, type Situacao } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Alert, Button, Card, EmptyState, Field, Input, PageHeader, Pagination, Select, SituacaoBadge } from "../components/ui";

const CAMPOS = ["nome", "matricula", "rg", "cpf", "celular", "situacao"] as const;

/**
 * Pesquisa de alunos. Também cobre o antigo "Relatório de situações" (filtro por situação).
 * Os filtros ficam na URL: dá para compartilhar o link e usar o botão voltar.
 */
export function AlunosPage() {
  const [params, setParams] = useSearchParams();
  const { isAdmin } = useAuth();
  const excluir = useExcluirAluno();

  const filtro: FiltroAlunos = {
    nome: params.get("nome") ?? "",
    matricula: params.get("matricula") ?? "",
    rg: params.get("rg") ?? "",
    cpf: params.get("cpf") ?? "",
    celular: params.get("celular") ?? "",
    situacao: (params.get("situacao") ?? "") as Situacao | "",
    page: Number(params.get("page") ?? 0),
  };
  const { data, isLoading, isFetching, error } = useAlunos(filtro);

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const next = new URLSearchParams();
    for (const c of CAMPOS) {
      const v = String(fd.get(c) ?? "").trim();
      if (v) next.set(c, v);
    }
    setParams(next);
  }

  function irParaPagina(page: number) {
    const next = new URLSearchParams(params);
    next.set("page", String(page));
    setParams(next);
  }

  function onExcluir(matricula: string, nome: string) {
    if (confirm(`Excluir o aluno ${nome} (${matricula})?`)) excluir.mutate(matricula);
  }

  return (
    <>
      <PageHeader
        title="Alunos"
        subtitle="Pesquise por qualquer combinação de critérios"
        actions={
          <Link to="/alunos/novo">
            <Button>Novo aluno</Button>
          </Link>
        }
      />

      <Card>
        <form onSubmit={onSubmit} key={params.toString()} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
          <Field label="Nome" className="lg:col-span-2">
            <Input name="nome" defaultValue={filtro.nome} placeholder="Parte do nome" />
          </Field>
          <Field label="Matrícula">
            <Input name="matricula" defaultValue={filtro.matricula} inputMode="numeric" />
          </Field>
          <Field label="RG">
            <Input name="rg" defaultValue={filtro.rg} />
          </Field>
          <Field label="CPF">
            <Input name="cpf" defaultValue={filtro.cpf} inputMode="numeric" />
          </Field>
          <Field label="Situação">
            <Select name="situacao" defaultValue={filtro.situacao}>
              <option value="">Todas</option>
              {SITUACOES.map((s) => (
                <option key={s} value={s}>
                  {s.charAt(0) + s.slice(1).toLowerCase()}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Celular" className="lg:col-span-2">
            <Input name="celular" defaultValue={filtro.celular} inputMode="tel" />
          </Field>
          <div className="flex items-end gap-2 lg:col-span-4 lg:justify-end">
            <Button type="button" variant="ghost" onClick={() => setParams(new URLSearchParams())}>
              Limpar
            </Button>
            <Button type="submit">Pesquisar</Button>
          </div>
        </form>
      </Card>

      <div className="mt-6 space-y-4">
        {error && <Alert>{(error as Error).message}</Alert>}
        {excluir.error instanceof ApiError && <Alert>{excluir.error.message}</Alert>}

        <Card>
          {data?.content.length ? (
            <div className={`overflow-x-auto transition-opacity ${isFetching ? "opacity-60" : ""}`}>
              <table className="w-full text-left text-sm">
                <thead className="text-xs tracking-wide text-zinc-500 uppercase">
                  <tr className="border-b border-zinc-200">
                    <th className="py-2 pr-4 font-medium">Matrícula</th>
                    <th className="py-2 pr-4 font-medium">Nome</th>
                    <th className="py-2 pr-4 font-medium">RG</th>
                    <th className="py-2 pr-4 font-medium">Celular</th>
                    <th className="py-2 pr-4 font-medium">Situação</th>
                    <th className="py-2 font-medium" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100">
                  {data.content.map((a) => (
                    <tr key={a.matricula} className="hover:bg-zinc-50">
                      <td className="py-2.5 pr-4 text-zinc-500 tabular-nums">{a.matricula}</td>
                      <td className="py-2.5 pr-4 font-medium text-zinc-900">
                        {a.nome}
                        {a.email && <span className="block text-xs font-normal text-zinc-500">{a.email}</span>}
                      </td>
                      <td className="py-2.5 pr-4 tabular-nums">{a.rg}</td>
                      <td className="py-2.5 pr-4 tabular-nums">{a.celular}</td>
                      <td className="py-2.5 pr-4">
                        <SituacaoBadge situacao={a.situacao} />
                      </td>
                      <td className="py-2.5 text-right whitespace-nowrap">
                        <Link to={`/alunos/${a.matricula}`} className="text-sm font-medium text-zinc-700 hover:underline">
                          Editar
                        </Link>
                        <Link
                          to={`/relatorios/acessos?matricula=${a.matricula}`}
                          className="ml-4 text-sm text-zinc-500 hover:underline"
                        >
                          Acessos
                        </Link>
                        {isAdmin && (
                          <button
                            onClick={() => onExcluir(a.matricula, a.nome)}
                            className="ml-4 text-sm text-rose-600 hover:underline"
                          >
                            Excluir
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState>{isLoading ? "Carregando…" : "Nenhum aluno encontrado."}</EmptyState>
          )}
          {data && (
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onChange={irParaPagina}
            />
          )}
        </Card>
      </div>
    </>
  );
}
