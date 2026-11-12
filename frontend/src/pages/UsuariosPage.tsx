import { useState, type FormEvent } from "react";
import { ApiError } from "../api/client";
import {
  useAlterarStatusUsuario,
  useAtualizarUsuario,
  useCriarUsuario,
  useRedefinirSenha,
  useUsuarios,
} from "../api/hooks";
import { PAPEIS, type Papel, type Usuario } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Alert, Button, Card, EmptyState, Field, Input, PageHeader, Select } from "../components/ui";
import { gerarSenhaProvisoria } from "../lib/senha";

const rotulo = (p: Papel) => PAPEIS.find((x) => x.valor === p)?.rotulo ?? p;

/** Mensagem para repassar ao funcionário: aparece uma única vez, a senha não fica guardada em lugar nenhum. */
function Credencial({ username, senha, onFechar }: { username: string; senha: string; onFechar: () => void }) {
  return (
    <Alert tone="success">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <span>
          Repasse ao funcionário: usuário <strong className="font-mono">{username}</strong> · senha provisória{" "}
          <strong className="font-mono">{senha}</strong>. Peça para trocá-la em "Minha senha" no primeiro acesso.
        </span>
        <Button variant="secondary" onClick={onFechar}>
          Ok, anotei
        </Button>
      </div>
    </Alert>
  );
}

