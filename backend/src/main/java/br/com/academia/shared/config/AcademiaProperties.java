package br.com.academia.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Configurações de negócio externalizadas (application.yml / variáveis de ambiente).
 */
@Validated
@ConfigurationProperties(prefix = "academia")
public record AcademiaProperties(
        @NotNull ZoneId zone,
        // @Valid: sem ele o Spring não valida as anotações DENTRO dos records aninhados
        @NotNull @Valid Acesso acesso,
        @NotNull @Valid Jwt jwt,
        Admin admin
) {

    /**
     * @param permanenciaMaxima tempo após o qual uma entrada sem saída é considerada abandonada:
     *                          o próximo registro do aluno abre uma nova entrada em vez de fechar a antiga.
     */
    public record Acesso(@NotNull Duration permanenciaMaxima) {
    }

    /**
     * @param secret chave HMAC (mínimo 32 bytes) usada para assinar os tokens. Vem SEMPRE da
     *               variável de ambiente JWT_SECRET; quem a conhece consegue emitir token de qualquer academia.
     * @param ttl    validade do token de acesso.
     */
    public record Jwt(@NotBlank(message = "defina a variável de ambiente JWT_SECRET") String secret, @NotNull Duration ttl, @NotBlank String issuer) {
    }

    /**
     * Usuário administrador criado no primeiro start, apenas se não existir nenhum usuário.
     */
    public record Admin(String username, String password, String nome) {
    }
}
