package br.com.academia.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sem JWT_SECRET a aplicação não pode subir: não existe mais valor padrão que alguém
 * pudesse copiar do repositório para forjar tokens.
 * <p>
 * O ApplicationContextRunner sobe só um pedaço do Spring (aqui, apenas as propriedades),
 * então o teste roda em milissegundos, sem banco e sem Docker.
 */
class JwtSecretObrigatorioTest {

    @EnableConfigurationProperties(AcademiaProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class)
            .withPropertyValues(
                    "academia.zone=America/Belem",
                    "academia.acesso.permanencia-maxima=12h",
                    "academia.jwt.ttl=8h",
                    "academia.jwt.issuer=academia-api");

    @Test
    void naoSobeSemSegredo() {
        runner.withPropertyValues("academia.jwt.secret=").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(stackTrace(ctx.getStartupFailure())).contains("JWT_SECRET");
        });
    }

    @Test
    void sobeComSegredoInformado() {
        runner.withPropertyValues("academia.jwt.secret=um-segredo-qualquer-com-mais-de-32-bytes")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    private static String stackTrace(Throwable t) {
        var out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
