package br.com.academia.aluno;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "11144477735", "98765432100"})
    void aceitaCpfsValidos(String cpf) {
        assertThat(Cpf.isValido(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "11111111111", "1234567890", "123456789012", "abc"})
    void rejeitaCpfsInvalidos(String cpf) {
        assertThat(Cpf.isValido(cpf)).isFalse();
    }

    @Test
    void cpfAcimaDoLimiteDoIntNaoEstouraMais() {
        // 987.654.321-00 > Integer.MAX_VALUE: quebrava no sistema antigo (CPF como Integer)
        assertThat(Long.parseLong("98765432100")).isGreaterThan(Integer.MAX_VALUE);
        assertThat(Cpf.isValido("98765432100")).isTrue();
    }

    @Test
    void normalizaRemovendoMascara() {
        assertThat(Cpf.normalizar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(Cpf.normalizar(null)).isNull();
    }
}
