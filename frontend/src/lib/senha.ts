/** Senha provisória legível (sem 0/O, 1/l/I), gerada com o gerador criptográfico do navegador. */
export function gerarSenhaProvisoria(tamanho = 12) {
  const alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789#@!";
  const bytes = crypto.getRandomValues(new Uint32Array(tamanho));
  return Array.from(bytes, (b) => alfabeto[b % alfabeto.length]).join("");
}
