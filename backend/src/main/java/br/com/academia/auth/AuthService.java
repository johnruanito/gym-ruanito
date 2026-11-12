package br.com.academia.auth;

import br.com.academia.academia.AcademiaRepository;
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

@Service
@Transactional(readOnly = true)
public class AuthService {

    public record Token(String accessToken, Instant expiresAt, String nome, Papel papel, String academia) {
    }

    /**
     * Hash fixo usado quando o usuário não existe: a verificação BCrypt roda do mesmo jeito,
     * evitando que o tempo de resposta revele quais usernames existem.
     */
    private final String hashFicticio;

    private final UsuarioRepository usuarios;
    private final AcademiaRepository academias;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final AcademiaProperties.Jwt jwtProps;
    private final Clock clock;

    AuthService(UsuarioRepository usuarios, AcademiaRepository academias, PasswordEncoder encoder,
                JwtEncoder jwtEncoder, AcademiaProperties props, Clock clock) {
        this.usuarios = usuarios;
        this.academias = academias;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProps = props.jwt();
        this.clock = clock;
        this.hashFicticio = encoder.encode(UUID.randomUUID().toString());
    }

    /** Nome de exibição do usuário: buscado no banco, já que não viaja mais no token. */
    public String nomeDe(String username) {
        return usuarios.findByUsernameIgnoreCase(username).map(Usuario::getNome).orElse(username);
    }

    public Token autenticar(String username, String senha) {
        var usuario = usuarios.findByUsernameIgnoreCase(username.strip()).filter(Usuario::isAtivo);
        boolean senhaOk = encoder.matches(senha, usuario.map(Usuario::getSenhaHash).orElse(hashFicticio));
        if (usuario.isEmpty() || !senhaOk) {
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }
        var academia = academias.findById(usuario.get().getAcademiaId())
                .filter(a -> a.isAtiva())
                .orElseThrow(() -> new BadCredentialsException("Academia inativa. Entre em contato com o suporte."));
        return emitir(usuario.get(), academia.getNome());
    }

    /**
     * O payload do JWT é só Base64, legível por qualquer um que capture o token (logs de proxy,
     * ferramentas de monitoramento). Por isso ele leva apenas o necessário para autorizar:
     * quem é (sub), o que pode (roles) e de qual academia (academia). Nada de nome, CPF ou e-mail.
     */
    private Token emitir(Usuario u, String nomeAcademia) {
        Instant agora = clock.instant();
        Instant expira = agora.plus(jwtProps.ttl());
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
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(token, expira, u.getNome(), u.getPapel(), nomeAcademia);
    }
}
