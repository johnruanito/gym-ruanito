package br.com.academia.aluno;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Celular(
        @Column(name = "celular_ddd", nullable = false, length = 2) String ddd,
        @Column(name = "celular_numero", nullable = false, length = 9) String numero
) {
}
