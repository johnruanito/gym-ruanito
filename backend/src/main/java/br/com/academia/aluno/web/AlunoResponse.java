package br.com.academia.aluno.web;

import br.com.academia.aluno.Aluno;
import br.com.academia.aluno.Sexo;
import br.com.academia.aluno.Situacao;

import java.time.Instant;
import java.time.LocalDate;

public record AlunoResponse(
        String matricula,
        String nome,
        Sexo sexo,
        String rg,
        String cpf,
        LocalDate dataNascimento,
        Situacao situacao,
        String email,
        AlunoRequest.EnderecoDto endereco,
        AlunoRequest.CelularDto celular,
        Instant criadoEm,
        Instant atualizadoEm,
        long versao
) {

    public static AlunoResponse of(Aluno a) {
        var e = a.getEndereco();
        var c = a.getCelular();
        return new AlunoResponse(
                a.getMatricula(), a.getNome(), a.getSexo(), a.getRg(), a.getCpf(), a.getDataNascimento(),
                a.getSituacao(), a.getEmail(),
                new AlunoRequest.EnderecoDto(e.cep(), e.logradouro(), e.numero(), e.complemento(), e.bairro(), e.cidade(), e.estado()),
                new AlunoRequest.CelularDto(c.ddd(), c.numero()),
                a.getCriadoEm(), a.getAtualizadoEm(), a.getVersao());
    }
}
