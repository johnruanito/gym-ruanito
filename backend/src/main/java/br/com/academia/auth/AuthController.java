package br.com.academia.auth;

import br.com.academia.auth.sessao.SessaoService.SessaoInvalidaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Sessão via cookies HttpOnly. O corpo das respostas NUNCA contém token: só o perfil e quando o
 * access token expira (para o frontend saber, se quiser, renovar antes).
 */
@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record SessaoResponse(AuthService.Perfil perfil, Instant accessExpiraEm, Instant sessaoExpiraEm) {
    }

    private final AuthService service;
    private final SessaoCookies cookies;

    AuthController(AuthService service, SessaoCookies cookies) {
        this.service = service;
        this.cookies = cookies;
    }

    @Operation(summary = "Autentica e grava a sessão em cookies HttpOnly")
    @PostMapping("/login")
    ResponseEntity<SessaoResponse> login(@Valid @RequestBody LoginRequest request,
                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return comCookies(service.autenticar(request.username(), request.password(), userAgent));
    }

    @Operation(summary = "Troca o refresh token (cookie) por um novo par de tokens")
    @PostMapping("/refresh")
    ResponseEntity<SessaoResponse> refresh(@CookieValue(name = SessaoCookies.REFRESH, required = false) String refresh,
                                           @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return comCookies(service.renovar(refresh, userAgent));
    }

    @Operation(summary = "Encerra a sessão no servidor e apaga os cookies")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = SessaoCookies.REFRESH, required = false) String refresh) {
        service.encerrar(refresh);
        return semCookies(ResponseEntity.status(HttpStatus.NO_CONTENT)).build();
    }

    @GetMapping("/me")
    AuthService.Perfil me(@AuthenticationPrincipal Jwt jwt) {
        return service.perfil(jwt.getSubject());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail credenciaisInvalidas(BadCredentialsException ex) {
        return naoAutorizado(ex.getMessage());
    }

    /** Sessão inválida: além do 401, apaga os cookies para o navegador não insistir com eles. */
    @ExceptionHandler(SessaoInvalidaException.class)
    ResponseEntity<ProblemDetail> sessaoInvalida(SessaoInvalidaException ex) {
        return semCookies(ResponseEntity.status(HttpStatus.UNAUTHORIZED)).body(naoAutorizado(ex.getMessage()));
    }

    private ResponseEntity<SessaoResponse> comCookies(AuthService.Tokens t) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.access(t.accessToken(), t.accessExpiraEm()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh(t.refreshToken(), t.refreshExpiraEm()).toString())
                // A resposta carrega credenciais: nenhum proxy ou cache pode guardar
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new SessaoResponse(t.perfil(), t.accessExpiraEm(), t.refreshExpiraEm()));
    }

    private ResponseEntity.BodyBuilder semCookies(ResponseEntity.BodyBuilder builder) {
        for (ResponseCookie c : cookies.apagar()) {
            builder.header(HttpHeaders.SET_COOKIE, c.toString());
        }
        return builder;
    }

    private static ProblemDetail naoAutorizado(String detalhe) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detalhe);
        pd.setTitle("Não autorizado");
        return pd;
    }
}
