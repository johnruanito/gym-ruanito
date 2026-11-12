import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate, useParams } from "react-router";
import { ApiError } from "../api/client";
import { useAluno, useEstados, useSalvarAluno } from "../api/hooks";
import { SITUACOES, type Aluno, type AlunoRequest } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Alert, Button, Card, Field, Input, PageHeader, Select } from "../components/ui";
import { cpfValido } from "../lib/cpf";
import { fmtDataHora } from "../lib/format";

const obrigatorio = "Obrigatório";

const schema = z.object({
  nome: z.string().trim().min(3, "Mínimo de 3 caracteres").max(100),
  sexo: z.enum(["MASCULINO", "FEMININO"], { error: obrigatorio }),
  rg: z.string().trim().regex(/^[0-9A-Za-z.-]{5,20}$/, "RG inválido"),
  cpf: z.string().refine(cpfValido, "CPF inválido"),
  dataNascimento: z
    .string()
    .min(1, obrigatorio)
    .refine((d) => new Date(d) < new Date(), "Deve ser uma data passada"),
  situacao: z.enum(["ATIVO", "INATIVO", "PENDENTE"]),
  email: z.union([z.literal(""), z.email("E-mail inválido")]),
  endereco: z.object({
    cep: z.string().regex(/^\d{5}-?\d{3}$/, "CEP deve ter 8 dígitos"),
    logradouro: z.string().trim().min(3, obrigatorio).max(128),
    numero: z.string().max(10),
    complemento: z.string().max(64),
    bairro: z.string().trim().min(1, obrigatorio).max(128),
    cidade: z.string().trim().min(1, obrigatorio).max(64),
    estado: z.string().length(2, obrigatorio),
  }),
  celular: z.object({
    ddd: z.string().regex(/^\d{2}$/, "DDD inválido"),
    numero: z.string().regex(/^\d{4,5}-?\d{4}$/, "Número inválido"),
  }),
});

type FormValues = z.infer<typeof schema>;

const vazio: FormValues = {
  nome: "",
  sexo: "FEMININO",
  rg: "",
  cpf: "",
  dataNascimento: "",
  situacao: "ATIVO",
  email: "",
  endereco: { cep: "", logradouro: "", numero: "", complemento: "", bairro: "", cidade: "", estado: "" },
  celular: { ddd: "", numero: "" },
};

function toForm(a: Aluno): FormValues {
  return {
    ...a,
    email: a.email ?? "",
    endereco: { ...a.endereco, numero: a.endereco.numero ?? "", complemento: a.endereco.complemento ?? "" },
  };
}

