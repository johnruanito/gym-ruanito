package br.com.academia.auth;

import br.com.academia.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record Me(String username, String nome, List<String> roles, Long academia) {
    }

    private final AuthService service;

    AuthController(AuthService service) {
        this.service = service;
    }

    @Operation(summary = "Autentica e devolve um JWT (Bearer)")
    @PostMapping("/login")
    AuthService.Token login(@Valid @RequestBody LoginRequest request) {
        return service.autenticar(request.username(), request.password());
    }

    @GetMapping("/me")
    Me me(@AuthenticationPrincipal Jwt jwt) {
        return new Me(jwt.getSubject(), service.nomeDe(jwt.getSubject()), jwt.getClaimAsStringList("roles"),
                jwt.<Number>getClaim(TenantContext.CLAIM).longValue());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail credenciaisInvalidas(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Não autorizado");
        return pd;
    }
}
