package br.com.academia.aluno;

import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatriculaTest {

    @Test
    void formatoAnoMaisSequencialComQuatroDigitos() {
        assertThat(Matricula.formatar(Year.of(2026), 1)).isEqualTo("20260001");
        assertThat(Matricula.formatar(Year.of(2026), 42)).isEqualTo("20260042");
    }

    @Test
    void sequencialAcimaDe9999CresceEmVezDeQuebrar() {
        assertThat(Matricula.formatar(Year.of(2026), 10000)).isEqualTo("202610000");
    }

    @Test
    void rejeitaSequencialNaoPositivo() {
        assertThatThrownBy(() -> Matricula.formatar(Year.of(2026), 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
