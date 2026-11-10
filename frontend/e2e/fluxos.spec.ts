import { expect, test } from "@playwright/test";
import { alunoBase, login, mockApi } from "./mock-api";

test.describe("autenticação", () => {
  test("rota protegida redireciona para o login", async ({ page }) => {
    await mockApi(page);
    await page.goto("/alunos");
    await expect(page).toHaveURL(/\/login$/);
  });

  test("senha errada mostra erro e não entra", async ({ page }) => {
    await mockApi(page);
    await page.goto("/login");
    await page.getByLabel("Usuário").fill("admin");
    await page.getByLabel("Senha").fill("errada");
    await page.getByRole("button", { name: "Entrar" }).click();
    await expect(page.getByRole("alert")).toContainText("Usuário ou senha inválidos");
    await expect(page).toHaveURL(/\/login$/);
  });

  test("após o login volta para a página que pediu", async ({ page }) => {
    await mockApi(page);
    await page.goto("/relatorios/acessos");
    await page.getByLabel("Usuário").fill("admin");
    await page.getByLabel("Senha").fill("senha-correta");
    await page.getByRole("button", { name: "Entrar" }).click();
    await expect(page.getByRole("heading", { name: "Entradas e saídas" })).toBeVisible();
  });
});

test.describe("cadastro de aluno", () => {
  test("valida CPF no cliente e cadastra com matrícula gerada", async ({ page }) => {
    await mockApi(page);
    await login(page);
    await page.goto("/alunos/novo");

    await page.getByLabel("Nome").fill("Bruno Costa Lima");
    await page.getByLabel("Data de nascimento").fill("1992-01-15");
    await page.getByLabel("CPF").fill("111.111.111-11");
    await page.getByLabel("RG").fill("1234567");
    await page.getByLabel("DDD").fill("91");
    await page.getByLabel("Celular").fill("98888-7777");
    await page.getByLabel("CEP").fill("66035-000");
    await page.getByLabel("Logradouro").fill("Av. Nazaré");
    await page.getByLabel("Bairro").fill("Nazaré");
    await page.getByLabel("Cidade").fill("Belém");
    await page.getByLabel("UF").selectOption("PA");
    await page.getByRole("button", { name: "Salvar" }).click();
    await expect(page.getByText("CPF inválido")).toBeVisible();

    await page.getByLabel("CPF").fill("529.982.247-25");
    await page.getByRole("button", { name: "Salvar" }).click();
    await expect(page).toHaveURL(/\/alunos\/20260001$/);
    await expect(page.getByText("Matrícula 20260001")).toBeVisible();
  });

  test("erro de validação do servidor aparece no campo certo", async ({ page }) => {
    await mockApi(page, { alunos: { "20260001": alunoBase({ matricula: "20260001" }) } });
    await login(page);
    await page.goto("/alunos/20260001");
    await page.getByLabel("CEP").fill("00000-000");
    await page.getByRole("button", { name: "Salvar" }).click();
    await expect(page.getByText("CEP inexistente")).toBeVisible();
  });
});

test.describe("controle de acesso", () => {
  test("alterna entrada (matrícula) e saída (RG) e devolve o foco ao campo", async ({ page }) => {
    await mockApi(page, { alunos: { "20260001": alunoBase({ matricula: "20260001" }) } });
    await login(page);
    await page.goto("/acesso");

    const campo = page.getByLabel("Matrícula");
    await campo.fill("20260001");
    await campo.press("Enter");
    await expect(page.getByText("Entrada registrada")).toBeVisible();
    await expect(campo).toBeFocused(); // pronto para o próximo aluno (leitor de código de barras)

    await page.getByRole("tab", { name: "RG" }).click();
    await page.getByLabel("RG").fill("7654321");
    await page.getByLabel("RG").press("Enter");
    await expect(page.getByText("Saída registrada")).toBeVisible();
  });

  test("bloqueia entrada de aluno inativo", async ({ page }) => {
    await mockApi(page, { alunos: { "20260002": alunoBase({ matricula: "20260002", situacao: "INATIVO" }) } });
    await login(page);
    await page.goto("/acesso");
    await page.getByLabel("Matrícula").fill("20260002");
    await page.getByLabel("Matrícula").press("Enter");
    await expect(page.getByRole("alert")).toContainText("Entrada bloqueada");
  });
});

test("pesquisa por situação fica na URL e sobrevive ao recarregar", async ({ page }) => {
  await mockApi(page, {
    alunos: {
      "20260001": alunoBase({ matricula: "20260001", nome: "Ana Ativa" }),
      "20260002": alunoBase({ matricula: "20260002", nome: "Pedro Pendente", situacao: "PENDENTE" }),
    },
  });
  await login(page);
  await page.goto("/alunos");
  await page.getByLabel("Situação").selectOption("PENDENTE");
  await page.getByRole("button", { name: "Pesquisar" }).click();

  await expect(page).toHaveURL(/situacao=PENDENTE/);
  await expect(page.getByRole("cell", { name: "Pedro Pendente" })).toBeVisible();
  await expect(page.getByRole("cell", { name: "Ana Ativa" })).toHaveCount(0);

  await page.reload();
  await expect(page.getByRole("cell", { name: "Pedro Pendente" })).toBeVisible();
});
