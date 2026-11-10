package br.com.academia.acesso;

import br.com.academia.aluno.Aluno;
import br.com.academia.aluno.Celular;
import br.com.academia.aluno.DadosAluno;
import br.com.academia.aluno.Endereco;
import br.com.academia.aluno.Sexo;
import br.com.academia.aluno.Situacao;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcessoTest {

    private static final Instant T0 = Instant.parse("2026-11-08T13:00:00Z");
    private static final Duration LIMITE = Duration.ofHours(12);

    private final Aluno aluno = new Aluno("20260001", new DadosAluno(
            "Maria", Sexo.FEMININO, "1234567", "52998224725", LocalDate.of(1995, 3, 10), Situacao.ATIVO, null,
            new Endereco("66035000", "Av. Nazaré", "100", null, "Nazaré", "Belém", "PA"),
            new Celular("91", "988887777")));

    @Test
    void entradaAbertaEFechadaCalculaDuracao() {
        Acesso a = Acesso.abrir(aluno, T0);
        assertThat(a.aberto()).isTrue();
        assertThat(a.duracao()).isNull();

        a.fechar(T0.plus(Duration.ofMinutes(95)));

        assertThat(a.aberto()).isFalse();
        assertThat(a.duracao()).isEqualTo(Duration.ofMinutes(95));
    }

    @Test
    void entradaAntigaSemSaidaEhConsideradaAbandonada() {
        Acesso a = Acesso.abrir(aluno, T0);
        assertThat(a.abandonado(T0.plus(Duration.ofHours(2)), LIMITE)).isFalse();
        assertThat(a.abandonado(T0.plus(Duration.ofHours(13)), LIMITE)).isTrue();
    }

    @Test
    void naoPermiteFecharDuasVezesNemSaidaAntesDaEntrada() {
        Acesso a = Acesso.abrir(aluno, T0);
        assertThatThrownBy(() -> a.fechar(T0.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        a.fechar(T0.plusSeconds(60));
        assertThatThrownBy(() -> a.fechar(T0.plusSeconds(120))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void somenteAtivoPodeEntrar() {
        assertThat(Situacao.ATIVO.permiteEntrada()).isTrue();
        assertThat(Situacao.INATIVO.permiteEntrada()).isFalse();
        assertThat(Situacao.PENDENTE.permiteEntrada()).isFalse();
    }
}
