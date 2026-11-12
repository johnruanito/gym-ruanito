package br.com.academia.usuario;

import br.com.academia.shared.error.RegraNegocioException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Regras mínimas de senha, aplicadas em todo lugar que define uma senha (cadastro, redefinição, troca).
 */
public final class SenhaPolicy {

    public static final int MINIMO = 8;
    /** O BCrypt só considera os primeiros 72 BYTES; o resto seria ignorado silenciosamente. */
    public static final int MAXIMO_BYTES = 72;

    private SenhaPolicy() {
    }

    public static void validar(String senha, String username) {
        if (senha == null || senha.length() < MINIMO) {
            throw new RegraNegocioException("A senha deve ter pelo menos " + MINIMO + " caracteres");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            throw new RegraNegocioException("A senha deve ter no máximo " + MAXIMO_BYTES + " bytes");
        }
        if (username != null && senha.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw new RegraNegocioException("A senha não pode conter o nome de usuário");
        }
    }
}
