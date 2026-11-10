package br.com.academia.aluno;

import java.time.Year;

/**
 * Formato da matrícula preservado do sistema antigo: ano com 4 dígitos + sequencial com 4 dígitos
 * (ex.: 20260042). Se um ano passar de 9999 cadastros, o sequencial cresce em vez de quebrar.
 */
public final class Matricula {

    private Matricula() {
    }

    public static String formatar(Year ano, int sequencial) {
        if (sequencial < 1) {
            throw new IllegalArgumentException("Sequencial deve ser positivo: " + sequencial);
        }
        return "%d%04d".formatted(ano.getValue(), sequencial);
    }
}
