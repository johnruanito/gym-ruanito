package br.com.academia.acesso;

import br.com.academia.aluno.Aluno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Uma visita do aluno: entrada obrigatória, saída preenchida no segundo registro.
 * Horários em {@link Instant} (TIMESTAMPTZ): sem ambiguidade de fuso/horário de verão.
 */
@Entity
@Table(name = "acesso")
public class Acesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_matricula", nullable = false, updatable = false)
    private Aluno aluno;

    @Column(name = "entrada", nullable = false, updatable = false)
    private Instant entrada;

    @Column(name = "saida")
    private Instant saida;

    protected Acesso() {
    }

    private Acesso(Aluno aluno, Instant entrada) {
        this.aluno = Objects.requireNonNull(aluno);
        this.entrada = Objects.requireNonNull(entrada);
    }

    public static Acesso abrir(Aluno aluno, Instant agora) {
        return new Acesso(aluno, agora);
    }

    public boolean aberto() {
        return saida == null;
    }

    /**
     * Entrada sem saída há mais tempo que o limite (aluno esqueceu de registrar a saída).
     * No sistema antigo ela ficava aberta para sempre e o próximo registro — dias depois — virava "saída".
     */
    public boolean abandonado(Instant agora, Duration permanenciaMaxima) {
        return aberto() && entrada.plus(permanenciaMaxima).isBefore(agora);
    }

    public void fechar(Instant agora) {
        if (!aberto()) {
            throw new IllegalStateException("Acesso " + id + " já possui saída registrada");
        }
        if (agora.isBefore(entrada)) {
            throw new IllegalArgumentException("Saída anterior à entrada");
        }
        this.saida = agora;
    }

    /** Duração da visita, ou null se ainda não houve saída. */
    public Duration duracao() {
        return saida == null ? null : Duration.between(entrada, saida);
    }

    public Long getId() { return id; }
    public Aluno getAluno() { return aluno; }
    public Instant getEntrada() { return entrada; }
    public Instant getSaida() { return saida; }
}