function NovoMembro({ onCriado }: { onCriado: (username: string, senha: string) => void }) {
  const criar = useCriarUsuario();
  const [senha, setSenha] = useState(gerarSenhaProvisoria);

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    const username = String(fd.get("username")).trim().toLowerCase();
    criar.mutate(
      { username, nome: String(fd.get("nome")).trim(), papel: fd.get("papel") as Papel, senha },
      {
        onSuccess: () => {
          onCriado(username, senha);
          form.reset();
          setSenha(gerarSenhaProvisoria());
        },
      },
    );
  }

  const erros = criar.error instanceof ApiError ? criar.error.problem.errors : undefined;

  return (
    <Card title="Novo membro da equipe">
      <form onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <Field label="Nome" className="lg:col-span-2" error={erros?.nome}>
          <Input name="nome" required maxLength={100} />
        </Field>
        <Field label="Usuário (login)" error={erros?.username}>
          <Input name="username" required pattern="[a-zA-Z0-9._\-]{3,50}" placeholder="ex.: maria.silva" />
        </Field>
        <Field label="Papel">
          <Select name="papel" defaultValue="RECEPCAO">
            {PAPEIS.map((p) => (
              <option key={p.valor} value={p.valor}>
                {p.rotulo}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Senha provisória">
          <div className="flex gap-2">
            <Input value={senha} readOnly className="font-mono" aria-label="Senha provisória" />
            <Button type="button" variant="ghost" onClick={() => setSenha(gerarSenhaProvisoria())} title="Gerar outra">
              ↻
            </Button>
          </div>
        </Field>
        <div className="flex items-end justify-between gap-4 sm:col-span-2 lg:col-span-5">
          <p className="text-xs text-zinc-500">
            {PAPEIS.map((p) => `${p.rotulo}: ${p.descricao.toLowerCase()}`).join(" · ")}
          </p>
          <Button type="submit" disabled={criar.isPending}>
            Cadastrar
          </Button>
        </div>
        {criar.error && !erros && (
          <div className="lg:col-span-5">
            <Alert>{criar.error.message}</Alert>
          </div>
        )}
      </form>
    </Card>
  );
}

function LinhaUsuario({ u, souEu, onSenha }: { u: Usuario; souEu: boolean; onSenha: (senha: string) => void }) {
  const atualizar = useAtualizarUsuario();
  const status = useAlterarStatusUsuario();
  const redefinir = useRedefinirSenha();
  const erro = [atualizar.error, status.error, redefinir.error].find((e) => e instanceof ApiError) as
    | ApiError
    | undefined;

  function trocarPapel(papel: Papel) {
    atualizar.mutate({ id: u.id, nome: u.nome, papel });
  }

  function redefinirSenha() {
    if (!confirm(`Gerar uma nova senha provisória para ${u.nome}? A senha atual deixará de funcionar.`)) return;
    const novaSenha = gerarSenhaProvisoria();
    redefinir.mutate({ id: u.id, novaSenha }, { onSuccess: () => onSenha(novaSenha) });
  }

  return (
    <tr className={u.ativo ? "" : "text-zinc-400"}>
      <td className="py-2.5 pr-4">
        <span className="font-medium text-zinc-900">{u.nome}</span>
        {souEu && <span className="ml-2 text-xs text-zinc-500">(você)</span>}
        <span className="block font-mono text-xs text-zinc-500">{u.username}</span>
        {erro && <span className="block text-xs text-rose-600">{erro.message}</span>}
      </td>
      <td className="py-2.5 pr-4">
        <Select
          aria-label={`Papel de ${u.nome}`}
          value={u.papel}
          disabled={souEu || !u.ativo || atualizar.isPending}
          onChange={(e) => trocarPapel(e.target.value as Papel)}
          className="max-w-44"
        >
          {PAPEIS.map((p) => (
            <option key={p.valor} value={p.valor}>
              {p.rotulo}
            </option>
          ))}
        </Select>
      </td>
      <td className="py-2.5 pr-4">
        <span
          className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${
            u.ativo ? "bg-emerald-50 text-emerald-700 ring-emerald-200" : "bg-zinc-100 text-zinc-500 ring-zinc-200"
          }`}
        >
          {u.ativo ? "Ativo" : "Desativado"}
        </span>
      </td>
      <td className="py-2.5 text-right whitespace-nowrap">
        {!souEu && (
          <>
            {u.ativo && (
              <button onClick={redefinirSenha} className="text-sm text-zinc-600 hover:underline">
                Redefinir senha
              </button>
            )}
            <button
              onClick={() => status.mutate({ id: u.id, ativo: !u.ativo })}
              className={`ml-4 text-sm hover:underline ${u.ativo ? "text-rose-600" : "text-emerald-700"}`}
            >
              {u.ativo ? "Desativar" : "Reativar"}
            </button>
          </>
        )}
      </td>
    </tr>
  );
}

/** Tela "Equipe": só aparece para o administrador (o backend também recusa os demais com 403). */
export function UsuariosPage() {
  const { session } = useAuth();
  const { data, isLoading, error } = useUsuarios();
  const [credencial, setCredencial] = useState<{ username: string; senha: string } | null>(null);

  return (
    <>
      <PageHeader title="Equipe" subtitle="Quem acessa o sistema desta academia e o que cada um pode fazer" />
      <div className="space-y-6">
        {credencial && <Credencial {...credencial} onFechar={() => setCredencial(null)} />}
        <NovoMembro onCriado={(username, senha) => setCredencial({ username, senha })} />
        <Card title="Membros">
          {error && <Alert>{(error as Error).message}</Alert>}
          {data?.length ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-xs tracking-wide text-zinc-500 uppercase">
                  <tr className="border-b border-zinc-200">
                    <th className="py-2 pr-4 font-medium">Nome</th>
                    <th className="py-2 pr-4 font-medium">Papel</th>
                    <th className="py-2 pr-4 font-medium">Status</th>
                    <th className="py-2 font-medium" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100">
                  {data.map((u) => (
                    <LinhaUsuario
                      key={u.id}
                      u={u}
                      souEu={u.username === session?.username}
                      onSenha={(senha) => setCredencial({ username: u.username, senha })}
                    />
                  ))}
                </tbody>
              </table>
              <p className="mt-3 text-xs text-zinc-500">
                Desativar encerra as sessões da pessoa: ela perde o acesso em até 15 minutos.
              </p>
            </div>
          ) : (
            <EmptyState>{isLoading ? "Carregando…" : "Nenhum membro cadastrado."}</EmptyState>
          )}
        </Card>
        <p className="text-xs text-zinc-400">
          Ninguém desativa o próprio usuário, e a academia sempre mantém ao menos um {rotulo("ADMIN").toLowerCase()} ativo.
        </p>
      </div>
    </>
  );
}
