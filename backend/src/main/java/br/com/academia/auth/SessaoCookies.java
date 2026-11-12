package br.com.academia.auth;

import br.com.academia.shared.config.AcademiaProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Os dois cookies da sessão. Atributos e por quê:
 * <ul>
 *   <li>{@code HttpOnly}: JavaScript não lê. Uma falha de XSS não consegue roubar a sessão.</li>
 *   <li>{@code Secure}: só trafega em HTTPS.</li>
 *   <li>{@code SameSite=Strict}: o navegador não envia o cookie em requisições vindas de outro site.
 *       É a proteção contra CSRF (um site malicioso disparando ações com a sua sessão).</li>
 *   <li>{@code Path}: o refresh só é enviado para /api/auth (login/refresh/logout), nunca para o resto
 *       da API. Menos lugares por onde ele passa, menos chance de vazar em log.</li>
 * </ul>
 */
@Component
public class SessaoCookies {

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";

    private static final String PATH_ACCESS = "/api";
    private static final String PATH_REFRESH = "/api/auth";

    private final boolean seguro;
    private final Clock clock;

    SessaoCookies(AcademiaProperties props, Clock clock) {
        this.seguro = props.sessao().cookieSeguro();
        this.clock = clock;
    }

    ResponseCookie access(String token, Instant expiraEm) {
        return cookie(ACCESS, token, PATH_ACCESS, ate(expiraEm));
    }

    ResponseCookie refresh(String token, Instant expiraEm) {
        return cookie(REFRESH, token, PATH_REFRESH, ate(expiraEm));
    }

    /** Max-Age=0 manda o navegador apagar. Precisa ter o mesmo nome E o mesmo Path do original. */
    ResponseCookie[] apagar() {
        return new ResponseCookie[]{
                cookie(ACCESS, "", PATH_ACCESS, Duration.ZERO),
                cookie(REFRESH, "", PATH_REFRESH, Duration.ZERO)};
    }

    private Duration ate(Instant expiraEm) {
        Duration d = Duration.between(clock.instant(), expiraEm);
        return d.isNegative() ? Duration.ZERO : d;
    }

    private ResponseCookie cookie(String nome, String valor, String path, Duration maxAge) {
        return ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(seguro)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
