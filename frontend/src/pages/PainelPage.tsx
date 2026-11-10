import { Link } from "react-router";
import { usePainel, usePresentes } from "../api/hooks";
import { Card, EmptyState, PageHeader } from "../components/ui";
import { fmtHora } from "../lib/format";

function Stat({ label, value, to, accent }: { label: string; value?: number; to?: string; accent?: string }) {
  const body = (
    <div className="rounded-xl bg-white p-5 shadow-sm ring-1 ring-zinc-200 transition hover:ring-zinc-300">
      <p className="text-xs font-medium tracking-wide text-zinc-500 uppercase">{label}</p>
      <p className={`mt-2 text-3xl font-semibold tabular-nums ${accent ?? "text-zinc-900"}`}>{value ?? "–"}</p>
    </div>
  );
  return to ? <Link to={to}>{body}</Link> : body;
}

export function PainelPage() {
  const painel = usePainel();
  const presentes = usePresentes();
  const p = painel.data;

  return (
    <>
      <PageHeader title="Painel" subtitle="Visão geral de hoje, atualizada a cada 30 segundos" />
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        <Stat label="Presentes agora" value={p?.presentesAgora} accent="text-rose-600" />
        <Stat label="Entradas hoje" value={p?.entradasHoje} to="/relatorios/acessos" />
        <Stat label="Ativos" value={p?.alunosPorSituacao.ATIVO} to="/alunos?situacao=ATIVO" />
        <Stat label="Pendentes" value={p?.alunosPorSituacao.PENDENTE} to="/alunos?situacao=PENDENTE" />
        <Stat label="Inativos" value={p?.alunosPorSituacao.INATIVO} to="/alunos?situacao=INATIVO" />
      </div>

      <div className="mt-8">
        <Card title="Na academia agora">
          {presentes.data?.length ? (
            <ul className="divide-y divide-zinc-100">
              {presentes.data.map((a) => (
                <li key={a.id} className="flex items-center justify-between py-2.5 text-sm">
                  <Link to={`/alunos/${a.matricula}`} className="font-medium text-zinc-900 hover:underline">
                    {a.nome}
                  </Link>
                  <span className="text-zinc-500 tabular-nums">
                    {a.matricula} · entrou às {fmtHora(a.entrada)}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState>{presentes.isLoading ? "Carregando…" : "Ninguém na academia no momento."}</EmptyState>
          )}
        </Card>
      </div>
    </>
  );
}
