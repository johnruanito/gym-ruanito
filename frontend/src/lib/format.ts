const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });
const hora = new Intl.DateTimeFormat("pt-BR", { timeStyle: "short" });

export const fmtDataHora = (iso: string | null) => (iso ? dataHora.format(new Date(iso)) : "—");
export const fmtHora = (iso: string) => hora.format(new Date(iso));

export function fmtDuracao(min: number | null) {
  if (min === null) return "—";
  const h = Math.floor(min / 60);
  const m = min % 60;
  return `${String(h).padStart(2, "0")}h${String(m).padStart(2, "0")}`;
}

export const fmtCpf = (cpf: string) => cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");

/** Data local (YYYY-MM-DD) para inputs type=date. */
export function isoDateLocal(d: Date) {
  const off = d.getTimezoneOffset() * 60_000;
  return new Date(d.getTime() - off).toISOString().slice(0, 10);
}
