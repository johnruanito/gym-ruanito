package br.com.academia.aluno;

import java.time.LocalDate;

/**
 * Dados editáveis de um aluno, já validados na borda (API). Mantém a entidade livre de DTOs web.
 */
public record DadosAluno(
        String nome,
        Sexo sexo,
        String rg,
        String cpf,
        LocalDate dataNascimento,
        Situacao situacao,
        String email,
        Endereco endereco,
        Celular celular
) {
}