export function AlunoFormPage() {
  const { matricula } = useParams();
  const editando = !!matricula;
  const navigate = useNavigate();
  const aluno = useAluno(matricula);
  const estados = useEstados();
  const salvar = useSalvarAluno(matricula);
  const { podeOperar } = useAuth();
  const [salvo, setSalvo] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    setValue,
    getValues,
    formState: { errors, isDirty },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: vazio });

  useEffect(() => {
    if (aluno.data) reset(toForm(aluno.data));
  }, [aluno.data, reset]);

  /** Preenche o endereço pelo CEP (ViaCEP). Falha silenciosa: o usuário digita manualmente. */
  async function buscarCep() {
    const cep = getValues("endereco.cep").replace(/\D/g, "");
    if (cep.length !== 8) return;
    try {
      const res = await fetch(`https://viacep.com.br/ws/${cep}/json/`);
      const d = await res.json();
      if (d.erro) return;
      const opts = { shouldDirty: true, shouldValidate: true };
      setValue("endereco.logradouro", d.logradouro ?? "", opts);
      setValue("endereco.bairro", d.bairro ?? "", opts);
      setValue("endereco.cidade", d.localidade ?? "", opts);
      setValue("endereco.estado", d.uf ?? "", opts);
    } catch {
      /* sem internet ou serviço fora do ar */
    }
  }

  function onSubmit(v: FormValues) {
    setSalvo(false);
    const body: AlunoRequest = {
      ...v,
      email: v.email || null,
      endereco: { ...v.endereco, numero: v.endereco.numero || null, complemento: v.endereco.complemento || null },
      versao: aluno.data?.versao ?? null,
    };
    salvar.mutate(body, {
      onSuccess: (a) => {
        setSalvo(true);
        if (!editando) navigate(`/alunos/${a.matricula}`, { replace: true });
      },
      onError: (err) => {
        // Erros de validação do servidor voltam por campo (ex.: "endereco.cep") e caem no campo certo
        if (err instanceof ApiError && err.problem.errors) {
          for (const [campo, msg] of Object.entries(err.problem.errors)) {
            setError(campo as keyof FormValues, { message: msg });
          }
        }
      },
    });
  }

  if (editando && aluno.isLoading) return <p className="text-sm text-zinc-500">Carregando…</p>;
  if (editando && aluno.error) return <Alert>{(aluno.error as Error).message}</Alert>;

  const err = (path: string) =>
    path.split(".").reduce<unknown>((o, k) => (o as Record<string, unknown> | undefined)?.[k], errors) as
      | { message?: string }
      | undefined;

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      <PageHeader
        title={editando ? aluno.data?.nome ?? "Aluno" : "Novo aluno"}
        subtitle={
          editando
            ? `Matrícula ${matricula} · atualizado em ${fmtDataHora(aluno.data?.atualizadoEm ?? null)}`
            : "A matrícula é gerada automaticamente ao salvar"
        }
        actions={
          <div className="flex gap-2">
            {editando && (
              <Link to={`/relatorios/acessos?matricula=${matricula}`}>
                <Button type="button" variant="secondary">
                  Entradas e saídas
                </Button>
              </Link>
            )}
            {podeOperar && (
              <Button type="submit" disabled={salvar.isPending || (editando && !isDirty)}>
                {salvar.isPending ? "Salvando…" : "Salvar"}
              </Button>
            )}
          </div>
        }
      />

      {/* fieldset disabled: trava todos os campos de uma vez para quem só pode consultar */}
      <fieldset disabled={!podeOperar} className="space-y-4">
        {salvar.error && <Alert>{salvar.error.message}</Alert>}
        {salvo && !salvar.error && <Alert tone="success">Dados gravados com sucesso.</Alert>}

        <Card title="Dados pessoais">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Field label="Nome" error={errors.nome?.message} className="sm:col-span-2">
              <Input {...register("nome")} aria-invalid={!!errors.nome} />
            </Field>
            <Field label="Sexo" error={errors.sexo?.message}>
              <Select {...register("sexo")}>
                <option value="FEMININO">Feminino</option>
                <option value="MASCULINO">Masculino</option>
              </Select>
            </Field>
            <Field label="Data de nascimento" error={errors.dataNascimento?.message}>
              <Input type="date" {...register("dataNascimento")} aria-invalid={!!errors.dataNascimento} />
            </Field>
            <Field label="CPF" error={errors.cpf?.message}>
              <Input {...register("cpf")} inputMode="numeric" placeholder="000.000.000-00" aria-invalid={!!errors.cpf} />
            </Field>
            <Field label="RG" error={errors.rg?.message}>
              <Input {...register("rg")} aria-invalid={!!errors.rg} />
            </Field>
            <Field label="Situação" error={errors.situacao?.message}>
              <Select {...register("situacao")}>
                {SITUACOES.map((s) => (
                  <option key={s} value={s}>
                    {s.charAt(0) + s.slice(1).toLowerCase()}
                  </option>
                ))}
              </Select>
            </Field>
          </div>
        </Card>

        <Card title="Contato">
          <div className="grid gap-4 sm:grid-cols-4">
            <Field label="E-mail" error={errors.email?.message} className="sm:col-span-2">
              <Input type="email" {...register("email")} aria-invalid={!!errors.email} />
            </Field>
            <Field label="DDD" error={err("celular.ddd")?.message}>
              <Input {...register("celular.ddd")} inputMode="numeric" maxLength={2} placeholder="91" />
            </Field>
            <Field label="Celular" error={err("celular.numero")?.message}>
              <Input {...register("celular.numero")} inputMode="tel" placeholder="98888-7777" />
            </Field>
          </div>
        </Card>

        <Card title="Endereço">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
            <Field label="CEP" error={err("endereco.cep")?.message}>
              <Input
                {...register("endereco.cep", { onBlur: buscarCep })}
                inputMode="numeric"
                placeholder="00000-000"
              />
            </Field>
            <Field label="Logradouro" error={err("endereco.logradouro")?.message} className="lg:col-span-4">
              <Input {...register("endereco.logradouro")} />
            </Field>
            <Field label="Número" error={err("endereco.numero")?.message}>
              <Input {...register("endereco.numero")} />
            </Field>
            <Field label="Complemento" error={err("endereco.complemento")?.message} className="lg:col-span-2">
              <Input {...register("endereco.complemento")} />
            </Field>
            <Field label="Bairro" error={err("endereco.bairro")?.message} className="lg:col-span-2">
              <Input {...register("endereco.bairro")} />
            </Field>
            <Field label="Cidade" error={err("endereco.cidade")?.message}>
              <Input {...register("endereco.cidade")} />
            </Field>
            <Field label="UF" error={err("endereco.estado")?.message}>
              <Select {...register("endereco.estado")}>
                <option value="">—</option>
                {estados.data?.map((e) => (
                  <option key={e.sigla} value={e.sigla}>
                    {e.sigla} – {e.nome}
                  </option>
                ))}
              </Select>
            </Field>
          </div>
        </Card>
      </fieldset>
    </form>
  );
}
