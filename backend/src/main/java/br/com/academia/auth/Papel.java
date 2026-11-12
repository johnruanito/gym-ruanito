package br.com.academia.auth;

/**
 * O que cada pessoa da equipe pode fazer. As regras ficam nos controllers via {@code @PreAuthorize}.
 */
public enum Papel {
    /** Dono/gestor: tudo, inclusive gerenciar a equipe e excluir alunos. */
    ADMIN,
    /** Recepção: cadastra e edita alunos, registra entrada/saída, vê relatórios. */
    RECEPCAO,
    /** Instrutor: só consulta (alunos, presentes, relatórios). Não altera cadastro nem catraca. */
    INSTRUTOR
}
