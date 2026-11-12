package br.com.academia.auth.sessao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Um refresh token emitido. Guarda só o HASH do token: o valor real existe apenas no cookie do navegador.
 */
@Entity
@Table(name = "sessao")
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "familia", nullable = false, updatable = false)
    private UUID familia;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "user_agent", length = 200)
    private String userAgent;

    protected Sessao() {
    }

    Sessao(Long usuarioId, UUID familia, String tokenHash, Instant agora, Instant expiraEm, String userAgent) {
        this.usuarioId = usuarioId;
        this.familia = familia;
        this.tokenHash = tokenHash;
        this.criadoEm = agora;
        this.expiraEm = expiraEm;
        this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(200, userAgent.length()));
    }

    boolean expirada(Instant agora) {
        return !agora.isBefore(expiraEm);
    }

    boolean revogada() {
        return revogadoEm != null;
    }

    /** Revogada há pouco tempo: provável corrida entre duas abas renovando juntas, não roubo. */
    boolean revogadaHaMenosDe(Duration tolerancia, Instant agora) {
        return revogadoEm != null && revogadoEm.plus(tolerancia).isAfter(agora);
    }

    void revogar(Instant agora) {
        if (revogadoEm == null) {
            revogadoEm = agora;
        }
    }

    Long getUsuarioId() { return usuarioId; }
    UUID getFamilia() { return familia; }
    Instant getExpiraEm() { return expiraEm; }
}
