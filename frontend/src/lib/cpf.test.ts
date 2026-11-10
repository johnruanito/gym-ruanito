import { describe, expect, it } from "vitest";
import { cpfValido } from "./cpf";

describe("cpfValido", () => {
  it.each(["52998224725", "529.982.247-25", "11144477735", "98765432100"])("aceita %s", (cpf) => {
    expect(cpfValido(cpf)).toBe(true);
  });

  it.each(["52998224724", "11111111111", "1234567890", "123456789012", ""])("rejeita '%s'", (cpf) => {
    expect(cpfValido(cpf)).toBe(false);
  });
});
