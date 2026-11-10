package br.com.academia.aluno;

/**
 * Regras de CPF. No sistema antigo o CPF era um {@code Integer} — 11 dígitos estouram o int,
 * então qualquer CPF acima de 2.147.483.647 falhava. Agora é texto normalizado (só dígitos)
 * com validação dos dígitos verificadores.
 */
public final class Cpf {

    private Cpf() {
    }

    /** Remove máscara/pontuação. Retorna null para entrada nula. */
    public static String normalizar(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    public static boolean isValido(String valor) {
        String cpf = normalizar(valor);
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digito(cpf, 9) == cpf.charAt(9) - '0'
                && digito(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digito(String cpf, int posicoes) {
        int soma = 0;
        for (int i = 0; i < posicoes; i++) {
            soma += (cpf.charAt(i) - '0') * (posicoes + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
