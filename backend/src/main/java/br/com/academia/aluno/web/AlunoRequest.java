package br.com.academia.aluno.web;

import br.com.academia.aluno.Celular;
import br.com.academia.aluno.DadosAluno;
import br.com.academia.aluno.Endereco;
import br.com.academia.aluno.Sexo;
import br.com.academia.aluno.Situacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AlunoRequest(
        @NotBlank @Size(min = 3, max = 100) String nome,
        @NotNull Sexo sexo,
        @NotBlank @Pattern(regexp = "[0-9A-Za-z.\\-]{5,20}", message = "RG inválido") String rg,
        @NotBlank @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF deve ter 11 dígitos") String cpf,
        @NotNull @Past LocalDate dataNascimento,
        @NotNull Situacao situacao,
        @Email @Size(max = 120) String email,
        @NotNull @Valid EnderecoDto endereco,
        @NotNull @Valid CelularDto celular,
        /* Obrigatório apenas na edição: versão lida pelo cliente, para detectar edição concorrente. */
        Long versao
) {

    public record EnderecoDto(
            @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve ter 8 dígitos") String cep,
            @NotBlank @Size(min = 3, max = 128) String logradouro,
            @Size(max = 10) String numero,
            @Size(max = 64) String complemento,
            @NotBlank @Size(max = 128) String bairro,
            @NotBlank @Size(max = 64) String cidade,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "UF deve ter 2 letras") String estado
    ) {
    }

    public record CelularDto(
            @NotBlank @Pattern(regexp = "\\d{2}", message = "DDD deve ter 2 dígitos") String ddd,
            @NotBlank @Pattern(regexp = "\\d{4,5}-?\\d{4}", message = "Número de celular inválido") String numero
    ) {
    }

    public DadosAluno toDados() {
        return new DadosAluno(
                nome,
                sexo,
                rg.strip(),
                cpf,
                dataNascimento,
                situacao,
                email == null || email.isBlank() ? null : email.strip().toLowerCase(),
                new Endereco(
                        endereco.cep().replaceAll("\\D", ""),
                        endereco.logradouro().strip(),
                        blankToNull(endereco.numero()),
                        blankToNull(endereco.complemento()),
                        endereco.bairro().strip(),
                        endereco.cidade().strip(),
                        endereco.estado().toUpperCase()),
                new Celular(celular.ddd(), celular.numero().replaceAll("\\D", "")));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
