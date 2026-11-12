// Contratos da API (espelham os records Java do backend)

export type Sexo = "MASCULINO" | "FEMININO";
export type Situacao = "ATIVO" | "INATIVO" | "PENDENTE";
export type Papel = "ADMIN" | "RECEPCAO" | "INSTRUTOR";

export const PAPEIS: { valor: Papel; rotulo: string; descricao: string }[] = [
  { valor: "ADMIN", rotulo: "Administrador", descricao: "Tudo, inclusive gerenciar a equipe" },
  { valor: "RECEPCAO", rotulo: "Recepção", descricao: "Cadastra alunos e registra entrada/saída" },
  { valor: "INSTRUTOR", rotulo: "Instrutor", descricao: "Apenas consulta" },
];
export type TipoAcesso = "ENTRADA" | "SAIDA";

export const SITUACOES: Situacao[] = ["ATIVO", "INATIVO", "PENDENTE"];

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Endereco {
  cep: string;
  logradouro: string;
  numero: string | null;
  complemento: string | null;
  bairro: string;
  cidade: string;
  estado: string;
}

export interface Celular {
  ddd: string;
  numero: string;
}

export interface AlunoResumo {
  matricula: string;
  nome: string;
  rg: string;
  email: string | null;
  celular: string;
  situacao: Situacao;
}

export interface Aluno {
  matricula: string;
  nome: string;
  sexo: Sexo;
  rg: string;
  cpf: string;
  dataNascimento: string;
  situacao: Situacao;
  email: string | null;
  endereco: Endereco;
  celular: Celular;
  criadoEm: string;
  atualizadoEm: string;
  versao: number;
}

export type AlunoRequest = Omit<Aluno, "matricula" | "criadoEm" | "atualizadoEm" | "versao"> & {
  versao?: number | null;
};

export interface Estado {
  sigla: string;
  nome: string;
}

export interface RegistroAcesso {
  tipo: TipoAcesso;
  matricula: string;
  nome: string;
  horario: string;
}

export interface Acesso {
  id: number;
  matricula: string;
  nome: string;
  entrada: string;
  saida: string | null;
  duracaoMinutos: number | null;
}

export interface Painel {
  alunosPorSituacao: Record<Situacao, number>;
  presentesAgora: number;
  entradasHoje: number;
}

export interface Token {
  accessToken: string;
  expiresAt: string;
  nome: string;
  papel: Papel;
  /** Nome da academia do usuário (o sistema é multi-academia). */
  academia: string;
  /** Username digitado no login (guardado no cliente para identificar "você" na tela de equipe). */
  username?: string;
}

/** RFC 9457 Problem Details, com `errors` por campo nas validações. */
export interface Problem {
  title?: string;
  detail?: string;
  status: number;
  errors?: Record<string, string>;
}

export interface Usuario {
  id: number;
  username: string;
  nome: string;
  papel: Papel;
  ativo: boolean;
  criadoEm: string;
}
