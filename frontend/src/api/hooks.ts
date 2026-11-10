import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client";
import type {
  Acesso,
  Aluno,
  AlunoRequest,
  AlunoResumo,
  Estado,
  Page,
  Painel,
  RegistroAcesso,
  Situacao,
} from "./types";

export interface FiltroAlunos {
  matricula?: string;
  nome?: string;
  rg?: string;
  cpf?: string;
  celular?: string;
  situacao?: Situacao | "";
  page?: number;
}

export function useAlunos(filtro: FiltroAlunos) {
  return useQuery({
    queryKey: ["alunos", filtro],
    queryFn: () => api<Page<AlunoResumo>>("/alunos", { query: { ...filtro, size: 20 } }),
    placeholderData: keepPreviousData,
  });
}

export function useAluno(matricula: string | undefined) {
  return useQuery({
    queryKey: ["aluno", matricula],
    queryFn: () => api<Aluno>(`/alunos/${matricula}`),
    enabled: !!matricula,
  });
}

export function useSalvarAluno(matricula?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AlunoRequest) =>
      matricula
        ? api<Aluno>(`/alunos/${matricula}`, { method: "PUT", body })
        : api<Aluno>("/alunos", { method: "POST", body }),
    onSuccess: (aluno) => {
      qc.setQueryData(["aluno", aluno.matricula], aluno);
      qc.invalidateQueries({ queryKey: ["alunos"] });
      qc.invalidateQueries({ queryKey: ["painel"] });
    },
  });
}

export function useExcluirAluno() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (matricula: string) => api<void>(`/alunos/${matricula}`, { method: "DELETE" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["alunos"] });
      qc.invalidateQueries({ queryKey: ["painel"] });
    },
  });
}

export function useEstados() {
  return useQuery({
    queryKey: ["estados"],
    queryFn: () => api<Estado[]>("/estados"),
    staleTime: Infinity,
  });
}

export function useRegistrarAcesso() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { matricula?: string; rg?: string }) =>
      api<RegistroAcesso>("/acessos", { method: "POST", body }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["presentes"] });
      qc.invalidateQueries({ queryKey: ["painel"] });
      qc.invalidateQueries({ queryKey: ["acessos"] });
    },
  });
}

export interface FiltroAcessos {
  matricula?: string;
  de?: string;
  ate?: string;
  page?: number;
}

export function useAcessos(filtro: FiltroAcessos) {
  return useQuery({
    queryKey: ["acessos", filtro],
    queryFn: () => api<Page<Acesso>>("/acessos", { query: { ...filtro, size: 50 } }),
    placeholderData: keepPreviousData,
  });
}

export function usePresentes() {
  return useQuery({
    queryKey: ["presentes"],
    queryFn: () => api<Acesso[]>("/acessos/presentes"),
    refetchInterval: 30_000,
  });
}

export function usePainel() {
  return useQuery({
    queryKey: ["painel"],
    queryFn: () => api<Painel>("/painel"),
    refetchInterval: 30_000,
  });
}
