package br.com.academia.usuario.web;

import br.com.academia.usuario.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ações do usuário sobre a PRÓPRIA conta. Qualquer papel pode usar.
 * O "quem" vem do token ({@code sub}), nunca de um id na URL: ninguém troca a senha de outra pessoa por aqui.
 */
@Tag(name = "Minha conta")
@RestController
@RequestMapping("/api/conta")
class MinhaContaController {

    record TrocarSenhaRequest(@NotBlank String senhaAtual, @NotBlank String novaSenha) {
    }

    private final UsuarioService service;

    MinhaContaController(UsuarioService service) {
        this.service = service;
    }

    @Operation(summary = "Troca a própria senha (exige a senha atual)")
    @PutMapping("/senha")
    ResponseEntity<Void> trocarSenha(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TrocarSenhaRequest req) {
        service.trocarMinhaSenha(jwt.getSubject(), req.senhaAtual(), req.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
