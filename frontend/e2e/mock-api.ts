import type { Page, Route } from "@playwright/test";

/**
 * API em memória interceptada no navegador (page.route). Permite testar os fluxos de UI
 * de forma determinística e sem backend. O contrato real da API é coberto pelo
 * AcademiaApiIT (Testcontainers) no backend.
 */
export interface MockState {
  alunos: Record<string, Record<string, unknown>>;
  acessos: { id: number; matricula: string; nome: string; entrada: string; saida: string | null; duracaoMinutos: number | null }[];
  seq: number;
  usuarios: { id: number; username: string; nome: string; papel: string; ativo: boolean; criadoEm: string; senha: string }[];
  /**
   * Simula os cookies HttpOnly: o mock guarda quem está logado (o navegador não vê token nenhum).
   * accessValido=false simula o access token de 15 min vencido; refreshValido=false, a sessão encerrada.
   */
  sessao: { username: string; accessValido: boolean; refreshValido: boolean } | null;
  renovacoes: number;
}

const json = (route: Route, status: number, body?: unknown) =>
  route.fulfill({ status, contentType: "application/json", body: body === undefined ? "" : JSON.stringify(body) });

const problem = (route: Route, status: number, detail: string, errors?: Record<string, string>) =>
  json(route, status, { status, title: "Erro", detail, errors });

export function alunoBase(over: Record<string, unknown> = {}) {
  return {
    nome: "Ana Paula Santos",
    sexo: "FEMININO",
    rg: "7654321",
    cpf: "98765432100",
    dataNascimento: "1990-05-20",
    situacao: "ATIVO",
    email: "ana@example.com",
    endereco: { cep: "66035000", logradouro: "Av. Nazaré", numero: "100", complemento: null, bairro: "Nazaré", cidade: "Belém", estado: "PA" },
    celular: { ddd: "91", numero: "988887777" },
    criadoEm: new Date().toISOString(),
    atualizadoEm: new Date().toISOString(),
    versao: 0,
    ...over,
  };
}

