package br.com.academia.auth.sessao;

import br.com.academia.shared.config.AcademiaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh tokens: emissão, rotação, revogação e detecção de reuso.
 *
 * <pre>
 * login ──► token A (família F)
 * renovar(A) ──► A revogado, emite B (família F, mesmo prazo final)
 * renovar(B) ──► B revogado, emite C ...
 * renovar(A) de novo ──► A já foi trocado: alguém copiou o token → revoga TODA a família F
 * </pre>
 */
@Service
public class SessaoService {

    /** Refresh token ausente, desconhecido, expirado, revogado ou reutilizado. Vira HTTP 401. */
    public static class SessaoInvalidaException extends RuntimeException {
        public SessaoInvalidaException(String message) {
            super(message);
        }
    }

    public record Emitida(Long usuarioId, String token, Instant expiraEm) {
    }

    private static final Logger log = LoggerFactory.getLogger(SessaoService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SessaoRepository sessoes;
    private final Clock clock;
    private final Duration duracao;
    private final Duration toleranciaReuso;

    SessaoService(SessaoRepository sessoes, Clock clock, AcademiaProperties props) {
        this.sessoes = sessoes;
        this.clock = clock;
        this.duracao = props.sessao().duracao();
        this.toleranciaReuso = props.sessao().toleranciaReuso();
    }

    /** Novo login: nova família, prazo final = agora + duração da sessão. */
    @Transactional
    public Emitida iniciar(Long usuarioId, String userAgent) {
        Instant agora = clock.instant();
        return emitir(usuarioId, UUID.randomUUID(), agora.plus(duracao), userAgent);
    }

    /**
     * Troca um refresh token válido por outro (rotação). O prazo final da família NÃO muda:
     * renovar não permite ficar logado para sempre.
     * <p>
     * {@code noRollbackFor}: quando detectamos reuso, revogamos a família E lançamos a exceção.
     * Sem isso, a exceção desfaria (rollback) justamente a revogação.
     */
    @Transactional(noRollbackFor = SessaoInvalidaException.class)
    public Emitida renovar(String token, String userAgent) {
        Instant agora = clock.instant();
        Sessao atual = sessoes.findByTokenHashForUpdate(hash(token))
                .orElseThrow(() -> new SessaoInvalidaException("Sessão inválida"));

        if (atual.revogada()) {
            if (atual.revogadaHaMenosDe(toleranciaReuso, agora) && !atual.expirada(agora)) {
                // Duas abas renovaram quase juntas: não é roubo, só emite outro token da mesma família.
                return emitir(atual.getUsuarioId(), atual.getFamilia(), atual.getExpiraEm(), userAgent);
            }
            int revogadas = sessoes.revogarFamilia(atual.getFamilia(), agora);
            log.warn("Reuso de refresh token já trocado (usuarioId={}, familia={}): {} sessão(ões) revogada(s)",
                    atual.getUsuarioId(), atual.getFamilia(), revogadas);
            throw new SessaoInvalidaException("Sessão encerrada por segurança. Faça login novamente.");
        }
        if (atual.expirada(agora)) {
            throw new SessaoInvalidaException("Sessão expirada. Faça login novamente.");
        }

        atual.revogar(agora);
        return emitir(atual.getUsuarioId(), atual.getFamilia(), atual.getExpiraEm(), userAgent);
    }

    /** Logout: encerra o login inteiro (a família), não só o token atual. */
    @Transactional
    public void encerrar(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessoes.findByTokenHashForUpdate(hash(token))
                .ifPresent(s -> sessoes.revogarFamilia(s.getFamilia(), clock.instant()));
    }

    /** Derruba todas as sessões do usuário (desativação, troca ou redefinição de senha). */
    @Transactional
    public void revogarTodas(Long usuarioId) {
        sessoes.revogarDoUsuario(usuarioId, clock.instant());
    }

    /** Faxina diária: sessões vencidas há mais de 7 dias não têm mais utilidade nem para auditoria rápida. */
    @Scheduled(cron = "0 17 3 * * *")
    @Transactional
    public void apagarExpiradas() {
        int apagadas = sessoes.apagarExpiradasAntesDe(clock.instant().minus(Duration.ofDays(7)));
        if (apagadas > 0) {
            log.info("{} sessão(ões) expirada(s) removida(s)", apagadas);
        }
    }

    private Emitida emitir(Long usuarioId, UUID familia, Instant expiraEm, String userAgent) {
        String token = gerarToken();
        sessoes.save(new Sessao(usuarioId, familia, hash(token), clock.instant(), expiraEm, userAgent));
        return new Emitida(usuarioId, token, expiraEm);
    }

    /** 256 bits aleatórios do gerador criptográfico: impossível de adivinhar por força bruta. */
    private static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 é suficiente aqui (diferente de senha, que exige BCrypt): o token já tem 256 bits de
     * entropia, então não existe "dicionário" para atacar.
     */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
