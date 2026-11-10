import { describe, expect, it } from "vitest";
import { fmtCpf, fmtDuracao, isoDateLocal } from "./format";

describe("formatadores", () => {
  it("formata duração em horas e minutos", () => {
    expect(fmtDuracao(95)).toBe("01h35");
    expect(fmtDuracao(0)).toBe("00h00");
    expect(fmtDuracao(null)).toBe("—");
  });

  it("aplica máscara de CPF", () => {
    expect(fmtCpf("52998224725")).toBe("529.982.247-25");
  });

  it("gera data local no formato do input date", () => {
    expect(isoDateLocal(new Date(2026, 10, 8, 23, 30))).toBe("2026-11-08");
  });
});
