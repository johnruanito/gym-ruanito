package br.com.academia.auth;

import br.com.academia.academia.Academia;
import br.com.academia.academia.AcademiaRepository;
import br.com.academia.auth.sessao.SessaoService;
import br.com.academia.auth.sessao.SessaoService.SessaoInvalidaException;
import br.com.academia.shared.config.AcademiaProperties;
import br.com.academia.shared.tenant.TenantContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Login, renovação e logout. O resultado ({@link Tokens}) vira dois cookies HttpOnly no controller:
 * nenhum token passa pelo JavaScript do navegador.
 */
@Service
public class AuthService {

    /** O que a tela precisa saber sobre quem está logado (sem nenhum token). */
    public record Perfil(String username, String nome, Papel papel, String academia) {
    }

    /** Access token (JWT curto) + refresh token (opaco, rotativo) + perfil. */
    public record Tokens(String accessToken, Instant accessExpiraEm,
                         String refreshToken, Instant refreshExpiraEm, Perfil perfil) {
    }

    /**
     * Hash fixo usado quando o usuário não existe: a verificação BCrypt roda do mesmo jeito,
     * evitando que o tempo de resposta revele quais usernames existem.
     */
    private final String hashFicticio;

    private final UsuarioRepository usuarios;
    private final AcademiaRepository academias;
    private final SessaoService sessoes;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final AcademiaProperties.Jwt jwtProps;
    private final Clock clock;

    AuthService(UsuarioRepository usuarios, AcademiaRepository academias, SessaoService sessoes,
                PasswordEncoder encoder, JwtEncoder jwtEncoder, AcademiaProperties props, Clock clock) {
        this.usuarios = usuarios;
        this.academias = academias;
        this.sessoes = sessoes;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProps = props.jwt();
        this.clock = clock;
        this.hashFicticio = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public Tokens autenticar(String username, String senha, String userAgent) {
        var usuario = usuarios.findByUsernameIgnoreCase(username.strip()).filter(Usuario::isAtivo);
        boolean senhaOk = encoder.matches(senha, usuario.map(Usuario::getSenhaHash).orElse(hashFicticio));
        if (usuario.isEmpty() || !senhaOk) {
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }
        Academia academia = academiaAtiva(usuario.get())
                .orElseThrow(() -> new BadCredentialsException("Academia inativa. Entre em contato com o suporte."));
        var refresh = sessoes.iniciar(usuario.get().getId(), userAgent);
        return montar(usuario.get(), academia, refresh);
    }

    /**
     * Troca o refresh token por um novo par de tokens. É aqui que desativação de usuário e suspensão
     * de academia passam a valer: no máximo até o access token atual (15 min) expirar.
     */
    @Transactional(noRollbackFor = SessaoInvalidaException.class)
    public Tokens renovar(String refreshToken, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SessaoInvalidaException("Sessão ausente");
        }
        var refresh = sessoes.renovar(refreshToken, userAgent);
        var usuario = usuarios.findById(refresh.usuarioId()).filter(Usuario::isAtivo);
        var academia = usuario.flatMap(this::academiaAtiva);
        if (usuario.isEmpty() || academia.isEmpty()) {
            sessoes.revogarTodas(refresh.usuarioId());
            throw new SessaoInvalidaException("Acesso desativado. Fale com o administrador.");
        }
        return montar(usuario.get(), academia.get(), refresh);
    }

    @Transactional
    public void encerrar(String refreshToken) {
        sessoes.encerrar(refreshToken);
    }

    @Transactional(readOnly = true)
    public Perfil perfil(String username) {
        var usuario = usuarios.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new SessaoInvalidaException("Usuário não encontrado"));
        String nomeAcademia = academias.findById(usuario.getAcademiaId()).map(Academia::getNome).orElse("");
        return new Perfil(usuario.getUsername(), usuario.getNome(), usuario.getPapel(), nomeAcademia);
    }

    private java.util.Optional<Academia> academiaAtiva(Usuario u) {
        return academias.findById(u.getAcademiaId()).filter(Academia::isAtiva);
    }

    private Tokens montar(Usuario u, Academia academia, SessaoService.Emitida refresh) {
        Instant agora = clock.instant();
        Instant expira = agora.plus(jwtProps.ttl());
        String access = emitirAccessToken(u, agora, expira);
        var perfil = new Perfil(u.getUsername(), u.getNome(), u.getPapel(), academia.getNome());
        return new Tokens(access, expira, refresh.token(), refresh.expiraEm(), perfil);
    }

    /**
     * O payload do JWT é só Base64, legível por qualquer um que capture o token (logs de proxy,
     * ferramentas de monitoramento). Por isso ele leva apenas o necessário para autorizar:
     * quem é (sub), o que pode (roles) e de qual academia (academia). Nada de nome, CPF ou e-mail.
     */
    private String emitirAccessToken(Usuario u, Instant agora, Instant expira) {
        var claims = JwtClaimsSet.builder()
                .issuer(jwtProps.issuer())
                .subject(u.getUsername())
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("roles", List.of(u.getPapel().name()))
                // Daqui em diante, tudo o que este usuário fizer fica restrito a esta academia
                .claim(TenantContext.CLAIM, u.getAcademiaId())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
