import { expect, test } from "@playwright/test";
import { login, mockApi } from "./mock-api";

test("recarregar a página mantém a sessão (cookie), sem token no navegador", async ({ page }) => {
  await mockApi(page);
  await login(page);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Painel" })).toBeVisible();

  // Nada de token em sessionStorage/localStorage: a sessão vive só em cookie HttpOnly
  const armazenado = await page.evaluate(() => JSON.stringify({ ...sessionStorage, ...localStorage }));
  expect(armazenado).not.toMatch(/eyJ|token/i);
});

test("access token vencido é renovado sem o usuário perceber", async ({ page }) => {
  const state = await mockApi(page);
  await login(page);

  state.sessao!.accessValido = false; // passaram-se 15 minutos
  await page.getByRole("link", { name: "Alunos" }).click();

  await expect(page.getByRole("heading", { name: "Alunos" })).toBeVisible();
  expect(state.renovacoes).toBeGreaterThanOrEqual(1);
  await expect(page).toHaveURL(/\/alunos$/);
});

test("sessão encerrada no servidor (ex.: usuário desativado) leva ao login", async ({ page }) => {
  const state = await mockApi(page);
  await login(page);

  state.sessao!.accessValido = false;
  state.sessao!.refreshValido = false; // admin desativou, ou a sessão de 12h acabou
  await page.getByRole("link", { name: "Alunos" }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText("Sua sessão foi encerrada")).toBeVisible();
});

test("primeira visita vai ao login sem aviso de sessão encerrada", async ({ page }) => {
  await mockApi(page);
  await page.goto("/");
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText("Sua sessão foi encerrada")).toHaveCount(0);
});

test("sair encerra a sessão no servidor", async ({ page }) => {
  const state = await mockApi(page);
  await login(page);
  await page.getByRole("button", { name: "Sair" }).click();

  await expect(page).toHaveURL(/\/login$/);
  expect(state.sessao).toBeNull();
  await page.goto("/alunos");
  await expect(page).toHaveURL(/\/login$/);
});
