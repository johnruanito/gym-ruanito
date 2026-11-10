package br.com.academia.shared.config;

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
        @NotNull Acesso acesso,
        @NotNull Jwt jwt,
        Admin admin
) {

    /**
     * @param permanenciaMaxima tempo após o qual uma entrada sem saída é considerada abandonada:
     *                          o próximo registro do aluno abre uma nova entrada em vez de fechar a antiga.
     */
    public record Acesso(@NotNull Duration permanenciaMaxima) {
    }

    /**
     * @param secret chave HMAC (mínimo 32 bytes) usada para assinar os tokens.
     * @param ttl    validade do token de acesso.
     */
    public record Jwt(@NotBlank String secret, @NotNull Duration ttl, @NotBlank String issuer) {
    }

    /**
     * Usuário administrador criado no primeiro start, apenas se não existir nenhum usuário.
     */
    public record Admin(String username, String password, String nome) {
    }
}
