package br.com.academia.usuario.web;

import br.com.academia.auth.Papel;
import br.com.academia.auth.Usuario;
import br.com.academia.usuario.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.List;

/**
 * Equipe da academia. A anotação na CLASSE vale para todos os métodos: só o dono (ADMIN) gerencia.
 */
@Tag(name = "Equipe")
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
class UsuarioController {

    record NovoUsuarioRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}",
                    message = "Use 3 a 50 caracteres: letras, números, ponto, hífen ou sublinhado") String username,
            @NotBlank @Size(max = 100) String nome,
            @NotNull Papel papel,
            @NotBlank String senha) {
    }

    record AtualizarUsuarioRequest(@NotBlank @Size(max = 100) String nome, @NotNull Papel papel) {
    }

    record RedefinirSenhaRequest(@NotBlank String novaSenha) {
    }

    /** Nunca devolve o hash da senha: a resposta tem só o que a tela precisa. */
    record UsuarioResponse(Long id, String username, String nome, Papel papel, boolean ativo, Instant criadoEm) {
        static UsuarioResponse of(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getUsername(), u.getNome(), u.getPapel(), u.isAtivo(), u.getCriadoEm());
        }
    }

    private final UsuarioService service;

    UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    List<UsuarioResponse> listar() {
        return service.listar().stream().map(UsuarioResponse::of).toList();
    }

    @Operation(summary = "Cadastra um membro da equipe com senha provisória")
    @PostMapping
    ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody NovoUsuarioRequest req) {
        var usuario = service.criar(new UsuarioService.NovoUsuario(req.username(), req.nome(), req.papel(), req.senha()));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(usuario.getId()).toUri();
        return ResponseEntity.created(location).body(UsuarioResponse.of(usuario));
    }

    @PutMapping("/{id}")
    UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarUsuarioRequest req) {
        return UsuarioResponse.of(service.atualizar(id, req.nome(), req.papel()));
    }

    @Operation(summary = "Bloqueia o login do usuário (o histórico é preservado)")
    @PostMapping("/{id}/desativar")
    UsuarioResponse desativar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.of(service.desativar(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/reativar")
    UsuarioResponse reativar(@PathVariable Long id) {
        return UsuarioResponse.of(service.reativar(id));
    }

    @Operation(summary = "Define uma senha provisória (ex.: funcionário esqueceu a senha)")
    @PostMapping("/{id}/redefinir-senha")
    ResponseEntity<Void> redefinirSenha(@PathVariable Long id, @Valid @RequestBody RedefinirSenhaRequest req) {
        service.redefinirSenha(id, req.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
