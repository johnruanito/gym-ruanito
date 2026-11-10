package br.com.academia.aluno;

public enum Situacao {
    ATIVO,
    INATIVO,
    PENDENTE;

    /** Somente alunos ativos podem registrar entrada na academia. */
    public boolean permiteEntrada() {
        return this == ATIVO;
    }
}
