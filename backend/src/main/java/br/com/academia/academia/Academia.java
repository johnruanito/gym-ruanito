package br.com.academia.academia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Cliente do SaaS: cada academia que assina o sistema.
 * Não tem {@code @TenantId}: é a própria definição do tenant.
 */
@Entity
@Table(name = "academia")
public class Academia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Academia() {
    }

    public Academia(String nome) {
        this.nome = nome;
    }

    @PrePersist
    void prePersist() {
        criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtiva() { return ativa; }
    public Instant getCriadoEm() { return criadoEm; }
}
