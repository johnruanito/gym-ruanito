package br.com.academia.usuario;

import br.com.academia.shared.error.RegraNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SenhaPolicyTest {

    @Test
    void aceitaSenhaAdequada() {
        assertThatCode(() -> SenhaPolicy.validar("Recepcao#2026", "maria")).doesNotThrowAnyException();
    }

    @Test
    void rejeitaSenhaCurta() {
        assertThatThrownBy(() -> SenhaPolicy.validar("1234567", "maria"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("8");
    }

    @Test
    void rejeitaSenhaQueContemOUsername() {
        assertThatThrownBy(() -> SenhaPolicy.validar("MARIA2026!", "maria"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("nome de usuário");
    }

    @Test
    void rejeitaSenhaAcimaDe72Bytes() {
        // 40 caracteres acentuados = 80 bytes em UTF-8: o BCrypt ignoraria o final
        assertThatThrownBy(() -> SenhaPolicy.validar("é".repeat(40), "maria"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("72");
    }
}