export async function mockApi(page: Page, seed: Partial<MockState> = {}): Promise<MockState> {
  const state: MockState = {
    alunos: {},
    acessos: [],
    seq: 0,
    usuarios: [
      { id: 1, username: "admin", nome: "Dona Ana", papel: "ADMIN", ativo: true, criadoEm: new Date().toISOString(), senha: "senha-correta" },
      { id: 2, username: "instrutor", nome: "Iago Instrutor", papel: "INSTRUTOR", ativo: true, criadoEm: new Date().toISOString(), senha: "senha-correta" },
    ],
    sessao: null,
    renovacoes: 0,
    ...seed,
  };
  const semSenha = (u: MockState["usuarios"][number]) => {
    const copia: Partial<MockState["usuarios"][number]> = { ...u };
    delete copia.senha;
    return copia;
  };

  // ViaCEP fora do ar nos testes: o formulário precisa continuar funcionando
  await page.route("https://viacep.com.br/**", (r) => r.abort());

  await page.route("**/api/**", async (route) => {
    const req = route.request();
    const url = new URL(req.url());
    const path = url.pathname.replace(/^\/api/, "");
    const method = req.method();
    const body = req.postDataJSON?.() as Record<string, unknown> | undefined;

    const perfil = (username: string) => {
      const u = state.usuarios.find((x) => x.username === username)!;
      return { username: u.username, nome: u.nome, papel: u.papel, academia: "Academia Teste" };
    };
    const respostaSessao = (username: string) => ({
      perfil: perfil(username),
      accessExpiraEm: new Date(Date.now() + 9e5).toISOString(),
      sessaoExpiraEm: new Date(Date.now() + 4.32e7).toISOString(),
    });

    if (method === "POST" && path === "/auth/login") {
      const u = state.usuarios.find((x) => x.username === body?.username && x.ativo && x.senha === body?.password);
      if (!u) return problem(route, 401, "Usuário ou senha inválidos");
      state.sessao = { username: u.username, accessValido: true, refreshValido: true };
      return json(route, 200, respostaSessao(u.username));
    }
    if (method === "POST" && path === "/auth/refresh") {
      if (!state.sessao?.refreshValido) return problem(route, 401, "Sessão expirada");
      state.renovacoes++;
      state.sessao.accessValido = true;
      return json(route, 200, respostaSessao(state.sessao.username));
    }
    if (method === "POST" && path === "/auth/logout") {
      state.sessao = null;
      return route.fulfill({ status: 204 });
    }
    if (!state.sessao?.accessValido) return problem(route, 401, "Não autenticado");
    if (path === "/auth/me") return json(route, 200, perfil(state.sessao.username));

    // Papel de quem está logado: permite simular o 403 do backend
    const papel = perfil(state.sessao.username).papel;

    if (path === "/usuarios" || path.startsWith("/usuarios/")) {
      if (papel !== "ADMIN") return problem(route, 403, "Acesso negado");
      if (method === "GET") return json(route, 200, state.usuarios.map(semSenha));
      if (method === "POST" && path === "/usuarios") {
        if (state.usuarios.some((u) => u.username === body?.username)) return problem(route, 409, "Nome de usuário já está em uso");
        const novo = { id: state.usuarios.length + 1, ativo: true, criadoEm: new Date().toISOString(), username: String(body?.username), nome: String(body?.nome), papel: String(body?.papel), senha: String(body?.senha) };
        state.usuarios.push(novo);
        return json(route, 201, semSenha(novo));
      }
      const m = path.match(/^\/usuarios\/(\d+)\/(desativar|reativar)$/);
      if (m) {
        const u = state.usuarios.find((x) => x.id === Number(m[1]))!;
        u.ativo = m[2] === "reativar";
        return json(route, 200, semSenha(u));
      }
    }

    if (path === "/conta/senha" && method === "PUT") {
      return body?.senhaAtual === "senha-correta"
        ? route.fulfill({ status: 204 })
        : problem(route, 422, "Senha atual incorreta");
    }

    if (path === "/estados") return json(route, 200, [{ sigla: "PA", nome: "Pará" }, { sigla: "SP", nome: "São Paulo" }]);

    if (path === "/painel") {
      const lista = Object.values(state.alunos);
      const count = (s: string) => lista.filter((a) => a.situacao === s).length;
      return json(route, 200, {
        alunosPorSituacao: { ATIVO: count("ATIVO"), INATIVO: count("INATIVO"), PENDENTE: count("PENDENTE") },
        presentesAgora: state.acessos.filter((a) => !a.saida).length,
        entradasHoje: state.acessos.length,
      });
    }

    if (path === "/alunos" && method === "GET") {
      const situacao = url.searchParams.get("situacao");
      const nome = url.searchParams.get("nome")?.toLowerCase();
      const content = Object.values(state.alunos)
        .filter((a) => !situacao || a.situacao === situacao)
        .filter((a) => !nome || String(a.nome).toLowerCase().includes(nome))
        .map((a) => ({ matricula: a.matricula, nome: a.nome, rg: a.rg, email: a.email, celular: "(91) 98888-7777", situacao: a.situacao }));
      return json(route, 200, { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });
    }

    if (path === "/alunos" && method === "POST") {
      if (body?.rg === "duplicado") return problem(route, 409, "Já existe aluno cadastrado com este RG");
      if (String((body?.endereco as Record<string, string>)?.cep).startsWith("00000")) {
        return problem(route, 400, "Um ou mais campos são inválidos.", { "endereco.cep": "CEP inexistente" });
      }
      const matricula = `2026${String(++state.seq).padStart(4, "0")}`;
      state.alunos[matricula] = alunoBase({ ...body, matricula, cpf: String(body?.cpf).replace(/\D/g, "") });
      return json(route, 201, state.alunos[matricula]);
    }

    const alunoMatch = path.match(/^\/alunos\/(\d+)$/);
    if (alunoMatch) {
      const aluno = state.alunos[alunoMatch[1]!];
      if (!aluno) return problem(route, 404, "Aluno não encontrado");
      if (method === "PUT") {
        if (String((body?.endereco as Record<string, string>)?.cep).startsWith("00000")) {
          return problem(route, 400, "Um ou mais campos são inválidos.", { "endereco.cep": "CEP inexistente" });
        }
        Object.assign(aluno, body, { versao: Number(aluno.versao) + 1 });
      }
      return json(route, 200, aluno);
    }

    if (path === "/acessos" && method === "POST") {
      const aluno = Object.values(state.alunos).find((a) => a.matricula === body?.matricula || a.rg === body?.rg);
      if (!aluno) return problem(route, 404, "Aluno não encontrado");
      const aberto = state.acessos.find((a) => a.matricula === aluno.matricula && !a.saida);
      const agora = new Date().toISOString();
      if (aberto) {
        aberto.saida = agora;
        aberto.duracaoMinutos = 0;
        return json(route, 200, { tipo: "SAIDA", matricula: aluno.matricula, nome: aluno.nome, horario: agora });
      }
      if (aluno.situacao !== "ATIVO") return problem(route, 422, `Entrada bloqueada: aluno com situação ${aluno.situacao}`);
      state.acessos.unshift({ id: state.acessos.length + 1, matricula: String(aluno.matricula), nome: String(aluno.nome), entrada: agora, saida: null, duracaoMinutos: null });
      return json(route, 200, { tipo: "ENTRADA", matricula: aluno.matricula, nome: aluno.nome, horario: agora });
    }

    if (path === "/acessos/presentes") return json(route, 200, state.acessos.filter((a) => !a.saida));
    if (path === "/acessos") {
      return json(route, 200, { content: state.acessos, page: 0, size: 50, totalElements: state.acessos.length, totalPages: 1 });
    }

    return problem(route, 404, `Rota não mockada: ${method} ${path}`);
  });

  return state;
}

export async function login(page: Page, username = "admin") {
  await page.goto("/login");
  await page.getByLabel("Usuário").fill(username);
  await page.getByLabel("Senha").fill("senha-correta");
  await page.getByRole("button", { name: "Entrar" }).click();
  await page.getByRole("heading", { name: "Painel" }).waitFor();
}
