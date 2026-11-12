package br.com.academia.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import java.util.Arrays;
import java.util.Set;

/**
 * De onde o Spring Security tira o access token de cada requisição:
 * <ol>
 *   <li>cabeçalho {@code Authorization: Bearer ...} — clientes de API, Swagger, testes;</li>
 *   <li>cookie {@code access_token} — o navegador (SPA).</li>
 * </ol>
 * Nas rotas de login/refresh/logout NENHUM token é lido: elas funcionam justamente quando o access token
 * expirou. Se ele fosse validado ali, um token vencido devolveria 401 antes de chegar ao refresh.
 */
class CookieOuHeaderTokenResolver implements BearerTokenResolver {

    private static final Set<String> SEM_TOKEN = Set.of("/api/auth/login", "/api/auth/refresh", "/api/auth/logout");

    private final DefaultBearerTokenResolver header = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (SEM_TOKEN.contains(request.getRequestURI())) {
            return null;
        }
        String doHeader = header.resolve(request);
        if (doHeader != null) {
            return doHeader;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> SessaoCookies.ACCESS.equals(c.getName()) && !c.getValue().isBlank())
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
