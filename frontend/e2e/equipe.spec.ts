import { expect, test } from "@playwright/test";
import { alunoBase, login, mockApi } from "./mock-api";

test("administrador cadastra membro, recebe a senha provisória e desativa", async ({ page }) => {
  await mockApi(page);
  await login(page);
  await page.getByRole("link", { name: "Equipe" }).click();

  await page.getByLabel("Nome", { exact: true }).fill("Rita Recepção");
  await page.getByLabel("Usuário (login)").fill("rita");
  await page.getByRole("button", { name: "Cadastrar" }).click();

  // A senha provisória aparece uma vez, para ser repassada
  const credencial = page.getByRole("status").filter({ hasText: "Repasse ao funcionário" });
  await expect(credencial).toContainText("rita");
  await expect(page.getByRole("row", { name: /Rita Recepção/ })).toBeVisible();

  const linha = page.getByRole("row", { name: /Rita Recepção/ });
  await linha.getByRole("button", { name: "Desativar" }).click();
  await expect(linha.getByText("Desativado")).toBeVisible();
  await expect(linha.getByRole("button", { name: "Reativar" })).toBeVisible();

  // O próprio administrador não tem botão de desativar
  await expect(page.getByRole("row", { name: /Dona Ana/ }).getByRole("button", { name: "Desativar" })).toHaveCount(0);
});

test("instrutor só consulta: sem menu de equipe/catraca e com formulário travado", async ({ page }) => {
  await mockApi(page, { alunos: { "20260001": alunoBase({ matricula: "20260001" }) } });
  await login(page, "instrutor");

  await expect(page.getByRole("link", { name: "Equipe" })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Controle de acesso" })).toHaveCount(0);

  await page.getByRole("link", { name: "Alunos" }).click();
  await expect(page.getByRole("button", { name: "Novo aluno" })).toHaveCount(0);
  await page.getByRole("link", { name: "Ver" }).click();

  await expect(page.getByLabel("Nome")).toBeDisabled();
  await expect(page.getByRole("button", { name: "Salvar" })).toHaveCount(0);
});

test("troca da própria senha confere a confirmação e a senha atual", async ({ page }) => {
  await mockApi(page);
  await login(page);
  await page.getByRole("link", { name: "Minha senha" }).click();

  await page.getByLabel("Senha atual").fill("senha-correta");
  await page.getByLabel("Nova senha", { exact: true }).fill("NovaSenha#2026");
  await page.getByLabel("Confirme a nova senha").fill("Diferente#2026");
  await page.getByRole("button", { name: "Alterar senha" }).click();
  await expect(page.getByRole("alert")).toContainText("confirmação não confere");

  await page.getByLabel("Confirme a nova senha").fill("NovaSenha#2026");
  await page.getByRole("button", { name: "Alterar senha" }).click();
  await expect(page.getByText("Senha alterada")).toBeVisible();
});
