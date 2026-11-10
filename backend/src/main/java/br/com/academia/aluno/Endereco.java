package br.com.academia.aluno;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Endereço do aluno. CEP e número agora são texto: o CEP perdia zeros à esquerda como Integer
 * (ex.: 01310-100) e o número de imóvel pode ter letras ("120A", "S/N").
 */
@Embeddable
public record Endereco(
        @Column(name = "cep", nullable = false, length = 8) String cep,
        @Column(name = "logradouro", nullable = false, length = 128) String logradouro,
        @Column(name = "numero", length = 10) String numero,
        @Column(name = "complemento", length = 64) String complemento,
        @Column(name = "bairro", nullable = false, length = 128) String bairro,
        @Column(name = "cidade", nullable = false, length = 64) String cidade,
        @Column(name = "estado_sigla", nullable = false, length = 2) String estado
) {
}
